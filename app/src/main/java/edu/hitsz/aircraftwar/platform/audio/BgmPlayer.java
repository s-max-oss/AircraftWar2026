package edu.hitsz.aircraftwar.platform.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.hitsz.aircraftwar.R;

/**
 * BGM 播放器：{@code bgm} 与 {@code bgm_boss} 两首互斥循环。
 *
 * <p>迁移说明：替代桌面版 {@code SoundManager} 里的 {@code javax.sound.sampled.Clip}
 * （commit 50474cc）。桌面版用 {@code Clip.loop(LOOP_CONTINUOUSLY)}，MediaPlayer 用
 * {@code setLooping(true)}，语义一致；桌面版按相对路径 {@code src/videos/*.wav} 读文件，
 * 在 APK 里必然失败，这里改走 {@code R.raw}（AGENTS §4 陷阱 3）。
 *
 * <p>为什么加载放到后台线程：两首 WAV 合计 9MB，进游戏时 {@code MediaPlayer.create}
 * 要建提取器 + 准备解码器，实测主线程上会吃掉几百毫秒——点"开始游戏"到画面出现会卡一下。
 * 这里改成后台建好、回主线程再播：调用方放心地在主线程 {@link #playNormal()}，还没准备好就先记账。
 *
 * <p>互斥规则来自 {@code 实验3.pdf} p22 注：「boss 机背景音乐出现时，要暂停游戏背景音乐」。
 * 因此任何时刻最多只有一首在播——这也是"切后台回来不叠音"的根因防线。
 *
 * @author hitsz
 */
public final class BgmPlayer {

    private static final String TAG = "AircraftWar";
    private static final String TRACK_NORMAL = "bgm";
    private static final String TRACK_BOSS = "bgm_boss";

    private final Context appContext;
    private final float volume;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bgm-loader");
        t.setDaemon(true);
        return t;
    });

    private MediaPlayer normalPlayer;
    private MediaPlayer bossPlayer;

    /** 期望播放的曲目；null 表示"不该有 BGM"。真实播放状态由 {@link #applyState()} 推导。 */
    private String desiredTrack;
    private boolean enabled = true;
    private boolean paused;
    private boolean released;

    public BgmPlayer(Context context, float volume) {
        this.appContext = context.getApplicationContext();
        this.volume = volume;
    }

    /** 预热：后台创建两个 MediaPlayer，准备好后回主线程按当前期望状态补播。 */
    public void preloadAsync() {
        loader.execute(() -> {
            MediaPlayer normal = createLooping(R.raw.bgm);
            MediaPlayer boss = createLooping(R.raw.bgm_boss);
            mainHandler.post(() -> {
                if (released) {
                    releaseQuietly(normal);
                    releaseQuietly(boss);
                    return;
                }
                normalPlayer = normal;
                bossPlayer = boss;
                applyState();
            });
        });
    }

    public void playNormal() {
        desiredTrack = TRACK_NORMAL;
        paused = false;
        applyState();
    }

    public void playBoss() {
        desiredTrack = TRACK_BOSS;
        paused = false;
        applyState();
    }

    /** 停止并回到"没有 BGM"的期望状态（游戏结束 / 退出游戏页）。 */
    public void stop() {
        desiredTrack = null;
        applyState();
    }

    /** 切后台：记住"本来在播"，回来时由 {@link #resume()} 恢复，避免重复 start 叠音。 */
    public void pause() {
        paused = true;
        applyState();
    }

    public void resume() {
        paused = false;
        applyState();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        applyState();
    }

    /** 是否有"应该在播"的曲目。恢复播放前据此决定要不要重新申请音频焦点。 */
    public boolean hasDesiredTrack() {
        return desiredTrack != null;
    }

    public void release() {
        released = true;
        loader.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        releaseQuietly(normalPlayer);
        releaseQuietly(bossPlayer);
        normalPlayer = null;
        bossPlayer = null;
    }

    // ==================== 内部 ====================

    /**
     * 把"期望状态"落成真实的 start/pause。所有入口都只改字段再调这里，
     * 保证"互斥 + 开关 + 暂停"三条规则只在一处推导，不会互相打架。
     */
    private void applyState() {
        if (released) {
            return;
        }
        boolean shouldPlay = enabled && !paused && desiredTrack != null;
        applyToPlayer(TRACK_NORMAL, normalPlayer, shouldPlay && TRACK_NORMAL.equals(desiredTrack));
        applyToPlayer(TRACK_BOSS, bossPlayer, shouldPlay && TRACK_BOSS.equals(desiredTrack));
    }

    private void applyToPlayer(String track, MediaPlayer player, boolean play) {
        if (player == null) {
            return; // 还在后台加载，加载完成会重新 applyState()
        }
        try {
            if (play) {
                if (!player.isPlaying()) {
                    player.setVolume(volume, volume);
                    player.start();
                    Log.i(TAG, "bgm: start " + track);
                }
            } else if (player.isPlaying()) {
                player.pause();
                Log.i(TAG, "bgm: pause " + track);
            }
        } catch (IllegalStateException e) {
            // MediaPlayer 状态机被系统回收（如播放设备切换）时会抛这个，重建交给下次 preload
            Log.w(TAG, "bgm: " + track + " 状态异常，忽略本次操作", e);
        }
    }

    private MediaPlayer createLooping(int resId) {
        MediaPlayer player = MediaPlayer.create(appContext, resId);
        if (player == null) {
            Log.e(TAG, "bgm: 资源创建失败 resId=" + resId);
            return null;
        }
        player.setLooping(true);
        player.setVolume(volume, volume);
        return player;
    }

    private static void releaseQuietly(MediaPlayer player) {
        if (player == null) {
            return;
        }
        try {
            player.release();
        } catch (IllegalStateException ignored) {
            // release 本身失败没有补救手段
        }
    }
}

package edu.hitsz.aircraftwar.platform.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import edu.hitsz.aircraftwar.R;

/**
 * 游戏音频门面：把"BGM 切歌 / 音效触发 / 音乐开关 / 音频焦点 / 生命周期"收在一处。
 *
 * <p>对应 {@code 实验3.pdf} p21「封装音频管理类」：音频逻辑与 Activity/Fragment 解耦，
 * 由 {@code ui} 层只调语义方法（{@code startBgm()}、{@code playHit()}…），
 * 不直接碰 {@code MediaPlayer}/{@code SoundPool}（AGENTS §7 铁律 2）。
 *
 * <p>课件按 {@code MyMediaPlayer}/{@code MySoundPool} 两个类讲，本工程拆得更细一层：
 * {@link AudioSettings}（偏好） + {@link BgmPlayer}（MediaPlayer） + {@link SfxPlayer}（SoundPool）
 * + 本类（门面 + 焦点）。答辩说法：职责跟课件一一对应，只是把"偏好持久化"和"焦点管理"
 * 从播放器里拿出来，避免一个类既管文件又管系统策略。
 *
 * @author hitsz
 */
public final class GameAudio {

    private static final String TAG = "AircraftWar";

    private final AudioManager audioManager;
    private final AudioSettings settings;
    private final BgmPlayer bgm;
    private final SfxPlayer sfx;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AudioFocusRequest focusRequest;
    private boolean focusHeld;
    /** 因被别的应用抢焦点而暂停；回焦时据此决定要不要恢复，避免"用户自己暂停"被误恢复。 */
    private boolean pausedByFocusLoss;
    private boolean released;

    public GameAudio(Context context) {
        Context app = context.getApplicationContext();
        audioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        settings = new AudioSettings(app);
        bgm = new BgmPlayer(app, settings.bgmVolume());
        sfx = new SfxPlayer(app, settings.sfxVolume());
        applyMusicEnabled(settings.isMusicEnabled());
        bgm.preloadAsync();
        focusRequest = buildFocusRequest();
    }

    // ==================== 偏好 ====================

    public boolean isMusicEnabled() {
        return settings.isMusicEnabled();
    }

    /** 音乐开关。关掉立即静音；打开时如果还在游戏里，BGM 会自己接回来（BgmPlayer 记着期望曲目）。 */
    public void setMusicEnabled(boolean enabled) {
        settings.setMusicEnabled(enabled);
        applyMusicEnabled(enabled);
    }

    private void applyMusicEnabled(boolean enabled) {
        bgm.setEnabled(enabled);
        sfx.setEnabled(enabled);
    }

    // ==================== 游戏内语义 ====================

    /** 开局：申请音频焦点并起普通 BGM。 */
    public void startBgm() {
        requestFocus();
        bgm.playNormal();
    }

    /** Boss 出场：切 Boss BGM（普通 BGM 由 BgmPlayer 自动暂停，课件 p22 注）。 */
    public void playBossBgm() {
        bgm.playBoss();
    }

    /** Boss 被击落：切回普通 BGM。 */
    public void stopBossBgm() {
        bgm.playNormal();
    }

    public void playHit() {
        sfx.play(R.raw.bullet_hit);
    }

    public void playPowerUp() {
        sfx.play(R.raw.get_supply);
    }

    public void playBomb() {
        sfx.play(R.raw.bomb_explosion);
    }

    /**
     * 升级音效：每 5 级用长音（{@code level_up}），其余用短音（{@code level_up_short}）。
     *
     * <p>桌面版 {@code SoundManager.playLevelUpSound()} 实际播的是 {@code gameOverClip}——历史缺陷。
     * 音频层是 S2 新写的代码，这里按资源本意选曲，不复制缺陷（见 {@code docs/04} §3）。
     */
    public void playLevelUp(int level) {
        sfx.play(level % 5 == 0 ? R.raw.level_up : R.raw.level_up_short);
    }

    /** 游戏结束：停 BGM，放结算音。 */
    public void playGameOver() {
        bgm.stop();
        sfx.play(R.raw.game_over);
        abandonFocus();
    }

    /** 中途退出游戏页：只停 BGM，不播结算音。 */
    public void stopBgm() {
        bgm.stop();
        abandonFocus();
    }

    // ==================== 生命周期 ====================

    /** 游戏页 {@code onPause}：暂停 BGM、交还音频焦点。不释放资源，回前台要能接着放。 */
    public void onPause() {
        bgm.pause();
        abandonFocus();
    }

    /** 游戏页 {@code onResume}：重新申请焦点并恢复 BGM（只在原本就该有声时）。 */
    public void onResume() {
        // 没有"该播的曲目"时（例如停在主菜单）不要抢焦点，否则会打断用户正在播的音乐
        if (bgm.hasDesiredTrack()) {
            requestFocus();
        }
        bgm.resume();
    }

    /** 离开游戏页：停 BGM + 释放两个播放器（AGENTS §8 生命周期）。重复调用安全。 */
    public void release() {
        if (released) {
            return;
        }
        released = true;
        abandonFocus();
        mainHandler.removeCallbacksAndMessages(null);
        bgm.release();
        sfx.release();
    }

    // ==================== 音频焦点 ====================

    private AudioFocusRequest buildFocusRequest() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        return new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                // 被别的应用短暂占用时直接暂停，比"压低音量继续放"更符合游戏预期
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(this::onFocusChange, mainHandler)
                .build();
    }

    private void requestFocus() {
        if (released || focusHeld || audioManager == null) {
            return;
        }
        int result = audioManager.requestAudioFocus(focusRequest);
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        Log.i(TAG, "audio focus: request -> " + (focusHeld ? "granted" : "denied"));
    }

    private void abandonFocus() {
        if (!focusHeld || audioManager == null) {
            return;
        }
        audioManager.abandonAudioFocusRequest(focusRequest);
        focusHeld = false;
    }

    private void onFocusChange(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // 丢焦点一律先停：游戏音效压不住会显得很脏，回来再续上
                pausedByFocusLoss = true;
                focusHeld = false;
                bgm.pause();
                Log.i(TAG, "audio focus: loss(" + change + ") -> bgm paused");
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                focusHeld = true;
                if (pausedByFocusLoss) {
                    pausedByFocusLoss = false;
                    bgm.resume();
                    Log.i(TAG, "audio focus: gain -> bgm resumed");
                }
                break;
            default:
                break;
        }
    }
}

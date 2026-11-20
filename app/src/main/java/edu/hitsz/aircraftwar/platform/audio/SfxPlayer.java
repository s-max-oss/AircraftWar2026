package edu.hitsz.aircraftwar.platform.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.hitsz.aircraftwar.R;

/**
 * 音效播放器：短促、可并发、低延迟，用 {@link SoundPool}。
 *
 * <p>迁移说明：替代桌面版 {@code SoundManager} 的 {@code Clip}（commit 50474cc）。
 * 桌面版每个音效一个 {@code Clip}、播前要 {@code stop()+setFramePosition(0)}，
 * 同一音效连续触发（扫射命中）时会被自己打断；SoundPool 用 stream 天然支持并发，
 * 这正是课件 p20「预加载解码，延迟低 / 支持并发播放」要讲的点。
 *
 * <p>{@code load} 是异步的：解码没完成就 play 会被静默丢弃，所以这里用
 * {@link SoundPool.OnLoadCompleteListener} 记账，只播已就绪的样本。
 *
 * @author hitsz
 */
public final class SfxPlayer {

    private static final String TAG = "AircraftWar";

    /** 同屏最多 8 个音效实例：够覆盖"连续命中 + 炸弹 + 升级"叠加，再多会互相盖住反而糊。 */
    private static final int MAX_STREAMS = 8;

    private final SoundPool pool;
    /** resId → SoundPool 的 sampleId */
    private final Map<Integer, Integer> sampleIds = new HashMap<>();
    /** sampleId → resId，onLoadComplete 回调只给 sampleId */
    private final Map<Integer, Integer> sampleToRes = new HashMap<>();
    private final Set<Integer> readySamples = new HashSet<>();

    private float volume;
    private boolean enabled = true;
    private boolean released;

    public SfxPlayer(Context context, float volume) {
        this.volume = volume;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(attributes)
                .build();
        pool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                readySamples.add(sampleId);
            } else {
                Log.w(TAG, "sfx: 加载失败 sampleId=" + sampleId + " status=" + status);
            }
        });

        load(context, R.raw.bullet_hit);
        load(context, R.raw.bomb_explosion);
        load(context, R.raw.get_supply);
        load(context, R.raw.game_over);
        load(context, R.raw.level_up_short);
        load(context, R.raw.level_up);
    }

    private void load(Context context, int resId) {
        int sampleId = pool.load(context, resId, 1);
        sampleIds.put(resId, sampleId);
        sampleToRes.put(sampleId, resId);
    }

    /**
     * 播放一个音效。未就绪或已静音时静默返回——游戏音效允许丢帧，不允许阻塞渲染线程。
     */
    public void play(int resId) {
        if (!enabled || released) {
            return;
        }
        Integer sampleId = sampleIds.get(resId);
        if (sampleId == null || !readySamples.contains(sampleId)) {
            return;
        }
        pool.play(sampleId, volume, volume, 1, 0, 1f);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    public void release() {
        released = true;
        pool.release();
    }
}

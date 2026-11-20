package edu.hitsz.aircraftwar.platform.audio;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 音频偏好的唯一来源（音乐开关 + 音量）。
 *
 * <p>为什么开关要落 {@code SharedPreferences}：`实验3.pdf` p3 把"添加音乐开关"列为独立要求，
 * 而验收会连着做"关掉 → 杀进程 → 重开"，只在内存里记住的开关过不了这一关。
 *
 * <p>音量默认值照抄桌面版 {@code SoundManager}（0.7 / 0.8），避免"听感与桌面版不一样"这种无谓差异。
 *
 * @author hitsz
 */
public final class AudioSettings {

    private static final String PREF_NAME = "audio_settings";
    private static final String KEY_ENABLED = "music_enabled";
    private static final String KEY_BGM_VOLUME = "bgm_volume";
    private static final String KEY_SFX_VOLUME = "sfx_volume";

    private static final float DEFAULT_BGM_VOLUME = 0.7f;
    private static final float DEFAULT_SFX_VOLUME = 0.8f;

    private final SharedPreferences prefs;

    public AudioSettings(Context context) {
        // 只用 ApplicationContext：本类会被 Activity/Presenter 长期持有，
        // 拿 Activity 的 Context 就会把它钉在内存里（AGENTS §8「禁止静态持有 Activity」）。
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 音乐总开关：关掉 = BGM 与音效同时静音。 */
    public boolean isMusicEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public void setMusicEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public float bgmVolume() {
        return prefs.getFloat(KEY_BGM_VOLUME, DEFAULT_BGM_VOLUME);
    }

    public void setBgmVolume(float volume) {
        prefs.edit().putFloat(KEY_BGM_VOLUME, clamp(volume)).apply();
    }

    public float sfxVolume() {
        return prefs.getFloat(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME);
    }

    public void setSfxVolume(float volume) {
        prefs.edit().putFloat(KEY_SFX_VOLUME, clamp(volume)).apply();
    }

    private static float clamp(float volume) {
        return Math.max(0f, Math.min(1f, volume));
    }
}

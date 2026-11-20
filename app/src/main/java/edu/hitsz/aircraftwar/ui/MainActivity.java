package edu.hitsz.aircraftwar.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.platform.audio.GameAudio;

/**
 * 单 Activity 宿主：三页由 Navigation 换成 Fragment（AGENTS §2 决策 9）。
 *
 * <p>迁移说明：S1 的 MainActivity 直接 {@code new GameView(this)} 承载游戏画面；
 * S2 起画面下沉到 {@code ui/game/GameFragment}，本类只剩两件事：沉浸式全屏 + 持有音频实例。
 *
 * <p>为什么音频放在 Activity 而不是 Fragment：{@link GameAudio} 里有两个 9MB 的
 * {@code MediaPlayer} 和一个 {@code SoundPool}。若挂在游戏页上，每次"结算 → 再来一局"
 * 都要重建一遍（还会在结算音还没播完时被 release 掐断）；挂在 Activity 上则一次创建、
 * 全程唯一，这同时也是"切后台不叠音"的结构性保证——**全局只有一套播放器**。
 * 释放点在 {@link #onDestroy()}，与进程同生共死。
 */
public class MainActivity extends AppCompatActivity {

    private GameAudio gameAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyImmersiveMode();
        setContentView(R.layout.activity_main);
        gameAudio = new GameAudio(this);
    }

    /** 游戏画面按 512×768 等比居中；系统栏隐藏，向上滑可临时唤出。 */
    private void applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    /** 全局唯一的音频门面。三个 Fragment 都从这里取，禁止各自 new。 */
    public GameAudio audio() {
        return gameAudio;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 切后台：BGM 暂停并交还音频焦点（AGENTS §8 生命周期）
        gameAudio.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameAudio.onResume();
    }

    @Override
    protected void onDestroy() {
        gameAudio.release();
        super.onDestroy();
    }
}

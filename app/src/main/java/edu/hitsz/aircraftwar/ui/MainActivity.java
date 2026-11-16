package edu.hitsz.aircraftwar.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.GameEvents;
import edu.hitsz.aircraftwar.core.logic.GameLogic;
import edu.hitsz.aircraftwar.core.logic.NormalGame;
import edu.hitsz.aircraftwar.platform.render.GameView;

/**
 * 游戏页：全屏宿主 {@link GameView}。
 *
 * <p>S1 只做"能跑起来"：难度固定为普通、音效走日志占位（S2 接 {@code SoundPool/MediaPlayer}）、
 * 结算暂不跳页（S2 加 {@code GameOverFragment}）。这样 S1 的 DoD（下落/拖动/碰撞计分）
 * 可以独立于音效与导航验证。
 */
public class MainActivity extends AppCompatActivity implements GameEvents {

    private static final String TAG = "AircraftWar";

    private GameView gameView;
    private GameLogic game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏沉浸：游戏画面按 512×768 等比缩放居中，系统栏隐藏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        gameView = new GameView(this);
        setContentView(buildRoot());

        game = new NormalGame("玩家", this);
        gameView.setGame(game);
        game.start();
    }

    /**
     * 布局：黑底 + 按 512:768 居中的 GameView。
     *
     * <p>为什么要手动算宽高：GameView 的 Surface 缓冲被固定成 512×768（省填充率），
     * 视图若与之一致就不会被拉伸；屏幕比例不同时用黑边补，而不是把画面拉变形。
     */
    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int viewWidth = metrics.widthPixels;
        int viewHeight = (int) (viewWidth * (float) GameConfig.WINDOW_HEIGHT / GameConfig.WINDOW_WIDTH);
        if (viewHeight > metrics.heightPixels) {
            viewHeight = metrics.heightPixels;
            viewWidth = (int) (viewHeight * (float) GameConfig.WINDOW_WIDTH / GameConfig.WINDOW_HEIGHT);
        }

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(viewWidth, viewHeight, Gravity.CENTER);
        root.addView(gameView, params);
        return root;
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }

    // ==================== GameEvents：S1 只用日志验证时序 ====================

    @Override
    public void onBgmStart() {
        Log.i(TAG, "event: bgm start（S2 接 MediaPlayer）");
    }

    @Override
    public void onHit() {
        // 每次命中都打日志会刷屏，S1 静默，S2 接 SoundPool
    }

    @Override
    public void onPowerUp() {
        Log.i(TAG, "event: power up");
    }

    @Override
    public void onBomb() {
        Log.i(TAG, "event: bomb");
    }

    @Override
    public void onLevelUp(int level, int hpBonus, int powerBonus) {
        Log.i(TAG, "event: level up -> " + level + " hp+" + hpBonus + " power+" + powerBonus);
    }

    @Override
    public void onBossBgmStart() {
        Log.i(TAG, "event: boss bgm start");
    }

    @Override
    public void onBossBgmStop() {
        Log.i(TAG, "event: boss bgm stop");
    }

    @Override
    public void onGameOver() {
        Log.i(TAG, "event: game over, score=" + game.getScore()
                + " kills=" + game.getEnemiesKilled()
                + " difficulty=" + game.getDifficultyLevel());
    }
}

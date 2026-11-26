package edu.hitsz.aircraftwar.ui.game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.GameEvents;
import edu.hitsz.aircraftwar.core.logic.GameLogic;
import edu.hitsz.aircraftwar.core.logic.HardGame;
import edu.hitsz.aircraftwar.core.logic.NormalGame;
import edu.hitsz.aircraftwar.core.logic.SimpleGame;
import edu.hitsz.aircraftwar.databinding.FragmentGameBinding;
import edu.hitsz.aircraftwar.platform.audio.GameAudio;
import edu.hitsz.aircraftwar.platform.render.GameView;
import edu.hitsz.aircraftwar.ui.MainActivity;
import edu.hitsz.aircraftwar.ui.common.ArcadeAccessibility;
import edu.hitsz.aircraftwar.ui.common.DifficultyLabels;

/**
 * 游戏页：把 S1 的 {@code MainActivity} 内容（SurfaceView 组装 + 逻辑实例）搬成一个 Fragment。
 *
 * <p>三件事都在这里：
 * <ol>
 *   <li>按 {@code difficulty} 参数选 {@link SimpleGame}/{@link NormalGame}/{@link HardGame}（课件 p3 第 2 项）；</li>
 *   <li>实现 {@link GameEvents}，把 core 的事件翻译成平台层动作（音效 / BGM / 跳页）；</li>
 *   <li><b>Handler 回主线程</b>：{@link #onGameOver()} 发生在 game-loop 线程，直接 {@code navigate()}
 *       会抛 {@code CalledFromWrongThreadException}（课件 p29–p30 讲的就是这个），所以用
 *       {@code Handler(Looper.getMainLooper())} 把"跳结算页"投递回 UI 线程。</li>
 * </ol>
 *
 * <p>为什么结算前要等 2 秒：{@code game_over.wav} 长 2.09 秒，立刻跳页会把音效掐断；
 * 这 2 秒里画面上仍然停着 S1 画的 {@code GAME OVER}，正好当死亡停顿。
 */
public class GameFragment extends Fragment implements GameEvents {

    public static final String ARG_PLAYER_NAME = "playerName";
    public static final String ARG_DIFFICULTY = "difficulty";

    public static final int DIFFICULTY_SIMPLE = 1;
    public static final int DIFFICULTY_NORMAL = 2;
    public static final int DIFFICULTY_HARD = 3;

    /** 与 game_over.wav 时长（2.09s）对齐，宁可多等一点也不要截断。 */
    private static final long GAME_OVER_DELAY_MS = 2000L;

    private FragmentGameBinding binding;
    private GameView gameView;
    private GameLogic game;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private OnBackPressedCallback backCallback;
    private boolean paused;
    private boolean gameOverHandled;
    /** 因切后台而暂停：回前台自动续上；用户手动暂停的则不自动续。 */
    private boolean pausedByLifecycle;
    /** 结算时 App 在后台：先记下来，回前台再跳页。 */
    private boolean pendingGameOver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArcadeAccessibility.asButton(binding.btnResume);
        ArcadeAccessibility.asButton(binding.btnQuit);

        // 先注入视口再建游戏：逻辑画布"长高"到屏幕比例，战场铺满、不再上下留黑
        applyViewport();

        game = createGame();
        gameView = new GameView(requireContext());
        gameView.setGame(game);
        binding.gameContainer.addView(gameView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        binding.btnPause.setOnClickListener(v -> openPauseMenu());
        binding.btnResume.setOnClickListener(v -> resumeGame());
        binding.btnQuit.setOnClickListener(v -> quitToMenu());

        // 返回键：游戏中最怕"一按就退"，先弹暂停浮层让玩家选（AGENTS §8 生命周期）
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (paused) {
                    resumeGame();
                } else if (!gameOverHandled) {
                    openPauseMenu();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);

        // 开局：GameLogic.start() 会回调 onBgmStart()，BGM 从那里起
        game.start();
    }

    // ==================== 组装 ====================

    private GameLogic createGame() {
        Bundle args = getArguments();
        String playerName = args == null ? null : args.getString(ARG_PLAYER_NAME);
        if (playerName == null || playerName.isEmpty()) {
            playerName = getString(R.string.menu_default_player);
        }
        int difficulty = args == null ? DIFFICULTY_NORMAL : args.getInt(ARG_DIFFICULTY, DIFFICULTY_NORMAL);
        switch (difficulty) {
            case DIFFICULTY_SIMPLE:
                return new SimpleGame(playerName, this);
            case DIFFICULTY_HARD:
                return new HardGame(playerName, this);
            default:
                return new NormalGame(playerName, this);
        }
    }

    /**
     * 把屏幕比例注入逻辑视口：宽仍 512，高度按屏高/屏宽换算（1080×2400 → 1138）。
     *
     * <p>为什么要这么做：桌面版是 512×768（2:3），手机是 9:20；沿用 768 只能 letterbox，
     * 上下各留 32% 的黑边。长高之后战场铺满，敌机从物理顶边出生、英雄机贴底边，
     * 飞机尺寸与碰撞判定都不变（宽度没动）。
     */
    private void applyViewport() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (metrics.widthPixels <= 0) {
            return;
        }
        int height = Math.round(GameConfig.WINDOW_WIDTH
                * (metrics.heightPixels / (float) metrics.widthPixels));
        GameConfig.setViewportHeight(height);
    }

    // ==================== 暂停 / 退出 ====================

    private void openPauseMenu() {
        if (paused || gameOverHandled || game.isGameOver()) {
            return;
        }
        paused = true;
        gameView.pause();
        audio().onPause();
        binding.textPauseScore.setText(String.valueOf(game.getScore()));
        binding.textPauseKills.setText(String.valueOf(game.getEnemiesKilled()));
        binding.textPauseDifficulty.setText(DifficultyLabels.of(requireContext(), game.getInitialDifficulty()));
        binding.pauseOverlay.setVisibility(View.VISIBLE);
    }

    private void resumeGame() {
        paused = false;
        binding.pauseOverlay.setVisibility(View.GONE);
        gameView.resume();
        if (!gameOverHandled) {
            audio().onResume();
        }
    }

    private void quitToMenu() {
        gameView.pause();
        audio().stopBgm();
        NavHostFragment.findNavController(this).popBackStack(R.id.menuFragment, false);
    }

    // ==================== 生命周期 ====================

    @Override
    public void onPause() {
        super.onPause();
        // 绘制线程必须停：切后台还在跑就是白烧电，验收也会用 Logcat 里的 fps 日志抓这条
        gameView.pause();
        if (!paused && !gameOverHandled) {
            pausedByLifecycle = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pausedByLifecycle && !paused && !gameOverHandled) {
            pausedByLifecycle = false;
            resumeGame();
        }
        if (pendingGameOver) {
            pendingGameOver = false;
            showGameOver();
        }
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacksAndMessages(null);
        if (backCallback != null) {
            backCallback.remove();
            backCallback = null;
        }
        if (gameView != null) {
            gameView.pause();
        }
        binding = null;
        super.onDestroyView();
    }

    // ==================== GameEvents：core → 平台层 ====================

    @Override
    public void onBgmStart() {
        audio().startBgm();
    }

    @Override
    public void onHit() {
        audio().playHit();
    }

    @Override
    public void onPowerUp() {
        audio().playPowerUp();
    }

    @Override
    public void onBomb() {
        audio().playBomb();
    }

    @Override
    public void onLevelUp(int level, int hpBonus, int powerBonus) {
        audio().playLevelUp(level);
    }

    @Override
    public void onBossBgmStart() {
        audio().playBossBgm();
    }

    @Override
    public void onBossBgmStop() {
        audio().stopBossBgm();
    }

    /**
     * 注意：本方法在 <b>game-loop 子线程</b>里被调用（{@code GameLogic.tick()} 内）。
     * 这里只做两件线程安全的事：放音效 + 往主线程投递消息；不碰任何 View。
     */
    @Override
    public void onGameOver() {
        audio().playGameOver();
        mainHandler.post(() -> {
            if (binding != null) {
                binding.btnPause.setVisibility(View.GONE);
            }
        });
        mainHandler.postDelayed(this::showGameOver, GAME_OVER_DELAY_MS);
    }

    // ==================== 跳结算页（主线程） ====================

    private void showGameOver() {
        if (gameOverHandled || binding == null || !isAdded()) {
            return;
        }
        if (!isResumed()) {
            // App 在后台时不跳页：回前台再跳，避免在不可见状态下改回退栈
            pendingGameOver = true;
            return;
        }
        gameOverHandled = true;
        gameView.pause();

        Bundle args = new Bundle();
        args.putString(ARG_PLAYER_NAME, game.getUserName());
        args.putInt(ARG_DIFFICULTY, game.getInitialDifficulty());
        args.putInt(GameOverArgs.SCORE, game.getScore());
        args.putInt(GameOverArgs.KILLS, game.getEnemiesKilled());
        args.putInt(GameOverArgs.DURATION_SEC, (int) (game.getGameTime() / 1000L));
        args.putInt(GameOverArgs.FINAL_LEVEL, game.getDifficultyLevel());
        args.putLong(GameOverArgs.GAME_TIME_MS, game.getGameTime());
        args.putLong(GameOverArgs.RECORD_TIME, System.currentTimeMillis());
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_game_to_gameover, args);
    }

    private GameAudio audio() {
        return ((MainActivity) requireActivity()).audio();
    }

    /** 结算页参数名，避免 GameFragment / GameOverFragment 两边写字符串写错。 */
    public static final class GameOverArgs {
        public static final String SCORE = "score";
        public static final String KILLS = "kills";
        public static final String DURATION_SEC = "durationSec";
        public static final String FINAL_LEVEL = "finalLevel";
        public static final String GAME_TIME_MS = "gameTimeMs";
        public static final String RECORD_TIME = "recordTime";
        public static final String PLAYER_NAME = ARG_PLAYER_NAME;
        public static final String DIFFICULTY = ARG_DIFFICULTY;

        private GameOverArgs() {
        }
    }
}

package edu.hitsz.aircraftwar.ui.gameover;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.data.repository.RecordRepository;
import edu.hitsz.aircraftwar.databinding.FragmentGameoverBinding;
import edu.hitsz.aircraftwar.ui.MainActivity;
import edu.hitsz.aircraftwar.ui.common.ArcadeAccessibility;
import edu.hitsz.aircraftwar.ui.common.DifficultyLabels;
import edu.hitsz.aircraftwar.ui.common.SimpleViewModelFactory;
import edu.hitsz.aircraftwar.ui.game.GameFragment;

/**
 * 结算页：只负责把 {@code Bundle} 里的数字摆到界面上，不做任何计算。
 *
 * <p>数据全部来自 {@link GameFragment} 跳转时带的参数——包括难度档位，
 * 这样"再来一局"能原样带着同一难度回去，不需要在页面之间共享状态。
 *
 * <p>返回键与"返回主菜单"走同一条路：{@code popBackStack(menuFragment)}，
 * 保证结算页不会留在回退栈里（否则按返回会回到已经结束的那一局）。
 */
public class GameOverFragment extends Fragment {

    private FragmentGameoverBinding binding;
    private String playerName;
    private int difficulty;
    private OnBackPressedCallback backCallback;
    private GameOverViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGameoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArcadeAccessibility.asButton(binding.btnRestart);
        ArcadeAccessibility.asButton(binding.btnBackMenu);

        Bundle args = getArguments();
        int score = args == null ? 0 : args.getInt(GameFragment.GameOverArgs.SCORE, 0);
        int kills = args == null ? 0 : args.getInt(GameFragment.GameOverArgs.KILLS, 0);
        int durationSec = args == null ? 0 : args.getInt(GameFragment.GameOverArgs.DURATION_SEC, 0);
        difficulty = args == null ? GameFragment.DIFFICULTY_NORMAL
                : args.getInt(GameFragment.GameOverArgs.DIFFICULTY, GameFragment.DIFFICULTY_NORMAL);
        playerName = args == null ? getString(R.string.menu_default_player)
                : args.getString(GameFragment.GameOverArgs.PLAYER_NAME, getString(R.string.menu_default_player));
        if (playerName == null || playerName.isEmpty()) {
            playerName = getString(R.string.menu_default_player);
        }

        binding.textScore.setText(String.valueOf(score));
        binding.textPlayer.setText(getString(R.string.gameover_player, playerName));
        binding.textKills.setText(getString(R.string.gameover_kills_value, kills));
        binding.textDuration.setText(getString(R.string.gameover_duration_value,
                durationSec / 60, durationSec % 60));
        binding.textDifficulty.setText(DifficultyLabels.of(requireContext(), difficulty));

        int finalLevel = args == null ? difficulty
                : args.getInt(GameFragment.GameOverArgs.FINAL_LEVEL, difficulty);
        long gameTimeMs = args == null ? durationSec * 1000L
                : args.getLong(GameFragment.GameOverArgs.GAME_TIME_MS, durationSec * 1000L);
        long recordTime = args == null ? 0L
                : args.getLong(GameFragment.GameOverArgs.RECORD_TIME, 0L);
        if (recordTime <= 0L) {
            // 兼容旧导航参数：没有死亡时刻时退化为当前时间，保证 recordTime 可排序。
            recordTime = System.currentTimeMillis();
        }
        // 页面只负责把 Bundle 参数拼成纯数据对象；查旧最佳、插入都在 ViewModel 的
        // Executor 里做，主线程不碰数据库（AGENTS §8 线程约定）。
        GameRecord record = new GameRecord(0, playerName, score, kills, gameTimeMs,
                recordTime, finalLevel, difficulty);
        RecordRepository repository = ((MainActivity) requireActivity()).records();
        viewModel = new ViewModelProvider(this,
                new SimpleViewModelFactory(() -> new GameOverViewModel(repository, record)))
                .get(GameOverViewModel.class);
        // LiveData 只在该结算页可见时回调；旋转后 ViewModel 还在，不会重复入库。
        viewModel.bestResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }
            binding.textBest.setText(getString(R.string.gameover_best, result.bestScore));
            binding.textNewRecord.setVisibility(result.newRecord ? View.VISIBLE : View.GONE);
        });

        binding.btnRestart.setOnClickListener(v -> restart());
        binding.btnBackMenu.setOnClickListener(v -> backToMenu());

        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToMenu();
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);
    }

    /** 再来一局：先把这一局（game + gameover）从回退栈清掉，再用同一难度重进游戏页。 */
    private void restart() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.popBackStack(R.id.gameFragment, true);

        Bundle args = new Bundle();
        args.putString(GameFragment.ARG_PLAYER_NAME, playerName);
        args.putInt(GameFragment.ARG_DIFFICULTY, difficulty);
        navController.navigate(R.id.action_menu_to_game, args);
    }

    private void backToMenu() {
        NavHostFragment.findNavController(this).popBackStack(R.id.menuFragment, false);
    }

    @Override
    public void onDestroyView() {
        if (backCallback != null) {
            backCallback.remove();
            backCallback = null;
        }
        binding = null;
        super.onDestroyView();
    }
}

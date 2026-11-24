package edu.hitsz.aircraftwar.ui.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.databinding.FragmentMenuBinding;
import edu.hitsz.aircraftwar.ui.MainActivity;
import edu.hitsz.aircraftwar.ui.game.GameFragment;

/**
 * 主菜单：难度选择 + 音乐开关 + 开始游戏（第二页的数据由 {@link #startGame()} 装进 Bundle）。
 *
 * <p>对应 {@code 实验3.pdf} p3 的①「开发难度选择页面」与③「添加音乐开关」。
 * 难度是**三档**（简单/普通/困难），与桌面版 {@code MainMenuPanel} 的
 * {@code JComboBox} 一一对应；游戏内 1–5 级是随时间提升的难度，不是这里的档位。
 *
 * <p>记录与排行榜在功能接入前不展示入口，避免把开发阶段标记暴露给玩家。
 */
public class MainMenuFragment extends Fragment {

    private FragmentMenuBinding binding;
    private int selectedDifficulty = GameFragment.DIFFICULTY_NORMAL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 图标按钮仍控制同一份持久化设置，音频行为不变。
        updateMusicButton();
        binding.switchMusic.setOnClickListener(v -> {
            audio().setMusicEnabled(!audio().isMusicEnabled());
            updateMusicButton();
        });

        binding.btnSimple.setOnClickListener(v -> selectDifficulty(GameFragment.DIFFICULTY_SIMPLE));
        binding.btnNormal.setOnClickListener(v -> selectDifficulty(GameFragment.DIFFICULTY_NORMAL));
        binding.btnHard.setOnClickListener(v -> selectDifficulty(GameFragment.DIFFICULTY_HARD));
        binding.btnStart.setOnClickListener(v -> startGame());

        selectDifficulty(GameFragment.DIFFICULTY_NORMAL);
    }

    private MainActivity activity() {
        return (MainActivity) requireActivity();
    }

    private edu.hitsz.aircraftwar.platform.audio.GameAudio audio() {
        return activity().audio();
    }

    private void updateMusicButton() {
        boolean enabled = audio().isMusicEnabled();
        binding.switchMusic.setImageResource(enabled ? R.drawable.ic_music : R.drawable.ic_music_off);
        binding.switchMusic.setContentDescription(getString(enabled
                ? R.string.menu_music_on : R.string.menu_music_off));
        binding.switchMusic.setSelected(enabled);
    }

    /** 选中态同时改变底色和可访问性状态。 */
    private void selectDifficulty(int difficulty) {
        selectedDifficulty = difficulty;
        applySegment(binding.btnSimple, difficulty == GameFragment.DIFFICULTY_SIMPLE);
        applySegment(binding.btnNormal, difficulty == GameFragment.DIFFICULTY_NORMAL);
        applySegment(binding.btnHard, difficulty == GameFragment.DIFFICULTY_HARD);
        binding.textDifficultyDesc.setText(descriptionOf(difficulty));
    }

    private void applySegment(TextView button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_arcade_selected : R.drawable.bg_arcade_pill);
        button.setSelected(selected);
    }

    private int descriptionOf(int difficulty) {
        switch (difficulty) {
            case GameFragment.DIFFICULTY_SIMPLE:
                return R.string.menu_difficulty_desc_simple;
            case GameFragment.DIFFICULTY_HARD:
                return R.string.menu_difficulty_desc_hard;
            default:
                return R.string.menu_difficulty_desc_normal;
        }
    }

    private void startGame() {
        String playerName = binding.inputPlayerName.getText().toString().trim();
        if (playerName.isEmpty()) {
            playerName = getString(R.string.menu_default_player);
        }
        Bundle args = new Bundle();
        args.putString(GameFragment.ARG_PLAYER_NAME, playerName);
        args.putInt(GameFragment.ARG_DIFFICULTY, selectedDifficulty);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_menu_to_game, args);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}

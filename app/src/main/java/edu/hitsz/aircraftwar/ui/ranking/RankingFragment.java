package edu.hitsz.aircraftwar.ui.ranking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.util.List;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.data.repository.RecordRepository;
import edu.hitsz.aircraftwar.databinding.FragmentRankingBinding;
import edu.hitsz.aircraftwar.ui.MainActivity;
import edu.hitsz.aircraftwar.ui.common.ArcadeAccessibility;
import edu.hitsz.aircraftwar.ui.common.RecordListAdapter;
import edu.hitsz.aircraftwar.ui.common.SimpleViewModelFactory;

/**
 * 排行榜页：视角只做三件事——切筛选、把查询结果交给 Adapter、删除后提示。
 *
 * <p>对应 {@code 实验4.pdf} p3 的“开发排行榜页面 / 展示 / 删除”三条；数据来自
 * {@link RankingViewModel} 的 LiveData，页面不直接碰 Cursor 或 SQLiteDatabase。
 */
public class RankingFragment extends Fragment {

    private FragmentRankingBinding binding;
    private RecordListAdapter adapter;
    private RankingViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRankingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArcadeAccessibility.asButton(binding.btnBack);
        ArcadeAccessibility.asSingleChoice(binding.chipAll);
        ArcadeAccessibility.asSingleChoice(binding.chipSimple);
        ArcadeAccessibility.asSingleChoice(binding.chipNormal);
        ArcadeAccessibility.asSingleChoice(binding.chipHard);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        RecordRepository repository = ((MainActivity) requireActivity()).records();
        viewModel = new ViewModelProvider(this,
                new SimpleViewModelFactory(() -> new RankingViewModel(repository)))
                .get(RankingViewModel.class);

        adapter = new RecordListAdapter(requireContext(), true, this::deleteRecord);
        binding.listRanking.setAdapter(adapter);
        binding.listRanking.setEmptyView(binding.emptyState);

        viewModel.items().observe(getViewLifecycleOwner(), this::render);

        binding.chipAll.setOnClickListener(v -> selectFilter(RecordRepository.ALL_DIFFICULTIES));
        binding.chipSimple.setOnClickListener(v -> selectFilter(1));
        binding.chipNormal.setOnClickListener(v -> selectFilter(2));
        binding.chipHard.setOnClickListener(v -> selectFilter(3));

        renderFilter(viewModel.currentDifficulty());
    }

    private void render(List<GameRecord> records) {
        adapter.submit(records);
        binding.textCount.setText(getString(R.string.ranking_count, records.size()));
    }

    private void selectFilter(int difficulty) {
        renderFilter(difficulty);
        viewModel.selectDifficulty(difficulty);
    }

    private void renderFilter(int difficulty) {
        applyChip(binding.chipAll, difficulty == RecordRepository.ALL_DIFFICULTIES);
        applyChip(binding.chipSimple, difficulty == 1);
        applyChip(binding.chipNormal, difficulty == 2);
        applyChip(binding.chipHard, difficulty == 3);
    }

    private static void applyChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected
                ? R.drawable.bg_arcade_selected : R.drawable.bg_arcade_pill);
        chip.setSelected(selected);
    }

    private void deleteRecord(GameRecord record) {
        viewModel.delete(record);
        Toast.makeText(requireContext(), R.string.record_deleted, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        adapter = null;
        super.onDestroyView();
    }
}

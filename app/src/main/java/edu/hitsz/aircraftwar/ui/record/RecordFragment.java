package edu.hitsz.aircraftwar.ui.record;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.data.repository.RecordRepository;
import edu.hitsz.aircraftwar.databinding.FragmentRecordBinding;
import edu.hitsz.aircraftwar.ui.MainActivity;
import edu.hitsz.aircraftwar.ui.common.ArcadeAccessibility;
import edu.hitsz.aircraftwar.ui.common.RecordListAdapter;
import edu.hitsz.aircraftwar.ui.common.SimpleViewModelFactory;

/**
 * 记录页：全部记录按时间倒序，支持单条删除与清空。
 *
 * <p>比课件示例多这一页的原因：桌面版有 {@code RecordPanel}（序号/难度/时间/删除/清空），
 * 迁移对照表里要对得上；排行榜页只承载“按分数排名”，记录页承载“全部流水”。
 * debug 构建多一个“生成 1000 条”入口，专门用于性能验收，release 不显示。
 */
public class RecordFragment extends Fragment {

    private static final int SEED_COUNT = 1000;

    private FragmentRecordBinding binding;
    private RecordListAdapter adapter;
    private RecordViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArcadeAccessibility.asButton(binding.btnBack);
        ArcadeAccessibility.asButton(binding.btnClear);
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        RecordRepository repository = ((MainActivity) requireActivity()).records();
        viewModel = new ViewModelProvider(this,
                new SimpleViewModelFactory(() -> new RecordViewModel(repository)))
                .get(RecordViewModel.class);

        adapter = new RecordListAdapter(requireContext(), false, this::deleteRecord);
        binding.listRecords.setAdapter(adapter);
        binding.listRecords.setEmptyView(binding.emptyState);

        viewModel.items().observe(getViewLifecycleOwner(), this::render);
        binding.btnClear.setOnClickListener(v -> confirmClear());
        binding.textCount.setText(getString(R.string.record_count, 0));

        if (isDebuggable()) {
            binding.btnSeed.setVisibility(View.VISIBLE);
            binding.btnSeed.setOnClickListener(v -> {
                viewModel.seedSamples(SEED_COUNT);
                Toast.makeText(requireContext(), R.string.record_seed_started,
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void render(List<GameRecord> records) {
        adapter.submit(records);
        binding.textCount.setText(getString(R.string.record_count, records.size()));
    }

    private void deleteRecord(GameRecord record) {
        viewModel.delete(record);
        Toast.makeText(requireContext(), R.string.record_deleted, Toast.LENGTH_SHORT).show();
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.record_clear_title)
                .setMessage(R.string.record_clear_message)
                .setNegativeButton(R.string.record_clear_cancel, null)
                .setPositiveButton(R.string.record_clear_ok, (dialog, which) -> {
                    viewModel.clearAll();
                    Toast.makeText(requireContext(), R.string.record_cleared,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private boolean isDebuggable() {
        return (requireContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        adapter = null;
        super.onDestroyView();
    }
}

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
        // 与排行榜同一套模式：Fragment 只观察 LiveData，查库在 ViewModel 的 Executor。
        viewModel = new ViewModelProvider(this,
                new SimpleViewModelFactory(() -> new RecordViewModel(repository)))
                .get(RecordViewModel.class);

        adapter = new RecordListAdapter(requireContext(), false, this::deleteRecord);
        binding.listRecords.setAdapter(adapter);
        // 0 条时 ListView 自动显示空态；不要让 Adapter 自己造一行“暂无记录”。
        binding.listRecords.setEmptyView(binding.emptyState);

        viewModel.items().observe(getViewLifecycleOwner(), this::render);
        binding.btnClear.setOnClickListener(v -> confirmClear());
        binding.textCount.setText(getString(R.string.record_count, 0));

        // 造数入口只在 debug 构建出现：release 包不暴露 1000 条测试数据按钮。
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
        // 清空是破坏性操作（排行榜一起清），必须二次确认；MaterialAlertDialogBuilder 是
        // Material 组件库对 AlertDialog 的封装，主题由 AppTheme 提供。
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
        // 不引入 BuildConfig：直接读 ApplicationInfo 的 FLAG_DEBUGGABLE，debug/release 都成立。
        return (requireContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    @Override
    public void onDestroyView() {
        // 与排行榜页同理：Fragment 视图销毁后不再持有 Binding / Adapter。
        binding = null;
        adapter = null;
        super.onDestroyView();
    }
}

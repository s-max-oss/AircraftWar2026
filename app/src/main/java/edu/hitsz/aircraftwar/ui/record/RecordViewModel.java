package edu.hitsz.aircraftwar.ui.record;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.data.repository.RecordRepository;

/**
 * 记录页状态：全部记录、删除、清空，以及 debug 构建的 1000 条造数。
 *
 * <p>造数走 {@link RecordRepository#addAll}，底层是一次事务——这也是课件 p16
 * “批量写入放进事务”的实测场景（逐条 insert 一千次会明显卡）。
 */
public class RecordViewModel extends ViewModel {

    private static final String TAG = "RecordViewModel";

    private final RecordRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<GameRecord>> items =
            new MutableLiveData<>(Collections.emptyList());

    public RecordViewModel(RecordRepository repository) {
        this.repository = repository;
        load();
    }

    public LiveData<List<GameRecord>> items() {
        return items;
    }

    public void delete(GameRecord record) {
        executor.execute(() -> {
            try {
                repository.remove(record.getId());
            } catch (RuntimeException e) {
                Log.e(TAG, "删除记录失败 id=" + record.getId(), e);
            }
            load();
        });
    }

    public void clearAll() {
        executor.execute(() -> {
            try {
                repository.clear();
            } catch (RuntimeException e) {
                Log.e(TAG, "清空记录失败", e);
            }
            load();
        });
    }

    /** 仅 debug 构建的造数入口；一次事务写入，用于验证 1000 条滚动与排序。 */
    public void seedSamples(int count) {
        executor.execute(() -> {
            List<GameRecord> batch = new ArrayList<>(count);
            long now = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                int initialDifficulty = 1 + (i % 3);
                int finalLevel = 1 + (i % 5);
                int score = 50 + (i * 37) % 950;
                int kills = 1 + (i * 13) % 40;
                long gameTime = (20L + (i % 300)) * 1000L;
                long recordTime = now - (long) i * 60_000L;
                String name = String.format(Locale.CHINA, "测试%03d", i % 1000);
                batch.add(new GameRecord(0, name, score, kills, gameTime,
                        recordTime, finalLevel, initialDifficulty));
            }
            try {
                repository.addAll(batch);
            } catch (RuntimeException e) {
                Log.e(TAG, "批量造数失败", e);
            }
            load();
        });
    }

    private void load() {
        executor.execute(() -> {
            List<GameRecord> result;
            try {
                result = repository.all();
            } catch (RuntimeException e) {
                Log.e(TAG, "查询全部记录失败", e);
                result = Collections.emptyList();
            }
            items.postValue(result);
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
    }
}

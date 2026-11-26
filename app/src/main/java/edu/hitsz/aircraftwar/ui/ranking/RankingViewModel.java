package edu.hitsz.aircraftwar.ui.ranking;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.data.repository.RecordRepository;

/**
 * 排行榜页的状态：当前筛选 + 查询结果。
 *
 * <p>数据库查询放在单线程 Executor；每次筛选变化用 {@link AtomicInteger} 发一个序号，
 * 只有最新一次结果能回写 LiveData，避免快速点筛选时旧结果覆盖新结果。
 */
public class RankingViewModel extends ViewModel {

    private static final String TAG = "RankingViewModel";

    private final RecordRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger requestSeq = new AtomicInteger();
    private final MutableLiveData<List<GameRecord>> items =
            new MutableLiveData<>(Collections.emptyList());

    private volatile int difficulty = RecordRepository.ALL_DIFFICULTIES;

    public RankingViewModel(RecordRepository repository) {
        this.repository = repository;
        load();
    }

    public LiveData<List<GameRecord>> items() {
        return items;
    }

    public int currentDifficulty() {
        return difficulty;
    }

    public void selectDifficulty(int newDifficulty) {
        if (difficulty == newDifficulty) {
            return;
        }
        difficulty = newDifficulty;
        load();
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

    private void load() {
        int seq = requestSeq.incrementAndGet();
        int requested = difficulty;
        executor.execute(() -> {
            List<GameRecord> result;
            try {
                result = repository.ranking(requested);
            } catch (RuntimeException e) {
                Log.e(TAG, "查询排行榜失败 difficulty=" + requested, e);
                result = Collections.emptyList();
            }
            if (seq == requestSeq.get()) {
                items.postValue(result);
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
    }
}

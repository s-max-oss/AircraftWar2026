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
        // 页面只观察结果；Cursor、SQLiteDatabase 都不进 UI 层。
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
        // 删完立刻按当前筛选重查：排名/计数都由数据库重新算，不在内存里做“减一”。
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
        // 每次筛选变化发一个序号；旧请求即使后返回也不能覆盖新筛选的结果。
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
                // postValue：从 Executor 线程回主线程；setValue 只能在主线程调。
                items.postValue(result);
            }
        });
    }

    @Override
    protected void onCleared() {
        // shutdown 允许已排队的查询跑完；shutdownNow 会中断，删除后重查可能被丢掉。
        executor.shutdown();
        super.onCleared();
    }
}

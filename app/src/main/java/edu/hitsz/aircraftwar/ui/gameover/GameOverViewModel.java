package edu.hitsz.aircraftwar.ui.gameover;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.data.repository.RecordRepository;

/**
 * 结算页的数据任务：先把本局入库，再算“历史最佳”。
 *
 * <p>顺序不能反：先查旧最佳、再插入，才能判断本局是不是新纪录；查询与插入放在同一个
 * 单线程任务里，旋转屏幕时 ViewModel 不重建，也就不会重复入库。
 */
public class GameOverViewModel extends ViewModel {

    private static final String TAG = "GameOverViewModel";

    /** 结算页要展示的结果；bestScore 已包含本局。 */
    public static final class BestResult {
        public final int bestScore;
        public final boolean newRecord;
        public final boolean saved;

        BestResult(int bestScore, boolean newRecord, boolean saved) {
            this.bestScore = bestScore;
            this.newRecord = newRecord;
            this.saved = saved;
        }
    }

    private final MutableLiveData<BestResult> bestResult = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GameOverViewModel(RecordRepository repository, GameRecord record) {
        executor.execute(() -> {
            int previousBest = 0;
            boolean saved = true;
            try {
                previousBest = repository.bestScore(record.getInitialDifficulty());
                repository.add(record);
            } catch (RuntimeException e) {
                Log.e(TAG, "保存记录失败", e);
                saved = false;
            }
            int best = Math.max(previousBest, record.getScore());
            boolean newRecord = saved && record.getScore() > previousBest;
            bestResult.postValue(new BestResult(best, newRecord, saved));
        });
    }

    public LiveData<BestResult> bestResult() {
        return bestResult;
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
        super.onCleared();
    }
}

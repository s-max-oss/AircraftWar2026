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
        // 先查旧最佳、再插入：顺序反了会把本局算进“历史最佳”，永远显示新纪录。
        // 两步放在同一个单线程任务里，避免查询与插入竞态；ViewModel 旋转不重建，
        // 所以配置变化不会重复插入（进程被杀重建的极端情况见 docs/05 §6）。
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
        // 结算页只观察“最佳分/是否新纪录”，不直接碰数据库。
        return bestResult;
    }

    @Override
    protected void onCleared() {
        // 用户立刻退出结算页时，已排队的入库任务仍允许跑完，不能丢成绩。
        executor.shutdown();
        super.onCleared();
    }
}

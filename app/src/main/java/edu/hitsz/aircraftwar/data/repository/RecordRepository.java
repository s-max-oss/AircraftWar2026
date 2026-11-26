package edu.hitsz.aircraftwar.data.repository;

import java.util.List;

import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.core.record.GameRecordDao;

/**
 * 记录仓库：界面层只认这一层，不直接碰 DAO 实现。
 *
 * <p>这样换存储（SQLite ↔ 文件）只需要换构造参数；查询/删除的排序与筛选语义也收在这里，
 * Adapter 只负责把 {@link GameRecord} 摆到行布局上。
 *
 * <p>纯 Java，不 import android.*；异步由 ViewModel 的 Executor 负责。
 */
public class RecordRepository {

    /** 排行榜“全部难度”的筛选值；1/2/3 是主菜单三档。 */
    public static final int ALL_DIFFICULTIES = 0;

    private final GameRecordDao dao;

    public RecordRepository(GameRecordDao dao) {
        this.dao = dao;
    }

    public void add(GameRecord record) {
        dao.addRecord(record);
    }

    public void addAll(List<GameRecord> records) {
        dao.addRecords(records);
    }

    /** 记录页：全部记录，按记录时间倒序。 */
    public List<GameRecord> all() {
        return dao.getAllRecords();
    }

    /** 排行榜：全部或某一开局档位，按分数降序。 */
    public List<GameRecord> ranking(int initialDifficulty) {
        if (initialDifficulty == ALL_DIFFICULTIES) {
            return dao.getRankingRecords();
        }
        return dao.getRecordsByInitialDifficulty(initialDifficulty);
    }

    public int bestScore(int initialDifficulty) {
        return dao.getBestScore(initialDifficulty);
    }

    public void remove(int id) {
        dao.removeRecord(id);
    }

    public void clear() {
        dao.clearAllRecords();
    }
}

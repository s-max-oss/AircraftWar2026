package edu.hitsz.aircraftwar.core.record;

import java.util.List;

/**
 * 成绩记录 DAO 契约：换实现不换契约（S3 起由 {@code platform/storage} 下的 SQLite/文件实现）。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.dao.GameRecordDao}（commit 50474cc）。
 * 在原 5 个方法上补了 {@link #addRecords(List)} 与 {@link #getBestScore(int)}：
 * 前者对应课件 p16 的“批量写入放事务”，后者给结算页“历史最佳”用，避免把整表读进内存再取最大值。
 *
 * @author hitsz
 */
public interface GameRecordDao {

    void addRecord(GameRecord record);

    /** 批量写入；实现应保证一次落盘/一个事务，避免 1000 条逐条提交。 */
    void addRecords(List<GameRecord> records);

    void removeRecord(int id);

    List<GameRecord> getAllRecords();

    /** 按开局档位筛选，按分数降序；同分按记录时间升序（先到先得）。 */
    List<GameRecord> getRecordsByInitialDifficulty(int initialDifficulty);

    /** 排行榜“全部难度”：不筛选，按分数降序；同分按记录时间升序。 */
    List<GameRecord> getRankingRecords();

    /** 某开局档位的历史最高分；没有记录时返回 0。 */
    int getBestScore(int initialDifficulty);

    void clearAllRecords();
}

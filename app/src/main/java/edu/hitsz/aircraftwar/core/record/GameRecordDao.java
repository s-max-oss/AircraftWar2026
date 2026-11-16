package edu.hitsz.aircraftwar.core.record;

import java.util.List;

/**
 * 成绩记录 DAO 契约：换实现不换契约（S3 由 Room 实现）。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.dao.GameRecordDao}（commit 50474cc）。
 *
 * @author hitsz
 */
public interface GameRecordDao {

    void addRecord(GameRecord record);

    void removeRecord(int id);

    List<GameRecord> getAllRecords();

    List<GameRecord> getRecordsByInitialDifficulty(int initialDifficulty);

    void clearAllRecords();
}

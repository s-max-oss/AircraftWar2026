package edu.hitsz.aircraftwar.platform.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.hitsz.aircraftwar.core.record.GameRecord;

/**
 * 文件版 DAO 的契约测试：同一个 {@code GameRecordDao} 接口，SQLite 实现应给出同样的顺序语义。
 *
 * <p>用临时目录而不是 Android 沙箱，所以能在 JVM 单测里跑；这也是把
 * {@code FileRecordDao} 的构造参数设计成 {@link File} 的原因。
 */
public class FileRecordDaoTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void addRecordPersistsAcrossInstances() throws IOException {
        File dir = temporaryFolder.newFolder("records");
        FileRecordDao first = new FileRecordDao(dir);
        first.addRecord(record("Alice", 120, 2, 1_000L));

        FileRecordDao reopened = new FileRecordDao(dir);
        List<GameRecord> all = reopened.getAllRecords();

        assertEquals(1, all.size());
        assertEquals("Alice", all.get(0).getName());
        assertEquals(120, all.get(0).getScore());
        assertEquals(1, all.get(0).getId());
    }

    @Test
    public void rankingAllOrdersByScoreThenEarlierFirst() {
        FileRecordDao dao = new FileRecordDao(temporaryFolder.getRoot());
        dao.addRecord(record("low", 100, 2, 3_000L));
        dao.addRecord(record("late", 200, 1, 2_000L));
        dao.addRecord(record("early", 200, 3, 1_000L));

        List<GameRecord> ranking = dao.getRankingRecords();

        assertEquals(Arrays.asList("early", "late", "low"), names(ranking));
    }

    @Test
    public void rankingByDifficultyFiltersAndSorts() {
        FileRecordDao dao = new FileRecordDao(temporaryFolder.getRoot());
        dao.addRecord(record("normal-high", 300, 2, 1_000L));
        dao.addRecord(record("normal-low", 100, 2, 2_000L));
        dao.addRecord(record("hard", 999, 3, 3_000L));

        List<GameRecord> normal = dao.getRecordsByInitialDifficulty(2);

        assertEquals(Arrays.asList("normal-high", "normal-low"), names(normal));
        assertTrue(normal.stream().allMatch(r -> r.getInitialDifficulty() == 2));
    }

    @Test
    public void allRecordsOrderByRecordTimeDesc() {
        FileRecordDao dao = new FileRecordDao(temporaryFolder.getRoot());
        dao.addRecord(record("older", 10, 1, 1_000L));
        dao.addRecord(record("newer", 20, 1, 2_000L));

        assertEquals(Arrays.asList("newer", "older"), names(dao.getAllRecords()));
    }

    @Test
    public void removeKeepsOtherIdsStable() {
        FileRecordDao dao = new FileRecordDao(temporaryFolder.getRoot());
        dao.addRecord(record("one", 10, 1, 1_000L));
        dao.addRecord(record("two", 20, 1, 2_000L));
        dao.addRecord(record("three", 30, 1, 3_000L));

        dao.removeRecord(2);

        List<GameRecord> remaining = dao.getAllRecords();
        assertEquals(2, remaining.size());
        assertTrue(remaining.stream().anyMatch(r -> r.getId() == 1));
        assertTrue(remaining.stream().anyMatch(r -> r.getId() == 3));
    }

    @Test
    public void clearRemovesEverythingAndPersists() {
        File dir = temporaryFolder.getRoot();
        FileRecordDao dao = new FileRecordDao(dir);
        dao.addRecord(record("one", 10, 1, 1_000L));
        dao.addRecord(record("two", 20, 1, 2_000L));

        dao.clearAllRecords();

        assertEquals(0, dao.getAllRecords().size());
        assertEquals(0, new FileRecordDao(dir).getAllRecords().size());
    }

    @Test
    public void bestScoreIsZeroWhenEmptyAndMaxOtherwise() {
        FileRecordDao dao = new FileRecordDao(temporaryFolder.getRoot());
        assertEquals(0, dao.getBestScore(2));

        dao.addRecord(record("a", 120, 2, 1_000L));
        dao.addRecord(record("b", 340, 2, 2_000L));
        dao.addRecord(record("c", 999, 3, 3_000L));

        assertEquals(340, dao.getBestScore(2));
        assertEquals(999, dao.getBestScore(3));
        assertEquals(0, dao.getBestScore(1));
    }

    @Test
    public void addRecordsAssignsSequentialIds() {
        FileRecordDao dao = new FileRecordDao(temporaryFolder.getRoot());
        List<GameRecord> batch = new ArrayList<>();
        batch.add(record("a", 1, 1, 1_000L));
        batch.add(record("b", 2, 1, 2_000L));

        dao.addRecords(batch);

        assertEquals(1, batch.get(0).getId());
        assertEquals(2, batch.get(1).getId());
        assertEquals(2, dao.getAllRecords().size());
    }

    @Test
    public void corruptedFileFallsBackToEmpty() throws IOException {
        File dir = temporaryFolder.newFolder("corrupted");
        File file = new File(dir, "game_records.dat");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("not a java serialization stream");
        }

        assertEquals(0, new FileRecordDao(dir).getAllRecords().size());
    }

    private static GameRecord record(String name, int score, int initialDifficulty, long recordTime) {
        return new GameRecord(0, name, score, 3, 60_000L, recordTime,
                initialDifficulty, initialDifficulty);
    }

    private static List<String> names(List<GameRecord> records) {
        List<String> names = new ArrayList<>();
        for (GameRecord record : records) {
            names.add(record.getName());
        }
        return names;
    }
}

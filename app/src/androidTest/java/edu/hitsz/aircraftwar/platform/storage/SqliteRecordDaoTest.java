package edu.hitsz.aircraftwar.platform.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.hitsz.aircraftwar.core.record.GameRecord;

/**
 * SQLite 实现的设备侧契约测试：JVM 单测跑不了 {@code android.database}，所以放到 AVD 上跑。
 *
 * <p>用独立的 {@code test_records.db}，不碰正式的 {@code aircraftwar.db}；
 * 每个用例前删库重建，保证顺序断言不受历史数据影响。
 */
@RunWith(AndroidJUnit4.class)
public class SqliteRecordDaoTest {

    private static final String TEST_DB = "test_records.db";

    private Context context;
    private RecordDbHelper helper;
    private SqliteRecordDao dao;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DB);
        helper = new RecordDbHelper(context, TEST_DB);
        dao = new SqliteRecordDao(helper);
    }

    @After
    public void tearDown() {
        helper.close();
        context.deleteDatabase(TEST_DB);
    }

    @Test
    public void addAndReopenKeepsRecord() {
        dao.addRecord(record("Alice", 120, 2, 1_000L));

        helper.close();
        helper = new RecordDbHelper(context, TEST_DB);
        dao = new SqliteRecordDao(helper);

        List<GameRecord> all = dao.getAllRecords();
        assertEquals(1, all.size());
        assertEquals("Alice", all.get(0).getName());
        assertEquals(120, all.get(0).getScore());
        assertEquals(1, all.get(0).getId());
    }

    @Test
    public void rankingAllOrdersByScoreThenEarlierFirst() {
        dao.addRecord(record("low", 100, 2, 3_000L));
        dao.addRecord(record("late", 200, 1, 2_000L));
        dao.addRecord(record("early", 200, 3, 1_000L));

        assertEquals(Arrays.asList("early", "late", "low"), names(dao.getRankingRecords()));
    }

    @Test
    public void rankingByDifficultyFiltersAndSorts() {
        dao.addRecord(record("normal-high", 300, 2, 1_000L));
        dao.addRecord(record("normal-low", 100, 2, 2_000L));
        dao.addRecord(record("hard", 999, 3, 3_000L));

        List<GameRecord> normal = dao.getRecordsByInitialDifficulty(2);

        assertEquals(Arrays.asList("normal-high", "normal-low"), names(normal));
        assertTrue(normal.stream().allMatch(r -> r.getInitialDifficulty() == 2));
    }

    @Test
    public void allRecordsOrderByRecordTimeDesc() {
        dao.addRecord(record("older", 10, 1, 1_000L));
        dao.addRecord(record("newer", 20, 1, 2_000L));

        assertEquals(Arrays.asList("newer", "older"), names(dao.getAllRecords()));
    }

    @Test
    public void removeKeepsOtherIdsStable() {
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
    public void clearRemovesEverything() {
        dao.addRecord(record("one", 10, 1, 1_000L));
        dao.addRecord(record("two", 20, 1, 2_000L));

        dao.clearAllRecords();

        assertEquals(0, dao.getAllRecords().size());
    }

    @Test
    public void bestScoreIsZeroWhenEmptyAndMaxOtherwise() {
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
        List<GameRecord> batch = new ArrayList<>();
        batch.add(record("a", 1, 1, 1_000L));
        batch.add(record("b", 2, 1, 2_000L));

        dao.addRecords(batch);

        assertEquals(1, batch.get(0).getId());
        assertEquals(2, batch.get(1).getId());
        assertEquals(2, dao.getAllRecords().size());
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

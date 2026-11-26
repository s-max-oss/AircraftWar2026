package edu.hitsz.aircraftwar.platform.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.core.record.GameRecordDao;

/**
 * {@link GameRecordDao} 的文件实现，**只作对照/教学**，正式存储是 {@link SqliteRecordDao}。
 *
 * <p>它刻意保留桌面版 {@code GameRecordDaoImpl} 的做法：整表 Java 序列化进一个文件。
 * 这样答辩时能直接展示 {@code 实验4.pdf} p14 说的三个局限：
 * 改一条要整文件读改写、查询要全量进内存再排序、中途写坏会丢整表。
 *
 * <p>与桌面版的一处改进：删除后不再重排 id（id 是稳定主键，排名由查询顺序决定）。
 *
 * <p>构造参数是目录 {@link File} 而不是 Context，所以这个类可以在 JVM 单测里用临时目录
 * 直接跑，不需要模拟器。注意：{@link GameRecord} 没有显式 {@code serialVersionUID}，
 * 升级字段后旧文件会读不出来——这也正是文件存储不适合当正式方案的证据。
 */
public class FileRecordDao implements GameRecordDao {

    private static final Logger LOGGER = Logger.getLogger(FileRecordDao.class.getName());
    private static final String FILE_NAME = "game_records.dat";

    private static final Comparator<GameRecord> ALL_ORDER =
            Comparator.comparingLong(GameRecord::getRecordTime).reversed()
                    .thenComparing(Comparator.comparingInt(GameRecord::getId).reversed());

    private static final Comparator<GameRecord> RANKING_ORDER =
            Comparator.comparingInt(GameRecord::getScore).reversed()
                    .thenComparingLong(GameRecord::getRecordTime)
                    .thenComparingInt(GameRecord::getId);

    private final File file;
    private List<GameRecord> records;

    public FileRecordDao(File directory) {
        this.file = new File(directory, FILE_NAME);
    }

    @Override
    public synchronized void addRecord(GameRecord record) {
        List<GameRecord> list = records();
        record.setId(nextId(list));
        list.add(record);
        save(list);
    }

    @Override
    public synchronized void addRecords(List<GameRecord> additions) {
        if (additions == null || additions.isEmpty()) {
            return;
        }
        List<GameRecord> list = records();
        int id = nextId(list);
        for (GameRecord record : additions) {
            record.setId(id++);
            list.add(record);
        }
        save(list);
    }

    @Override
    public synchronized void removeRecord(int id) {
        List<GameRecord> list = records();
        list.removeIf(record -> record.getId() == id);
        save(list);
    }

    @Override
    public synchronized List<GameRecord> getAllRecords() {
        List<GameRecord> copy = new ArrayList<>(records());
        copy.sort(ALL_ORDER);
        return copy;
    }

    @Override
    public synchronized List<GameRecord> getRecordsByInitialDifficulty(int initialDifficulty) {
        List<GameRecord> filtered = new ArrayList<>();
        for (GameRecord record : records()) {
            if (record.getInitialDifficulty() == initialDifficulty) {
                filtered.add(record);
            }
        }
        filtered.sort(RANKING_ORDER);
        return filtered;
    }

    @Override
    public synchronized List<GameRecord> getRankingRecords() {
        List<GameRecord> copy = new ArrayList<>(records());
        copy.sort(RANKING_ORDER);
        return copy;
    }

    @Override
    public synchronized int getBestScore(int initialDifficulty) {
        int best = 0;
        for (GameRecord record : records()) {
            if (record.getInitialDifficulty() == initialDifficulty) {
                best = Math.max(best, record.getScore());
            }
        }
        return best;
    }

    @Override
    public synchronized void clearAllRecords() {
        records().clear();
        save(records());
    }

    private List<GameRecord> records() {
        if (records == null) {
            records = load();
        }
        return records;
    }

    @SuppressWarnings("unchecked")
    private List<GameRecord> load() {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object value = input.readObject();
            if (value instanceof List<?>) {
                return new ArrayList<>((List<GameRecord>) value);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            // 文件损坏/类版本变化时不让游戏崩，按空表处理；正式方案用 SQLite 避免这类整表故障。
            LOGGER.log(Level.WARNING, "读取文件记录失败，按空表处理: " + file, e);
        }
        return new ArrayList<>();
    }

    private void save(List<GameRecord> list) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            LOGGER.warning("创建记录目录失败: " + parent);
            return;
        }
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(new ArrayList<>(list));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "写入文件记录失败: " + file, e);
        }
    }

    private static int nextId(List<GameRecord> list) {
        int max = 0;
        for (GameRecord record : list) {
            max = Math.max(max, record.getId());
        }
        return max + 1;
    }
}

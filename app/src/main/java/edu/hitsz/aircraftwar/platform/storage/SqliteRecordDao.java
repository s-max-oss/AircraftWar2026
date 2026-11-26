package edu.hitsz.aircraftwar.platform.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.aircraftwar.core.record.GameRecord;
import edu.hitsz.aircraftwar.core.record.GameRecordDao;

/**
 * {@link GameRecordDao} 的 SQLite 实现（{@code 实验4.pdf} p14–p16 的正式存储）。
 *
 * <p>与老师 {@code SQLiteDemo} 的区别（答辩时可直接对照）：
 * <ul>
 *   <li>查询用 {@code whereArgs} 占位，不把参数拼进 SQL；</li>
 *   <li>Cursor 放在 try-with-resources 里，异常路径也会关闭；</li>
 *   <li>批量写入包在一个事务里，1000 条不会逐条提交；</li>
 *   <li>不静态持有 Activity，{@link RecordDbHelper} 只拿 applicationContext。</li>
 * </ul>
 *
 * <p>线程约定：本类不做异步，调用方（ViewModel 的 Executor）负责不在主线程调用。
 *
 * <p>本工程不提供“修改成绩”的入口，因此 DAO 契约只有增/查/删；课件 p16 的
 * {@code update} 是通用 API，这里不为凑 CRUD 加一个用不到的写接口——答辩时说明这个取舍。
 */
public class SqliteRecordDao implements GameRecordDao {

    private final RecordDbHelper helper;

    public SqliteRecordDao(Context context) {
        this(RecordDbHelper.getInstance(context));
    }

    /** 测试可注入独立库；正式代码走上面的 Context 入口。 */
    public SqliteRecordDao(RecordDbHelper helper) {
        this.helper = helper;
    }

    @Override
    public void addRecord(GameRecord record) {
        // ContentValues 是 Android 对“一行键值”的封装（课件 p16 的 insert 参数）；
        // insertOrThrow 失败直接抛异常，不把 -1 悄悄带给上层。
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insertOrThrow(RecordDbHelper.TABLE, null, toValues(record));
        record.setId((int) id);
    }

    @Override
    public void addRecords(List<GameRecord> records) {
        // 课件 p16 的事务：beginTransaction → setTransactionSuccessful → endTransaction。
        // 1000 条测试数据必须一次提交，否则每条一次 fsync 会明显慢。
        if (records == null || records.isEmpty()) {
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (GameRecord record : records) {
                long id = db.insertOrThrow(RecordDbHelper.TABLE, null, toValues(record));
                record.setId((int) id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void removeRecord(int id) {
        // whereArgs 占位，不把 id 拼进 SQL；与老师 demo 的字符串拼接形成对照。
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(RecordDbHelper.TABLE, RecordDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    @Override
    public List<GameRecord> getAllRecords() {
        // 记录页：全部流水按“最近打的在前”；排行榜用下面的 getRankingRecords。
        return query(null, null, RecordDbHelper.COL_RECORD_TIME + " DESC, "
                + RecordDbHelper.COL_ID + " DESC");
    }

    @Override
    public List<GameRecord> getRecordsByInitialDifficulty(int initialDifficulty) {
        // 课件 p16 的 orderBy：score DESC 直接得到排行榜顺序；同分用 recordTime/id
        // 做稳定 tie-breaker，避免刷新时同分行来回跳。
        return query(RecordDbHelper.COL_INITIAL_DIFFICULTY + "=?",
                new String[]{String.valueOf(initialDifficulty)},
                RecordDbHelper.COL_SCORE + " DESC, "
                        + RecordDbHelper.COL_RECORD_TIME + " ASC, "
                        + RecordDbHelper.COL_ID + " ASC");
    }

    @Override
    public List<GameRecord> getRankingRecords() {
        // “全部难度”排行榜：不筛选，仍按分数降序；难度列由行内文案展示。
        return query(null, null, RecordDbHelper.COL_SCORE + " DESC, "
                + RecordDbHelper.COL_RECORD_TIME + " ASC, "
                + RecordDbHelper.COL_ID + " ASC");
    }

    @Override
    public int getBestScore(int initialDifficulty) {
        // 用 SQL 的 MAX 聚合，不把整表读进内存再取最大值（对比文件实现的循环）。
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT MAX(" + RecordDbHelper.COL_SCORE + ") FROM " + RecordDbHelper.TABLE
                + " WHERE " + RecordDbHelper.COL_INITIAL_DIFFICULTY + "=?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(initialDifficulty)})) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getInt(0);
            }
            return 0;
        }
    }

    @Override
    public void clearAllRecords() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(RecordDbHelper.TABLE, null, null);
    }

    private List<GameRecord> query(String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<GameRecord> records = new ArrayList<>();
        // try-with-resources 保证异常路径也 close()；对比老师 demo 的 cursor.close() 只在正常路径。
        try (Cursor cursor = db.query(RecordDbHelper.TABLE, null, selection, selectionArgs,
                null, null, orderBy)) {
            while (cursor.moveToNext()) {
                records.add(fromCursor(cursor));
            }
        }
        return records;
    }

    private static ContentValues toValues(GameRecord record) {
        ContentValues values = new ContentValues();
        values.put(RecordDbHelper.COL_NAME, record.getName() == null ? "" : record.getName());
        values.put(RecordDbHelper.COL_SCORE, record.getScore());
        values.put(RecordDbHelper.COL_ENEMIES_KILLED, record.getEnemiesKilled());
        values.put(RecordDbHelper.COL_GAME_TIME, record.getGameTime());
        values.put(RecordDbHelper.COL_RECORD_TIME, record.getRecordTime());
        values.put(RecordDbHelper.COL_DIFFICULTY, record.getDifficulty());
        values.put(RecordDbHelper.COL_INITIAL_DIFFICULTY, record.getInitialDifficulty());
        // synced 用建表默认值 0；S4 上传成功后由同步层更新，不在本阶段写。
        return values;
    }

    private static GameRecord fromCursor(Cursor cursor) {
        GameRecord record = new GameRecord();
        record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(RecordDbHelper.COL_ID)));
        record.setName(cursor.getString(cursor.getColumnIndexOrThrow(RecordDbHelper.COL_NAME)));
        record.setScore(cursor.getInt(cursor.getColumnIndexOrThrow(RecordDbHelper.COL_SCORE)));
        record.setEnemiesKilled(cursor.getInt(
                cursor.getColumnIndexOrThrow(RecordDbHelper.COL_ENEMIES_KILLED)));
        record.setGameTime(cursor.getLong(
                cursor.getColumnIndexOrThrow(RecordDbHelper.COL_GAME_TIME)));
        record.setRecordTime(cursor.getLong(
                cursor.getColumnIndexOrThrow(RecordDbHelper.COL_RECORD_TIME)));
        record.setDifficulty(cursor.getInt(
                cursor.getColumnIndexOrThrow(RecordDbHelper.COL_DIFFICULTY)));
        record.setInitialDifficulty(cursor.getInt(
                cursor.getColumnIndexOrThrow(RecordDbHelper.COL_INITIAL_DIFFICULTY)));
        return record;
    }
}

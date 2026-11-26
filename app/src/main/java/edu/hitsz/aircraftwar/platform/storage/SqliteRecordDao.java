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
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insertOrThrow(RecordDbHelper.TABLE, null, toValues(record));
        record.setId((int) id);
    }

    @Override
    public void addRecords(List<GameRecord> records) {
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
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(RecordDbHelper.TABLE, RecordDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    @Override
    public List<GameRecord> getAllRecords() {
        return query(null, null, RecordDbHelper.COL_RECORD_TIME + " DESC, "
                + RecordDbHelper.COL_ID + " DESC");
    }

    @Override
    public List<GameRecord> getRecordsByInitialDifficulty(int initialDifficulty) {
        return query(RecordDbHelper.COL_INITIAL_DIFFICULTY + "=?",
                new String[]{String.valueOf(initialDifficulty)},
                RecordDbHelper.COL_SCORE + " DESC, "
                        + RecordDbHelper.COL_RECORD_TIME + " ASC, "
                        + RecordDbHelper.COL_ID + " ASC");
    }

    @Override
    public List<GameRecord> getRankingRecords() {
        return query(null, null, RecordDbHelper.COL_SCORE + " DESC, "
                + RecordDbHelper.COL_RECORD_TIME + " ASC, "
                + RecordDbHelper.COL_ID + " ASC");
    }

    @Override
    public int getBestScore(int initialDifficulty) {
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

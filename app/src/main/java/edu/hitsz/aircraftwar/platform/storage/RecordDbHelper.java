package edu.hitsz.aircraftwar.platform.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * 记录库的建库/升级入口（{@code 实验4.pdf} p15 的 {@code SQLiteOpenHelper} 用法）。
 *
 * <p>为什么用 applicationContext 做进程级单例：{@code SQLiteOpenHelper} 内部缓存了
 * {@code SQLiteDatabase}，每 new 一次都会多一条连接；用 Activity 又会泄漏。这里只存
 * applicationContext，既保证全进程一个连接池，也不把页面带进静态字段。
 *
 * <p>数据库文件落在应用沙箱：{@code /data/data/edu.hitsz.aircraftwar/databases/aircraftwar.db}
 * （课件 p13/p15 的 Device Explorer 路径）。代码不硬编码这个绝对路径，而是交给
 * {@code SQLiteOpenHelper} 按包名创建。
 *
 * <p>为什么 {@code _id} 用 AUTOINCREMENT 且删除后不重排：桌面版把 id 当“序号”，删除一条后
 * 把后面所有 id 减一，列表刷新时行号会跳；排行榜的“排名”应该是查询结果的位置，不该落库。
 * 稳定的 {@code _id} 也让删除/更新有可靠主键。
 */
public class RecordDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "RecordDbHelper";

    public static final String DB_NAME = "aircraftwar.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE = "game_record";
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_SCORE = "score";
    public static final String COL_ENEMIES_KILLED = "enemiesKilled";
    public static final String COL_GAME_TIME = "gameTime";
    public static final String COL_RECORD_TIME = "recordTime";
    public static final String COL_DIFFICULTY = "difficulty";
    public static final String COL_INITIAL_DIFFICULTY = "initialDifficulty";
    /** S4 云榜上传标记；本阶段恒为 0，建表时就留着，避免以后再加列。 */
    public static final String COL_SYNCED = "synced";

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COL_NAME + " TEXT NOT NULL,"
                    + COL_SCORE + " INTEGER NOT NULL,"
                    + COL_ENEMIES_KILLED + " INTEGER NOT NULL,"
                    + COL_GAME_TIME + " INTEGER NOT NULL,"
                    + COL_RECORD_TIME + " INTEGER NOT NULL,"
                    + COL_DIFFICULTY + " INTEGER NOT NULL,"
                    + COL_INITIAL_DIFFICULTY + " INTEGER NOT NULL,"
                    + COL_SYNCED + " INTEGER NOT NULL DEFAULT 0"
                    + ");";

    private static final String CREATE_RANKING_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_record_difficulty_score ON " + TABLE
                    + "(" + COL_INITIAL_DIFFICULTY + ", " + COL_SCORE + " DESC);";

    private static volatile RecordDbHelper instance;

    private RecordDbHelper(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /** 供 instrumentation 测试用独立库文件，避免污染真实记录；正式代码只用上面的单例入口。 */
    public RecordDbHelper(@NonNull Context context, @NonNull String dbName) {
        super(context, dbName, null, DB_VERSION);
    }

    public static RecordDbHelper getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (RecordDbHelper.class) {
                if (instance == null) {
                    instance = new RecordDbHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // onCreate 只在数据库文件第一次创建时调一次；表结构用显式 TEXT/INTEGER，
        // 不依赖 SQLite 的动态类型，列名与 GameRecord 字段一一对应，便于答辩时对着讲。
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_RANKING_INDEX);
        Log.i(TAG, "onCreate: " + DB_NAME + " v" + DB_VERSION);
    }

    /**
     * 升级钩子。当前只有 v1，不写 DROP：{@code 实验4.pdf} p15 的示例用“删表重建”，
     * 那会把玩家记录清空；真实升级应按 oldVersion 逐版 ALTER/迁移，失败可回滚。
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "onUpgrade " + oldVersion + " -> " + newVersion + "：暂无迁移脚本");
    }
}

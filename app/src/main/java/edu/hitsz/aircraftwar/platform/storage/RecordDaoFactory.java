package edu.hitsz.aircraftwar.platform.storage;

import android.content.Context;

import edu.hitsz.aircraftwar.core.record.GameRecordDao;

/**
 * 选择 {@link GameRecordDao} 的实现：正式用 SQLite，文件实现作对照。
 *
 * <p>课件 p14 的原话是“文件存储与数据库存储对外提供同一套 DAO 接口，只需替换实现类，
 * Activity 与 Adapter 无需改动”。本工程把这个开关收在这一行：把 {@link #USE_FILE_STORE}
 * 改成 {@code true}，界面层一行都不用动（数据当然会换成另一个文件里的）。
 */
public final class RecordDaoFactory {

    /** 对照实现开关；正式构建保持 false。 */
    private static final boolean USE_FILE_STORE = false;

    private RecordDaoFactory() {
    }

    public static GameRecordDao create(Context context) {
        Context appContext = context.getApplicationContext();
        if (USE_FILE_STORE) {
            return new FileRecordDao(appContext.getFilesDir());
        }
        return new SqliteRecordDao(appContext);
    }
}

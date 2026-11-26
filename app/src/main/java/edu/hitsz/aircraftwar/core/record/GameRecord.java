package edu.hitsz.aircraftwar.core.record;

/**
 * 一局游戏的成绩记录。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.dao.GameRecord}（commit 50474cc），8 个字段与序列化能力保留。
 * S3 起持久化改为 SQLite（{@code platform/storage/SqliteRecordDao}），本类仍是纯数据对象；
 * 难度中文名统一由 UI 的 {@code DifficultyLabels} 映射，这里不再保留两套不一致的 switch。
 *
 * @author hitsz
 */
public class GameRecord implements java.io.Serializable {

    private int id;
    private String name;
    private int score;
    private int enemiesKilled;
    private long gameTime;
    private long recordTime;
    /** 终局等级 1–5（随游戏时间提升）。 */
    private int difficulty;
    /** 开局档位 1/2/3（主菜单选择），排行榜按它筛选。 */
    private int initialDifficulty;

    public GameRecord() {
    }

    public GameRecord(int id, String name, int score, int enemiesKilled, long gameTime,
                      long recordTime, int difficulty, int initialDifficulty) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.enemiesKilled = enemiesKilled;
        this.gameTime = gameTime;
        this.recordTime = recordTime;
        this.difficulty = difficulty;
        this.initialDifficulty = initialDifficulty;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getEnemiesKilled() {
        return enemiesKilled;
    }

    public void setEnemiesKilled(int enemiesKilled) {
        this.enemiesKilled = enemiesKilled;
    }

    public long getGameTime() {
        return gameTime;
    }

    public void setGameTime(long gameTime) {
        this.gameTime = gameTime;
    }

    public long getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(long recordTime) {
        this.recordTime = recordTime;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getInitialDifficulty() {
        return initialDifficulty;
    }

    public void setInitialDifficulty(int initialDifficulty) {
        this.initialDifficulty = initialDifficulty;
    }

    @Override
    public String toString() {
        return "GameRecord{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", score=" + score +
                ", enemiesKilled=" + enemiesKilled +
                ", gameTime=" + gameTime +
                ", recordTime=" + recordTime +
                ", difficulty=" + difficulty +
                ", initialDifficulty=" + initialDifficulty +
                '}';
    }
}

package edu.hitsz.aircraftwar.core.record;

/**
 * 一局游戏的成绩记录。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.dao.GameRecord}（commit 50474cc），字段与难度名映射原样保留。
 * S3 换 Room 时把 {@code Serializable} 去掉、加 {@code @Entity}，字段语义不变。
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
    private int difficulty;
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

    public String getInitialDifficultyName() {
        return difficultyName(initialDifficulty);
    }

    public String getDifficultyName() {
        return difficultyName(difficulty);
    }

    /**
     * 难度等级到中文名的映射。原实现两份 switch 完全相同，这里合并为一个私有方法，
     * 映射内容一字未动（注意 1→普通、2/3→困难、4/5→噩梦 的不对称是原样保留）。
     */
    private static String difficultyName(int level) {
        switch (level) {
            case 1:
                return "普通";
            case 2:
                return "困难";
            case 3:
                return "困难";
            case 4:
                return "噩梦";
            case 5:
                return "噩梦";
            default:
                return "普通";
        }
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

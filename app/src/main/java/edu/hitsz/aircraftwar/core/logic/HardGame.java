package edu.hitsz.aircraftwar.core.logic;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;
import edu.hitsz.aircraftwar.core.aircraft.factory.EnemyAircraftFactory;
import edu.hitsz.aircraftwar.core.aircraft.factory.EnemyAircraftFactoryManager;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 困难难度：20s 起升难度，Boss 血量随召唤次数递增、门槛 +400。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.application.HardGame}（commit 50474cc）。
 * Boss 血量公式 {@code 400 + 100 × 次数} 改用 GameConfig 的基准与增量，数值不变。
 *
 * @author hitsz
 */
public class HardGame extends GameLogic {

    private int bossSpawnCount = 0;
    private boolean bossWarningShown = false;

    public HardGame(String userName, GameEvents events) {
        super(userName, 3, events);
        initializeDifficultySettings();
    }

    private void initializeDifficultySettings() {
        baseEnemyPower = 70;
        enemyMaxNumber = 15;
        eliteEnemyMaxNumber = 8;
        elitePlusEnemyMaxNumber = 4;
        enemySpawnCycle = GameConfig.HARD_ENEMY_SPAWN_CYCLE;
        eliteEnemySpawnCycle = GameConfig.HARD_ELITE_SPAWN_CYCLE;
        elitePlusEnemySpawnCycle = GameConfig.HARD_ELITE_PLUS_SPAWN_CYCLE;
        eliteProEnemySpawnCycle = GameConfig.HARD_ELITE_PRO_SPAWN_CYCLE;
        bossScoreThreshold = GameConfig.BOSS_SCORE_THRESHOLD_HARD;
        backgroundIndex = 2;
    }

    @Override
    protected void spawnEnemyAircrafts() {
        if (enemyAircrafts.size() < enemyMaxNumber) {
            int x = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - GameConfig.Sprite.MOB.width));
            int y = 0;
            EnemyAircraftFactory factory = EnemyAircraftFactoryManager.getFactory("MobEnemy");
            enemyAircrafts.add(factory.createAircraft(x, y, difficultyLevel));
        }

        eliteEnemySpawnCounter++;
        if (eliteEnemySpawnCounter >= eliteEnemySpawnCycle) {
            eliteEnemySpawnCounter = 0;
            if (enemyAircrafts.size() < eliteEnemyMaxNumber + enemyMaxNumber) {
                int x = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - GameConfig.Sprite.ELITE.width));
                int y = 0;
                EnemyAircraftFactory factory = EnemyAircraftFactoryManager.getFactory("EliteEnemy");
                enemyAircrafts.add(factory.createAircraft(x, y, difficultyLevel));
            }
        }

        elitePlusEnemySpawnCounter++;
        if (elitePlusEnemySpawnCounter >= elitePlusEnemySpawnCycle) {
            elitePlusEnemySpawnCounter = 0;
            if (enemyAircrafts.size() < elitePlusEnemyMaxNumber + eliteEnemyMaxNumber + enemyMaxNumber) {
                int x = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - GameConfig.Sprite.ELITE_PLUS.width));
                int y = 0;
                EnemyAircraftFactory factory = EnemyAircraftFactoryManager.getFactory("ElitePlusEnemy");
                enemyAircrafts.add(factory.createAircraft(x, y, difficultyLevel));
            }
        }

        eliteProEnemySpawnCounter++;
        if (eliteProEnemySpawnCounter >= eliteProEnemySpawnCycle) {
            eliteProEnemySpawnCounter = 0;
            if (enemyAircrafts.size() < eliteProEnemyMaxNumber + elitePlusEnemyMaxNumber
                    + eliteEnemyMaxNumber + enemyMaxNumber) {
                int x = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - GameConfig.Sprite.ELITE_PRO.width));
                int y = 0;
                EnemyAircraftFactory factory = EnemyAircraftFactoryManager.getFactory("EliteProEnemy");
                enemyAircrafts.add(factory.createAircraft(x, y, difficultyLevel));
            }
        }

        // 生成 Boss
        if (canSpawnBoss()) {
            int bossCount = 0;
            for (AbstractAircraft aircraft : enemyAircrafts) {
                if (aircraft instanceof Boss) {
                    bossCount++;
                }
            }
            if (bossCount < bossMaxNumber && score >= lastBossScore + bossScoreThreshold) {
                int x = (int) (Math.random() * (GameConfig.WINDOW_WIDTH - GameConfig.Sprite.BOSS.width));
                int y = 0;
                Boss boss = new Boss(x, y, 3, 0, getBossHp());
                enemyAircrafts.add(boss);
                lastBossScore = score;
                bossScoreThreshold += GameConfig.BOSS_SCORE_STEP_HARD;
                bossSpawnCount++;
                events.onBossBgmStart();
                bossWarningShown = false;
            }
        }
    }

    @Override
    protected void updateDifficultyLevel(long gameTime) {
        int newDifficulty = 3;

        if (gameTime >= 20 && gameTime < 90) {
            newDifficulty = 4;
            backgroundIndex = 2;
        } else if (gameTime >= 90 && gameTime < 240) {
            newDifficulty = 5;
            backgroundIndex = 4;
        }

        if (newDifficulty > difficultyLevel) {
            difficultyLevel = newDifficulty;
            adjustDifficultySettings(newDifficulty);
        }
    }

    private void adjustDifficultySettings(int level) {
        switch (level) {
            case 4:
                baseEnemyPower = 70;
                enemyMaxNumber = 14;
                eliteEnemyMaxNumber = 8;
                break;
            case 5:
                baseEnemyPower = 90;
                enemyMaxNumber = 16;
                eliteEnemyMaxNumber = 10;
                break;
            default:
                break;
        }
    }

    @Override
    protected void printDifficultyInfo() {
        if (!bossWarningShown && score >= lastBossScore + bossScoreThreshold - GameConfig.BOSS_WARNING_LEAD
                && score < lastBossScore + bossScoreThreshold) {
            bossWarningShown = true;
        }
    }

    @Override
    protected boolean canSpawnBoss() {
        return true;
    }

    @Override
    protected int getBossHp() {
        // 困难难度 Boss 血量随召唤次数增加
        return GameConfig.BOSS_HP_HARD_BASE + bossSpawnCount * GameConfig.BOSS_HP_HARD_STEP;
    }
}

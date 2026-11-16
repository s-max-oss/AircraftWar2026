package edu.hitsz.aircraftwar.core.logic;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;
import edu.hitsz.aircraftwar.core.aircraft.factory.EnemyAircraftFactory;
import edu.hitsz.aircraftwar.core.aircraft.factory.EnemyAircraftFactoryManager;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 普通难度：30s/120s/300s 三段升难度，Boss 血量固定、每次召唤门槛 +500。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.application.NormalGame}（commit 50474cc）。
 * 生成周期与 Boss 门槛改由 {@link GameConfig} 提供；攻防数值留到 S2 Profile 化。
 *
 * @author hitsz
 */
public class NormalGame extends GameLogic {

    private int bossSpawnCount = 0;
    private boolean bossWarningShown = false;

    public NormalGame(String userName, GameEvents events) {
        super(userName, 2, events);
        initializeDifficultySettings();
    }

    private void initializeDifficultySettings() {
        baseEnemyPower = 50;
        enemyMaxNumber = 12;
        eliteEnemyMaxNumber = 6;
        elitePlusEnemyMaxNumber = 3;
        enemySpawnCycle = GameConfig.NORMAL_ENEMY_SPAWN_CYCLE;
        eliteEnemySpawnCycle = GameConfig.NORMAL_ELITE_SPAWN_CYCLE;
        elitePlusEnemySpawnCycle = GameConfig.NORMAL_ELITE_PLUS_SPAWN_CYCLE;
        backgroundIndex = 1;
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
                Boss boss = new Boss(x, y, 2, 0, getBossHp());
                enemyAircrafts.add(boss);
                lastBossScore = score;
                bossScoreThreshold += GameConfig.BOSS_SCORE_STEP_NORMAL;
                bossSpawnCount++;
                events.onBossBgmStart();
                bossWarningShown = false;
            }
        }
    }

    @Override
    protected void updateDifficultyLevel(long gameTime) {
        int newDifficulty = 2;

        if (gameTime >= 30 && gameTime < 120) {
            newDifficulty = 3;
            backgroundIndex = 1;
        } else if (gameTime >= 120 && gameTime < 300) {
            newDifficulty = 4;
            backgroundIndex = 2;
        } else if (gameTime >= 300) {
            newDifficulty = 5;
            backgroundIndex = 3;
        }

        if (newDifficulty > difficultyLevel) {
            difficultyLevel = newDifficulty;
            adjustDifficultySettings(newDifficulty);
        }
    }

    private void adjustDifficultySettings(int level) {
        switch (level) {
            case 3:
                baseEnemyPower = 50;
                enemyMaxNumber = 11;
                eliteEnemyMaxNumber = 6;
                break;
            case 4:
                baseEnemyPower = 60;
                enemyMaxNumber = 12;
                eliteEnemyMaxNumber = 7;
                break;
            case 5:
                baseEnemyPower = 70;
                enemyMaxNumber = 13;
                eliteEnemyMaxNumber = 8;
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
        // 普通难度 Boss 血量固定
        return GameConfig.BOSS_HP_NORMAL;
    }
}

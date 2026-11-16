package edu.hitsz.aircraftwar.core.logic;

import edu.hitsz.aircraftwar.core.aircraft.EnemyAircraft;
import edu.hitsz.aircraftwar.core.aircraft.factory.EnemyAircraftFactory;
import edu.hitsz.aircraftwar.core.aircraft.factory.EnemyAircraftFactoryManager;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 简单难度：不召唤 Boss、不升难度、敌机只有普通与精英两种。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.application.SimpleGame}（commit 50474cc）。
 * 生成周期改由 {@link GameConfig} 提供；攻防数值（35/10/4/2）按计划留到 S2 的
 * 难度 Profile 化重构，S1 保持原值。
 *
 * @author hitsz
 */
public class SimpleGame extends GameLogic {

    public SimpleGame(String userName, GameEvents events) {
        super(userName, 1, events);
        initializeDifficultySettings();
    }

    private void initializeDifficultySettings() {
        baseEnemyPower = 35;
        enemyMaxNumber = 10;
        eliteEnemyMaxNumber = 4;
        elitePlusEnemyMaxNumber = 2;
        enemySpawnCycle = GameConfig.SIMPLE_ENEMY_SPAWN_CYCLE;
        eliteEnemySpawnCycle = GameConfig.SIMPLE_ELITE_SPAWN_CYCLE;
        elitePlusEnemySpawnCycle = GameConfig.SIMPLE_ELITE_PLUS_SPAWN_CYCLE;
        backgroundIndex = 0;
    }

    @Override
    protected void spawnEnemyAircrafts() {
        // 简单难度只生成普通敌机和精英敌机
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
    }

    @Override
    protected void updateDifficultyLevel(long gameTime) {
        // 简单难度不提升难度等级
        difficultyLevel = 1;
        backgroundIndex = 0;
    }

    @Override
    protected void printDifficultyInfo() {
        // 简单难度不输出额外信息
    }

    @Override
    protected boolean canSpawnBoss() {
        // 简单难度不允许生成 Boss
        return false;
    }

    @Override
    protected int getBossHp() {
        return 0;
    }
}

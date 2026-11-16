package edu.hitsz.aircraftwar.core.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 难度档配置回归测试：初始等级、Boss 血量公式、生成周期来源。
 *
 * <p>放在 {@code core.logic} 包里，才能访问 {@code protected} 的 {@code getBossHp()}。
 */
public class DifficultyTest {

    /** 空事件实现：这里只验证难度数值，不关心音效/结算。 */
    private static final GameEvents NO_OP = new GameEvents() {
        @Override
        public void onBgmStart() {
        }

        @Override
        public void onHit() {
        }

        @Override
        public void onPowerUp() {
        }

        @Override
        public void onBomb() {
        }

        @Override
        public void onLevelUp(int level, int hpBonus, int powerBonus) {
        }

        @Override
        public void onBossBgmStart() {
        }

        @Override
        public void onBossBgmStop() {
        }

        @Override
        public void onGameOver() {
        }
    };

    @Test
    public void initialDifficultyMatchesSource() {
        assertEquals(1, new SimpleGame("t", NO_OP).getDifficultyLevel());
        assertEquals(2, new NormalGame("t", NO_OP).getDifficultyLevel());
        assertEquals(3, new HardGame("t", NO_OP).getDifficultyLevel());
    }

    @Test
    public void bossHpMatchesSourceFormula() {
        assertEquals(0, new SimpleGame("t", NO_OP).getBossHp());
        assertEquals(GameConfig.BOSS_HP_NORMAL, new NormalGame("t", NO_OP).getBossHp());
        // 困难难度：400 + 100 × 召唤次数，首次召唤前次数为 0
        assertEquals(GameConfig.BOSS_HP_HARD_BASE, new HardGame("t", NO_OP).getBossHp());
    }

    @Test
    public void spawnCyclesComeFromGameConfig() {
        assertEquals(16.0, GameConfig.NORMAL_ENEMY_SPAWN_CYCLE, 1e-9);
        assertEquals(45.0, GameConfig.NORMAL_ELITE_SPAWN_CYCLE, 1e-9);
        assertEquals(80.0, GameConfig.NORMAL_ELITE_PLUS_SPAWN_CYCLE, 1e-9);
        assertEquals(12.0, GameConfig.HARD_ENEMY_SPAWN_CYCLE, 1e-9);
        assertEquals(35.0, GameConfig.HARD_ELITE_SPAWN_CYCLE, 1e-9);
        assertEquals(60.0, GameConfig.HARD_ELITE_PLUS_SPAWN_CYCLE, 1e-9);
        assertEquals(100.0, GameConfig.HARD_ELITE_PRO_SPAWN_CYCLE, 1e-9);
    }

    @Test
    public void scoreTableMatchesSource() {
        assertEquals(10, GameConfig.SCORE_MOB);
        assertEquals(30, GameConfig.SCORE_ELITE);
        assertEquals(50, GameConfig.SCORE_ELITE_PLUS);
        assertEquals(100, GameConfig.SCORE_ELITE_PRO);
        assertEquals(500, GameConfig.SCORE_BOSS);
    }
}

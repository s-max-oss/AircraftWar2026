package edu.hitsz.aircraftwar.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.PlayerLevelManager;

/**
 * 等级成长回归测试：经验门槛、升级加血、攻速加成档位。
 */
public class PlayerLevelManagerTest {

    private HeroAircraft hero;
    private PlayerLevelManager manager;

    @Before
    public void setUp() {
        hero = HeroAircraft.getInstance();
        hero.reset();
        manager = PlayerLevelManager.getInstance();
        manager.reset();
    }

    @Test
    public void levelUpHappensExactlyAtThreshold() {
        manager.addExp(GameConfig.EXP_THRESHOLDS[1] - 1);
        assertEquals(1, manager.getLevel());
        manager.addExp(1);
        assertEquals(2, manager.getLevel());
    }

    @Test
    public void levelUpIncreasesHp() {
        int before = hero.getHp();
        manager.addExp(GameConfig.EXP_THRESHOLDS[1]);
        assertTrue("升级后当前血量应增加", hero.getHp() > before);
    }

    @Test
    public void expProgressReflectsCurrentLevel() {
        manager.addExp(300);
        assertEquals(2, manager.getLevel());
        assertEquals(300, manager.getExp());
        assertEquals(GameConfig.EXP_THRESHOLDS[2], manager.getCurrentThreshold());
        assertTrue(manager.getExpProgress() > 0 && manager.getExpProgress() < 1);
    }

    @Test
    public void shootSpeedBonusStartsAtLevelFive() {
        manager.addExp(GameConfig.EXP_THRESHOLDS[4]);
        assertEquals(5, manager.getLevel());
        assertEquals(GameConfig.SHOOT_SPEED_BONUS, manager.getShootSpeedBonus(), 1e-9);
    }
}

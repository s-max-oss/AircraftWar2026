package edu.hitsz.aircraftwar.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * "再来一局"的状态复位：S2 三页流转里最容易漏的一环。
 *
 * <p>桌面版靠 {@code HeroAircraft.resetInstance()} 丢单例来清状态；Android 侧不能丢
 * （{@code PlayerLevelManager} 还缓存着 {@code hero} 引用），所以新增
 * {@link HeroAircraft#resetForNewGame()}。这里锁住它清干净了哪些东西。
 */
public class HeroResetTest {

    @After
    public void tearDown() {
        HeroAircraft.getInstance().resetForNewGame();
    }

    @Test
    public void resetForNewGameRestoresMaxHp() {
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.increaseMaxHp(150);
        assertEquals(GameConfig.HERO_BASE_HP + 150, hero.getMaxHp());

        hero.resetForNewGame();

        assertEquals(GameConfig.HERO_BASE_HP, hero.getMaxHp());
        assertEquals(GameConfig.HERO_BASE_HP, hero.getHp());
    }

    @Test
    public void resetForNewGameRestoresPositionAndAlive() {
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.setLocation(10, 10);
        hero.decreaseHp(GameConfig.HERO_BASE_HP + 100);
        assertTrue(hero.notValid());

        hero.resetForNewGame();

        assertEquals(GameConfig.WINDOW_WIDTH / 2, hero.getLocationX());
        assertEquals(GameConfig.WINDOW_HEIGHT - GameConfig.HERO_RESET_BOTTOM_MARGIN, hero.getLocationY());
        assertTrue(hero.isValid());
    }

    @Test
    public void resetForNewGameClearsPowerUpBullets() {
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.increaseBulletNum(3);   // 火力道具：子弹数 1 → 4，并记下 originalShootNum

        assertEquals(4, hero.shoot().size());

        hero.resetForNewGame();

        // 复原后回到单发（散弹策略按 shootNum 出弹），说明道具状态没有跨局残留
        assertEquals(1, hero.shoot().size());
    }
}

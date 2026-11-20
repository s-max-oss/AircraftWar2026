package edu.hitsz.aircraftwar.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.aircraft.MobEnemy;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 视口注入（S2 修复黑边）：宽度仍是 512，高度按屏幕比例"长高"。
 *
 * <p>锁三件事：
 * <ol>
 *   <li>英雄机出生点跟着新视口贴底边，不会悬在半空；</li>
 *   <li>敌机在新的下边界才消失，不会在可见区域里凭空消失；</li>
 *   <li>纵向速度倍率换算正确（保证穿屏时间与桌面版一致）。</li>
 * </ol>
 */
public class ViewportTest {

    /** 1080×2400 屏幕换算出来的逻辑高度（512 × 2400 / 1080 ≈ 1138）。 */
    private static final int PHONE_VIEWPORT_HEIGHT = 1138;

    @After
    public void tearDown() {
        GameConfig.setViewportHeight(GameConfig.DESIGN_HEIGHT);
        HeroAircraft.getInstance().resetForNewGame();
    }

    @Test
    public void heroSitsRelativeToInjectedViewport() {
        GameConfig.setViewportHeight(PHONE_VIEWPORT_HEIGHT);

        HeroAircraft hero = HeroAircraft.getInstance();
        hero.resetForNewGame();

        assertEquals(PHONE_VIEWPORT_HEIGHT - GameConfig.HERO_RESET_BOTTOM_MARGIN, hero.getLocationY());
        assertEquals(GameConfig.WINDOW_WIDTH / 2, hero.getLocationX());
    }

    @Test
    public void enemyIsCulledAtInjectedViewportBottom() {
        GameConfig.setViewportHeight(PHONE_VIEWPORT_HEIGHT);
        MobEnemy enemy = new MobEnemy(100, 0, 0, 100, 30);

        // 走过桌面版的 768 位置时还在场上（旧视口会在这里误删）
        for (int i = 0; i < 8; i++) {
            enemy.forward();
        }
        assertTrue("过 768 就被回收说明视口没生效", enemy.isValid());
        assertEquals(800, enemy.getLocationY());

        // 越过新视口下界才消失
        for (int i = 0; i < 4; i++) {
            enemy.forward();
        }
        assertFalse(enemy.isValid());
    }

    @Test
    public void scaledSpeedFollowsViewport() {
        GameConfig.setViewportHeight(GameConfig.DESIGN_HEIGHT);
        assertEquals(1.0, GameConfig.verticalScale(), 0.0001);
        assertEquals(5, GameConfig.scaledSpeed(5));

        GameConfig.setViewportHeight(PHONE_VIEWPORT_HEIGHT);
        assertEquals(PHONE_VIEWPORT_HEIGHT / (double) GameConfig.DESIGN_HEIGHT,
                GameConfig.verticalScale(), 0.0001);
        // 5 × 1.4818 ≈ 7.41 → 7：敌机穿屏的"时间"与桌面版一致
        assertEquals(7, GameConfig.scaledSpeed(5));
    }
}

package edu.hitsz.aircraftwar.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.aircraft.MobEnemy;
import edu.hitsz.aircraftwar.core.bullet.EnemyBullet;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 碰撞判定的回归测试：保证迁移后"矩形框 + 飞机纵向收窄"的语义没变。
 */
public class CollisionTest {

    @Test
    public void bulletOverlappingEnemyCrashes() {
        MobEnemy enemy = new MobEnemy(100, 100, 0, 2, 30);
        EnemyBullet bullet = new EnemyBullet(100, 100, 0, 5, 10);
        assertTrue(enemy.crash(bullet));
    }

    @Test
    public void bulletFarFromEnemyDoesNotCrash() {
        MobEnemy enemy = new MobEnemy(100, 100, 0, 2, 30);
        EnemyBullet bullet = new EnemyBullet(300, 600, 0, 5, 10);
        assertFalse(enemy.crash(bullet));
    }

    @Test
    public void heroCrashesWithOverlappingEnemy() {
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.reset();
        MobEnemy enemy = new MobEnemy(hero.getLocationX(), hero.getLocationY(), 0, 2, 30);
        assertTrue(hero.crash(enemy));
    }

    @Test
    public void spriteSizeDrivesCollisionBox() {
        MobEnemy enemy = new MobEnemy(0, 0, 0, 0, 1);
        assertEquals(GameConfig.Sprite.MOB.width, enemy.getWidth());
        assertEquals(GameConfig.Sprite.MOB.height, enemy.getHeight());
    }

    @Test
    public void crashAtHorizontalEdgeOnlyJustFails() {
        MobEnemy enemy = new MobEnemy(256, 100, 0, 0, 1);
        int halfSum = (GameConfig.Sprite.MOB.width + GameConfig.Sprite.ENEMY_BULLET.width) / 2;
        EnemyBullet far = new EnemyBullet(256 + halfSum + 1, 100, 0, 0, 1);
        EnemyBullet near = new EnemyBullet(256 + halfSum - 1, 100, 0, 0, 1);
        assertFalse(enemy.crash(far));
        assertTrue(enemy.crash(near));
    }
}

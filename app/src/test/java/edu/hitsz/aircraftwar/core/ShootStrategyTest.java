package edu.hitsz.aircraftwar.core;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import edu.hitsz.aircraftwar.core.aircraft.Boss;
import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.strategy.RingShootStrategy;
import edu.hitsz.aircraftwar.core.strategy.ScatterShootStrategy;
import edu.hitsz.aircraftwar.core.strategy.StraightShootStrategy;

/**
 * 三种弹道策略的回归测试：子弹数量、发射点偏移、纵向速度方向。
 */
public class ShootStrategyTest {

    private HeroAircraft hero;

    @Before
    public void setUp() {
        hero = HeroAircraft.getInstance();
        hero.reset();
    }

    @Test
    public void straightShootFiresOneBulletUpwards() {
        List<BaseBullet> bullets = new StraightShootStrategy(1, 30, -1).shoot(hero);
        assertEquals(1, bullets.size());
        assertEquals(-5, bullets.get(0).getSpeedY());
        assertEquals(hero.getLocationY() - 2, bullets.get(0).getLocationY());
        assertEquals(hero.getLocationX(), bullets.get(0).getLocationX());
    }

    @Test
    public void scatterShootSpreadsHorizontally() {
        List<BaseBullet> bullets = new ScatterShootStrategy(3, 30, -1).shoot(hero);
        assertEquals(3, bullets.size());
        // offsetX = (i * 2 - shootNum + 1) * 10 => -20 / 0 / +20
        assertEquals(hero.getLocationX() - 20, bullets.get(0).getLocationX());
        assertEquals(hero.getLocationX(), bullets.get(1).getLocationX());
        assertEquals(hero.getLocationX() + 20, bullets.get(2).getLocationX());
        assertEquals(30, bullets.get(0).getPower());
    }

    @Test
    public void ringShootFiresAllDirections() {
        Boss boss = new Boss(256, 200, 0, 0, 300);
        List<BaseBullet> bullets = new RingShootStrategy(20, 40).shoot(boss);
        assertEquals(20, bullets.size());
        assertEquals(40, bullets.get(0).getPower());
    }
}

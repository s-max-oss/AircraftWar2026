package edu.hitsz.aircraftwar.core.strategy;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.EnemyAircraft;
import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.bullet.EnemyBullet;
import edu.hitsz.aircraftwar.core.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;

/**
 * 直线射击策略：子弹沿直线飞行。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.strategy.StraightShootStrategy}（commit 50474cc），
 * 偏移量与速度公式原样保留（发射点前移 2px、纵向基速 +5）。
 */
public class StraightShootStrategy implements ShootStrategy {

    // 每次射击发射子弹数量
    private int shootNum;
    // 子弹威力
    private int power;
    // 子弹射击方向 (向上发射：-1，向下发射：1)
    private int direction;

    public StraightShootStrategy(int shootNum, int power, int direction) {
        this.shootNum = shootNum;
        this.power = power;
        this.direction = direction;
    }

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> res = new LinkedList<>();
        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY() + direction * 2;
        int speedX = 0;
        int speedY = aircraft.getSpeedY() + direction * 5;
        BaseBullet bullet;

        for (int i = 0; i < shootNum; i++) {
            // 子弹发射位置相对飞机位置向前偏移
            if (aircraft instanceof HeroAircraft) {
                bullet = new HeroBullet(x, y, speedX, speedY, power);
            } else if (aircraft instanceof EnemyAircraft) {
                bullet = new EnemyBullet(x, y, speedX, speedY, power);
            } else {
                continue;
            }
            res.add(bullet);
        }
        return res;
    }
}

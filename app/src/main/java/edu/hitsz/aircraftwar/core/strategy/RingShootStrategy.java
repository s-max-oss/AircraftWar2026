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
 * 环形射击策略：子弹以自身为中心环形散开（Boss 与超级火力）。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.strategy.RingShootStrategy}（commit 50474cc），
 * 子弹速度 5、按机身半宽/半高外扩的公式原样保留。
 */
public class RingShootStrategy implements ShootStrategy {

    // 每次射击发射子弹数量
    private int shootNum;
    // 子弹威力
    private int power;

    public RingShootStrategy(int shootNum, int power) {
        this.shootNum = shootNum;
        this.power = power;
    }

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> res = new LinkedList<>();
        int x = aircraft.getLocationX();
        int y = aircraft.getLocationY();
        // 飞机宽高用于把子弹放到机身外侧
        int width = aircraft.getWidth();
        int height = aircraft.getHeight();
        BaseBullet bullet;

        // 每个子弹的角度（360 度均分）
        double angleStep = 2 * Math.PI / shootNum;
        int bulletSpeed = 5;

        for (int i = 0; i < shootNum; i++) {
            double angle = i * angleStep;
            int speedX = (int) (bulletSpeed * Math.cos(angle));
            int speedY = (int) (bulletSpeed * Math.sin(angle));

            int bulletX = x + (int) (Math.cos(angle) * width / 2);
            int bulletY = y + (int) (Math.sin(angle) * height / 2);

            if (aircraft instanceof HeroAircraft) {
                bullet = new HeroBullet(bulletX, bulletY, speedX, speedY, power);
            } else if (aircraft instanceof EnemyAircraft) {
                bullet = new EnemyBullet(bulletX, bulletY, speedX, speedY, power);
            } else {
                continue;
            }
            res.add(bullet);
        }
        return res;
    }
}

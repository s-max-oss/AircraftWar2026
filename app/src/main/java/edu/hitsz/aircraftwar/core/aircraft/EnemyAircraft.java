package edu.hitsz.aircraftwar.core.aircraft;

import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.drop.Drop;

import java.util.LinkedList;
import java.util.List;

/**
 * 所有敌机的抽象父类。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.EnemyAircraft}（commit 50474cc），逻辑未改动。
 *
 * @author hitsz
 */
public abstract class EnemyAircraft extends AbstractAircraft {

    public EnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    @Override
    public void forward() {
        super.forward();
        // 敌机通用的移动逻辑保留在这里，具体出界判定交给子类
    }

    @Override
    public List<BaseBullet> shoot() {
        return super.shoot();
    }

    /**
     * 敌机掉落道具：默认不掉落。
     *
     * @return 掉落物列表
     */
    @Override
    public List<Drop> drop() {
        return new LinkedList<>();
    }
}

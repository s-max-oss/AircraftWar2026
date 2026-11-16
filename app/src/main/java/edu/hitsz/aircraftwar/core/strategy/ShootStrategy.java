package edu.hitsz.aircraftwar.core.strategy;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;

import java.util.List;

/**
 * 射击策略接口：定义不同飞机的射击方式。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.strategy.ShootStrategy}（commit 50474cc）。
 */
public interface ShootStrategy {

    /**
     * 执行射击。
     *
     * @param aircraft 执行射击的飞机
     * @return 射击产生的子弹列表
     */
    List<BaseBullet> shoot(AbstractAircraft aircraft);
}

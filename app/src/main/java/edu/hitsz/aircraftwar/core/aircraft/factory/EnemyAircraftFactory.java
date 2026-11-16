package edu.hitsz.aircraftwar.core.aircraft.factory;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;

/**
 * 敌机工厂接口。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.factory.EnemyAircraftFactory}（commit 50474cc）。
 */
public interface EnemyAircraftFactory {

    AbstractAircraft createAircraft(int locationX, int locationY, int difficultyLevel);
}

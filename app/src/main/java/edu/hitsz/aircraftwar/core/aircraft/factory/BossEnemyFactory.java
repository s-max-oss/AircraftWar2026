package edu.hitsz.aircraftwar.core.aircraft.factory;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;

/** 迁移说明：来自 {@code edu.hitsz.aircraft.factory.BossEnemyFactory}（commit 50474cc）。 */
public class BossEnemyFactory implements EnemyAircraftFactory {

    @Override
    public AbstractAircraft createAircraft(int locationX, int locationY, int difficultyLevel) {
        // 横向速度 1-5，纵向速度 0
        int speedX = (int) (Math.random() * 4 + 1);
        int speedY = 0;
        int hp = 300;
        return new Boss(locationX, locationY, speedX, speedY, hp);
    }
}

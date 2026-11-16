package edu.hitsz.aircraftwar.core.aircraft.factory;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.MobEnemy;

/** 迁移说明：来自 {@code edu.hitsz.aircraft.factory.MobEnemyFactory}（commit 50474cc）。 */
public class MobEnemyFactory implements EnemyAircraftFactory {

    @Override
    public AbstractAircraft createAircraft(int locationX, int locationY, int difficultyLevel) {
        int speedX = 0;
        // 纵向速度 2-4
        int speedY = 2 + (int) (Math.random() * 3);
        int hp = 30;
        return new MobEnemy(locationX, locationY, speedX, speedY, hp);
    }
}

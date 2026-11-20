package edu.hitsz.aircraftwar.core.aircraft.factory;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.aircraft.ElitePlusEnemy;

/** 迁移说明：来自 {@code edu.hitsz.aircraft.factory.ElitePlusEnemyFactory}（commit 50474cc）。 */
public class ElitePlusEnemyFactory implements EnemyAircraftFactory {

    @Override
    public AbstractAircraft createAircraft(int locationX, int locationY, int difficultyLevel) {
        // 横向速度 0-3，纵向速度 4-6
        int speedX = (int) (Math.random() * 4);
        int speedY = GameConfig.scaledSpeed(4 + (int) (Math.random() * 3));
        int hp = 100;
        return new ElitePlusEnemy(locationX, locationY, speedX, speedY, hp);
    }
}

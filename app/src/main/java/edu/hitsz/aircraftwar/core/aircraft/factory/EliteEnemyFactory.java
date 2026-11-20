package edu.hitsz.aircraftwar.core.aircraft.factory;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.aircraft.EliteEnemy;

/** 迁移说明：来自 {@code edu.hitsz.aircraft.factory.EliteEnemyFactory}（commit 50474cc）。 */
public class EliteEnemyFactory implements EnemyAircraftFactory {

    @Override
    public AbstractAircraft createAircraft(int locationX, int locationY, int difficultyLevel) {
        // 横向速度 0-2，纵向速度 3-5
        int speedX = (int) (Math.random() * 3);
        int speedY = GameConfig.scaledSpeed(3 + (int) (Math.random() * 3));
        int hp = 60;
        return new EliteEnemy(locationX, locationY, speedX, speedY, hp);
    }
}

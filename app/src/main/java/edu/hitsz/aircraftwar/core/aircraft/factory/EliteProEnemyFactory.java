package edu.hitsz.aircraftwar.core.aircraft.factory;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.aircraft.EliteProEnemy;

/** 迁移说明：来自 {@code edu.hitsz.aircraft.factory.EliteProEnemyFactory}（commit 50474cc）。 */
public class EliteProEnemyFactory implements EnemyAircraftFactory {

    @Override
    public AbstractAircraft createAircraft(int locationX, int locationY, int difficultyLevel) {
        // 横向速度 0-3，纵向速度 3-5
        int speedX = (int) (Math.random() * 4);
        int speedY = GameConfig.scaledSpeed(3 + (int) (Math.random() * 3));
        int hp = 120;
        return new EliteProEnemy(locationX, locationY, speedX, speedY, hp);
    }
}

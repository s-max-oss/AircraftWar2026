package edu.hitsz.aircraftwar.core.drop.factory;

import edu.hitsz.aircraftwar.core.drop.Bomb;
import edu.hitsz.aircraftwar.core.drop.Drop;

/** 迁移说明：来自 {@code edu.hitsz.drop.factory.BombDropFactory}（commit 50474cc）。 */
public class BombDropFactory implements DropFactory {

    @Override
    public Drop createDrop(int locationX, int locationY, int speedX, int speedY) {
        return new Bomb(locationX, locationY, speedX, speedY);
    }
}

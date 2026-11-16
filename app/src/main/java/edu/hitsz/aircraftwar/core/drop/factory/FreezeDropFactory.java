package edu.hitsz.aircraftwar.core.drop.factory;

import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.drop.Freeze;

/** 迁移说明：来自 {@code edu.hitsz.drop.factory.FreezeDropFactory}（commit 50474cc）。 */
public class FreezeDropFactory implements DropFactory {

    @Override
    public Drop createDrop(int locationX, int locationY, int speedX, int speedY) {
        return new Freeze(locationX, locationY, speedX, speedY);
    }
}

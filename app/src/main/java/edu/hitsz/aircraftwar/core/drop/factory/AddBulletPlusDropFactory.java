package edu.hitsz.aircraftwar.core.drop.factory;

import edu.hitsz.aircraftwar.core.drop.AddBulletPlus;
import edu.hitsz.aircraftwar.core.drop.Drop;

/** 迁移说明：来自 {@code edu.hitsz.drop.factory.AddBulletPlusDropFactory}（commit 50474cc）。 */
public class AddBulletPlusDropFactory implements DropFactory {

    @Override
    public Drop createDrop(int locationX, int locationY, int speedX, int speedY) {
        return new AddBulletPlus(locationX, locationY, speedX, speedY);
    }
}

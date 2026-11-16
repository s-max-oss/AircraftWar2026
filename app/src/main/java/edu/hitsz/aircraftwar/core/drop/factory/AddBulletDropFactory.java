package edu.hitsz.aircraftwar.core.drop.factory;

import edu.hitsz.aircraftwar.core.drop.AddBullet;
import edu.hitsz.aircraftwar.core.drop.Drop;

/** 迁移说明：来自 {@code edu.hitsz.drop.factory.AddBulletDropFactory}（commit 50474cc）。 */
public class AddBulletDropFactory implements DropFactory {

    @Override
    public Drop createDrop(int locationX, int locationY, int speedX, int speedY) {
        return new AddBullet(locationX, locationY, speedX, speedY);
    }
}

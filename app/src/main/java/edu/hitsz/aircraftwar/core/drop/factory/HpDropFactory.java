package edu.hitsz.aircraftwar.core.drop.factory;

import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.drop.Hp;

/** 迁移说明：来自 {@code edu.hitsz.drop.factory.HpDropFactory}（commit 50474cc）。 */
public class HpDropFactory implements DropFactory {

    @Override
    public Drop createDrop(int locationX, int locationY, int speedX, int speedY) {
        return new Hp(locationX, locationY, speedX, speedY);
    }
}

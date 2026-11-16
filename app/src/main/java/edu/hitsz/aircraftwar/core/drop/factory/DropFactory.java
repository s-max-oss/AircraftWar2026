package edu.hitsz.aircraftwar.core.drop.factory;

import edu.hitsz.aircraftwar.core.drop.Drop;

/**
 * 道具工厂接口。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.factory.DropFactory}（commit 50474cc）。
 */
public interface DropFactory {

    Drop createDrop(int locationX, int locationY, int speedX, int speedY);
}

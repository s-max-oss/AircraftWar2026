package edu.hitsz.aircraftwar.core.drop.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * 道具工厂注册表：按名字取工厂。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.factory.DropFactoryManager}（commit 50474cc），
 * 注册表内容原样保留。
 */
public class DropFactoryManager {

    private static final Map<String, DropFactory> factoryMap = new HashMap<>();

    static {
        factoryMap.put("Hp", new HpDropFactory());
        factoryMap.put("AddBullet", new AddBulletDropFactory());
        factoryMap.put("AddBulletPlus", new AddBulletPlusDropFactory());
        factoryMap.put("Bomb", new BombDropFactory());
        factoryMap.put("Freeze", new FreezeDropFactory());
    }

    private DropFactoryManager() {
    }

    public static DropFactory getFactory(String dropType) {
        return factoryMap.get(dropType);
    }
}

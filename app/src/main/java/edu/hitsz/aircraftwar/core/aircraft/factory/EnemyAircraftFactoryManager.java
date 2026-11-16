package edu.hitsz.aircraftwar.core.aircraft.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * 敌机工厂注册表：按名字取工厂。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.factory.EnemyAircraftFactoryManager}（commit 50474cc），
 * 注册表内容原样保留。
 */
public class EnemyAircraftFactoryManager {

    private static final Map<String, EnemyAircraftFactory> factoryMap = new HashMap<>();

    static {
        factoryMap.put("MobEnemy", new MobEnemyFactory());
        factoryMap.put("EliteEnemy", new EliteEnemyFactory());
        factoryMap.put("ElitePlusEnemy", new ElitePlusEnemyFactory());
        factoryMap.put("EliteProEnemy", new EliteProEnemyFactory());
        factoryMap.put("Boss", new BossEnemyFactory());
    }

    private EnemyAircraftFactoryManager() {
    }

    public static EnemyAircraftFactory getFactory(String aircraftType) {
        return factoryMap.get(aircraftType);
    }
}

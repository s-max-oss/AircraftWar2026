package edu.hitsz.aircraftwar.core.drop;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;
import edu.hitsz.aircraftwar.core.aircraft.EliteEnemy;
import edu.hitsz.aircraftwar.core.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraftwar.core.aircraft.EliteProEnemy;
import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.aircraft.MobEnemy;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.BattleContext;

import java.util.List;

/**
 * 冰冻道具：按敌机类型施加冻结/减速，同时冻结在场敌弹。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.Freeze}（commit 50474cc）。
 * 冻结时长（普通永久、精英 4s、精锐 3s、王牌减速 5s、子弹 5s）原样保留；
 * 该类的 {@code FreezeType} 枚举与 {@code AbstractAircraft} 的重复，属既有设计，
 * S1 只做迁移、不合并（见 docs/03 遗留问题）。
 *
 * @author hitsz
 */
public class Freeze extends Drop {

    public Freeze(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.PROP_FREEZE;
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        vanish();
    }

    @Override
    public void activate(HeroAircraft heroAircraft, BattleContext context) {
        List<AbstractAircraft> enemyAircrafts = context.getEnemyAircrafts();
        List<BaseBullet> enemyBullets = context.getEnemyBullets();

        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy.isValid()) {
                if (enemy instanceof Boss) {
                    continue;
                }

                int originalSpeedX = enemy.getSpeedX();
                int originalSpeedY = enemy.getSpeedY();

                if (enemy instanceof MobEnemy) {
                    enemy.setSpeedX(0);
                    enemy.setSpeedY(0);
                    enemy.setFreezeType(AbstractAircraft.FreezeType.PERMANENT);
                } else if (enemy instanceof EliteEnemy) {
                    enemy.setSpeedX(0);
                    enemy.setSpeedY(0);
                    enemy.setFreezeEndTime(System.currentTimeMillis() + 4000);
                    enemy.setFreezeType(AbstractAircraft.FreezeType.TEMPORARY_4S);
                    enemy.setOriginalSpeed(originalSpeedX, originalSpeedY);
                } else if (enemy instanceof ElitePlusEnemy) {
                    enemy.setSpeedX(0);
                    enemy.setSpeedY(0);
                    enemy.setFreezeEndTime(System.currentTimeMillis() + 3000);
                    enemy.setFreezeType(AbstractAircraft.FreezeType.TEMPORARY_3S);
                    enemy.setOriginalSpeed(originalSpeedX, originalSpeedY);
                } else if (enemy instanceof EliteProEnemy) {
                    enemy.setSpeedX(originalSpeedX / 2);
                    enemy.setSpeedY(originalSpeedY / 2);
                    enemy.setFreezeEndTime(System.currentTimeMillis() + 5000);
                    enemy.setFreezeType(AbstractAircraft.FreezeType.SLOW_5S);
                    enemy.setOriginalSpeed(originalSpeedX, originalSpeedY);
                }
            }
        }

        for (BaseBullet bullet : enemyBullets) {
            if (bullet.isValid()) {
                int originalSpeedX = bullet.getSpeedX();
                int originalSpeedY = bullet.getSpeedY();
                bullet.setSpeedX(0);
                bullet.setSpeedY(0);
                bullet.setFreezeEndTime(System.currentTimeMillis() + 5000);
                bullet.setOriginalSpeed(originalSpeedX, originalSpeedY);
            }
        }

        vanish();
    }

    public enum FreezeType {
        NONE, PERMANENT, TEMPORARY_3S, TEMPORARY_4S, TEMPORARY_5S, SLOW_5S
    }
}

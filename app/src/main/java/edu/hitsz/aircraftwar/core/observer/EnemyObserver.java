package edu.hitsz.aircraftwar.core.observer;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;
import edu.hitsz.aircraftwar.core.aircraft.EliteEnemy;
import edu.hitsz.aircraftwar.core.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraftwar.core.aircraft.EliteProEnemy;
import edu.hitsz.aircraftwar.core.aircraft.MobEnemy;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.drop.Bomb;
import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.drop.Freeze;

/**
 * 敌机/敌弹对道具事件的响应者。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.observer.EnemyObserver}（commit 50474cc）。
 * 冰冻时长（4s/3s/5s）、Boss 免疫、ElitePro 只掉血不死的规则原样保留；
 * 该类自己那份 {@code FreezeType} 枚举与 {@code AbstractAircraft} 里的重复，
 * 属于既有设计，S1 不做合并（合并是重构，见 docs/03 遗留问题）。
 *
 * @author hitsz
 */
public class EnemyObserver implements Observer {

    private AbstractAircraft enemy;
    private BaseBullet bullet;
    private boolean isBullet;
    private long freezeEndTime = 0;
    private int originalSpeedX = 0;
    private int originalSpeedY = 0;
    private FreezeType freezeType = FreezeType.NONE;

    public enum FreezeType {
        NONE,
        PERMANENT,
        TEMPORARY_3S,
        TEMPORARY_4S,
        TEMPORARY_5S,
        SLOW_5S
    }

    public EnemyObserver(AbstractAircraft enemy) {
        this.enemy = enemy;
        this.isBullet = false;
    }

    public EnemyObserver(BaseBullet bullet) {
        this.bullet = bullet;
        this.isBullet = true;
    }

    @Override
    public void update(Observable observable) {
        if (observable instanceof Drop) {
            Drop drop = (Drop) observable;

            if (drop instanceof Bomb) {
                handleBombEffect();
            } else if (drop instanceof Freeze) {
                handleFreezeEffect();
            }
        }
    }

    private void handleBombEffect() {
        if (isBullet) {
            if (bullet != null) {
                bullet.vanish();
            }
            return;
        }

        if (enemy instanceof Boss) {
            // Boss 不受炸弹影响
            return;
        } else if (enemy instanceof EliteProEnemy) {
            // 王牌敌机掉血
            enemy.decreaseHp(100);
        } else {
            // 普通、精英、精锐敌机坠毁
            enemy.vanish();
        }
    }

    private void handleFreezeEffect() {
        if (isBullet) {
            if (bullet != null) {
                originalSpeedX = bullet.getSpeedX();
                originalSpeedY = bullet.getSpeedY();
                bullet.setSpeedX(0);
                bullet.setSpeedY(0);
                freezeEndTime = System.currentTimeMillis() + 5000;
                freezeType = FreezeType.TEMPORARY_5S;
            }
            return;
        }

        if (enemy instanceof Boss) {
            // Boss 不受冰冻影响
            return;
        }

        originalSpeedX = enemy.getSpeedX();
        originalSpeedY = enemy.getSpeedY();

        if (enemy instanceof MobEnemy) {
            // 普通敌机永久静止
            enemy.setSpeedX(0);
            enemy.setSpeedY(0);
            freezeType = FreezeType.PERMANENT;
        } else if (enemy instanceof EliteEnemy) {
            // 精英敌机静止 4 秒后恢复
            enemy.setSpeedX(0);
            enemy.setSpeedY(0);
            freezeEndTime = System.currentTimeMillis() + 4000;
            freezeType = FreezeType.TEMPORARY_4S;
        } else if (enemy instanceof ElitePlusEnemy) {
            // 精锐敌机静止 3 秒后恢复
            enemy.setSpeedX(0);
            enemy.setSpeedY(0);
            freezeEndTime = System.currentTimeMillis() + 3000;
            freezeType = FreezeType.TEMPORARY_3S;
        } else if (enemy instanceof EliteProEnemy) {
            // 王牌敌机减速 5 秒后恢复
            enemy.setSpeedX(originalSpeedX / 2);
            enemy.setSpeedY(originalSpeedY / 2);
            freezeEndTime = System.currentTimeMillis() + 5000;
            freezeType = FreezeType.SLOW_5S;
        }
    }

    public void updateFreezeState() {
        if (freezeType == FreezeType.NONE || freezeType == FreezeType.PERMANENT) {
            return;
        }

        if (System.currentTimeMillis() >= freezeEndTime) {
            if (isBullet && bullet != null) {
                bullet.setSpeedX(originalSpeedX);
                bullet.setSpeedY(originalSpeedY);
            } else if (enemy != null && enemy.isValid()) {
                enemy.setSpeedX(originalSpeedX);
                enemy.setSpeedY(originalSpeedY);
            }
            freezeType = FreezeType.NONE;
        }
    }

    public AbstractAircraft getEnemy() {
        return enemy;
    }

    public BaseBullet getBullet() {
        return bullet;
    }

    public boolean isFrozen() {
        return freezeType != FreezeType.NONE;
    }
}

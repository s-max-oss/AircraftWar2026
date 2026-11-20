package edu.hitsz.aircraftwar.core.aircraft;

import edu.hitsz.aircraftwar.core.basic.AbstractFlyingObject;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.strategy.ShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 所有种类飞机的抽象父类。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.AbstractAircraft}（commit 50474cc），
 * 逻辑未改动，只换包名。
 *
 * @author hitsz
 */
public abstract class AbstractAircraft extends AbstractFlyingObject {

    // 最大生命值
    protected int maxHp;
    protected int hp;

    // 射击策略
    protected ShootStrategy shootStrategy;

    // 冰冻相关字段
    protected long freezeEndTime = 0;
    protected int originalSpeedX = 0;
    protected int originalSpeedY = 0;
    protected FreezeType freezeType = FreezeType.NONE;

    public enum FreezeType {
        NONE, PERMANENT, TEMPORARY_3S, TEMPORARY_4S, TEMPORARY_5S, SLOW_5S
    }

    public AbstractAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY);
        this.hp = hp;
        this.maxHp = hp;
    }

    public void decreaseHp(int decrease) {
        hp -= decrease;
        if (hp <= 0) {
            hp = 0;
            vanish();
        }
    }

    public void increaseHp(int increase) {
        hp += increase;
        if (hp > maxHp) {
            hp = maxHp;
        }
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getSpeedX() {
        return speedX;
    }

    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    @Override
    public int getSpeedY() {
        return speedY;
    }

    public void setSpeedY(int speedY) {
        this.speedY = speedY;
    }

    /**
     * 设置射击策略。
     *
     * @param shootStrategy 射击策略
     */
    public void setShootStrategy(ShootStrategy shootStrategy) {
        this.shootStrategy = shootStrategy;
    }

    /**
     * 飞机射击：可射击对象需实现策略，非可射击对象空实现返回空列表。
     *
     * @return 子弹列表
     */
    public List<BaseBullet> shoot() {
        if (shootStrategy != null) {
            return shootStrategy.shoot(this);
        }
        return new LinkedList<>();
    }

    /**
     * 飞机掉落道具：可掉落对象返回掉落物列表，其余空实现。
     *
     * @return 掉落物列表
     */
    public List<Drop> drop() {
        return new LinkedList<>();
    }

    public void setFreezeEndTime(long time) {
        this.freezeEndTime = time;
    }

    public void setOriginalSpeed(int speedX, int speedY) {
        this.originalSpeedX = speedX;
        this.originalSpeedY = speedY;
    }

    public void setFreezeType(FreezeType type) {
        this.freezeType = type;
    }

    public FreezeType getFreezeType() {
        return this.freezeType;
    }

    public void updateFreezeState() {
        if (freezeType == FreezeType.NONE || freezeType == FreezeType.PERMANENT) {
            return;
        }

        if (System.currentTimeMillis() >= freezeEndTime) {
            this.speedX = originalSpeedX;
            this.speedY = originalSpeedY;
            freezeType = FreezeType.NONE;
        }
    }
}

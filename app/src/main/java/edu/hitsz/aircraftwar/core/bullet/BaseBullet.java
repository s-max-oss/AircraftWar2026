package edu.hitsz.aircraftwar.core.bullet;

import edu.hitsz.aircraftwar.core.basic.AbstractFlyingObject;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 子弹基类。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.bullet.BaseBullet}（commit 50474cc），
 * 只把窗口尺寸换成 {@link GameConfig}，出界判定与冰冻字段逻辑未改动。
 *
 * @author hitsz
 */
public abstract class BaseBullet extends AbstractFlyingObject {

    private int power = 0;

    // 冰冻相关字段
    protected long freezeEndTime = 0;
    protected int originalSpeedX = 0;
    protected int originalSpeedY = 0;

    public BaseBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY);
        this.power = power;
    }

    @Override
    public void forward() {
        super.forward();

        // 判定 x 轴出界
        if (locationX <= 0 || locationX >= GameConfig.WINDOW_WIDTH) {
            vanish();
        }

        // 判定 y 轴出界
        if (speedY > 0 && locationY >= GameConfig.WINDOW_HEIGHT) {
            // 向下飞行出界
            vanish();
        } else if (locationY <= 0) {
            // 向上飞行出界
            vanish();
        }
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getSpeedX() {
        return speedX;
    }

    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    public void setSpeedY(int speedY) {
        this.speedY = speedY;
    }

    public void setFreezeEndTime(long time) {
        this.freezeEndTime = time;
    }

    public void setOriginalSpeed(int speedX, int speedY) {
        this.originalSpeedX = speedX;
        this.originalSpeedY = speedY;
    }

    public void updateFreezeState() {
        if (System.currentTimeMillis() >= freezeEndTime && freezeEndTime > 0) {
            this.speedX = originalSpeedX;
            this.speedY = originalSpeedY;
            freezeEndTime = 0;
        }
    }
}

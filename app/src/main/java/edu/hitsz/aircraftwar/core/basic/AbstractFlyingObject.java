package edu.hitsz.aircraftwar.core.basic;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 可飞行对象的基类。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.basic.AbstractFlyingObject}（commit 50474cc）。
 * 唯一改动是去掉 {@code BufferedImage image} / {@code getImage()} / {@code paint(Graphics)}：
 * 绘制属于平台层，逻辑层只声明"我长什么样"（{@link #getSprite()}），图片由
 * {@code platform/render} 按精灵取。宽高同理，改由 {@link GameConfig.Sprite} 提供，
 * 这样碰撞判定不必碰 {@code java.awt}。
 *
 * @author hitsz
 */
public abstract class AbstractFlyingObject {

    // locationX,locationY 为图片中心位置坐标
    protected int locationX;
    protected int locationY;

    // x,y 轴移动速度
    protected int speedX;
    protected int speedY;

    // x 轴长度，根据精灵尺寸获得, -1 表示未设置
    protected int width = -1;

    // y 轴长度，根据精灵尺寸获得, -1 表示未设置
    protected int height = -1;

    // 有效（生存）标记，标记为 false 的对象会在下次刷新时清除
    protected boolean isValid = true;

    public AbstractFlyingObject() {
    }

    public AbstractFlyingObject(int locationX, int locationY, int speedX, int speedY) {
        this.locationX = locationX;
        this.locationY = locationY;
        this.speedX = speedX;
        this.speedY = speedY;
    }

    /**
     * 本对象使用的精灵。桌面版靠类名反查图片，Android 侧由平台层按精灵取 Bitmap。
     *
     * @return 该对象的精灵标识
     */
    public abstract GameConfig.Sprite getSprite();

    /**
     * 可飞行对象根据速度移动；触碰到横向边界时横向速度反向。
     */
    public void forward() {
        locationX += speedX;
        locationY += speedY;
        if (locationX <= 0 || locationX >= GameConfig.WINDOW_WIDTH) {
            speedX = -speedX;
        }
    }

    /**
     * 碰撞检测，当对方坐标进入我方范围，判定我方击中。对方与我方覆盖区域有交叉即判定撞击。
     *
     * <p>非飞机对象区域：横向 [x - width/2, x + width/2]，纵向 [y - height/2, y + height/2]。
     * 飞机对象区域：横向同上，纵向收窄一半（[y - height/4, y + height/4]），
     * 这样机身穿插时更宽容，手感与桌面版一致。
     *
     * @param flyingObject 撞击对方
     * @return true: 我方被击中; false 我方未被击中
     */
    public boolean crash(AbstractFlyingObject flyingObject) {
        // 缩放因子，用于控制 y 轴方向区域范围
        int factor = this instanceof AbstractAircraft ? 2 : 1; // 我方
        int fFactor = flyingObject instanceof AbstractAircraft ? 2 : 1; // 对方

        // 对方坐标、宽度、高度
        int x = flyingObject.getLocationX();
        int y = flyingObject.getLocationY();
        int fWidth = flyingObject.getWidth();
        int fHeight = flyingObject.getHeight();

        return x + (fWidth + this.getWidth()) / 2 > locationX
                && x - (fWidth + this.getWidth()) / 2 < locationX
                && y + (fHeight / fFactor + this.getHeight() / factor) / 2 > locationY
                && y - (fHeight / fFactor + this.getHeight() / factor) / 2 < locationY;
    }

    public int getLocationX() {
        return locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public void setLocation(double locationX, double locationY) {
        this.locationX = (int) locationX;
        this.locationY = (int) locationY;
    }

    public int getSpeedY() {
        return speedY;
    }

    public int getWidth() {
        if (width == -1) {
            width = getSprite().width;
        }
        return width;
    }

    public int getHeight() {
        if (height == -1) {
            height = getSprite().height;
        }
        return height;
    }

    /**
     * 标记消失：{@code isValid = false}，下次刷新时被清除。
     */
    public void vanish() {
        isValid = false;
    }

    public boolean notValid() {
        return !this.isValid;
    }

    public boolean isValid() {
        return this.isValid;
    }
}

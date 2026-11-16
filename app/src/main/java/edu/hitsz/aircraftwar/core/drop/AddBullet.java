package edu.hitsz.aircraftwar.core.drop;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 加火力（子弹数量）道具。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.AddBullet}（commit 50474cc），
 * 原类重复实现了与父类相同的出界判定，这里删掉冗余覆盖（行为等价）。
 *
 * @author hitsz
 */
public class AddBullet extends Drop {

    // 增加子弹数
    protected int bulletNum = 1;

    public AddBullet(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.PROP_BULLET;
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        heroAircraft.increaseBulletNum(bulletNum);
        // 道具使用后消失
        vanish();
    }

    public int getBulletNum() {
        return bulletNum;
    }
}

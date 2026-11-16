package edu.hitsz.aircraftwar.core.drop;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 超级火力道具：加威力并切环形弹幕 10 秒。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.AddBulletPlus}（commit 50474cc），加成 10 与原逻辑一致。
 *
 * @author hitsz
 */
public class AddBulletPlus extends Drop {

    // 增加的子弹威力
    protected int power = 10;

    public AddBulletPlus(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.PROP_BULLET_PLUS;
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        heroAircraft.increasePower(power);
        // 激活环形射击策略
        heroAircraft.activateRingShoot();
        // 道具使用后消失
        vanish();
    }

    public int getPower() {
        return power;
    }
}

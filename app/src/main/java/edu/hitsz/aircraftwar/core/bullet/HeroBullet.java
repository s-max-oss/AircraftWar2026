package edu.hitsz.aircraftwar.core.bullet;

import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 英雄机子弹。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.bullet.HeroBullet}（commit 50474cc）。
 *
 * @author hitsz
 */
public class HeroBullet extends BaseBullet {

    public HeroBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.HERO_BULLET;
    }
}

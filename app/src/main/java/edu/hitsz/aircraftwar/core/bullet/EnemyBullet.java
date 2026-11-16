package edu.hitsz.aircraftwar.core.bullet;

import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 敌机子弹。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.bullet.EnemyBullet}（commit 50474cc）。
 *
 * @author hitsz
 */
public class EnemyBullet extends BaseBullet {

    public EnemyBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.ENEMY_BULLET;
    }
}

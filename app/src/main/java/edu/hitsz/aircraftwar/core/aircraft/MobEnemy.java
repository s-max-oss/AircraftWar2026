package edu.hitsz.aircraftwar.core.aircraft;

import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.drop.Drop;

import java.util.LinkedList;
import java.util.List;

/**
 * 普通敌机：不可射击、不掉落道具。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.MobEnemy}（commit 50474cc），逻辑未改动。
 *
 * @author hitsz
 */
public class MobEnemy extends EnemyAircraft {

    public MobEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.MOB;
    }

    @Override
    public void forward() {
        super.forward();
        // 判定 y 轴向下飞行出界
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }

    @Override
    public List<BaseBullet> shoot() {
        return new LinkedList<>();
    }

    /**
     * 普通敌机不掉落道具。
     *
     * @return 空列表
     */
    @Override
    public List<Drop> drop() {
        return new LinkedList<>();
    }
}

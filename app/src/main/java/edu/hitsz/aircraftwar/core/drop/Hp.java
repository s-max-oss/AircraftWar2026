package edu.hitsz.aircraftwar.core.drop;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 回血道具。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.Hp}（commit 50474cc），回复量 30 原样保留。
 *
 * @author hitsz
 */
public class Hp extends Drop {

    // 回复血量值
    private int restoreHp = 30;

    public Hp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.PROP_HP;
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        heroAircraft.increaseHp(restoreHp);
        // 道具使用后消失
        vanish();
    }

    public int getRestoreHp() {
        return restoreHp;
    }
}

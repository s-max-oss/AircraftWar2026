package edu.hitsz.aircraftwar.core.aircraft;

import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.drop.factory.DropFactory;
import edu.hitsz.aircraftwar.core.drop.factory.DropFactoryManager;
import edu.hitsz.aircraftwar.core.strategy.StraightShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 普通精英敌机：可射击、掉落加火力道具。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.EliteEnemy}（commit 50474cc），
 * 掉落概率与射击参数一字未动。
 *
 * @author hitsz
 */
public class EliteEnemy extends EnemyAircraft {

    // 每次射击发射子弹数量
    private int shootNum = 1;

    // 子弹威力
    private int power = 30;

    // 子弹射击方向 (向上发射：-1，向下发射：1)
    private int direction = 1;

    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        // 初始化射击策略为直线射击
        this.shootStrategy = new StraightShootStrategy(shootNum, power, direction);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.ELITE;
    }

    @Override
    public void forward() {
        super.forward();
        // 判定 y 轴向下飞行出界
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }

    /**
     * 普通精英敌机掉落：80% 概率掉加火力道具。
     *
     * @return 掉落物列表
     */
    @Override
    public List<Drop> drop() {
        List<Drop> res = new LinkedList<>();
        int x = this.getLocationX();
        int y = this.getLocationY();
        int speedX = 0;
        int speedY = this.getSpeedY();
        if (Math.random() < 0.8) {
            DropFactory factory = DropFactoryManager.getFactory("AddBullet");
            res.add(factory.createDrop(x, y, speedX, speedY + 2));
        }
        return res;
    }
}

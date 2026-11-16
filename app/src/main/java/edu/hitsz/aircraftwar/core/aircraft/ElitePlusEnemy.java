package edu.hitsz.aircraftwar.core.aircraft;

import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.drop.factory.DropFactory;
import edu.hitsz.aircraftwar.core.drop.factory.DropFactoryManager;
import edu.hitsz.aircraftwar.core.strategy.ScatterShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 高级精英敌机：可射击、按四档概率掉落道具。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.ElitePlusEnemy}（commit 50474cc），逻辑未改动。
 * 注意原实现的四段 {@code Math.random()} 是独立抽样，不是互斥分桶，行为原样保留。
 *
 * @author hitsz
 */
public class ElitePlusEnemy extends EnemyAircraft {

    // 每次射击发射子弹数量
    private int shootNum = 2;

    // 子弹威力
    private int power = 30;

    // 子弹射击方向 (向上发射：-1，向下发射：1)
    private int direction = 1;

    public ElitePlusEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        // 初始化射击策略为散弹射击
        this.shootStrategy = new ScatterShootStrategy(shootNum, power, direction);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.ELITE_PLUS;
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
     * 高级精英敌机掉落：连抽四档，先命中者生效。
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

        // 25% 概率掉 AddBulletPlus
        if (Math.random() < 0.25) {
            DropFactory factory = DropFactoryManager.getFactory("AddBulletPlus");
            res.add(factory.createDrop(x, y, speedX, speedY + 2));
        } else if (Math.random() < 0.5) {
            DropFactory factory = DropFactoryManager.getFactory("AddBullet");
            res.add(factory.createDrop(x, y, speedX, speedY + 2));
        } else if (Math.random() < 0.75) {
            DropFactory factory = DropFactoryManager.getFactory("Hp");
            res.add(factory.createDrop(x, y, speedX, speedY + 2));
        } else {
            DropFactory factory = DropFactoryManager.getFactory("Bomb");
            res.add(factory.createDrop(x, y, speedX, speedY + 2));
        }

        return res;
    }
}

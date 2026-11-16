package edu.hitsz.aircraftwar.core.aircraft;

import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.drop.factory.DropFactory;
import edu.hitsz.aircraftwar.core.drop.factory.DropFactoryManager;
import edu.hitsz.aircraftwar.core.strategy.RingShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * Boss 敌机：环形弹幕、固定 5 档掉落。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.Boss}（commit 50474cc），
 * 环形弹幕 20 发、掉落概率五等分的逻辑一字未动。
 *
 * @author hitsz
 */
public class Boss extends EnemyAircraft {

    // 每次射击发射子弹数量
    private int shootNum = 3;

    // 子弹威力
    private int power = GameConfig.BOSS_BULLET_POWER;

    // 子弹射击方向 (向上发射：-1，向下发射：1)
    private int direction = 1;

    public Boss(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        // 初始化射击策略为环形射击，发射 20 颗子弹形成环形（密度提高 150%）
        this.shootStrategy = new RingShootStrategy(20, power);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.BOSS;
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
     * Boss 掉落：五档等概率，先命中者生效。
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
        double rand = Math.random();
        if (rand < 0.2) {
            res.add(DropFactoryManager.getFactory("Bomb").createDrop(x, y, speedX, speedY + 2));
        } else if (rand < 0.4) {
            res.add(DropFactoryManager.getFactory("Freeze").createDrop(x, y, speedX, speedY + 2));
        } else if (rand < 0.6) {
            res.add(DropFactoryManager.getFactory("AddBullet").createDrop(x, y, speedX, speedY + 2));
        } else if (rand < 0.8) {
            res.add(DropFactoryManager.getFactory("Hp").createDrop(x, y, speedX, speedY + 2));
        } else {
            res.add(DropFactoryManager.getFactory("AddBulletPlus").createDrop(x, y, speedX, speedY + 2));
        }
        return res;
    }
}

package edu.hitsz.aircraftwar.core.aircraft;

import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.strategy.RingShootStrategy;
import edu.hitsz.aircraftwar.core.strategy.ScatterShootStrategy;
import edu.hitsz.aircraftwar.core.strategy.ShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 英雄飞机，游戏玩家操控。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.aircraft.HeroAircraft}（commit 50474cc）。
 * 改动仅两处读数来源：窗口尺寸与精灵高度改为 {@link GameConfig}，基础火力取
 * {@link GameConfig#HERO_POWER_BASE}；单例、火力道具计时、环形弹幕等逻辑一字未动。
 *
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {

    // 单例实例（volatile 保证可见性并禁止指令重排序）
    private static volatile HeroAircraft instance;

    // 每次射击发射子弹数量
    private int shootNum = 1;
    // 原始子弹数量
    private int originalShootNum = 1;

    // 子弹威力
    private int power = GameConfig.HERO_POWER_BASE;
    // 原始子弹威力
    private int originalPower = GameConfig.HERO_POWER_BASE;

    // 子弹射击方向 (向上发射：-1，向下发射：1)
    private int direction = -1;

    // 火力道具激活时间
    private long powerUpTime = 0;
    // 火力道具持续时间（毫秒）
    private static final long POWER_UP_DURATION = 10000;

    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        // 初始化射击策略为散弹射击
        this.shootStrategy = new ScatterShootStrategy(shootNum, power, direction);
    }

    // 获取单例实例（DCL 双重检查锁定）
    public static HeroAircraft getInstance() {
        if (instance == null) {
            synchronized (HeroAircraft.class) {
                if (instance == null) {
                    instance = new HeroAircraft(
                            GameConfig.WINDOW_WIDTH / 2,
                            GameConfig.WINDOW_HEIGHT - GameConfig.Sprite.HERO.height,
                            0, 0, 100
                    );
                }
            }
        }
        return instance;
    }

    // 重置英雄机（游戏重新开始时使用）
    public static synchronized void resetInstance() {
        instance = null;
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.HERO;
    }

    /**
     * 重置英雄机状态。
     */
    public void reset() {
        // 重置位置
        locationX = GameConfig.WINDOW_WIDTH / 2;
        locationY = GameConfig.WINDOW_HEIGHT - GameConfig.HERO_RESET_BOTTOM_MARGIN;
        // 重置生命值
        hp = maxHp;
        // 重置子弹数量和威力
        shootNum = 1;
        power = GameConfig.HERO_POWER_BASE;
        // 更新射击策略
        this.shootStrategy = new ScatterShootStrategy(shootNum, power, direction);
        // 重置有效状态
        isValid = true;
    }

    public void increasePower(int increase) {
        // 记录原始值（如果还没有记录）
        if (powerUpTime == 0) {
            originalShootNum = shootNum;
            originalPower = power;
        }
        // 增加子弹威力
        power += increase;
        // 更新射击策略
        this.shootStrategy = new ScatterShootStrategy(shootNum, power, direction);
        // 重置激活时间（无论是否已经有火力道具效果）
        powerUpTime = System.currentTimeMillis();
    }

    public void setPowerTo(int aim) {
        power = aim;
    }

    /**
     * 增加最大生命值。
     *
     * @param increase 增加的最大生命值
     */
    public void increaseMaxHp(int increase) {
        maxHp += increase;
        // 同时增加当前生命值
        hp += increase;
    }

    /**
     * 增加生命值。
     *
     * @param increase 增加的生命值
     */
    @Override
    public void increaseHp(int increase) {
        hp += increase;
        // 生命值不能超过最大值
        if (hp > maxHp) {
            hp = maxHp;
        }
    }

    /**
     * 增加子弹数量。
     *
     * @param increase 增加的子弹数量
     */
    public void increaseBulletNum(int increase) {
        // 记录原始值（如果还没有记录）
        if (powerUpTime == 0) {
            originalShootNum = shootNum;
            originalPower = power;
        }
        // 增加子弹数量
        shootNum += increase;
        // 更新射击策略
        this.shootStrategy = new ScatterShootStrategy(shootNum, power, direction);
        // 重置激活时间（无论是否已经有火力道具效果）
        powerUpTime = System.currentTimeMillis();
    }

    // 原始射击策略
    private ShootStrategy originalShootStrategy;
    // 环形射击策略激活时间
    private long ringShootTime = 0;
    // 环形射击策略持续时间（毫秒）
    private static final long RING_SHOOT_DURATION = 10000;

    /**
     * 检查火力道具是否过期；过期则恢复原始的子弹数量与威力。
     */
    public void checkPowerUpExpiration() {
        if (powerUpTime > 0 && System.currentTimeMillis() - powerUpTime >= POWER_UP_DURATION) {
            // 恢复原始值
            shootNum = originalShootNum;
            power = originalPower;
            // 更新射击策略
            this.shootStrategy = new ScatterShootStrategy(shootNum, power, direction);
            // 重置激活时间
            powerUpTime = 0;
        }

        // 检查环形射击策略是否过期
        if (ringShootTime > 0 && System.currentTimeMillis() - ringShootTime >= RING_SHOOT_DURATION) {
            // 恢复原始射击策略
            if (originalShootStrategy != null) {
                this.shootStrategy = originalShootStrategy;
            } else {
                this.shootStrategy = new ScatterShootStrategy(shootNum, power, direction);
            }
            // 重置环形射击激活时间
            ringShootTime = 0;
        }
    }

    /**
     * 激活环形射击策略（超级火力道具）。
     */
    public void activateRingShoot() {
        // 保存原始射击策略
        originalShootStrategy = this.shootStrategy;
        // 设置环形射击策略
        this.shootStrategy = new RingShootStrategy(8, power);
        // 设置激活时间
        ringShootTime = System.currentTimeMillis();
    }

    @Override
    public void forward() {
        // 英雄机由触摸控制，不通过 forward 移动
    }

    /**
     * 英雄机不掉落道具。
     *
     * @return 空列表
     */
    @Override
    public List<Drop> drop() {
        return new LinkedList<>();
    }
}

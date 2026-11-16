package edu.hitsz.aircraftwar.core.config;

/**
 * 全局常量的唯一来源（AGENTS §7 铁律 3）。
 *
 * <p>数值全部照抄迁移源 commit 50474cc，未改动任何平衡参数：逻辑层只依赖这里的数值，
 * 不再反向 import 桌面版的 {@code Main} / {@code ImageManager}。
 *
 * <p>为什么精灵尺寸也在这里：迁移源用 {@code ImageManager.*_IMAGE.getWidth()} 算敌人出生 x 坐标，
 * 把 {@code java.awt.image.BufferedImage} 带进了逻辑层。Android 侧图片由 {@code platform}
 * 注入，逻辑层只需要知道"这张图多大"，所以尺寸进常量、图片对象不进 core。
 *
 * <p>暂未收进来的：各难度档的攻防数值（基础攻击力、敌机数量上限、随难度提升的调整表）仍留在
 * 三个难度子类里，属 S2 的 Profile 化重构，见 {@code docs/03-实验二说明.md}「遗留问题」。
 *
 * @author hitsz
 */
public final class GameConfig {

    private GameConfig() {
    }

    // ==================== 画布与节拍 ====================

    /** 逻辑画布宽。渲染层按 letterbox 映射到真实屏幕，不清算坐标。 */
    public static final int WINDOW_WIDTH = 512;
    /** 逻辑画布高。 */
    public static final int WINDOW_HEIGHT = 768;
    /** 逻辑帧间隔（毫秒）。桌面版 {@code Timer(40ms)}，Android 渲染线程沿用同一节拍。 */
    public static final int TICK_MS = 40;

    // ==================== 精灵尺寸 ====================

    /**
     * 19 张资源图的原始像素尺寸，必须与 {@code res/drawable-nodpi/} 里的文件一致：
     * 碰撞判定直接把 width/height 当矩形框用，尺寸错了就会"看得见打不着"。
     */
    public enum Sprite {
        BACKGROUND(512, 768),
        HERO(100, 83),
        MOB(75, 50),
        ELITE(105, 68),
        ELITE_PLUS(105, 68),
        ELITE_PRO(100, 83),
        BOSS(300, 200),
        HERO_BULLET(10, 25),
        ENEMY_BULLET(10, 18),
        ICON(35, 27),
        PROP_HP(33, 30),
        PROP_BOMB(34, 34),
        PROP_BULLET(35, 33),
        PROP_BULLET_PLUS(34, 34),
        PROP_FREEZE(35, 35);

        public final int width;
        public final int height;

        Sprite(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    // ==================== 射击与伤害基准 ====================

    /** 英雄机射击周期（帧），实际周期还要除以等级攻速加成。 */
    public static final double SHOOT_HERO_CYCLE = 10;
    /** 敌机射击周期（帧）。 */
    public static final double SHOOT_ENEMY_CYCLE = 50;
    /** 敌弹威力基准；各难度在 {@code baseEnemyPower} 上做加减，公式固定为 {@code power + base - 40}。 */
    public static final int BASE_ENEMY_POWER = 40;
    /** 英雄机基础火力，等级加成在此之上累加。 */
    public static final int HERO_POWER_BASE = 30;

    // ==================== 生成周期（帧） ====================

    /** 基类默认周期，难度子类未覆盖时使用。 */
    public static final double DEFAULT_ENEMY_SPAWN_CYCLE = 20;
    public static final double DEFAULT_ELITE_SPAWN_CYCLE = 60;
    public static final double DEFAULT_ELITE_PLUS_SPAWN_CYCLE = 100;
    public static final double DEFAULT_ELITE_PRO_SPAWN_CYCLE = 150;
    public static final double DEFAULT_BOSS_SPAWN_CYCLE = 300;

    public static final double SIMPLE_ENEMY_SPAWN_CYCLE = 20;
    public static final double SIMPLE_ELITE_SPAWN_CYCLE = 60;
    public static final double SIMPLE_ELITE_PLUS_SPAWN_CYCLE = 100;

    public static final double NORMAL_ENEMY_SPAWN_CYCLE = 16;
    public static final double NORMAL_ELITE_SPAWN_CYCLE = 45;
    public static final double NORMAL_ELITE_PLUS_SPAWN_CYCLE = 80;

    public static final double HARD_ENEMY_SPAWN_CYCLE = 12;
    public static final double HARD_ELITE_SPAWN_CYCLE = 35;
    public static final double HARD_ELITE_PLUS_SPAWN_CYCLE = 60;
    public static final double HARD_ELITE_PRO_SPAWN_CYCLE = 100;

    // ==================== 同屏数量上限（基类默认） ====================

    public static final int BASE_ENEMY_MAX = 10;
    public static final int BASE_ELITE_MAX = 5;
    public static final int BASE_ELITE_PLUS_MAX = 2;
    public static final int BASE_ELITE_PRO_MAX = 1;
    public static final int BASE_BOSS_MAX = 1;

    // ==================== 分数表（经验与分数同值） ====================

    public static final int SCORE_MOB = 10;
    public static final int SCORE_ELITE = 30;
    public static final int SCORE_ELITE_PLUS = 50;
    public static final int SCORE_ELITE_PRO = 100;
    public static final int SCORE_BOSS = 500;

    // ==================== Boss 触发 ====================

    /** 基类默认的 Boss 召唤分数门槛。 */
    public static final int BOSS_SCORE_THRESHOLD = 1000;
    /** 普通难度的首次门槛与每次召唤后的增量。 */
    public static final int BOSS_SCORE_STEP_NORMAL = 500;
    /** 困难难度的首次门槛与每次召唤后的增量。 */
    public static final int BOSS_SCORE_THRESHOLD_HARD = 600;
    public static final int BOSS_SCORE_STEP_HARD = 400;
    /** "Boss 即将到来"提示的提前量（分数差）。 */
    public static final int BOSS_WARNING_LEAD = 200;
    /** 普通难度 Boss 固定血量。 */
    public static final int BOSS_HP_NORMAL = 300;
    /** 困难难度 Boss 血量 = 基准 + 召唤次数 × 增量。 */
    public static final int BOSS_HP_HARD_BASE = 400;
    public static final int BOSS_HP_HARD_STEP = 100;

    // ==================== 道具 ====================

    /** 冰冻道具持续时间（帧）。 */
    public static final int FREEZE_DURATION = 60;

    // ==================== 等级成长（PlayerLevelManager） ====================

    /** 第 i 级升到 i+1 所需的累计经验；下标即等级。只读，不要原地修改。 */
    public static final int[] EXP_THRESHOLDS = {0, 200, 500, 1000, 1800, 3000, 5000, 8000, 12000, 18000};
    /** 升级时增加的血量上限，下标为 {@code level - 1}。 */
    public static final int[] LEVEL_HP_BONUS = {100, 20, 20, 20, 30, 30, 30, 40, 40, 50};
    /** 升级时增加的火力，下标为 {@code level - 1}。 */
    public static final int[] LEVEL_POWER_BONUS = {30, 5, 5, 5, 10, 10, 10, 15, 15, 20};
    /** 每提升该倍数级数，攻速加成加一档。 */
    public static final double SHOOT_SPEED_INTERVAL = 5;
    /** 每档攻速加成比例。 */
    public static final double SHOOT_SPEED_BONUS = 0.1;
    /** 攻速加成上限。 */
    public static final double SHOOT_SPEED_BONUS_MAX = 0.5;
}

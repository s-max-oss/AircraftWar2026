package edu.hitsz.aircraftwar.core.logic;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 玩家等级与成长：经验、血量上限、火力、攻速加成。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.application.PlayerLevelManager}（commit 50474cc）。
 * 两处改动：
 * 1. 数值表移到 {@link GameConfig}（铁律 3），升级公式一字未动；
 * 2. 原来直接调 {@code SoundManager.playLevelUpSound()}，现改为注入
 *    {@link LevelUpListener}——否则 core 反向依赖平台音频（铁律 1/2）。
 *
 * @author hitsz
 */
public class PlayerLevelManager {

    /** 升级事件出口，由平台层接音效/UI。 */
    public interface LevelUpListener {
        void onLevelUp(int level, int hpBonus, int powerBonus);
    }

    private static PlayerLevelManager instance;

    private HeroAircraft hero;
    private LevelUpListener listener;

    private int level = 1;
    private int exp = 0;

    private PlayerLevelManager() {
        this.hero = HeroAircraft.getInstance();
    }

    public static PlayerLevelManager getInstance() {
        if (instance == null) {
            synchronized (PlayerLevelManager.class) {
                if (instance == null) {
                    instance = new PlayerLevelManager();
                }
            }
        }
        return instance;
    }

    public void setLevelUpListener(LevelUpListener listener) {
        this.listener = listener;
    }

    public void addExp(int amount) {
        exp += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        while (level < GameConfig.EXP_THRESHOLDS.length && exp >= GameConfig.EXP_THRESHOLDS[level]) {
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        applyLevelBonus();
        int hpBonus = GameConfig.LEVEL_HP_BONUS[level - 1];
        int powerBonus = GameConfig.LEVEL_POWER_BONUS[level - 1];
        if (listener != null) {
            listener.onLevelUp(level, hpBonus, powerBonus);
        }
    }

    private void applyLevelBonus() {
        int index = level - 1;
        hero.increaseMaxHp(GameConfig.LEVEL_HP_BONUS[index]);
        hero.setPowerTo(GameConfig.HERO_POWER_BASE + GameConfig.LEVEL_POWER_BONUS[index]);
    }

    public double getShootSpeedBonus() {
        if (level < GameConfig.SHOOT_SPEED_INTERVAL) {
            return 0.0;
        }
        int bonusLevels = (level - (int) GameConfig.SHOOT_SPEED_INTERVAL) / (int) GameConfig.SHOOT_SPEED_INTERVAL + 1;
        double bonus = bonusLevels * GameConfig.SHOOT_SPEED_BONUS;
        return Math.min(bonus, GameConfig.SHOOT_SPEED_BONUS_MAX);
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public int getExpToNextLevel() {
        if (level >= GameConfig.EXP_THRESHOLDS.length) {
            return 0;
        }
        return GameConfig.EXP_THRESHOLDS[level] - exp;
    }

    public int getCurrentThreshold() {
        if (level >= GameConfig.EXP_THRESHOLDS.length) {
            return GameConfig.EXP_THRESHOLDS[GameConfig.EXP_THRESHOLDS.length - 1];
        }
        return GameConfig.EXP_THRESHOLDS[level];
    }

    public void reset() {
        level = 1;
        exp = 0;
    }

    public double getExpProgress() {
        if (level >= GameConfig.EXP_THRESHOLDS.length) {
            return 1.0;
        }
        int prevThreshold = level > 1 ? GameConfig.EXP_THRESHOLDS[level - 1] : 0;
        int currentThreshold = GameConfig.EXP_THRESHOLDS[level];
        return (double) (exp - prevThreshold) / (currentThreshold - prevThreshold);
    }
}

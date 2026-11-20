package edu.hitsz.aircraftwar.core.logic;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;
import edu.hitsz.aircraftwar.core.aircraft.EliteEnemy;
import edu.hitsz.aircraftwar.core.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraftwar.core.aircraft.EliteProEnemy;
import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.basic.AbstractFlyingObject;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.drop.AddBullet;
import edu.hitsz.aircraftwar.core.drop.AddBulletPlus;
import edu.hitsz.aircraftwar.core.drop.Bomb;
import edu.hitsz.aircraftwar.core.drop.Drop;
import edu.hitsz.aircraftwar.core.drop.Freeze;
import edu.hitsz.aircraftwar.core.drop.Hp;
import edu.hitsz.aircraftwar.core.observer.EnemyObserver;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 游戏逻辑主循环（桌面版 {@code Game} 去掉 Swing/渲染/音效后的部分）。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.application.Game}（commit 50474cc）。
 * 搬运内容：tick 顺序、碰撞判定、掉落注册、冰冻状态、难度与分数。搬走内容：
 * {@code JPanel} 绘制（→ {@code platform/render}）、{@code Timer}（→ 渲染线程）、
 * {@code JFrame} 换面板（→ {@code ui} 导航）、{@code SoundManager}（→ {@link GameEvents}）。
 *
 * <p>TODO 级遗留：碰撞三段循环仍在本类内，拆 {@code CollisionSystem} 与难度 Profile 化
 * 一起排到 S2（见 {@code docs/03-实验二说明.md} 遗留问题）——先保证等价平移跑通。
 *
 * @author hitsz
 */
public abstract class GameLogic implements BattleContext {

    protected final HeroAircraft heroAircraft;
    protected final List<AbstractAircraft> enemyAircrafts;
    protected final List<BaseBullet> heroBullets;
    protected final List<BaseBullet> enemyBullets;
    protected final List<AbstractFlyingObject> drops;

    protected final long gameStartTime;
    protected int enemiesKilled;

    protected int enemyMaxNumber = GameConfig.BASE_ENEMY_MAX;
    protected int eliteEnemyMaxNumber = GameConfig.BASE_ELITE_MAX;
    protected int elitePlusEnemyMaxNumber = GameConfig.BASE_ELITE_PLUS_MAX;
    protected final int eliteProEnemyMaxNumber = GameConfig.BASE_ELITE_PRO_MAX;
    protected final int bossMaxNumber = GameConfig.BASE_BOSS_MAX;

    protected double enemySpawnCycle = GameConfig.DEFAULT_ENEMY_SPAWN_CYCLE;
    protected int enemySpawnCounter = 0;
    protected double eliteEnemySpawnCycle = GameConfig.DEFAULT_ELITE_SPAWN_CYCLE;
    protected int eliteEnemySpawnCounter = 0;
    protected double elitePlusEnemySpawnCycle = GameConfig.DEFAULT_ELITE_PLUS_SPAWN_CYCLE;
    protected int elitePlusEnemySpawnCounter = 0;
    protected double eliteProEnemySpawnCycle = GameConfig.DEFAULT_ELITE_PRO_SPAWN_CYCLE;
    protected int eliteProEnemySpawnCounter = 0;

    protected double shootHeroCycle = GameConfig.SHOOT_HERO_CYCLE;
    protected int shootHeroCounter = 0;

    protected double shootEnemyCycle = GameConfig.SHOOT_ENEMY_CYCLE;
    protected int shootEnemyCounter = 0;

    protected int score = 0;
    protected boolean gameOverFlag = false;

    protected boolean enemyFrozen = false;
    protected int freezeDuration = GameConfig.FREEZE_DURATION;
    protected int freezeTimer = 0;

    protected int bossScoreThreshold = GameConfig.BOSS_SCORE_THRESHOLD;
    protected int lastBossScore = 0;

    protected int difficultyLevel = 1;
    protected int initialDifficulty = 1;
    protected int baseEnemyPower = GameConfig.BASE_ENEMY_POWER;
    protected int backgroundIndex = 0;

    protected final String userName;
    protected final GameEvents events;
    protected final PlayerLevelManager levelManager;

    public GameLogic(String userName, int initialDifficulty, GameEvents events) {
        this.userName = userName;
        this.events = events;
        this.gameStartTime = System.currentTimeMillis();
        this.enemiesKilled = 0;
        this.difficultyLevel = initialDifficulty;
        this.initialDifficulty = initialDifficulty;

        heroAircraft = HeroAircraft.getInstance();
        // 重开要清的不只是 hp：maxHp（升级涨的）、道具剩余时长都在 HeroAircraft 里（S2 修）
        heroAircraft.resetForNewGame();

        enemyAircrafts = new CopyOnWriteArrayList<>();
        heroBullets = new CopyOnWriteArrayList<>();
        enemyBullets = new CopyOnWriteArrayList<>();
        drops = new CopyOnWriteArrayList<>();

        levelManager = PlayerLevelManager.getInstance();
        levelManager.reset();
        levelManager.setLevelUpListener(events::onLevelUp);
    }

    /**
     * 开局（对应桌面版 {@code action()} 里的 BGM 播放）。
     */
    public void start() {
        events.onBgmStart();
    }

    /**
     * 一个逻辑帧。渲染线程按 {@link GameConfig#TICK_MS} 节奏调用，
     * 顺序与桌面版 {@code TimerTask} 完全一致。
     */
    public void tick() {
        long gameTime = (System.currentTimeMillis() - gameStartTime) / 1000;

        updateDifficultyLevel(gameTime);
        printDifficultyInfo();

        enemySpawnCounter++;
        if (enemySpawnCounter >= enemySpawnCycle) {
            enemySpawnCounter = 0;
            spawnEnemyAircrafts();
        }

        heroShootAction();
        enemyShootAction();
        bulletsMoveAction();
        aircraftsMoveAction();
        dropsMoveAction();
        crashCheckAction();
        postProcessAction();
        heroAircraft.checkPowerUpExpiration();
        updateFreezeState();

        checkResultAction();
    }

    /**
     * 模板方法：生成敌机，由难度子类实现。
     */
    protected abstract void spawnEnemyAircrafts();

    /**
     * 模板方法：按时间推进难度等级。
     */
    protected abstract void updateDifficultyLevel(long gameTime);

    /**
     * 模板方法：输出难度提示（桌面版用于控制台，Android 侧可忽略）。
     */
    protected abstract void printDifficultyInfo();

    /**
     * 模板方法：当前难度是否允许召唤 Boss。
     */
    protected abstract boolean canSpawnBoss();

    /**
     * 模板方法：当前难度 Boss 血量。
     */
    protected abstract int getBossHp();

    protected void heroShootAction() {
        shootHeroCounter++;
        double speedBonus = levelManager.getShootSpeedBonus();
        double currentShootCycle = shootHeroCycle / (1 + speedBonus);
        if (shootHeroCounter >= currentShootCycle) {
            shootHeroCounter = 0;
            List<BaseBullet> bullets = heroAircraft.shoot();
            heroBullets.addAll(bullets);
        }
    }

    protected void enemyShootAction() {
        shootEnemyCounter++;
        if (shootEnemyCounter >= shootEnemyCycle) {
            shootEnemyCounter = 0;
            for (AbstractAircraft enemy : enemyAircrafts) {
                List<BaseBullet> bullets = enemy.shoot();
                for (BaseBullet bullet : bullets) {
                    bullet.setPower(bullet.getPower() + baseEnemyPower - GameConfig.BASE_ENEMY_POWER);
                }
                enemyBullets.addAll(bullets);
            }
        }
    }

    protected void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    protected void aircraftsMoveAction() {
        for (AbstractAircraft enemy : enemyAircrafts) {
            enemy.forward();
        }
    }

    protected void dropsMoveAction() {
        for (AbstractFlyingObject drop : drops) {
            drop.forward();
        }
    }

    /**
     * 碰撞判定与结算。三段循环（我方子弹打敌机、敌弹打英雄机、英雄机吃道具）原样保留。
     */
    protected void crashCheckAction() {
        List<BaseBullet> heroBulletsCopy = new LinkedList<>(heroBullets);
        for (BaseBullet bullet : heroBulletsCopy) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    continue;
                }
                if (enemyAircraft.crash(bullet)) {
                    enemyAircraft.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    events.onHit();
                    if (enemyAircraft.notValid()) {
                        // 按敌机类型给分与经验，数值取自 GameConfig 分数表
                        if (enemyAircraft instanceof Boss) {
                            score += GameConfig.SCORE_BOSS;
                            levelManager.addExp(GameConfig.SCORE_BOSS);
                            events.onBossBgmStop();
                        } else if (enemyAircraft instanceof EliteProEnemy) {
                            score += GameConfig.SCORE_ELITE_PRO;
                            levelManager.addExp(GameConfig.SCORE_ELITE_PRO);
                        } else if (enemyAircraft instanceof ElitePlusEnemy) {
                            score += GameConfig.SCORE_ELITE_PLUS;
                            levelManager.addExp(GameConfig.SCORE_ELITE_PLUS);
                        } else if (enemyAircraft instanceof EliteEnemy) {
                            score += GameConfig.SCORE_ELITE;
                            levelManager.addExp(GameConfig.SCORE_ELITE);
                        } else {
                            score += GameConfig.SCORE_MOB;
                            levelManager.addExp(GameConfig.SCORE_MOB);
                        }
                        enemiesKilled++;
                        List<Drop> newDrops = enemyAircraft.drop();
                        registerDropObservers(newDrops);
                        drops.addAll(newDrops);
                    }
                }
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                    enemyAircraft.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        List<BaseBullet> enemyBulletsCopy = new LinkedList<>(enemyBullets);
        for (BaseBullet bullet : enemyBulletsCopy) {
            if (bullet.notValid()) {
                continue;
            }
            if (heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        for (AbstractFlyingObject drop : drops) {
            if (drop.notValid()) {
                continue;
            }
            if (heroAircraft.crash(drop)) {
                events.onPowerUp();
                if (drop instanceof Hp) {
                    ((Hp) drop).activate(heroAircraft);
                }
                if (drop instanceof AddBullet) {
                    ((AddBullet) drop).activate(heroAircraft);
                }
                if (drop instanceof AddBulletPlus) {
                    ((AddBulletPlus) drop).activate(heroAircraft);
                }
                if (drop instanceof Bomb) {
                    ((Bomb) drop).activate(heroAircraft, this);
                    events.onBomb();
                }
                if (drop instanceof Freeze) {
                    ((Freeze) drop).activate(heroAircraft, this);
                }
            }
        }
    }

    /**
     * 新掉落的道具要挂上"当前在场敌机/敌弹"的观察者，这样炸弹/冰冻才能即时生效。
     */
    protected void registerDropObservers(List<Drop> newDrops) {
        for (Drop drop : newDrops) {
            for (AbstractAircraft enemy : enemyAircrafts) {
                if (enemy.isValid()) {
                    drop.addObserver(new EnemyObserver(enemy));
                }
            }
            for (BaseBullet bullet : enemyBullets) {
                if (bullet.isValid()) {
                    drop.addObserver(new EnemyObserver(bullet));
                }
            }
        }
    }

    protected void postProcessAction() {
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        drops.removeIf(AbstractFlyingObject::notValid);
    }

    protected void updateFreezeState() {
        if (enemyFrozen && freezeTimer > 0) {
            freezeTimer--;
            if (freezeTimer == 0) {
                enemyFrozen = false;
            }
        }

        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy.isValid()) {
                enemy.updateFreezeState();
            }
        }

        for (BaseBullet bullet : enemyBullets) {
            if (bullet.isValid()) {
                bullet.updateFreezeState();
            }
        }
    }

    protected void checkResultAction() {
        if (heroAircraft.getHp() <= 0 && !gameOverFlag) {
            gameOverFlag = true;
            events.onGameOver();
        }
    }

    public void clearEnemyBullets() {
        enemyBullets.clear();
    }

    public void freezeEnemies() {
        enemyFrozen = true;
        freezeTimer = freezeDuration;
        for (AbstractAircraft aircraft : enemyAircrafts) {
            aircraft.setSpeedX(0);
            aircraft.setSpeedY(0);
        }
    }

    // ==================== BattleContext ====================

    @Override
    public List<AbstractAircraft> getEnemyAircrafts() {
        return enemyAircrafts;
    }

    @Override
    public List<BaseBullet> getEnemyBullets() {
        return enemyBullets;
    }

    @Override
    public void addScore(int score) {
        this.score += score;
    }

    // ==================== 渲染/结算所需状态 ====================

    public HeroAircraft getHeroAircraft() {
        return heroAircraft;
    }

    public List<BaseBullet> getHeroBullets() {
        return heroBullets;
    }

    public List<AbstractFlyingObject> getDrops() {
        return drops;
    }

    public int getScore() {
        return score;
    }

    public int getEnemiesKilled() {
        return enemiesKilled;
    }

    public long getGameTime() {
        return System.currentTimeMillis() - gameStartTime;
    }

    public String getUserName() {
        return userName;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public int getInitialDifficulty() {
        return initialDifficulty;
    }

    public int getBackgroundIndex() {
        return backgroundIndex;
    }

    public boolean isGameOver() {
        return gameOverFlag;
    }

    public PlayerLevelManager getLevelManager() {
        return levelManager;
    }

    /**
     * 难度等级到中文名。与 {@code GameRecord} 里那份映射不同（那边 1→普通、2/3→困难），
     * 这里 1→简单；两处都按原样保留，未强行统一。
     */
    public String getDifficultyName(int difficulty) {
        switch (difficulty) {
            case 1:
                return "简单";
            case 2:
                return "普通";
            case 3:
                return "困难";
            case 4:
                return "试炼";
            case 5:
                return "炼狱";
            default:
                return "简单";
        }
    }
}

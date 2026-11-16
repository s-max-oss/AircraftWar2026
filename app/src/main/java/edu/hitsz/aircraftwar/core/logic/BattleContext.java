package edu.hitsz.aircraftwar.core.logic;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;

import java.util.List;

/**
 * 道具效果需要访问的战场视图。
 *
 * <p>迁移说明：桌面版的 {@code Drop.activate(HeroAircraft, Game)} 直接吃 {@code Game} 实体，
 * 而 {@code Game} 继承 {@code JPanel}，不可能进 core。这里按"逻辑层只依赖自己"的规则
 * 抽出最小接口，由 {@link GameLogic} 实现，行为不变（炸弹清屏、冰冻改速度、加分）。
 *
 * @author hitsz
 */
public interface BattleContext {

    /** 当前存活敌机（只读视图）。 */
    List<AbstractAircraft> getEnemyAircrafts();

    /** 当前敌方子弹（只读视图）。 */
    List<BaseBullet> getEnemyBullets();

    /** 加分。炸弹击落敌机时按分数表累加。 */
    void addScore(int score);
}

package edu.hitsz.aircraftwar.core.drop;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.basic.AbstractFlyingObject;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.BattleContext;
import edu.hitsz.aircraftwar.core.observer.Observable;
import edu.hitsz.aircraftwar.core.observer.Observer;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有掉落物的抽象父类（同时是被观察者）。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.Drop}（commit 50474cc）。
 * 改动仅一处：{@code activate(HeroAircraft, Game)} 的 {@code Game} 换成本包的
 * {@link BattleContext}——`Game` 继承 `JPanel`，不可能进 core。
 *
 * @author hitsz
 */
public abstract class Drop extends AbstractFlyingObject implements Observable {

    protected List<Observer> observers = new ArrayList<>();

    public Drop(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
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
     * 激活道具效果。
     *
     * @param heroAircraft 英雄机
     */
    public abstract void activate(HeroAircraft heroAircraft);

    /**
     * 激活道具效果（需要战场信息的道具，如炸弹/冰冻，重写此方法）。
     *
     * @param heroAircraft 英雄机
     * @param context      战场视图
     */
    public void activate(HeroAircraft heroAircraft, BattleContext context) {
        activate(heroAircraft);
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(this);
        }
    }

    @Override
    public List<Observer> getObservers() {
        return new ArrayList<>(observers);
    }
}

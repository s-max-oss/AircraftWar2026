package edu.hitsz.aircraftwar.core.observer;

/**
 * 观察者接口（观察者模式）。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.observer.Observer}（commit 50474cc）。
 */
public interface Observer {

    void update(Observable observable);
}

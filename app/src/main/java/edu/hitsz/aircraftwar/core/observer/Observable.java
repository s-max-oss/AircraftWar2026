package edu.hitsz.aircraftwar.core.observer;

/**
 * 被观察者接口（观察者模式）。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.observer.Observable}（commit 50474cc）。
 */
public interface Observable {

    void addObserver(Observer observer);

    void removeObserver(Observer observer);

    void notifyObservers();

    java.util.List<Observer> getObservers();
}

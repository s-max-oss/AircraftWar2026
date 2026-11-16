package edu.hitsz.aircraftwar.core.drop;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;
import edu.hitsz.aircraftwar.core.aircraft.EliteEnemy;
import edu.hitsz.aircraftwar.core.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraftwar.core.aircraft.EliteProEnemy;
import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.aircraft.MobEnemy;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.BattleContext;

import java.util.List;

/**
 * 炸弹道具：清空敌弹、击落普通敌机、对王牌敌机扣血、Boss 免疫。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.drop.Bomb}（commit 50474cc）。
 * 原实现把 30/50/10 三档分数写死在炸弹里，这里改用 {@link GameConfig} 的分数表，
 * 数值完全相同（铁律 3 常量集中）。
 *
 * @author hitsz
 */
public class Bomb extends Drop {

    public Bomb(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public GameConfig.Sprite getSprite() {
        return GameConfig.Sprite.PROP_BOMB;
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        vanish();
    }

    @Override
    public void activate(HeroAircraft heroAircraft, BattleContext context) {
        List<AbstractAircraft> enemyAircrafts = context.getEnemyAircrafts();
        List<BaseBullet> enemyBullets = context.getEnemyBullets();

        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy.isValid()) {
                if (enemy instanceof Boss) {
                    continue;
                } else if (enemy instanceof EliteProEnemy) {
                    enemy.decreaseHp(100);
                } else {
                    enemy.vanish();
                    int score = 0;
                    if (enemy instanceof EliteEnemy) {
                        score = GameConfig.SCORE_ELITE;
                    } else if (enemy instanceof ElitePlusEnemy) {
                        score = GameConfig.SCORE_ELITE_PLUS;
                    } else if (enemy instanceof MobEnemy) {
                        score = GameConfig.SCORE_MOB;
                    }
                    context.addScore(score);
                }
            }
        }

        for (BaseBullet bullet : enemyBullets) {
            if (bullet.isValid()) {
                bullet.vanish();
            }
        }

        vanish();
    }
}

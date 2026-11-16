package edu.hitsz.aircraftwar.core.logic;

/**
 * 逻辑层向外发的事件：音效、BGM、结算。
 *
 * <p>桌面版这些调用直接落在 {@code SoundManager} 与 {@code JFrame} 换面板上；
 * 迁移后 core 只负责"发生了什么事"，具体发声与跳转由 {@code platform}/{@code ui} 决定。
 *
 * @author hitsz
 */
public interface GameEvents {

    /** 开局：放 BGM。 */
    void onBgmStart();

    /** 打中敌机。 */
    void onHit();

    /** 捡到道具。 */
    void onPowerUp();

    /** 放炸弹。 */
    void onBomb();

    /** 升级。 */
    void onLevelUp(int level, int hpBonus, int powerBonus);

    /** Boss 出场：切 Boss BGM。 */
    void onBossBgmStart();

    /** Boss 被击落：切回普通 BGM。 */
    void onBossBgmStop();

    /** 游戏结束：由 UI 决定跳到结算页。 */
    void onGameOver();
}

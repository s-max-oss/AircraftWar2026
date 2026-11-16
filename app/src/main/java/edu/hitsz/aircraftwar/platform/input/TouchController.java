package edu.hitsz.aircraftwar.platform.input;

import android.view.MotionEvent;

import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 触摸控制英雄机。
 *
 * <p>迁移说明：替代桌面版 {@code HeroController}（commit 50474cc，{@code MouseAdapter.mouseDragged}）。
 * 原实现只响应拖动，且坐标越界直接丢弃；这里保留"越界忽略"的语义，
 * 额外接受 {@code ACTION_DOWN}（手指刚落下就吸附，移动端手感更好），
 * 逻辑坐标仍是 512×768，屏幕坐标由 {@link Mapper} 换算，游戏内数值一律不改。
 *
 * @author hitsz
 */
public class TouchController {

    /** 视图坐标 → 逻辑坐标的换算，由 {@code GameView} 提供。 */
    public interface Mapper {
        /**
         * @param viewX 视图 X（像素）
         * @param viewY 视图 Y（像素）
         * @param out   输出数组，长度 ≥ 2，写入逻辑坐标
         * @return 是否落在逻辑画布内
         */
        boolean mapToLogic(float viewX, float viewY, float[] out);
    }

    private final HeroAircraft hero;
    private final Mapper mapper;
    private final float[] point = new float[2];

    public TouchController(HeroAircraft hero, Mapper mapper) {
        this.hero = hero;
        this.mapper = mapper;
    }

    /**
     * 处理一次触摸事件。
     *
     * @param event 触摸事件
     * @return 是否消费（按下/移动都消费，避免父容器拦截）
     */
    public boolean onTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE) {
            return false;
        }
        if (!mapper.mapToLogic(event.getX(), event.getY(), point)) {
            // 原桌面版：超出窗口就忽略这次移动
            return true;
        }
        float x = point[0];
        float y = point[1];
        if (x < 0 || x > GameConfig.WINDOW_WIDTH || y < 0 || y > GameConfig.WINDOW_HEIGHT) {
            return true;
        }
        hero.setLocation(x, y);
        return true;
    }
}

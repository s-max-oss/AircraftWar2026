package edu.hitsz.aircraftwar.platform.render;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.GameLogic;
import edu.hitsz.aircraftwar.platform.input.TouchController;
import edu.hitsz.aircraftwar.platform.resource.ImageStore;

/**
 * 游戏画面：SurfaceView + 固定步长游戏循环线程。
 *
 * <p>迁移说明：替代桌面版 {@code Game extends JPanel} + {@code java.util.Timer}（commit 50474cc）。
 * 桌面版是"Timer 线程推进逻辑 + 事件线程 repaint"，两个线程都会碰状态；
 * 这里改成**单一循环线程**：同一线程里先 {@code tick()} 再绘制，状态只被这一个线程改，
 * 不再有竞态。UI 线程只负责 start/pause 与把触摸坐标递进来。
 *
 * <p>坐标映射：逻辑画布宽固定 512、高按屏幕比例注入（见 {@link GameConfig#WINDOW_HEIGHT}），
 * 触摸坐标反向换算，游戏内数值一律不改——这样不同分辨率手机上判定与桌面版一致。
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, TouchController.Mapper {

    private final SurfaceHolder holder;
    private final ImageStore images;
    private final GameRenderer renderer;

    private volatile boolean running;
    private Thread loopThread;

    private GameLogic game;
    private TouchController touchController;

    // 视口换算：surface 按视口等比固定（宽度 ≤720），放大交给系统合成器；触摸用 viewScale 反算
    private float viewScale = 1f;
    private final float[] logicPoint = new float[2];

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        images = new ImageStore(context.getResources());
        renderer = new GameRenderer(images);
    }

    /**
     * 绑定逻辑层。必须在 {@link #surfaceCreated} 之后调用（持有 Surface 前不启动循环）。
     *
     * @param game 具体难度实例
     */
    public void setGame(GameLogic game) {
        this.game = game;
        this.touchController = new TouchController(game.getHeroAircraft(), this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // Surface 缓冲按视口比例固定，但宽度压到 720：铺满屏幕的同时，
        // 每帧只写 720×1600 ≈ 1.15M 像素，而不是 1080×2400 ≈ 2.6M。
        // S1 实测过"画满整屏"就是 12fps 的元凶，放大交给系统合成器最省。
        int surfaceWidth = Math.max(GameConfig.WINDOW_WIDTH, Math.min(720, getWidth()));
        int surfaceHeight = Math.round(surfaceWidth * (float) GameConfig.WINDOW_HEIGHT
                / GameConfig.WINDOW_WIDTH);
        surfaceHolder.setFixedSize(surfaceWidth, surfaceHeight);
        running = true;
        loopThread = new Thread(this, "game-loop");
        loopThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        updateViewport();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopLoop();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateViewport();
    }

    @Override
    public void run() {
        long windowStart = SystemClock.elapsedRealtime();
        int frames = 0;
        long lastFrame = SystemClock.elapsedRealtime();
        long accumulator = 0;

        while (running) {
            long now = SystemClock.elapsedRealtime();
            accumulator += now - lastFrame;
            lastFrame = now;

            // 固定步长：不管渲染多慢，逻辑保持 25 次/秒。
            // 掉帧时一帧最多补 4 步（160ms），再多就丢弃积压，避免越积越卡。
            int steps = 0;
            while (accumulator >= GameConfig.TICK_MS && steps < 4) {
                if (game != null && !game.isGameOver()) {
                    game.tick();
                }
                accumulator -= GameConfig.TICK_MS;
                steps++;
            }
            if (steps == 4) {
                accumulator = 0;
            }

            renderFrame();
            frames++;

            // 每 50 帧（约 2 秒）打一条实测帧率，用于验收时对比桌面版
            if (frames >= 50) {
                long elapsed = SystemClock.elapsedRealtime() - windowStart;
                Log.i("AircraftWar", "fps≈" + String.format(java.util.Locale.US, "%.1f", frames * 1000f / elapsed)
                        + " (" + frames + " 帧 / " + elapsed + "ms)");
                frames = 0;
                windowStart = SystemClock.elapsedRealtime();
            }

            long cost = SystemClock.elapsedRealtime() - now;
            long sleep = GameConfig.TICK_MS - cost;
            if (sleep > 0) {
                SystemClock.sleep(sleep);
            }
        }
    }

    private void renderFrame() {
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas != null && game != null) {
                // Surface 宽度是 720、逻辑宽 512：缩放由渲染层一次性完成，不再有 letterbox 偏移
                float scale = canvas.getWidth() / (float) GameConfig.WINDOW_WIDTH;
                renderer.draw(canvas, game, scale, 0f, 0f);
            }
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * 触摸换算：视图坐标按 viewScale 折算回 512×视口高 的逻辑坐标。
     * surface 缓冲是固定尺寸，与视图尺寸无关，所以不能用 surface 尺寸算。
     */
    private void updateViewport() {
        int viewWidth = getWidth();
        if (viewWidth > 0) {
            viewScale = viewWidth / (float) GameConfig.WINDOW_WIDTH;
        }
    }

    @Override
    public boolean mapToLogic(float viewX, float viewY, float[] out) {
        if (viewScale <= 0f) {
            return false;
        }
        out[0] = viewX / viewScale;
        out[1] = viewY / viewScale;
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touchController == null) {
            return super.onTouchEvent(event);
        }
        return touchController.onTouch(event) || super.onTouchEvent(event);
    }

    /**
     * 生命周期：Activity onPause 调用，停循环线程。
     */
    public void pause() {
        stopLoop();
    }

    /**
     * 生命周期：Activity onResume 调用。Surface 仍有效时恢复循环。
     */
    public void resume() {
        if (!running && holder.getSurface() != null && holder.getSurface().isValid()) {
            surfaceCreated(holder);
        }
    }

    private void stopLoop() {
        running = false;
        if (loopThread != null) {
            try {
                loopThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            loopThread = null;
        }
    }
}

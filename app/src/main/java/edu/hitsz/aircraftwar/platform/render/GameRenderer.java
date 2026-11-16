package edu.hitsz.aircraftwar.platform.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.core.basic.AbstractFlyingObject;
import edu.hitsz.aircraftwar.core.bullet.BaseBullet;
import edu.hitsz.aircraftwar.core.config.GameConfig;
import edu.hitsz.aircraftwar.core.logic.GameLogic;
import edu.hitsz.aircraftwar.core.logic.PlayerLevelManager;
import edu.hitsz.aircraftwar.platform.resource.ImageStore;

/**
 * 逐帧绘制：原来 {@code Game.paint(Graphics)} 的内容 1:1 搬到 Canvas 上。
 *
 * <p>迁移说明：来自 {@code edu.hitsz.application.Game#paint}（commit 50474cc）。
 * 绘图顺序（背景→道具→我方子弹→敌方子弹→敌机→英雄机→HUD）与坐标公式原样保留；
 * 背景滚动值 {@code backGroundTop} 归渲染层所有，因为它是视觉状态而不是游戏状态。
 *
 * <p>性能取舍：世界画面先画到 512×768 的离屏位图，再整块放大贴到屏幕，
 * 避免每帧对"两张全屏背景"做缩放采样（模拟器软件渲染下这是主要开销）。
 * HUD 单独以屏幕分辨率绘制，文字不会被放大糊掉。
 */
public final class GameRenderer {

    private final ImageStore images;

    private final Paint worldPaint = new Paint();
    private final Paint hudPaint = new Paint();
    private final Paint barPaint = new Paint();
    private final Rect dst = new Rect();

    /** 512×768 世界画布，懒创建。 */
    private Bitmap frameBuffer;
    private Canvas frameCanvas;

    /** 背景滚动偏移，对应桌面版 {@code Game.backGroundTop}。 */
    private int backgroundTop = 0;

    public GameRenderer(ImageStore images) {
        this.images = images;
        hudPaint.setTypeface(Typeface.DEFAULT_BOLD);
        hudPaint.setColor(Color.WHITE);
        hudPaint.setAntiAlias(true);
    }

    /**
     * 画一帧。
     *
     * @param canvas   目标画布（已由 GameView 加锁）
     * @param game     逻辑状态
     * @param scale    逻辑像素 → 屏幕像素的缩放
     * @param offsetX  letterbox 左偏移
     * @param offsetY  letterbox 上偏移
     */
    public void draw(Canvas canvas, GameLogic game, float scale, float offsetX, float offsetY) {
        ensureBuffer();

        // 1) 世界层：512×768 上按逻辑坐标 1:1 画，不做任何缩放采样
        frameCanvas.drawColor(Color.BLACK);
        drawBackground(frameCanvas, game.getBackgroundIndex());
        for (AbstractFlyingObject drop : game.getDrops()) {
            draw(frameCanvas, drop);
        }
        for (BaseBullet bullet : game.getHeroBullets()) {
            draw(frameCanvas, bullet);
        }
        for (BaseBullet bullet : game.getEnemyBullets()) {
            draw(frameCanvas, bullet);
        }
        for (AbstractAircraft enemy : game.getEnemyAircrafts()) {
            draw(frameCanvas, enemy);
        }
        draw(frameCanvas, game.getHeroAircraft());

        // 2) 整块放大贴到屏幕（letterbox 居中）
        dst.set(Math.round(offsetX), Math.round(offsetY),
                Math.round(offsetX + GameConfig.WINDOW_WIDTH * scale),
                Math.round(offsetY + GameConfig.WINDOW_HEIGHT * scale));
        canvas.drawBitmap(frameBuffer, null, dst, worldPaint);

        // 3) HUD：直接按屏幕分辨率画，保证文字清晰
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);
        drawHud(canvas, game);
        canvas.restore();
    }

    private void ensureBuffer() {
        if (frameBuffer == null) {
            frameBuffer = Bitmap.createBitmap(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            frameCanvas = new Canvas(frameBuffer);
        }
    }

    private void drawBackground(Canvas canvas, int backgroundIndex) {
        Bitmap bg = images.background(backgroundIndex);
        canvas.drawBitmap(bg, 0, backgroundTop - GameConfig.WINDOW_HEIGHT, worldPaint);
        canvas.drawBitmap(bg, 0, backgroundTop, worldPaint);
        backgroundTop += 1;
        if (backgroundTop == GameConfig.WINDOW_HEIGHT) {
            backgroundTop = 0;
        }
    }

    private void draw(Canvas canvas, AbstractFlyingObject object) {
        if (object.notValid()) {
            return;
        }
        Bitmap bitmap = images.get(object.getSprite());
        canvas.drawBitmap(bitmap,
                object.getLocationX() - object.getWidth() / 2f,
                object.getLocationY() - object.getHeight() / 2f,
                worldPaint);
    }

    private void drawHud(Canvas canvas, GameLogic game) {
        HeroAircraft hero = game.getHeroAircraft();
        PlayerLevelManager level = game.getLevelManager();

        hudPaint.setTextSize(18f);
        canvas.drawText("分数: " + game.getScore(), 10, 25, hudPaint);
        canvas.drawText("击杀: " + game.getEnemiesKilled(), 10, 50, hudPaint);
        canvas.drawText("血量: " + hero.getHp(), 10, 75, hudPaint);
        canvas.drawText("难度: " + game.getDifficultyName(game.getDifficultyLevel()),
                GameConfig.WINDOW_WIDTH - 120, 25, hudPaint);

        int rightPanelX = GameConfig.WINDOW_WIDTH - 150;
        canvas.drawText("等级: " + level.getLevel(), rightPanelX, 50, hudPaint);

        double speedBonus = level.getShootSpeedBonus();
        if (speedBonus > 0) {
            hudPaint.setTextSize(12f);
            canvas.drawText("攻速+" + (int) (speedBonus * 100) + "%", rightPanelX, 65, hudPaint);
        }

        int expBarWidth = 120;
        int expBarHeight = 8;
        int expBarY = speedBonus > 0 ? 75 : 65;

        barPaint.setColor(Color.argb(150, 50, 50, 50));
        canvas.drawRect(rightPanelX, expBarY, rightPanelX + expBarWidth, expBarY + expBarHeight, barPaint);

        barPaint.setColor(Color.rgb(0, 255, 0));
        canvas.drawRect(rightPanelX, expBarY,
                rightPanelX + (int) (expBarWidth * level.getExpProgress()), expBarY + expBarHeight, barPaint);

        hudPaint.setTextSize(12f);
        canvas.drawText(level.getExp() + "/" + level.getCurrentThreshold(), rightPanelX, expBarY + 20, hudPaint);

        if (game.isGameOver()) {
            hudPaint.setTextSize(32f);
            canvas.drawText("GAME OVER", 150, GameConfig.WINDOW_HEIGHT / 2f, hudPaint);
        }
    }

    /**
     * 重开时复位背景滚动（S2 重玩入口用）。
     */
    public void resetBackground() {
        backgroundTop = 0;
    }
}

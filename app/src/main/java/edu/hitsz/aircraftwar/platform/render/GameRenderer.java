package edu.hitsz.aircraftwar.platform.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import edu.hitsz.aircraftwar.core.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.core.aircraft.Boss;
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
 * <p>性能取舍：世界画面先画到 {@code 512×WINDOW_HEIGHT} 的离屏位图，再整块放大贴到屏幕，
 * 避免每帧对"两张全屏背景"做缩放采样（模拟器软件渲染下这是主要开销）。
 * HUD 单独以屏幕分辨率绘制，文字不会被放大糊掉。
 *
 * <p>视口（S2 修复）：逻辑高度由 {@link GameConfig#WINDOW_HEIGHT} 决定。手机屏比 2:3 更高，
 * 所以背景按"高度铺满、左右裁切"（centerCrop）画，战场向上下延伸，不再出现黑边。
 */
public final class GameRenderer {

    private static final int HUD_INK = Color.rgb(34, 48, 74);
    private static final int HUD_PAPER = Color.rgb(255, 248, 230);
    private static final int HUD_YELLOW = Color.rgb(255, 201, 77);
    private static final int HUD_GREEN = Color.rgb(95, 194, 74);
    private static final int HUD_BLUE = Color.rgb(112, 183, 255);
    private static final int HUD_RED = Color.rgb(229, 72, 77);

    private final ImageStore images;

    private final Paint worldPaint = new Paint();
    private final Paint hudPaint = new Paint();
    private final Paint barPaint = new Paint();
    private final Rect dst = new Rect();

    /** 512×WINDOW_HEIGHT 世界画布，懒创建（视口变化时重建）。 */
    private Bitmap frameBuffer;
    private Canvas frameCanvas;
    private int frameBufferHeight = -1;

    /** 按视口缩放后的背景缓存（centerCrop）：换背景或换视口时重建，避免逐帧采样。 */
    private Bitmap scaledBackground;
    private int scaledBackgroundIndex = -1;
    private int scaledBackgroundHeight = -1;

    /** 背景滚动偏移，对应桌面版 {@code Game.backGroundTop}。 */
    private int backgroundTop = 0;

    public GameRenderer(ImageStore images) {
        this.images = images;
        // 离屏位图要放大贴到 Surface（512 → 720），开双线性过滤，边缘不出锯齿
        worldPaint.setFilterBitmap(true);
        hudPaint.setTypeface(Typeface.DEFAULT_BOLD);
        hudPaint.setColor(Color.WHITE);
        hudPaint.setAntiAlias(true);
        barPaint.setAntiAlias(true);
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

        // 1) 世界层：512×视口高 上按逻辑坐标 1:1 画，不做任何缩放采样
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
        if (frameBuffer == null || frameBufferHeight != GameConfig.WINDOW_HEIGHT) {
            if (frameBuffer != null && !frameBuffer.isRecycled()) {
                frameBuffer.recycle();
            }
            frameBuffer = Bitmap.createBitmap(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            frameCanvas = new Canvas(frameBuffer);
            frameBufferHeight = GameConfig.WINDOW_HEIGHT;
        }
    }

    /**
     * 背景：先把原图按"高度铺满"缩放一次并缓存（centerCrop），再上下拼接两张做滚动。
     *
     * <p>为什么不直接把 512×768 的图铺到 512×1138 的画布上：那样底下会露黑；
     * 而每帧现做缩放采样又是 S1 定位到的性能大头，所以只缩放一次。
     */
    private void drawBackground(Canvas canvas, int backgroundIndex) {
        Bitmap bg = scaledBackground(backgroundIndex);
        if (bg == null) {
            canvas.drawColor(Color.BLACK);
            return;
        }
        // 宽度超出 512 时左右各裁一半：保持纵向比例，不把地形拉长
        int dx = (GameConfig.WINDOW_WIDTH - bg.getWidth()) / 2;
        canvas.drawBitmap(bg, dx, backgroundTop - GameConfig.WINDOW_HEIGHT, worldPaint);
        canvas.drawBitmap(bg, dx, backgroundTop, worldPaint);
        backgroundTop += 1;
        if (backgroundTop >= GameConfig.WINDOW_HEIGHT) {
            backgroundTop = 0;
        }
    }

    private Bitmap scaledBackground(int backgroundIndex) {
        if (scaledBackground != null
                && scaledBackgroundIndex == backgroundIndex
                && scaledBackgroundHeight == GameConfig.WINDOW_HEIGHT
                && !scaledBackground.isRecycled()) {
            return scaledBackground;
        }
        Bitmap source = images.background(backgroundIndex);
        if (source == null) {
            return null;
        }
        float scale = GameConfig.WINDOW_HEIGHT / (float) source.getHeight();
        int width = Math.round(source.getWidth() * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(source, width, GameConfig.WINDOW_HEIGHT, true);
        if (scaledBackground != null && !scaledBackground.isRecycled()) {
            scaledBackground.recycle();
        }
        scaledBackground = scaled;
        scaledBackgroundIndex = backgroundIndex;
        scaledBackgroundHeight = GameConfig.WINDOW_HEIGHT;
        return scaledBackground;
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

        // 两块小底板只占顶部，不把游戏画面盖成表单。
        barPaint.setColor(Color.argb(210, 255, 248, 230));
        canvas.drawRoundRect(12, 12, 213, 91, 14, 14, barPaint);
        canvas.drawRoundRect(299, 12, 500, 91, 14, 14, barPaint);
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStrokeWidth(2f);
        barPaint.setColor(HUD_INK);
        canvas.drawRoundRect(12, 12, 213, 91, 14, 14, barPaint);
        canvas.drawRoundRect(299, 12, 500, 91, 14, 14, barPaint);
        barPaint.setStyle(Paint.Style.FILL);

        hudPaint.setColor(HUD_INK);
        hudPaint.setTextSize(16f);
        canvas.drawText("SCORE", 25, 31, hudPaint);
        hudPaint.setTextSize(29f);
        canvas.drawText(String.valueOf(game.getScore()), 25, 61, hudPaint);
        hudPaint.setTextSize(15f);
        canvas.drawText("击杀 " + game.getEnemiesKilled() + "  ·  "
                + game.getDifficultyName(game.getDifficultyLevel()), 25, 80, hudPaint);

        float hpFraction = hero.getMaxHp() <= 0 ? 0f
                : Math.max(0f, Math.min(1f, hero.getHp() / (float) hero.getMaxHp()));
        hudPaint.setTextSize(15f);
        canvas.drawText("HP  " + hero.getHp() + "/" + hero.getMaxHp(), 312, 31, hudPaint);
        drawBar(canvas, 312, 38, 175, 9, hpFraction, hpFraction < 0.3f ? HUD_RED : HUD_GREEN);
        canvas.drawText("LV." + level.getLevel() + "  ·  EXP " + level.getExp()
                + "/" + level.getCurrentThreshold(), 312, 64, hudPaint);
        drawBar(canvas, 312, 71, 175, 7, (float) level.getExpProgress(), HUD_BLUE);

        for (AbstractAircraft enemy : game.getEnemyAircrafts()) {
            if (enemy instanceof Boss && !enemy.notValid()) {
                float bossFraction = enemy.getMaxHp() <= 0 ? 0f
                        : Math.max(0f, Math.min(1f, enemy.getHp() / (float) enemy.getMaxHp()));
                barPaint.setColor(Color.argb(225, 34, 48, 74));
                canvas.drawRoundRect(80, 105, 432, 132, 12, 12, barPaint);
                drawBar(canvas, 89, 114, 334, 9, bossFraction, HUD_RED);
                hudPaint.setColor(HUD_PAPER);
                hudPaint.setTextSize(13f);
                canvas.drawText("BOSS", 92, 129, hudPaint);
                break;
            }
        }

        if (game.isGameOver()) {
            barPaint.setColor(Color.argb(150, 12, 25, 48));
            canvas.drawRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, barPaint);
            hudPaint.setColor(HUD_YELLOW);
            hudPaint.setTextAlign(Paint.Align.CENTER);
            hudPaint.setTextSize(36f);
            canvas.drawText("战斗结束", GameConfig.WINDOW_WIDTH / 2f,
                    GameConfig.WINDOW_HEIGHT / 2f, hudPaint);
            hudPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawBar(Canvas canvas, float x, float y, float width, float height,
                         float fraction, int fillColor) {
        barPaint.setColor(HUD_INK);
        canvas.drawRoundRect(x, y, x + width, y + height, height / 2, height / 2, barPaint);
        barPaint.setColor(fillColor);
        float inset = 2f;
        float fillWidth = Math.max(0f, (width - 2 * inset) * Math.max(0f, Math.min(1f, fraction)));
        if (fillWidth > 0f) {
            canvas.drawRoundRect(x + inset, y + inset, x + inset + fillWidth,
                    y + height - inset, (height - 2 * inset) / 2,
                    (height - 2 * inset) / 2, barPaint);
        }
    }

    /**
     * 重开时复位背景滚动（S2 重玩入口用）。
     */
    public void resetBackground() {
        backgroundTop = 0;
    }
}

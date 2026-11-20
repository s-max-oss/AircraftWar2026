package edu.hitsz.aircraftwar.platform.resource;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.EnumMap;
import java.util.Map;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.core.config.GameConfig;

/**
 * 图片仓库：按 {@link GameConfig.Sprite} 提供 Bitmap，首次访问时解码并缓存。
 *
 * <p>迁移说明：替代桌面版 {@code ImageManager}（commit 50474cc）。
 * 原实现用 {@code ImageIO} 按类名反查 {@code src/images/}，Android 侧没有工作目录也没有
 * ImageIO，改为资源 id + {@code BitmapFactory}，并且**必须放在 {@code drawable-nodpi}**
 * 才不会按屏幕密度缩放——缩放会改变图片尺寸，而碰撞框直接用尺寸，缩了就会出现
 * "看得见打不着"。
 */
public final class ImageStore {

    private final Resources resources;
    private final Map<GameConfig.Sprite, Bitmap> cache = new EnumMap<>(GameConfig.Sprite.class);

    public ImageStore(Resources resources) {
        this.resources = resources;
    }

    /**
     * 取素材对应的位图（带缓存）。
     *
     * @param sprite 素材标识
     * @return 解码后的位图
     */
    public Bitmap get(GameConfig.Sprite sprite) {
        Bitmap bitmap = cache.get(sprite);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(resources, resIdOf(sprite));
            if (bitmap == null) {
                throw new IllegalStateException("资源解码失败: " + sprite);
            }
            cache.put(sprite, bitmap);
        }
        return bitmap;
    }

    /**
     * 素材到 drawable 的映射。注意资源名不能有大写字母，
     * 因此源图的 {@code elitePlus.png} / {@code elitePro.png} / {@code prop_bulletPlus.png}
     * 落位时改名为 {@code elite_plus.png} / {@code elite_pro.png} / {@code prop_bullet_plus.png}。
     */
    private static int resIdOf(GameConfig.Sprite sprite) {
        switch (sprite) {
            case BACKGROUND:
                return R.drawable.bg;
            case HERO:
                return R.drawable.hero;
            case MOB:
                return R.drawable.mob;
            case ELITE:
                return R.drawable.elite;
            case ELITE_PLUS:
                return R.drawable.elite_plus;
            case ELITE_PRO:
                return R.drawable.elite_pro;
            case BOSS:
                return R.drawable.boss;
            case HERO_BULLET:
                return R.drawable.bullet_hero;
            case ENEMY_BULLET:
                return R.drawable.bullet_enemy;
            case ICON:
                return R.drawable.img;
            case PROP_HP:
                return R.drawable.prop_blood;
            case PROP_BOMB:
                return R.drawable.prop_bomb;
            case PROP_BULLET:
                return R.drawable.prop_bullet;
            case PROP_BULLET_PLUS:
                return R.drawable.prop_bullet_plus;
            case PROP_FREEZE:
                return R.drawable.prop_freeze;
            default:
                throw new IllegalArgumentException("未映射的素材: " + sprite);
        }
    }

    /**
     * 背景按关卡索引取图：0→bg，1→bg2 … 4→bg5。
     *
     * @param backgroundIndex 难度档给出的背景索引
     * @return 背景位图
     */
    public Bitmap background(int backgroundIndex) {
        switch (backgroundIndex) {
            case 1:
                return decodeResource(R.drawable.bg2);
            case 2:
                return decodeResource(R.drawable.bg3);
            case 3:
                return decodeResource(R.drawable.bg4);
            case 4:
                return decodeResource(R.drawable.bg5);
            default:
                return get(GameConfig.Sprite.BACKGROUND);
        }
    }

    private Bitmap decodeResource(int resId) {
        Bitmap cached = backgroundCache.get(resId);
        if (cached != null) {
            return cached;
        }
        Bitmap decoded = BitmapFactory.decodeResource(resources, resId);
        backgroundCache.put(resId, decoded);
        return decoded;
    }

    /** 背景图专用缓存：5 张 512×768 解码后约 7.5MB，属于内存里该花的钱。 */
    private final Map<Integer, Bitmap> backgroundCache = new java.util.HashMap<>();
}

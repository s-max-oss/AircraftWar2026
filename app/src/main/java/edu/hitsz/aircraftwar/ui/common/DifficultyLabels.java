package edu.hitsz.aircraftwar.ui.common;

import android.content.Context;

import edu.hitsz.aircraftwar.R;

/**
 * 初始难度（1/2/3）到中文名的映射。
 *
 * <p>为什么不复用 {@code GameLogic.getDifficultyName(int)}：那个方法是实例方法，
 * 且语义是"游戏内 1–5 级"（5=炼狱），与"玩家选的档位"不是一回事——
 * 同一个数字在两处含义不同，正是 S1 留下的坑（{@code docs/03} §3 第 6 条）。
 * 这里单独收一处，避免再混。
 */
public final class DifficultyLabels {

    private DifficultyLabels() {
    }

    public static String of(Context context, int initialDifficulty) {
        switch (initialDifficulty) {
            case 1:
                return context.getString(R.string.menu_difficulty_simple);
            case 3:
                return context.getString(R.string.menu_difficulty_hard);
            default:
                return context.getString(R.string.menu_difficulty_normal);
        }
    }
}

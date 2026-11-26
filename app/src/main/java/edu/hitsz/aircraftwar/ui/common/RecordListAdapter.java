package edu.hitsz.aircraftwar.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.core.record.GameRecord;

/**
 * 排行榜/记录页共用的 BaseAdapter（课件 p8–p9 的 4 个必写方法与 getView 复用）。
 *
 * <p>排行榜模式：名次=当前位置+1，第一行高亮；记录模式：名次仍是序号，但不做高亮，
 * 副信息多显示终局等级与记录时间。两种模式共用行布局，区别只在绑定文案。
 *
 * <p>删除按钮的点击在 Adapter 内部绑定（课件 p7：BaseAdapter 能在内部给控件绑事件），
 * 通过 {@link OnDeleteListener} 把“删哪条”交回页面，Adapter 不碰数据库。
 */
public class RecordListAdapter extends BaseAdapter {

    public interface OnDeleteListener {
        void onDelete(GameRecord record);
    }

    private final LayoutInflater inflater;
    private final Context context;
    private final boolean rankingMode;
    private final OnDeleteListener deleteListener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    private List<GameRecord> items = Collections.emptyList();

    public RecordListAdapter(Context context, boolean rankingMode, OnDeleteListener deleteListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.rankingMode = rankingMode;
        this.deleteListener = deleteListener;
    }

    public void submit(List<GameRecord> records) {
        // Adapter 只负责“数据变了，通知列表刷新”；查询/排序在 ViewModel 的 Executor 里完成。
        items = records == null ? Collections.emptyList() : records;
        notifyDataSetChanged();
    }

    /** BaseAdapter 四个必写方法之一：列表行数（课件 p8）。 */
    @Override
    public int getCount() {
        return items.size();
    }

    /** 返回指定位置的数据；删除按钮的回调也靠它拿到 GameRecord。 */
    @Override
    public GameRecord getItem(int position) {
        return items.get(position);
    }

    /** 主键来自 SQLite 的 _id；配合 hasStableIds=true，ListView 可保持选中/滚动位置。 */
    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    /** BaseAdapter 默认 false；我们的行 id 稳定，显式打开才能让 ListView 利用它。 */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * 核心方法（课件 p9）：convertView 为空才 inflate，否则复用；ViewHolder 缓存行内控件；
     * 每次都必须重新 setText/换背景，否则复用的行会带着上一行的数据。
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.item_record, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        GameRecord record = items.get(position);
        boolean top = rankingMode && position == 0;
        view.setBackgroundResource(top ? R.drawable.bg_arcade_row_top : R.drawable.bg_arcade_row);

        holder.rank.setText(String.valueOf(position + 1));
        holder.rank.setTextColor(context.getColor(top ? R.color.arcade_orange : R.color.arcade_ink));
        holder.name.setText(record.getName());
        holder.score.setText(String.valueOf(record.getScore()));

        String difficulty = DifficultyLabels.of(context, record.getInitialDifficulty());
        String duration = formatDuration(record.getGameTime());
        if (rankingMode) {
            holder.meta.setText(context.getString(R.string.record_meta_ranking,
                    difficulty, record.getEnemiesKilled(), duration));
            holder.metaSub.setVisibility(View.GONE);
        } else {
            holder.meta.setText(context.getString(R.string.record_meta_record,
                    difficulty, record.getDifficulty(), record.getEnemiesKilled()));
            String recordedAt = record.getRecordTime() > 0
                    ? dateFormat.format(new Date(record.getRecordTime())) : "--";
            holder.metaSub.setText(context.getString(R.string.record_meta_record_sub,
                    duration, recordedAt));
            holder.metaSub.setVisibility(View.VISIBLE);
        }

        holder.delete.setContentDescription(context.getString(
                R.string.record_delete_desc, record.getName(), record.getScore()));
        holder.delete.setOnClickListener(v -> deleteListener.onDelete(record));
        return view;
    }

    static String formatDuration(long milliseconds) {
        long totalSeconds = Math.max(0L, milliseconds / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /** 行内控件缓存；避免每次 getView 都 findViewById（课件 p9 的 ViewHolder 做法）。 */
    private static final class ViewHolder {
        final TextView rank;
        final TextView name;
        final TextView meta;
        final TextView metaSub;
        final TextView score;
        final TextView delete;

        ViewHolder(View row) {
            rank = row.findViewById(R.id.textRank);
            name = row.findViewById(R.id.textName);
            meta = row.findViewById(R.id.textMeta);
            metaSub = row.findViewById(R.id.textMetaSub);
            score = row.findViewById(R.id.textScore);
            delete = row.findViewById(R.id.btnDelete);
            ArcadeAccessibility.asButton(delete);
        }
    }
}

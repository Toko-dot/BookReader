package com.zy.reader.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zy.reader.R;
import com.zy.reader.utils.BookUtils;

import java.util.List;

public class ChapterListAdapter extends RecyclerView.Adapter<ChapterListAdapter.ViewHolder> {
    private List<BookUtils.Chapter> dataList;

    private OnClickItemListener onClickItemListener;

    public ChapterListAdapter(List<BookUtils.Chapter> dataList, OnClickItemListener onClickItemListener) {
        this.dataList = dataList;
        this.onClickItemListener = onClickItemListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_chapter_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookUtils.Chapter chapter = dataList.get(position);
        holder.tvChapterName.setText(chapter.name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickItemListener.onClick(chapter);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvChapterName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterName = itemView.findViewById(R.id.tv_chapter_name);
        }
    }

    public interface OnClickItemListener {
        void onClick(BookUtils.Chapter chapter);
    }

}

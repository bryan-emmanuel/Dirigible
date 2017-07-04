package com.piusvelte.dirigible.home;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.piusvelte.dirigible.databinding.VideoListItemBinding;
import com.piusvelte.dirigible.video.VideoItemViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bemmanuel on 3/9/16.
 */
public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private static final String TAG = Adapter.class.getSimpleName();

    @NonNull
    private OnLibraryItemClickListener mCallback;
    private final ArrayList<String> mBaseItems = new ArrayList<>();
    private final ArrayList<String> mVisibleItems = new ArrayList<>();

    @NonNull
    private final String mPath;

    public Adapter(@NonNull OnLibraryItemClickListener callback, @NonNull String path) {
        mCallback = callback;
        mPath = path;
    }

    public void addLibraryItems(@NonNull List<String> files) {
        mBaseItems.addAll(files);
    }

    public void clear() {
        mBaseItems.clear();
        mVisibleItems.clear();
    }

    public boolean isEmpty() {
        return mBaseItems.isEmpty();
    }

    @Override
    public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        VideoListItemBinding binding = VideoListItemBinding.inflate(LayoutInflater.from(parent.getContext()));
        return new Adapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(Adapter.ViewHolder holder, int position) {
        holder.bind(mVisibleItems.get(position), mPath, mCallback);
    }

    @Override
    public int getItemCount() {
        return mVisibleItems.size();
    }

    public void setQuery(String query) {
        mVisibleItems.clear();

        if (TextUtils.isEmpty(query)) {
            mVisibleItems.addAll(mBaseItems);
        } else {
            query = query.toLowerCase();

            for (String baseItem : mBaseItems) {
                if (baseItem.toLowerCase().contains(query)) {
                    mVisibleItems.add(baseItem);
                }
            }
        }

        Collections.sort(mVisibleItems);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final VideoListItemBinding mBinding;

        ViewHolder(@NonNull VideoListItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;
        }

        void bind(String name, String path, OnLibraryItemClickListener callback) {
            mBinding.setViewModel(new VideoItemViewModel(name, path, callback));
            mBinding.executePendingBindings();
        }
    }
}

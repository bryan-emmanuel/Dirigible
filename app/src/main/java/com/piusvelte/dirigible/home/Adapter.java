package com.piusvelte.dirigible.home;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.piusvelte.dirigible.R;
import com.squareup.picasso.Picasso;

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
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_list_item, parent, false);
        return new Adapter.ViewHolder(itemView, mCallback);
    }

    @Override
    public void onBindViewHolder(Adapter.ViewHolder holder, int position) {
        holder.bind(mVisibleItems.get(position), mPath);
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

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.recycle();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @NonNull
        private OnLibraryItemClickListener mCallback;
        @NonNull
        private ImageView mIcon;
        @NonNull
        private TextView mText;
        @Nullable
        private String mName;

        ViewHolder(@NonNull View itemView, @NonNull OnLibraryItemClickListener callback) {
            super(itemView);
            mCallback = callback;
            mIcon = (ImageView) itemView.findViewById(android.R.id.icon);
            mText = (TextView) itemView.findViewById(android.R.id.text1);
            itemView.findViewById(android.R.id.content).setOnClickListener(this);
        }

        void bind(@NonNull String name, @NonNull String path) {
            mName = name;
            int error;

            if (VideoUtils.isVideo(name) || VideoUtils.isStream(name)) {
                error = R.drawable.ic_movie_black_24dp;
            } else {
                error = R.drawable.ic_folder_open_black_24dp;
            }

            Picasso.with(mIcon.getContext())
                    .load(VideoUtils.getIconPath(path, mName))
                    .error(error)
                    .fit()
                    .centerCrop()
                    .into(mIcon);

            mText.setText(VideoUtils.getDecodedNameWithoutExtension(mName));
        }

        void recycle() {
            Picasso.with(mIcon.getContext()).cancelRequest(mIcon);
        }

        @Override
        public void onClick(View v) {
            if (TextUtils.isEmpty(mName)) return;
            mCallback.onLibraryItemClick(mName);
        }
    }
}

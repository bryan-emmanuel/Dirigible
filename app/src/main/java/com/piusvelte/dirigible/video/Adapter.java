package com.piusvelte.dirigible.video;

import android.os.Bundle;
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
import com.piusvelte.dirigible.util.PicassoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bemmanuel on 3/9/16.
 */
public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private static final String TAG = Adapter.class.getSimpleName();
    private static final String STATE_VIDEOS = TAG + ":state:videos";

    @NonNull
    private VideoItemCallback mCallback;
    private ArrayList<Video> mVideos = new ArrayList<>();

    public Adapter(@NonNull VideoItemCallback callback) {
        mCallback = callback;
    }

    public void addVideos(@NonNull List<Video> files) {
        mVideos.addAll(files);
    }

    public void clear() {
        mVideos.clear();
    }

    public boolean isEmpty() {
        return mVideos.isEmpty();
    }

    public void restoreState(@NonNull Bundle inState) {
        List<Video> videos = inState.getParcelableArrayList(STATE_VIDEOS);
        if (videos == null) return;
        mVideos.addAll(videos);
        notifyDataSetChanged();
    }

    public void saveState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(STATE_VIDEOS, mVideos);
    }

    @Override
    public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_list_item, parent, false);
        return new Adapter.ViewHolder(itemView, mCallback);
    }

    @Override
    public void onBindViewHolder(Adapter.ViewHolder holder, int position) {
        holder.bind(mVideos.get(position));
    }

    @Override
    public int getItemCount() {
        return mVideos.size();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.recycle();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @NonNull
        private VideoItemCallback mCallback;
        @NonNull
        private ImageView mIcon;
        @NonNull
        private TextView mText;
        @Nullable
        private Video mVideo;

        ViewHolder(@NonNull View itemView, @NonNull VideoItemCallback callback) {
            super(itemView);
            mCallback = callback;
            mIcon = (ImageView) itemView.findViewById(android.R.id.icon);
            mText = (TextView) itemView.findViewById(android.R.id.text1);
            itemView.findViewById(android.R.id.content).setOnClickListener(this);
        }

        void bind(@NonNull Video video) {
            mVideo = video;

            if (!TextUtils.isEmpty(mVideo.icon)) {
                PicassoUtils.getGoogleAuthPicasso(mIcon.getContext(), mCallback.getCredential())
                        .load(video.icon)
                        .error(R.drawable.ic_movie_black_24dp)
                        .fit()
                        .centerCrop()
                        .error(R.mipmap.ic_launcher)
                        .into(mIcon);
            } else {
                mIcon.setImageResource(R.drawable.ic_movie_black_24dp);
            }

            mText.setText(video.name);
        }

        void recycle() {
            PicassoUtils.getGoogleAuthPicasso(mIcon.getContext(), mCallback.getCredential())
                    .cancelRequest(mIcon);
        }

        @Override
        public void onClick(View v) {
            if (mVideo == null) return;
            mCallback.loadVideo(mVideo);
        }
    }
}

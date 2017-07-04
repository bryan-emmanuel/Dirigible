package com.piusvelte.dirigible.video;

import android.databinding.BaseObservable;
import android.databinding.BindingAdapter;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.piusvelte.dirigible.R;
import com.piusvelte.dirigible.home.OnLibraryItemClickListener;
import com.piusvelte.dirigible.home.VideoUtils;
import com.squareup.picasso.Picasso;

/**
 * Created by bemmanuel on 7/3/17.
 */
public class VideoItemViewModel extends BaseObservable {

    @NonNull
    private final String mName;
    @NonNull
    private final String mPath;
    @NonNull
    private final OnLibraryItemClickListener mCallback;

    @DrawableRes
    public final int error;
    @NonNull
    public final String imageUrl;

    public VideoItemViewModel(@NonNull String name, @NonNull String path, @NonNull OnLibraryItemClickListener callback) {
        mName = name;
        mPath = path;
        mCallback = callback;

        if (VideoUtils.isVideo(mName) || VideoUtils.isStream(mName)) {
            error = R.drawable.ic_movie_black_24dp;
        } else {
            error = R.drawable.ic_folder_open_black_24dp;
        }

        imageUrl = VideoUtils.getIconPath(mPath, mName);
    }

    public String getTitle() {
        return VideoUtils.getDecodedNameWithoutExtension(mName);
    }

    public void onCardClick(View view) {
        if (TextUtils.isEmpty(mName)) return;
        mCallback.onLibraryItemClick(mName);
    }

    @BindingAdapter({"bind:imageUrl", "bind:error"})
    public static void loadImage(ImageView view, String imageUrl, int error) {
        Picasso.with(view.getContext())
                .load(imageUrl)
                .error(error)
                .fit()
                .centerCrop()
                .into(view);
    }
}

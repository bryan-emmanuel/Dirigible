package com.piusvelte.dirigible.videos;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.piusvelte.dirigible.BuildConfig;
import com.piusvelte.dirigible.util.BaseAsyncTaskLoader;
import com.piusvelte.dirigible.util.CredentialProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * @author bemmanuel
 * @since 3/21/16
 */
public class MediaInfoLoader extends BaseAsyncTaskLoader<MediaInfo> {

    private static final String TAG = MediaInfoLoader.class.getSimpleName();

    public interface Player {
        void playVideo(@NonNull MediaInfo mediaInfo);
    }

    public static class Callbacks implements LoaderManager.LoaderCallbacks<MediaInfo> {

        private static final String TAG = Callbacks.class.getSimpleName();
        private static final String ARG_VIDEO = TAG + ":args:video";

        @NonNull
        private Context mContext;
        @NonNull
        private CredentialProvider mCredentialProvider;
        @NonNull
        private Player mPlayer;
        private int loaderId;

        public Callbacks(@NonNull Context context, @NonNull CredentialProvider credentialProvider, @NonNull Player player) {
            mContext = context;
            mCredentialProvider = credentialProvider;
            mPlayer = player;
        }

        public void init(@NonNull LoaderManager loaderManager, int id, @NonNull Video video) {
            loaderId = id;
            Bundle arguments = new Bundle(1);
            arguments.putParcelable(ARG_VIDEO, video);
            loaderManager.initLoader(id, arguments, this);
        }

        public void restart(@NonNull LoaderManager loaderManager, int id, @NonNull Video video) {
            loaderId = id;
            Bundle arguments = new Bundle(1);
            arguments.putParcelable(ARG_VIDEO, video);
            loaderManager.restartLoader(id, arguments, this);
        }

        @Override
        public Loader<MediaInfo> onCreateLoader(int id, Bundle args) {
            if (id == loaderId) {
                Video video = args != null ? (Video) args.getParcelable(ARG_VIDEO) : null;
                if (video == null) return null;
                return new MediaInfoLoader(mContext, video, mCredentialProvider.getCredential());
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<MediaInfo> loader, MediaInfo data) {
            if (loader.getId() == loaderId) {
                mPlayer.playVideo(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<MediaInfo> loader) {
            // NOOP
        }
    }

    @NonNull
    private Video mVideo;
    @NonNull
    private GoogleAccountCredential mGoogleAccountCredential;

    public MediaInfoLoader(@NonNull Context context, @NonNull Video video, @NonNull GoogleAccountCredential googleAccountCredential) {
        super(context);
        mVideo = video;
        mGoogleAccountCredential = googleAccountCredential;
    }

    @Override
    public MediaInfo loadInBackground() {
        Uri imageUrl = Uri.parse(mVideo.icon);
        WebImage image = new WebImage(imageUrl);

        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        metadata.putString(MediaMetadata.KEY_TITLE, mVideo.name);
        metadata.putString(MediaMetadata.KEY_SUBTITLE, mVideo.name);
        metadata.putString(MediaMetadata.KEY_STUDIO, mVideo.name);
        // notification
        metadata.addImage(image);
        // lockscreen
        metadata.addImage(image);

        // access token
        String accessToken;
        JSONObject customData = new JSONObject();

        try {
            accessToken = mGoogleAccountCredential.getToken();
            customData.put("accessToken", accessToken);
        } catch (IOException | GoogleAuthException e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error getting access token", e);
        } catch (JSONException e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "error putting access token: " + accessToken, e);
        }

        return new MediaInfo.Builder(mVideo.url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(Video.MIME_TYPE_MP4)
                .setCustomData(customData)
                .setMetadata(metadata)
                .build();
    }
}

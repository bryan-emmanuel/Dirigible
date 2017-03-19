package com.piusvelte.dirigible.home;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.piusvelte.dirigible.BuildConfig;
import com.piusvelte.dirigible.drive.Video;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by bemmanuel on 3/5/17.
 */
public class VideoUtils {

    private static final String TAG = VideoUtils.class.getSimpleName();

    private VideoUtils() {
        // not instantiable
    }

    public static boolean isStream(String name) {
        return !TextUtils.isEmpty(name) && name.endsWith(".mpd");
    }

    public static boolean isVideo(String name) {
        return !TextUtils.isEmpty(name) && name.endsWith(".mp4");
    }

    public static String getNameWithoutExtension(String name) {
        if (isVideo(name) || isStream(name)) return name.substring(0, name.length() - 4);
        return name;
    }

    public static String getDecodedNameWithoutExtension(String name) {
        return getDecodedName(getNameWithoutExtension(name));
    }

    public static String getDecodedName(String name) {
        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "error decoding: " + name, e);
            }
        }

        return name;
    }

    public static String getNameFromPath(String path) {
        int index = path.lastIndexOf("/");

        if (index < 0) return getDecodedName(path);
        return getDecodedName(path.substring(index + 1));
    }

    public static String getPath(String path, String name) {
        if (isStream(name)) {
            return String.format("%s/%s/stream.mpd", path, name.substring(0, name.length() - 4));
        }

        return String.format("%s/%s", path, name);
    }

    public static String getIconPath(String path, String name) {
        return String.format("%s/%s.jpg", path, getNameWithoutExtension(name));
    }

    public static MediaInfo buildMediaInfo(String path, String name) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        String title = getDecodedNameWithoutExtension(name);
        metadata.putString(MediaMetadata.KEY_TITLE, title);
        metadata.putString(MediaMetadata.KEY_SUBTITLE, title);

        Uri imageUrl = Uri.parse(getIconPath(path, name));
        WebImage image = new WebImage(imageUrl);

        // notification
        metadata.addImage(image);
        // lockscreen
        metadata.addImage(image);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "play media: " + getPath(path, name));
        }

        return new MediaInfo.Builder(getPath(path, name))
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(Video.MIME_TYPE_MP4)
                .setMetadata(metadata)
                .build();
    }
}

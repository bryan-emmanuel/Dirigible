package com.piusvelte.dirigible.home;

import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.piusvelte.dirigible.drive.Video;

/**
 * Created by bemmanuel on 3/5/17.
 */

public class VideoUtils {

    private static final String TAG = VideoUtils.class.getSimpleName();

    private VideoUtils() {
        // not instantiable
    }

    public static boolean isVideo(String name) {
        return !TextUtils.isEmpty(name) && name.endsWith(".mp4");
    }

    public static String getName(String name) {
        if (isVideo(name)) return name.substring(0, name.length() - 4);
        return name;
    }

    public static String getNameFromPath(String path) {
        int index = path.lastIndexOf("/");
        if (index < 0) return path;
        return path.substring(index + 1);
    }

    public static String getSpaceEncoded(String path) {
        return path.replaceAll(" ", "%20");
    }

    public static String getPath(String path, String name) {
        return getSpaceEncoded(getQualifiedPath(path) + "/" + name);
    }

    public static String getQualifiedPath(String path) {
        if (path.startsWith("http://")) return path;
        return "http://" + path;
    }

    public static String getIconPath(String path, String name) {
        return getSpaceEncoded(getQualifiedPath(path) + "/" + getName(name) + ".jpg");
    }

    public static MediaInfo buildMediaInfo(String path, String name) {
        MediaMetadata metadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        metadata.putString(MediaMetadata.KEY_TITLE, name);
        metadata.putString(MediaMetadata.KEY_SUBTITLE, name);

        Uri imageUrl = Uri.parse(getIconPath(path, name));
        WebImage image = new WebImage(imageUrl);

        // notification
        metadata.addImage(image);
        // lockscreen
        metadata.addImage(image);

        return new MediaInfo.Builder(getPath(path, name))
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(Video.MIME_TYPE_MP4)
                .setMetadata(metadata)
                .build();
    }
}

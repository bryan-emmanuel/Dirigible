package com.piusvelte.dirigible.video;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Created by bemmanuel on 3/9/16.
 */
public class Video implements Parcelable {

    public static final String MIME_TYPE_MP4 = "video/mp4";
    public static final String MIME_TYPE_JPEG = "image/jpeg";

    public String id;
    public String name;
    public String url;
    public String icon;

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            Video video = new Video(in.readString(), in.readString());
            video.url = in.readString();
            video.icon = in.readString();
            return video;
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    public Video(@NonNull String id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(icon);
    }
}

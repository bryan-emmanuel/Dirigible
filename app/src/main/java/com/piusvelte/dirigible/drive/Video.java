package com.piusvelte.dirigible.drive;

import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.api.services.drive.model.File;

/**
 * Created by bemmanuel on 3/9/16.
 */
public class Video extends LibraryItem {

    public static final String MIME_TYPE_MP4 = "video/mp4";
    public static final String MIME_TYPE_JPEG = "image/jpeg";

    public final String url;

    public Video(@NonNull File file, @NonNull String url) {
        super(file);
        this.url = url;
    }

    protected Video(Parcel in) {
        super(in);
        url = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(url);
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };
}

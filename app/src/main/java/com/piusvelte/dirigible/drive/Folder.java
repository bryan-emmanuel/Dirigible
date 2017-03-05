package com.piusvelte.dirigible.drive;

import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.api.services.drive.model.File;

/**
 * Created by bemmanuel on 2/25/17.
 */
public class Folder extends LibraryItem {

    public static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

    public Folder(@NonNull File file) {
        super(file);
    }

    protected Folder(Parcel in) {
        super(in);
    }

    public static final Creator<Folder> CREATOR = new Creator<Folder>() {
        @Override
        public Folder createFromParcel(Parcel in) {
            return new Folder(in);
        }

        @Override
        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };
}

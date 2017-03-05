package com.piusvelte.dirigible.drive;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.api.services.drive.model.File;

/**
 * Created by bemmanuel on 2/25/17.
 */
public abstract class LibraryItem implements Parcelable {

    @NonNull
    public final String id;
    @NonNull
    public final String name;
    @Nullable
    public String icon;

    LibraryItem(@NonNull File file) {
        this.id = file.getId();
        this.name = getNameWithoutExtension(file);
    }

    LibraryItem(Parcel in) {
        id = in.readString();
        name = in.readString();
        icon = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(icon);
    }

    boolean nameEquals(@NonNull File file) {
        return name.equals(getNameWithoutExtension(file));
    }

    @NonNull
    private static String getNameWithoutExtension(@NonNull File file) {
        String name = file.getName();
        int dot = name.lastIndexOf(".");

        if (dot > 0 && dot < name.length() && !name.substring(dot).contains(" ")) {
            return name.substring(0, dot);
        }

        return name;
    }
}

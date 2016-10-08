package com.piusvelte.dirigible.videos;

import android.support.annotation.NonNull;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

/**
 * @author bemmanuel
 * @since 3/4/16
 */
public interface VideoItemCallback {
    void loadVideo(@NonNull Video video);
}

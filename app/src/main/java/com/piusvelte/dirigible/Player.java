package com.piusvelte.dirigible;

import android.support.annotation.NonNull;

import com.google.android.gms.cast.MediaInfo;
import com.piusvelte.dirigible.util.CredentialProvider;

/**
 * Created by bemmanuel on 3/5/17.
 */
public interface Player {
    void onPlayVideo(@NonNull MediaInfo mediaInfo);
}

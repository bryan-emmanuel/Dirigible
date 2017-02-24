package com.piusvelte.dirigible.video;

import android.support.annotation.NonNull;

import com.piusvelte.dirigible.util.CredentialProvider;

/**
 * @author bemmanuel
 * @since 3/4/16
 */
public interface VideoItemCallback extends CredentialProvider {
    void loadVideo(@NonNull Video video);
}

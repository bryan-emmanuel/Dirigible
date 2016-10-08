package com.piusvelte.dirigible.util;

import android.support.annotation.NonNull;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

/**
 * Created by bemmanuel on 3/21/16.
 */
public interface CredentialProvider {

    @NonNull
    GoogleAccountCredential getCredential();
}

package com.piusvelte.dirigible.playservices;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.piusvelte.dirigible.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by bemmanuel on 3/21/16.
 */
public class GoogleAccountCredentialInterceptor implements Interceptor {

    private static final String TAG = GoogleAccountCredentialInterceptor.class.getSimpleName();

    private static GoogleAccountCredentialInterceptor sInterceptor;

    private GoogleAccountCredential googleAccountCredential;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        try {
            Request authRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer " + googleAccountCredential.getToken())
                    .build();
            return chain.proceed(authRequest);
        } catch (GoogleAuthException e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error getting access token", e);
        }

        return chain.proceed(originalRequest);
    }

    @NonNull
    public static GoogleAccountCredentialInterceptor getInterceptor(@NonNull GoogleAccountCredential googleAccountCredential) {
        if (sInterceptor == null) {
            sInterceptor = new GoogleAccountCredentialInterceptor();
        }

        sInterceptor.googleAccountCredential = googleAccountCredential;
        return sInterceptor;
    }
}

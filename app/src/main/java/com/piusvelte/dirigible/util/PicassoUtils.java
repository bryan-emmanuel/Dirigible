package com.piusvelte.dirigible.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.piusvelte.dirigible.playservices.GoogleAccountCredentialInterceptor;
import com.squareup.picasso.Picasso;

import okhttp3.OkHttpClient;

/**
 * Created by bemmanuel on 10/8/16.
 */

public class PicassoUtils {

    /**
     * Context used here is {@link android.app.Application}
     */
    @SuppressLint("StaticFieldLeak")
    private static Picasso sGoogleAuthPicasso;

    private PicassoUtils() {
        // not instantiable
    }

    public static Picasso getGoogleAuthPicasso(@NonNull Context context, @NonNull GoogleAccountCredentialInterceptor interceptor) {
        if (sGoogleAuthPicasso == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();
            OkHttp3Downloader downloader = new OkHttp3Downloader(client);
            sGoogleAuthPicasso = new Picasso.Builder(context.getApplicationContext())
                    .downloader(downloader)
                    .build();
        }

        return sGoogleAuthPicasso;
    }
}

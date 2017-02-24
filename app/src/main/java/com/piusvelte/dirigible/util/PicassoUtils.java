package com.piusvelte.dirigible.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.piusvelte.dirigible.BuildConfig;
import com.piusvelte.dirigible.account.GoogleAccountCredentialInterceptor;
import com.squareup.picasso.Picasso;

import okhttp3.OkHttpClient;

/**
 * Created by bemmanuel on 10/8/16.
 */
public class PicassoUtils {

    private static final String TAG = PicassoUtils.class.getSimpleName();

    /**
     * Context used here is {@link android.app.Application}
     */
    @SuppressLint("StaticFieldLeak")
    private static Picasso sGoogleAuthPicasso;

    private PicassoUtils() {
        // not instantiable
    }

    public static Picasso getGoogleAuthPicasso(@NonNull Context context, @NonNull GoogleAccountCredential credential) {
        if (sGoogleAuthPicasso == null) {
            GoogleAccountCredentialInterceptor interceptor = GoogleAccountCredentialInterceptor
                    .getInterceptor(credential);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();
            OkHttp3Downloader downloader = new OkHttp3Downloader(client);
            sGoogleAuthPicasso = new Picasso.Builder(context.getApplicationContext())
                    .downloader(downloader)
                    .listener(new Picasso.Listener() {
                        @Override
                        public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                            if (BuildConfig.DEBUG) {
                                Log.e(TAG, "error loading: " + uri, exception);
                            }
                        }
                    })
                    .build();
        }

        return sGoogleAuthPicasso;
    }
}

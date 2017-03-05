package com.piusvelte.dirigible.home;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piusvelte.dirigible.BuildConfig;
import com.piusvelte.dirigible.util.BaseAsyncTaskLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * @author bemmanuel
 * @since 3/9/16
 */
public class LibraryLoader extends BaseAsyncTaskLoader<LibraryLoader.Result> {

    private static final String TAG = LibraryLoader.class.getSimpleName();

    private static final OkHttpClient mOKHttpClient = new OkHttpClient();
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    public interface Viewer {
        void onLibraryLoaded(@NonNull LibraryLoader.Result result);
    }

    public static class Callbacks implements LoaderManager.LoaderCallbacks<LibraryLoader.Result> {

        private static final String TAG = Callbacks.class.getSimpleName();
        private static final String ARG_PATH = TAG + ":args:path";

        @NonNull
        private Context mContext;
        @NonNull
        private Viewer mViewer;
        private final int mLoaderId;

        public Callbacks(@NonNull Context context, @NonNull Viewer viewer, int loaderId) {
            mContext = context;
            mViewer = viewer;
            mLoaderId = loaderId;
        }

        @NonNull
        private Bundle getArguments(@Nullable String parent) {
            Bundle arguments = new Bundle(1);
            arguments.putString(ARG_PATH, parent);
            return arguments;
        }

        public void load(@NonNull LoaderManager loaderManager,
                         @NonNull String path) {
            loaderManager.initLoader(mLoaderId, getArguments(path), this);
        }

        public void reload(@NonNull LoaderManager loaderManager,
                           @NonNull String path) {
            loaderManager.restartLoader(mLoaderId, getArguments(path), this);
        }

        @Override
        public Loader<LibraryLoader.Result> onCreateLoader(int id, Bundle args) {
            if (id == mLoaderId) {
                String parent = args.getString(ARG_PATH);
                return new LibraryLoader(mContext,
                        parent);
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<LibraryLoader.Result> loader, LibraryLoader.Result data) {
            if (loader.getId() == mLoaderId) {
                mViewer.onLibraryLoaded(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<LibraryLoader.Result> loader) {
            // NOOP
        }
    }

    @NonNull
    private final String mPath;

    public LibraryLoader(@NonNull Context context,
                         @NonNull String path) {
        super(context);
        mPath = path;
    }

    @Override
    public Result loadInBackground() {
        Result result = new Result();

        Request request = new Request.Builder()
                .url(mPath)
                .build();

        try {
            String body = mOKHttpClient.newCall(request)
                    .execute()
                    .body()
                    .string();

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "body: " + body);
            }

            Response response = mObjectMapper.readValue(body, Response.class);

            if (response.data != null) {
                result.libraryItems = new ArrayList<>(response.data);
            }
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "error getting results: " + mPath, e);
            }
        }

        return result;
    }

    static class Response {
        @JsonProperty
        List<String> data;
    }

    static class Result {
        @Nullable
        ArrayList<String> libraryItems;
    }
}

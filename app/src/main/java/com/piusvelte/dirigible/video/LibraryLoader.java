package com.piusvelte.dirigible.video;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.piusvelte.dirigible.BuildConfig;
import com.piusvelte.dirigible.util.BaseAsyncTaskLoader;
import com.piusvelte.dirigible.util.CredentialProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author bemmanuel
 * @since 3/9/16
 */
public class LibraryLoader extends BaseAsyncTaskLoader<LibraryLoader.Result> {

    private static final String TAG = LibraryLoader.class.getSimpleName();

    private static final String ROOT = "root";

    public interface Viewer {
        void onLibraryLoaded(@NonNull LibraryLoader.Result result);
    }

    public static class Callbacks implements LoaderManager.LoaderCallbacks<LibraryLoader.Result> {

        private static final String TAG = Callbacks.class.getSimpleName();
        private static final String ARG_NEXT_PAGE_TOKEN = TAG + ":args:nextPageToken";
        private static final String ARG_QUERY = TAG + ":args:query";
        private static final String ARG_REQUIRE_COVER = TAG + ":args:requireCover";
        private static final String ARG_PARENT = TAG + ":args:parent";

        @NonNull
        private Context mContext;
        @NonNull
        private CredentialProvider mCredentialProvider;
        @NonNull
        private Viewer mViewer;
        private final int mLoaderId;

        public Callbacks(@NonNull Context context, @NonNull CredentialProvider credentialProvider, @NonNull Viewer viewer, int loaderId) {
            mContext = context;
            mCredentialProvider = credentialProvider;
            mViewer = viewer;
            mLoaderId = loaderId;
        }

        @NonNull
        private Bundle getArguments(@Nullable String query,
                                    @Nullable String nextPageToken,
                                    boolean requireCover,
                                    @Nullable String parent) {
            Bundle arguments = new Bundle(4);
            arguments.putString(ARG_NEXT_PAGE_TOKEN, nextPageToken);
            arguments.putString(ARG_QUERY, query);
            arguments.putBoolean(ARG_REQUIRE_COVER, requireCover);
            arguments.putString(ARG_PARENT, parent);
            return arguments;
        }

        public void load(@NonNull LoaderManager loaderManager,
                         @Nullable String query,
                         @Nullable String nextPageToken,
                         boolean requireCover,
                         @Nullable String parent) {
            loaderManager.initLoader(mLoaderId, getArguments(query, nextPageToken, requireCover, parent), this);
        }

        public void reload(@NonNull LoaderManager loaderManager,
                           @Nullable String query,
                           @Nullable String nextPageToken,
                           boolean requireCover,
                           @Nullable String parent) {
            loaderManager.restartLoader(mLoaderId, getArguments(query, nextPageToken, requireCover, parent), this);
        }

        @Override
        public Loader<LibraryLoader.Result> onCreateLoader(int id, Bundle args) {
            if (id == mLoaderId) {
                String query = args.getString(ARG_QUERY);
                String nextPageToken = args.getString(ARG_NEXT_PAGE_TOKEN);
                boolean requireCover = args.getBoolean(ARG_REQUIRE_COVER);
                String parent = args.getString(ARG_PARENT);
                return new LibraryLoader(mContext,
                        mCredentialProvider.getCredential(),
                        query,
                        nextPageToken,
                        requireCover,
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

    @Nullable
    private static Drive sDrive;

    @NonNull
    private GoogleAccountCredential mGoogleAccountCredential;

    @Nullable
    private final String mQuery;
    @Nullable
    private final String mNextPageToken;
    private final boolean mRequireCover;
    @NonNull
    private final String mParent;

    public LibraryLoader(@NonNull Context context,
                         @NonNull GoogleAccountCredential googleAccountCredential,
                         @Nullable String query,
                         @Nullable String nextPageToken,
                         boolean requireCover,
                         @Nullable String parent) {
        super(context);
        mGoogleAccountCredential = googleAccountCredential;
        mQuery = query;
        mNextPageToken = nextPageToken;
        mRequireCover = requireCover;

        if (TextUtils.isEmpty(parent)) {
            mParent = ROOT;
        } else {
            mParent = parent;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "initialized query=" + mQuery
                    + ", nextPageToken=" + mNextPageToken
                    + ", requireCover=" + mRequireCover
                    + ", parent=" + mParent);
        }
    }

    @Override
    public Result loadInBackground() {
        Result result = new Result();

        if (TextUtils.isEmpty(mGoogleAccountCredential.getSelectedAccountName())) {
            return result;
        }

        Drive drive = getDrive(mGoogleAccountCredential);

        try {
            addVideos(result, drive);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "result size: " + (result.libraryItems != null ? result.libraryItems.size() : null));
            }
        } catch (UserRecoverableAuthIOException e) {
            result.authorizationIntent = e.getIntent();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "error getting file list", e);
            }
        }

        return result;
    }

    @NonNull
    private String buildQ() {
        StringBuilder q = new StringBuilder();

        q.append("'")
                .append(mParent)
                .append("' in parents and (mimeType='")
                .append(Video.MIME_TYPE_MP4)
                .append("' or mimeType='")
                .append(Video.MIME_TYPE_JPEG)
                .append("' or mimeType='")
                .append(Folder.MIME_TYPE_FOLDER)
                .append("')");

        if (!TextUtils.isEmpty(mQuery)) {
            q.append(" and name contains '")
                    .append(mQuery)
                    .append("'");
        }

        String result = q.toString();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "drive query= " + result);
        }

        return result;
    }

    private void addVideos(@NonNull Result result, @NonNull Drive drive) throws IOException {
        result.libraryItems = new ArrayList<>();

        // get all videos, cache folders, and add videos based on parameters
        FileList listResult = drive.files().list()
                .setQ(buildQ())
                .setOrderBy("name")
                .setSpaces("drive")
                .setFields("nextPageToken, files")
                .setPageToken(mNextPageToken)
                .execute();

        if (listResult.getFiles() == null) return;// error

        for (int fileIndex = 0; fileIndex < listResult.getFiles().size(); fileIndex++) {
            File file = listResult.getFiles().get(fileIndex);
            LibraryItem item;

            if (Video.MIME_TYPE_MP4.equals(file.getMimeType())) {
                try {
                    item = new Video(file, getUrl(file, drive));
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "error getting url", e);
                    continue;// can't cache without a url
                }
            } else if (Folder.MIME_TYPE_FOLDER.equals(file.getMimeType())) {
                item = new Folder(file);
            } else {
                continue;// this should be an icon, and not added to the results directly, but to the items in the results
            }

            // try to set the icon, the results are sorted by name, so the icon should be nearby
            if (TextUtils.isEmpty(item.icon)) {
                // search before
                int iconSearchIndex = fileIndex - 1;
                File iconSearchItem;

                while (iconSearchIndex >= 0
                        && item.nameEquals((iconSearchItem = listResult.getFiles().get(iconSearchIndex)))) {
                    if (setIcon(item, iconSearchItem, drive)) break;
                    iconSearchIndex--;
                }

                // search after
                if (TextUtils.isEmpty(item.icon)) {
                    iconSearchIndex = fileIndex + 1;

                    while (iconSearchIndex < listResult.getFiles().size() - 1
                            && (item.nameEquals((iconSearchItem = listResult.getFiles().get(iconSearchIndex))))) {
                        if (setIcon(item, iconSearchItem, drive)) break;
                        iconSearchIndex++;
                    }
                }
            }

            if (mRequireCover && TextUtils.isEmpty(item.icon)) continue;
            result.libraryItems.add(item);
        }

        result.nextPageToken = listResult.getNextPageToken();
    }

    private boolean setIcon(@NonNull LibraryItem item, @NonNull File fileMatchingName, @NonNull Drive drive) {
        if (Video.MIME_TYPE_JPEG.equals(fileMatchingName.getMimeType())) {
            try {
                item.icon = getUrl(fileMatchingName, drive);
            } catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "error setting icon for item: " + item.name, e);
                }
            }

            return true;// this was the icon
        }

        return false;
    }

    @NonNull
    private static Drive getDrive(@NonNull GoogleAccountCredential credential) {
        if (sDrive == null) {
            sDrive = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                    .build();
        }

        return sDrive;
    }

    @NonNull
    private static String getUrl(@NonNull File file, @NonNull Drive drive) throws IOException {
        HttpRequest request = drive.getRequestFactory().buildGetRequest(drive.files()
                .get(file.getId())
                .set("alt", "media")
                .buildHttpRequestUrl());
        return request.getUrl().build();
    }

    public static class Result {
        @Nullable
        public ArrayList<LibraryItem> libraryItems;
        @Nullable
        public Intent authorizationIntent;
        @Nullable
        public String nextPageToken;
    }
}

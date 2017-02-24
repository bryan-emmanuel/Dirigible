package com.piusvelte.dirigible.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.cast.MediaInfo;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.piusvelte.dirigible.R;
import com.piusvelte.dirigible.util.CredentialProvider;
import com.piusvelte.dirigible.util.RecyclerViewUtils;

/**
 * Created by bemmanuel on 2/23/17.
 */
public class LibraryBrowser
        extends
        Fragment
        implements
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        VideoItemCallback,
        RecyclerViewUtils.OnScrollAutoPagingListener.OnAutoPageListener,
        LibraryLoader.Viewer,
        MediaInfoLoader.Player,
        CredentialProvider {

    private static final String TAG = LibraryBrowser.class.getSimpleName();

    private static final String STATE_AUTHORIZATION_INTENT = TAG + ":state:authorizationIntent";
    private static final String STATE_NEXT_PAGE_TOKEN = TAG + ":state:nextPageToken";
    private static final String STATE_PENDING_PLAY_VIDEO = TAG + ":state:pendingPlayVideo";

    private static final int LOADER_LIBRARY = 0;
    private static final int LOADER_MEDIA_INFO = 1;

    private static final int REQUEST_AUTHORIZATION = 4;

    private SwipeRefreshLayout mSwipeContainer;
    private RecyclerView mRecyclerView;
    private Button mEmpty;
    private ProgressBar mLoading;

    private Adapter mAdapter;
    @Nullable
    private String mNextPageToken;
    private LibraryLoader.Callbacks mLibraryLoaderCallbacks;
    private MediaInfoLoader.Callbacks mMediaInfoLoaderCallbacks;

    @Nullable
    private Video mPendingPlayVideo;

    private MediaInfoLoader.Player mPlayer;

    @Nullable
    private Intent mAuthorizationIntent;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MediaInfoLoader.Player) {
            mPlayer = (MediaInfoLoader.Player) context;
        } else {
            throw new IllegalStateException(TAG + " must be attached to a MediaInfoLoader.Player");
        }
    }

    @Override
    public void onDetach() {
        mPlayer = null;
        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.library_browser, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mRecyclerView = (RecyclerView) mSwipeContainer.findViewById(android.R.id.list);
        mEmpty = (Button) view.findViewById(android.R.id.empty);
        mLoading = (ProgressBar) view.findViewById(R.id.loading);

        mEmpty.setOnClickListener(this);

        mAdapter = new Adapter(this);

        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.gallery_span_count),
                LinearLayoutManager.VERTICAL,
                false));
        mRecyclerView.setAdapter(mAdapter);
        RecyclerViewUtils.addAutoPaging(mRecyclerView, this);

        mSwipeContainer.setOnRefreshListener(this);

        mLibraryLoaderCallbacks = new LibraryLoader.Callbacks(getContext(), this, this);
        mMediaInfoLoaderCallbacks = new MediaInfoLoader.Callbacks(getContext(), this, this);

        mLibraryLoaderCallbacks.init(getLoaderManager(), LOADER_LIBRARY);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            mAuthorizationIntent = savedInstanceState.getParcelable(STATE_AUTHORIZATION_INTENT);
            mNextPageToken = savedInstanceState.getString(STATE_NEXT_PAGE_TOKEN);
            mAdapter.restoreState(savedInstanceState);

            if (!mAdapter.isEmpty()) {
                mEmpty.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }

            mPendingPlayVideo = savedInstanceState.getParcelable(STATE_PENDING_PLAY_VIDEO);

            if (mPendingPlayVideo != null) {
                mMediaInfoLoaderCallbacks.init(getLoaderManager(), LOADER_MEDIA_INFO, mPendingPlayVideo);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter.isEmpty()) {
            onRefresh();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwipeContainer = null;
        mRecyclerView = null;
        mEmpty = null;
        mLoading = null;
        mAdapter = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(STATE_AUTHORIZATION_INTENT, mAuthorizationIntent);
        outState.putString(STATE_NEXT_PAGE_TOKEN, mNextPageToken);
        outState.putParcelable(STATE_PENDING_PLAY_VIDEO, mPendingPlayVideo);

        mAdapter.saveState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    mLibraryLoaderCallbacks.restart(getLoaderManager(), LOADER_LIBRARY);
                    mSwipeContainer.setRefreshing(true);
                } else {
                    // TODO
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void setEmptyState(@StringRes int stringRes) {
        mNextPageToken = null;
        mEmpty.setText(stringRes);
        mEmpty.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        onRefresh();
    }

    @Override
    public void onPlayVideo(@NonNull MediaInfo mediaInfo) {
        mPendingPlayVideo = null;
        mPlayer.onPlayVideo(mediaInfo);
    }

    @Override
    public void loadVideo(@NonNull Video video) {
        mPendingPlayVideo = video;
        mMediaInfoLoaderCallbacks.restart(getLoaderManager(), LOADER_MEDIA_INFO, mPendingPlayVideo);
    }

    @Override
    public void onRefresh() {
        int originalSize = mAdapter.getItemCount();

        if (originalSize > 0) {
            mAdapter.clear();
            mAdapter.notifyItemRangeRemoved(0, originalSize);
        }

        mLibraryLoaderCallbacks.restart(getLoaderManager(), LOADER_LIBRARY);
        mSwipeContainer.setRefreshing(true);
    }

    @Override
    public void onAutoPage() {
        if (mLoading.getVisibility() == View.VISIBLE) return;// already loading, wait
        if (!mAdapter.isEmpty() && TextUtils.isEmpty(mNextPageToken)) return;// no more pages
        mLoading.setVisibility(View.VISIBLE);
        mLibraryLoaderCallbacks.restart(getLoaderManager(), LOADER_LIBRARY, mNextPageToken);
    }

    @Override
    public void onLibraryLoaded(@NonNull LibraryLoader.Result data) {
        mLoading.setVisibility(View.GONE);
        mSwipeContainer.setRefreshing(false);
        mAuthorizationIntent = data.authorizationIntent;
        mNextPageToken = data.nextPageToken;

        if (mAuthorizationIntent != null) {
            setEmptyState(R.string.authorize_access);
        } else if (data.videos != null) {
            int originalSize = mAdapter.getItemCount();
            mAdapter.addVideos(data.videos);
            mAdapter.notifyItemRangeInserted(originalSize, data.videos.size());
            mEmpty.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    @Override
    public GoogleAccountCredential getCredential() {
        return mPlayer.getCredential();
    }
}

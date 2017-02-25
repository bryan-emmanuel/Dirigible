package com.piusvelte.dirigible.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
        CredentialProvider,
        SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener {

    private static final String TAG = LibraryBrowser.class.getSimpleName();

    private static final String STATE_AUTHORIZATION_INTENT = TAG + ":state:authorizationIntent";
    private static final String STATE_NEXT_PAGE_TOKEN = TAG + ":state:nextPageToken";
    private static final String STATE_QUERY = TAG + ":state:query";
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
    @Nullable
    private String mQuery;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mLibraryLoaderCallbacks = new LibraryLoader.Callbacks(getContext().getApplicationContext(), this, this, LOADER_LIBRARY);
        mMediaInfoLoaderCallbacks = new MediaInfoLoader.Callbacks(getContext().getApplicationContext(), this, this, LOADER_MEDIA_INFO);
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
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            mAuthorizationIntent = savedInstanceState.getParcelable(STATE_AUTHORIZATION_INTENT);
            mNextPageToken = savedInstanceState.getString(STATE_NEXT_PAGE_TOKEN);
            mQuery = savedInstanceState.getString(STATE_QUERY);
            mAdapter.restoreState(savedInstanceState);

            if (!mAdapter.isEmpty()) {
                mSwipeContainer.setRefreshing(false);
                mEmpty.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);

                getActivity().invalidateOptionsMenu();
            }

            mPendingPlayVideo = savedInstanceState.getParcelable(STATE_PENDING_PLAY_VIDEO);

            if (mPendingPlayVideo != null) {
                mMediaInfoLoaderCallbacks.init(getLoaderManager(), mPendingPlayVideo);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter.isEmpty()) {
            mSwipeContainer.setRefreshing(true);
            mLibraryLoaderCallbacks.load(getLoaderManager(), mQuery, mNextPageToken);
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
        outState.putString(STATE_QUERY, mQuery);
        outState.putParcelable(STATE_PENDING_PLAY_VIDEO, mPendingPlayVideo);

        mAdapter.saveState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    getLoaderManager().destroyLoader(LOADER_LIBRARY);
                    // this will load in onResume
                } else {
                    // TODO show the error
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.library_browser, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(this);
        searchView.setQueryHint(getString(R.string.search_title));

        MenuItemCompat.setOnActionExpandListener(searchItem, this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_search).setEnabled(!mAdapter.isEmpty());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                return true;
        }

        return super.onOptionsItemSelected(item);
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
        mMediaInfoLoaderCallbacks.restart(getLoaderManager(), mPendingPlayVideo);
    }

    @Override
    public void onRefresh() {
        int originalSize = mAdapter.getItemCount();

        if (originalSize > 0) {
            mAdapter.clear();
            mAdapter.notifyItemRangeRemoved(0, originalSize);
        }

        mLibraryLoaderCallbacks.reload(getLoaderManager(), mQuery, mNextPageToken);
        mSwipeContainer.setRefreshing(true);
    }

    @Override
    public void onAutoPage() {
        if (mLoading.getVisibility() == View.VISIBLE) return;// already loading, wait
        if (!mAdapter.isEmpty() && TextUtils.isEmpty(mNextPageToken)) return;// no more pages
        mLoading.setVisibility(View.VISIBLE);
        mLibraryLoaderCallbacks.reload(getLoaderManager(), mQuery, mNextPageToken);
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

        if (TextUtils.isEmpty(mQuery)) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @NonNull
    @Override
    public GoogleAccountCredential getCredential() {
        return mPlayer.getCredential();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mQuery = query;
        onRefresh();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query.length() < 2) return true;
        if (query.substring(query.length() - 2).contains(" ")) return true;

        mQuery = query;
        onRefresh();
        return true;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mQuery = null;
        onRefresh();
        return true;
    }
}

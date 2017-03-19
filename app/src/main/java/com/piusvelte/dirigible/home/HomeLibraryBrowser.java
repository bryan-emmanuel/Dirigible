package com.piusvelte.dirigible.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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

import com.piusvelte.dirigible.Player;
import com.piusvelte.dirigible.R;

/**
 * Created by bemmanuel on 2/23/17.
 */
public class HomeLibraryBrowser
        extends
        Fragment
        implements
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        OnLibraryItemClickListener,
        LibraryLoader.Viewer,
        SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener,
        FragmentManager.OnBackStackChangedListener {

    private static final String TAG = HomeLibraryBrowser.class.getSimpleName();

    private static final String ARG_PATH = TAG + ":arg:path";

    private static final String STATE_QUERY = TAG + ":state:setQuery";

    private static final int LOADER_LIBRARY = 0;

    private static final int REQUEST_AUTHORIZATION = 4;

    private SwipeRefreshLayout mSwipeContainer;
    private RecyclerView mRecyclerView;
    private Button mEmpty;
    private ProgressBar mLoading;

    private Adapter mAdapter;
    @Nullable
    private String mQuery;
    private LibraryLoader.Callbacks mLibraryLoaderCallbacks;

    private Player mPlayer;

    public static HomeLibraryBrowser newInstance(@NonNull String path) {
        HomeLibraryBrowser libraryBrowser = new HomeLibraryBrowser();

        Bundle args = new Bundle(1);
        args.putString(ARG_PATH, path);
        libraryBrowser.setArguments(args);

        return libraryBrowser;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Player) {
            mPlayer = (Player) context;
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

        mLibraryLoaderCallbacks = new LibraryLoader.Callbacks(getContext().getApplicationContext(), this, LOADER_LIBRARY);
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

        mAdapter = new Adapter(this, getArguments().getString(ARG_PATH));

        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(),
                getResources().getInteger(R.integer.gallery_span_count),
                LinearLayoutManager.VERTICAL,
                false));
        mRecyclerView.setAdapter(mAdapter);

        mSwipeContainer.setOnRefreshListener(this);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            mQuery = savedInstanceState.getString(STATE_QUERY);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onBackStackChanged() {
        if (!(getActivity() instanceof AppCompatActivity)) return;

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(VideoUtils.getNameFromPath(getArguments().getString(ARG_PATH)));
    }

    @Override
    public void onStop() {
        super.onStop();
        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!TextUtils.isEmpty(mQuery)) {
            onQueryChanged();
        } else if (mAdapter.isEmpty() && !mSwipeContainer.isRefreshing()) {
            mSwipeContainer.setRefreshing(true);
            mLibraryLoaderCallbacks.load(getLoaderManager(), getArguments().getString(ARG_PATH));
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
        outState.putString(STATE_QUERY, mQuery);
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
        searchView.setQueryHint(getString(R.string.action_search));

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
        mEmpty.setText(stringRes);
        mEmpty.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        onRefresh();
    }

    @Override
    public void onLibraryItemClick(@NonNull String name) {
        if (VideoUtils.isVideo(name) || VideoUtils.isStream(name)) {
            mPlayer.onPlayVideo(VideoUtils.buildMediaInfo(getArguments().getString(ARG_PATH), name));
        } else {
            String path = VideoUtils.getPath(getArguments().getString(ARG_PATH), name);
            String tag = TAG + ":fragment:" + VideoUtils.getNameFromPath(path);
            getFragmentManager().beginTransaction()
                    .addToBackStack(tag)
                    .replace(R.id.content_fragment, HomeLibraryBrowser.newInstance(path), tag)
                    .commit();
        }
    }

    @Override
    public void onRefresh() {
        int originalSize = mAdapter.getItemCount();

        if (originalSize > 0) {
            mAdapter.clear();
            mAdapter.notifyItemRangeRemoved(0, originalSize);
        }

        mLibraryLoaderCallbacks.reload(getLoaderManager(), getArguments().getString(ARG_PATH));
        mSwipeContainer.setRefreshing(true);
    }

    @Override
    public void onLibraryLoaded(@NonNull LibraryLoader.Result data) {
        mLoading.setVisibility(View.GONE);
        mSwipeContainer.setRefreshing(false);

        if (data.libraryItems != null) {
            if (!data.libraryItems.isEmpty()) {
                // TODO notify items add if there's no active query and we're just adding items
                mAdapter.addLibraryItems(data.libraryItems);
                mAdapter.setQuery(mQuery);
                mAdapter.notifyDataSetChanged();

                mEmpty.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            } else if (mAdapter.isEmpty()) {
                setEmptyState(R.string.empty_no_results);
            }
        } else {
            setEmptyState(R.string.empty_no_results);
        }

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mQuery = query;
        onQueryChanged();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (query.length() < 1) return true;
        if (query.substring(query.length() - 1).contains(" ")) return true;

        mQuery = query;
        onQueryChanged();
        return true;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mQuery = null;
        onQueryChanged();
        return true;
    }

    private void onQueryChanged() {
        mAdapter.setQuery(mQuery);
        mAdapter.notifyDataSetChanged();
    }
}

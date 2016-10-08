package com.piusvelte.dirigible;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.widgets.MiniController;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.piusvelte.dirigible.util.CredentialProvider;
import com.piusvelte.dirigible.util.RecyclerViewUtils;
import com.piusvelte.dirigible.util.SharedPreferencesUtils;
import com.piusvelte.dirigible.videos.Adapter;
import com.piusvelte.dirigible.videos.LibraryLoader;
import com.piusvelte.dirigible.videos.MediaInfoLoader;
import com.piusvelte.dirigible.videos.Video;
import com.piusvelte.dirigible.videos.VideoItemCallback;

import java.util.Collections;

/**
 * @author bemmanuel
 * @since 2/27/16
 */
public class MainActivity
        extends
        AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        VideoItemCallback,
        RecyclerViewUtils.OnScrollAutoPagingListener.OnAutoPageListener,
        LibraryLoader.Viewer,
        MediaInfoLoader.Player,
        CredentialProvider {

    static final String TAG = MainActivity.class.getSimpleName();

    private static final String STATE_AUTHORIZATION_INTENT = TAG + ":state:authorizationIntent";
    private static final String STATE_NEXT_PAGE_TOKEN = TAG + ":state:nextPageToken";
    private static final String STATE_PENDING_PLAY_VIDEO = TAG + ":state:pendingPlayVideo";

    private static final int REQUEST_ACCOUNT_CHOOSER = 0;
    private static final int REQUEST_AUTHORIZATION = 1;
    private static final int REQUEST_PLAY_SERVICES = 2;
    private static final int REQUEST_PERMISSIONS = 3;

    private static final int LOADER_LIBRARY = 0;
    private static final int LOADER_MEDIA_INFO = 1;

    private TextView mAccountNameView;
    private SwipeRefreshLayout mSwipeContainer;
    private RecyclerView mRecyclerView;
    private Button mEmpty;
    private ProgressBar mLoading;

    private Adapter mAdapter;
    private VideoCastManager mCastManager;
    private VideoCastConsumer mCastConsumer;
    @Nullable
    private MiniController mMiniController;
    @Nullable
    private Intent mAuthorizationIntent;
    private GoogleAccountCredential mCredential;
    @Nullable
    private String mNextPageToken;
    @Nullable
    private Video mPendingPlayVideo;
    private LibraryLoader.Callbacks mLibraryLoaderCallbacks;
    private MediaInfoLoader.Callbacks mMediaInfoLoaderCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        mAccountNameView = (TextView) navigationView.getHeaderView(0).findViewById(android.R.id.text2);
        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mRecyclerView = (RecyclerView) mSwipeContainer.findViewById(android.R.id.list);
        mEmpty = (Button) findViewById(android.R.id.empty);
        mLoading = (ProgressBar) findViewById(R.id.loading);

        navigationView.setNavigationItemSelectedListener(this);
        mEmpty.setOnClickListener(this);

        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(),
                Collections.singletonList(DriveScopes.DRIVE))
                .setBackOff(new ExponentialBackOff());
        String accountName = SharedPreferencesUtils.getAccount(this);

        if (!TextUtils.isEmpty(accountName)) {
            setAccount(accountName);
        }

        mAdapter = new Adapter(this, this, this);

        if (savedInstanceState != null) {
            mAuthorizationIntent = savedInstanceState.getParcelable(STATE_AUTHORIZATION_INTENT);
            mNextPageToken = savedInstanceState.getString(STATE_NEXT_PAGE_TOKEN);
            mPendingPlayVideo = savedInstanceState.getParcelable(STATE_PENDING_PLAY_VIDEO);
            mAdapter.restoreState(savedInstanceState);

            if (mPendingPlayVideo != null) {
                mMediaInfoLoaderCallbacks.init(getSupportLoaderManager(), LOADER_MEDIA_INFO, mPendingPlayVideo);
            }
        }

        mRecyclerView.setLayoutManager(new GridLayoutManager(this,
                getResources().getInteger(R.integer.gallery_span_count),
                LinearLayoutManager.VERTICAL,
                false));
        mRecyclerView.setAdapter(mAdapter);
        RecyclerViewUtils.addAutoPaging(mRecyclerView, this);

        mSwipeContainer.setOnRefreshListener(this);

        mCastManager = VideoCastManager.getInstance();
        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onFailed(int resourceId, int statusCode) {
                if (BuildConfig.DEBUG) {
                    String reason = "Not Available";

                    if (resourceId > 0) {
                        reason = getString(resourceId);
                    }

                    Log.e(TAG, "Action failed, reason:  " + reason + ", status code: " + statusCode);
                }
            }

            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDisconnected() {
                invalidateOptionsMenu();
            }

            @Override
            public void onConnectionSuspended(int cause) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onConnectionSuspended() was called with cause: " + cause);
                }
            }
        };

        mMiniController = (MiniController) findViewById(R.id.miniController);
        mCastManager.addMiniController(mMiniController);

        mLibraryLoaderCallbacks = new LibraryLoader.Callbacks(this, this, this);
        mMediaInfoLoaderCallbacks = new MediaInfoLoader.Callbacks(this, this, this);

        if (checkGooglePlayServicesAvailable() && hasPermissions()) {
            if (TextUtils.isEmpty(mCredential.getSelectedAccountName())) {
                setEmptyState(R.string.add_account);
            } else {
                if (mAdapter.isEmpty()) {
                    mLoading.setVisibility(View.VISIBLE);
                }

                mLibraryLoaderCallbacks.init(getSupportLoaderManager(), LOADER_LIBRARY);
            }
        }
    }

    private void setAccount(@NonNull String accountName) {
        mAccountNameView.setText(accountName);
        mCredential.setSelectedAccountName(accountName);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.incrementUiCounter();
    }

    @Override
    protected void onPause() {
        mCastManager.decrementUiCounter();
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mMiniController != null) mCastManager.removeMiniController(mMiniController);
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mCastManager.onDispatchVolumeKeyEvent(event, mCastManager.getVolumeStep())
                || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_AUTHORIZATION_INTENT, mAuthorizationIntent);
        outState.putString(STATE_NEXT_PAGE_TOKEN, mNextPageToken);

        if (mPendingPlayVideo != null) {
            outState.putParcelable(STATE_PENDING_PLAY_VIDEO, mPendingPlayVideo);
        }

        mAdapter.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // TODO clean up sample app code here
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void onAccountSelected(@NonNull String accountName) {
        SharedPreferencesUtils.putAccount(this, accountName);
        setAccount(accountName);
        mLibraryLoaderCallbacks.restart(getSupportLoaderManager(), LOADER_LIBRARY);
        mSwipeContainer.setRefreshing(true);
    }

    private void setEmptyState(@StringRes int stringRes) {
        mNextPageToken = null;
        mEmpty.setText(stringRes);
        mEmpty.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_CHOOSER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    if (!TextUtils.isEmpty(accountName)) {
                        onAccountSelected(accountName);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    // TODO
                }
                break;

            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    mLibraryLoaderCallbacks.restart(getSupportLoaderManager(), LOADER_LIBRARY);
                    mSwipeContainer.setRefreshing(true);
                } else {
                    // TODO
                }
                break;

            case REQUEST_PLAY_SERVICES:
                if (resultCode == RESULT_OK && hasPermissions()) {
                    if (TextUtils.isEmpty(mCredential.getSelectedAccountName())) {
                        setEmptyState(R.string.add_account);
                    } else {
                        mLibraryLoaderCallbacks.restart(getSupportLoaderManager(), LOADER_LIBRARY);
                        mSwipeContainer.setRefreshing(true);
                    }
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (permissions.length > 0
                        && Manifest.permission.GET_ACCOUNTS.equals(permissions[0])
                        && grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (TextUtils.isEmpty(mCredential.getSelectedAccountName())) {
                        setEmptyState(R.string.add_account);
                    } else {
                        mLibraryLoaderCallbacks.restart(getSupportLoaderManager(), LOADER_LIBRARY);
                        mSwipeContainer.setRefreshing(true);
                    }
                } else {
                    hasPermissions();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == android.R.id.empty) {
            if (TextUtils.isEmpty(mCredential.getSelectedAccountName())) {
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_CHOOSER);
            } else if (mAuthorizationIntent != null) {
                startActivityForResult(mAuthorizationIntent, REQUEST_AUTHORIZATION);
            }
        }
    }

    private boolean checkGooglePlayServicesAvailable() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int status = availability.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) return true;

        if (availability.isUserResolvableError(status)) {
            availability.getErrorDialog(this, status, REQUEST_PLAY_SERVICES)
                    .show();
        }

        return false;
    }

    private boolean hasPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) return true;

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.GET_ACCOUNTS},
                REQUEST_PERMISSIONS);

        return false;
    }

    @Override
    public void playVideo(@NonNull MediaInfo mediaInfo) {
        Log.d(TAG, "play: " + mediaInfo.getContentId());

        mPendingPlayVideo = null;
        mCastManager.startVideoCastControllerActivity(this, mediaInfo, 0, true);
    }

    @Override
    public void loadVideo(@NonNull Video video) {
        mPendingPlayVideo = video;
        mMediaInfoLoaderCallbacks.restart(getSupportLoaderManager(), LOADER_MEDIA_INFO, mPendingPlayVideo);
    }

    @Override
    public void onRefresh() {
        if (checkGooglePlayServicesAvailable() && hasPermissions()) {
            int originalSize = mAdapter.getItemCount();
            mAdapter.clear();
            mAdapter.notifyItemRangeRemoved(0, originalSize);
            mLibraryLoaderCallbacks.restart(getSupportLoaderManager(), LOADER_LIBRARY);
            mSwipeContainer.setRefreshing(true);
        }
    }

    @Override
    public void onAutoPage() {
        if (mLoading.getVisibility() == View.VISIBLE) return;// already loading, wait
        if (!mAdapter.isEmpty() && TextUtils.isEmpty(mNextPageToken)) return;// no more pages
        mLoading.setVisibility(View.VISIBLE);
        mLibraryLoaderCallbacks.restart(getSupportLoaderManager(), LOADER_LIBRARY, mNextPageToken);
    }

    @Override
    public void viewLibrary(@NonNull LibraryLoader.Result data) {
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
        return mCredential;
    }
}

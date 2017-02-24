package com.piusvelte.dirigible;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.piusvelte.dirigible.account.AccountChooser;
import com.piusvelte.dirigible.util.SharedPreferencesUtils;
import com.piusvelte.dirigible.video.LibraryBrowser;
import com.piusvelte.dirigible.video.MediaInfoLoader;

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
        AccountChooser.AccountListener,
        MediaInfoLoader.Player {

    static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PLAY_SERVICES = 0;

    private static final String FRAGMENT_CONTENT = TAG + ":fragment:content";

    private TextView mAccountNameView;
    @Nullable
    private MiniController mMiniController;

    private VideoCastManager mCastManager;
    private VideoCastConsumer mCastConsumer;

    private GoogleAccountCredential mCredential;

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
        navigationView.setNavigationItemSelectedListener(this);

        mAccountNameView = (TextView) navigationView.getHeaderView(0).findViewById(android.R.id.text2);

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

        if (checkGooglePlayServicesAvailable()) {
            onHasGooglePlayServices();
        }
    }

    private void onHasGooglePlayServices() {
        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(),
                Collections.singletonList(DriveScopes.DRIVE))
                .setBackOff(new ExponentialBackOff());

        String accountName = SharedPreferencesUtils.getAccount(this);

        if (!TextUtils.isEmpty(accountName)) {
            setAccount(accountName);

            if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT) == null) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_fragment, new LibraryBrowser(), FRAGMENT_CONTENT)
                        .commit();
            }
        } else if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_fragment, new AccountChooser(), FRAGMENT_CONTENT)
                    .commit();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PLAY_SERVICES:
                if (resultCode == RESULT_OK) {
                    onHasGooglePlayServices();
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
    public void onPlayVideo(@NonNull MediaInfo mediaInfo) {
        mCastManager.startVideoCastControllerActivity(this, mediaInfo, 0, true);
    }

    @NonNull
    @Override
    public GoogleAccountCredential getCredential() {
        return mCredential;
    }

    @Override
    public void onAccountSelected(@NonNull String accountName) {
        SharedPreferencesUtils.putAccount(this, accountName);
        setAccount(accountName);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_fragment, new LibraryBrowser(), FRAGMENT_CONTENT)
                .commit();
    }

    private void setAccount(@NonNull String accountName) {
        mAccountNameView.setText(accountName);
        mCredential.setSelectedAccountName(accountName);
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
}

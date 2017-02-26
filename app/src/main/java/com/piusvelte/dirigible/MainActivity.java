package com.piusvelte.dirigible;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

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
        AccountChooser.AccountListener,
        MediaInfoLoader.Player {

    static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PLAY_SERVICES = 0;

    private static final String FRAGMENT_CONTENT = TAG + ":fragment:content";

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_logout).setVisible(mCredential != null && !TextUtils.isEmpty(mCredential.getSelectedAccountName()));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                if (mCredential == null) return true;
                SharedPreferencesUtils.clearAccount(this);
                setAccount(null);
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_fragment, new AccountChooser(), FRAGMENT_CONTENT)
                        .commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
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

    private void setAccount(@Nullable String accountName) {
        mCredential.setSelectedAccountName(accountName);
        invalidateOptionsMenu();
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

package com.piusvelte.dirigible;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.piusvelte.dirigible.drive.DriveLibraryBrowser;
import com.piusvelte.dirigible.drive.account.AccountChooser;
import com.piusvelte.dirigible.home.HomeLibraryBrowser;
import com.piusvelte.dirigible.home.ServerInput;
import com.piusvelte.dirigible.home.VideoUtils;
import com.piusvelte.dirigible.util.SharedPreferencesUtils;

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
        Player,
        SessionManagerListener<CastSession>,
        ServerInput.OnServerInputListener {

    static final String TAG = MainActivity.class.getSimpleName();

    private static final boolean USE_DRIVE = false;

    private static final int REQUEST_PLAY_SERVICES = 0;

    private static final String FRAGMENT_CONTENT = TAG + ":fragment:content";
    private static final String FRAGMENT_DIALOG = TAG + ":fragment:dialog";

    private SessionManager mSessionManager;
    private CastSession mCastSession;

    private GoogleAccountCredential mCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSessionManager = CastContext.getSharedInstance(this).getSessionManager();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (USE_DRIVE) {
            if (checkGooglePlayServicesAvailable()) {
                onHasGooglePlayServices();
            }
        } else if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT) == null) {
            String server = SharedPreferencesUtils.getServer(this);

            if (TextUtils.isEmpty(server)) {
                new ServerInput().show(getSupportFragmentManager(), FRAGMENT_DIALOG);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_fragment, HomeLibraryBrowser.newInstance(server), FRAGMENT_CONTENT)
                        .commit();
            }
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
                        .add(R.id.content_fragment, DriveLibraryBrowser.newInstance(null), FRAGMENT_CONTENT)
                        .commit();
            }
        } else if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_fragment, new AccountChooser(), FRAGMENT_CONTENT)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        mCastSession = mSessionManager.getCurrentCastSession();
        mSessionManager.addSessionManagerListener(this, CastSession.class);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSessionManager.removeSessionManagerListener(this, CastSession.class);
        mCastSession = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                menu,
                R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (USE_DRIVE) {
            menu.findItem(R.id.action_logout).setVisible(mCredential != null && !TextUtils.isEmpty(mCredential.getSelectedAccountName()));
        } else if (TextUtils.isEmpty(SharedPreferencesUtils.getServer(this))) {
            menu.findItem(R.id.action_logout).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                if (USE_DRIVE) {
                    if (mCredential == null) return true;
                    SharedPreferencesUtils.clearAccount(this);
                    setAccount(null);
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_fragment, new AccountChooser(), FRAGMENT_CONTENT)
                            .commit();
                } else {
                    SharedPreferencesUtils.putServer(this, null);

                    if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT) != null) {
                        getSupportFragmentManager().beginTransaction()
                                .remove(getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT))
                                .commit();
                    }

                    new ServerInput().show(getSupportFragmentManager(), FRAGMENT_DIALOG);
                }
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
        if (mCastSession == null) return;
        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) return;

        remoteMediaClient.addListener(new RemoteMediaClientListener(this, remoteMediaClient));
        remoteMediaClient.load(mediaInfo, true, 0);
    }

    public static String getQualifiedPath(String path) {
        if (path.startsWith("http://")) return path;
        return "http://" + path;
    }

    @Override
    public void onServerInput(String server) {
        if (!TextUtils.isEmpty(server)) server = getQualifiedPath(server);
        SharedPreferencesUtils.putServer(this, server);

        if (!TextUtils.isEmpty(server)) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_fragment, HomeLibraryBrowser.newInstance(server), FRAGMENT_CONTENT)
                    .commit();
        } else {
            new ServerInput().show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    private static class RemoteMediaClientListener implements RemoteMediaClient.Listener {

        private final Context mContext;
        private final RemoteMediaClient mClient;

        RemoteMediaClientListener(Context context, RemoteMediaClient client) {
            mContext = context;
            mClient = client;
        }

        @Override
        public void onStatusUpdated() {
            Intent intent = new Intent(mContext, ExpandedControlsActivity.class);
            mContext.startActivity(intent);
            mClient.removeListener(this);
        }

        @Override
        public void onMetadataUpdated() {
            // NOOP
        }

        @Override
        public void onQueueStatusUpdated() {
            // NOOP
        }

        @Override
        public void onPreloadStatusUpdated() {
            // NOOP
        }

        @Override
        public void onSendingRemoteMediaRequest() {
            // NOOP
        }
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
                .replace(R.id.content_fragment, DriveLibraryBrowser.newInstance(null), FRAGMENT_CONTENT)
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

    @Override
    public void onSessionStarting(CastSession session) {
        // NOOP
    }

    @Override
    public void onSessionStarted(CastSession session, String s) {
        mCastSession = session;
        invalidateOptionsMenu();
    }

    @Override
    public void onSessionStartFailed(CastSession session, int i) {
        // NOOP
    }

    @Override
    public void onSessionEnding(CastSession session) {
        // NOOP
    }

    @Override
    public void onSessionEnded(CastSession session, int i) {
        if (session == mCastSession) mCastSession = null;
        invalidateOptionsMenu();
    }

    @Override
    public void onSessionResuming(CastSession session, String s) {
        // NOOP
    }

    @Override
    public void onSessionResumed(CastSession session, boolean b) {
        mCastSession = session;
        invalidateOptionsMenu();
    }

    @Override
    public void onSessionResumeFailed(CastSession session, int i) {
        // NOOP
    }

    @Override
    public void onSessionSuspended(CastSession session, int i) {
        // NOOP
    }
}

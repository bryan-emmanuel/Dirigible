package com.piusvelte.dirigible;

import android.app.Application;

import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

/**
 * Created by bemmanuel on 2/28/16.
 */
public class DirigibleApplication extends Application {

    private static final String STYLED_RECEIVER_ID = "D17D2263";

    @Override
    public void onCreate() {
        super.onCreate();

        CastConfiguration configuration = new CastConfiguration.Builder(STYLED_RECEIVER_ID)
                .enableLockScreen()
                .enableAutoReconnect()
                .enableNotification()
                .enableWifiReconnection()
                .setCastControllerImmersive(true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_REWIND, false)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE, true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT, true)
                .build();

        VideoCastManager.initialize(this, configuration);
    }
}

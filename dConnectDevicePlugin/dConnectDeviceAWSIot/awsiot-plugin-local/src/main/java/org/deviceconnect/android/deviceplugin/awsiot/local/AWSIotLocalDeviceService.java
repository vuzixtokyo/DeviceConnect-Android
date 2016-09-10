/*
 AWSIotDeviceService.java
 Copyright (c) 2016 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.awsiot.local;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.deviceconnect.android.deviceplugin.awsiot.core.AWSIotController;
import org.deviceconnect.android.deviceplugin.awsiot.core.AWSIotDeviceApplication;
import org.deviceconnect.android.deviceplugin.awsiot.core.AWSIotPrefUtil;
import org.deviceconnect.android.deviceplugin.awsiot.core.RemoteDeviceConnectManager;

public class AWSIotLocalDeviceService extends Service {

    private static final boolean DEBUG = true;
    private static final String TAG = "AWS-Local";

    public static final String ACTION_START = "org.deviceconnect.android.deviceplugin.awsiot.local.ACTION_START";
    public static final String ACTION_STOP = "org.deviceconnect.android.deviceplugin.awsiot.local.ACTION_STOP";

    private AWSIotLocalManager mAWSIoTLocalManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
//        android.os.Debug.waitForDebugger();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        stopAWSIot();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                startAWSIot();
            } else if (ACTION_STOP.equals(action)) {
                stopAWSIot();
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startAWSIot() {
        if (mAWSIoTLocalManager != null) {
            mAWSIoTLocalManager.disconnect();
        }

        if (DEBUG) {
            Log.i(TAG, "@@@@@@@ AWSIotDeviceService#startAWSIot()");
        }

        AWSIotPrefUtil pref = new AWSIotPrefUtil(this);
        RemoteDeviceConnectManager remote = new RemoteDeviceConnectManager(pref.getManagerName(), pref.getManagerUuid());
        mAWSIoTLocalManager = new AWSIotLocalManager(this, getAWSIotController(), remote);
        mAWSIoTLocalManager.connectAWSIoT();
    }

    private void stopAWSIot() {
        if (DEBUG) {
            Log.i(TAG, "@@@@@@@ AWSIotDeviceService#stopAWSIot()");
        }

        if (mAWSIoTLocalManager != null) {
            mAWSIoTLocalManager.disconnect();
            mAWSIoTLocalManager = null;
        }
    }

    private AWSIotController getAWSIotController() {
        return ((AWSIotDeviceApplication) getApplication()).getAWSIotController();
    }
}
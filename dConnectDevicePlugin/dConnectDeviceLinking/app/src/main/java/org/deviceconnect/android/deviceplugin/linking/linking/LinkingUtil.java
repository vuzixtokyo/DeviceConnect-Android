/*
 LinkingUtil.java
 Copyright (c) 2016 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.linking.linking;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import org.deviceconnect.android.deviceplugin.linking.BuildConfig;

public final class LinkingUtil {
    private static final String PACKAGE_NAME = "com.nttdocomo.android.smartdeviceagent";

    private static final int LED = 0x01;

    public static final int RESULT_OK = -1;
    public static final int RESULT_CANCEL = 1;
    public static final int RESULT_DEVICE_OFF = 4;
    public static final int RESULT_CONNECT_FAILURE = 5;
    public static final int RESULT_CONFLICT = 6;
    public static final int RESULT_PARAM_ERROR = 7;
    public static final int RESULT_SENSOR_UNSUPPORTED = 8;
    public static final int RESULT_OTHER_ERROR = 0;

    private LinkingUtil() {
    }

    public static boolean hasSensor(final LinkingDevice device) {
        return device.getSensor() != null;
    }

    public static boolean hasLED(final LinkingDevice device) {
        return device.isLED() && device.getIllumination() != null;
    }

    public static boolean hasVibration(LinkingDevice device) {
        return device.getVibration() != null;
    }

    public static IlluminationData.Setting getDefaultOffSettingOfLight(final LinkingDevice device) {
        byte[] illumination = device.getIllumination();
        if (illumination == null) {
            return null;
        }

        IlluminationData data = new IlluminationData(illumination);
        for (IlluminationData.Setting setting : data.getPattern().getChildren()) {
            if (setting.getName(0).getName().toLowerCase().contains("off")) {
                return setting;
            }
        }
        return null;
    }

    public static Integer getDefaultOffSettingOfLightId(final LinkingDevice device) {
        IlluminationData.Setting setting = getDefaultOffSettingOfLight(device);
        if (setting != null) {
            return (int) setting.getId();
        }
        return null;
    }

    public static VibrationData.Setting getDefaultOffSettingOfVibration(final LinkingDevice device) {
        byte[] vibration = device.getVibration();
        if (vibration == null) {
            return null;
        }

        VibrationData data = new VibrationData(vibration);
        for (VibrationData.Setting setting : data.getPattern().getChildren()) {
            if (setting.getName(0).getName().toLowerCase().contains("off")) {
                return setting;
            }
        }
        return null;
    }

    public static Integer getDefaultOffSettingOfVibrationId(final LinkingDevice device) {
        VibrationData.Setting setting = getDefaultOffSettingOfVibration(device);
        if (setting != null) {
            return (int) setting.getId();
        }
        return null;
    }

    public static void startGooglePlay(final Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_NAME));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void startLinkingApp(final Context context) {
        context.startActivity(context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME));
    }

    public static boolean isApplicationInstalled(final Context context) {
        try {
            context.getPackageManager().getPackageInfo(PACKAGE_NAME, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
        return false;
    }

}

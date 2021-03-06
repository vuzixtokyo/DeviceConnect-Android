package org.deviceconnect.android.deviceplugin.wear;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WearAppService extends Service implements SensorEventListener {
    /** radian. */
    private static final double RAD2DEG = 180 / Math.PI;

    /** Device NodeID . */
    private final List<String> mIds = Collections.synchronizedList(new ArrayList<String>());

    /** SensorManager. */
    private SensorManager mSensorManager;

    /** Gyro x. */
    private float mGyroX;

    /** Gyro y. */
    private float mGyroY;

    /** Gyro z. */
    private float mGyroZ;

    /** The start time for measuring the interval. */
    private long mStartTime;

    /** GyroSensor. */
    private Sensor mGyroSensor;

    /** AcceleratorSensor. */
    private Sensor mAccelerometer;

    /**
     * スレッド管理用クラス.
     */
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String id = intent.getStringExtra(WearConst.PARAM_SENSOR_ID);
            if (WearConst.DEVICE_TO_WEAR_DEIVCEORIENTATION_REGISTER.equals(action)) {
                if (!mIds.contains(id)) {
                    mIds.add(id);
                }
                registerSensor();
            } else if (WearConst.DEVICE_TO_WEAR_DEIVCEORIENTATION_UNREGISTER.equals(action)) {
                mIds.remove(id);
                if (mIds.isEmpty()) {
                    unregisterSensor();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mIds.clear();
        unregisterSensor();
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long time = System.currentTimeMillis();
            long interval = time - mStartTime;
            mStartTime = time;

            float accelX = sensorEvent.values[0];
            float accelY = sensorEvent.values[1];
            float accelZ = sensorEvent.values[2];
            final String data = accelX + "," + accelY + "," + accelZ
                    + "," + mGyroX + "," + mGyroY + "," + mGyroZ + "," + interval;
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (mIds) {
                        for (String id : mIds) {
                            sendSensorEvent(data, id);
                        }
                    }
                }
            });
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            mGyroX = (float) (sensorEvent.values[0] * RAD2DEG);
            mGyroY = (float) (sensorEvent.values[1] * RAD2DEG);
            mGyroZ = (float) (sensorEvent.values[2] * RAD2DEG);
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    }


    /**
     * センサーイベントをスマホ側に送信する.
     * @param data 送信するデータ
     * @param id 送信先のID
     */
    private void sendSensorEvent(final String data, final String id) {
        GoogleApiClient client = getClient();
        if (!client.isConnected()) {
            ConnectionResult connectionResult = client.blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                if (BuildConfig.DEBUG) {
                    Log.e("WEAR", "Failed to connect google play service.");
                }
                return;
            }
        }

        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(client, id,
                WearConst.WEAR_TO_DEVICE_DEIVCEORIENTATION_DATA, data.getBytes()).await();
        if (!result.getStatus().isSuccess()) {
            if (BuildConfig.DEBUG) {
                Log.e("WEAR", "Failed to send a sensor event.");
            }
        }
    }

    /**
     * センサーを登録する.
     */
    private synchronized void registerSensor() {
        if (mSensorManager != null) {
            return;
        }

        GoogleApiClient client = getClient();
        if (client == null || !client.isConnected()) {
            client = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
            client.connect();
            ConnectionResult connectionResult = client.blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                if (BuildConfig.DEBUG) {
                    Log.e("WEAR", "Failed to connect google play service.");
                }
                return;
            }
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> accelSensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (accelSensors.size() > 0) {
            mAccelerometer = accelSensors.get(0);
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        List<Sensor> gyroSensors = mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
        if (gyroSensors.size() > 0) {
            mGyroSensor = gyroSensors.get(0);
            mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        mStartTime = System.currentTimeMillis();
    }

    /**
     * センサーを解除する.
     */
    private synchronized void unregisterSensor() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mGyroSensor);
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }
    }

    /**
     * GoogleApiClientを取得する.
     * @return GoogleApiClient
     */
    private GoogleApiClient getClient() {
        return ((WearApplication) getApplication()).getGoogleApiClient();
    }
}

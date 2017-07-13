package com.vinhdn.taskcontrol;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.vinhdn.taskcontrol.data.BatteryInfo;
import com.vinhdn.taskcontrol.data.Data;
import com.vinhdn.taskcontrol.data.UserLocation;
import com.vinhdn.taskcontrol.log.LogUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ACCESS_LOCATION_CODE = 122;
    private static final String API_TEST_URL = "http://sigma-solutions.eu/test";

    private static final long TIME_WAIT_OF_LOCATION = 6 * 60 * 1000; // = 6 minutes
    private static final long TIME_WAIT_OF_BATTERY = 9 * 60 * 1000; // = 9 minutes

    private final List<Data> listData = new ArrayList<>();

    private int batteryLevel = 0;
    private int lastDataSize = 0;

    private Thread getLocationThread; //Thread get location
    private Thread getBatteryThread; //Thread get battery
    private Thread postDataThread; //Thread post data

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.startBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
        findViewById(R.id.stopBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
        //Check and request location permission
        checkPermissonLocationRequest(REQUEST_ACCESS_LOCATION_CODE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //register listener batter info
        registerReceiver(this.mBatteryInforReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Unregister listener battery info
        unregisterReceiver(this.mBatteryInforReceiver);
    }

    private void start() {
        //Create thread get location
        getLocationThread = new Thread() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //Lock list data for add user location data
                        synchronized (listData) {
                            Location location = getCurrentLocation();
                            UserLocation userLocation = new UserLocation(-1, -1);
                            if(location != null){ //If get location success create new User location with latitude and longitude
                                userLocation = new UserLocation(location.getLatitude(), location.getLongitude());
                            }
                            listData.add(userLocation);
                            LogUtil.d("Location Collected", listData.get(listData.size() - 1).toData());
                        }
                        sleep(TIME_WAIT_OF_LOCATION);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        getLocationThread.start();

        getBatteryThread = new Thread() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //Lock list data for add battery data
                        synchronized (listData) {
                            listData.add(new BatteryInfo(batteryLevel));
                            LogUtil.d("Battery Collected", listData.get(listData.size() - 1).toData());
                        }
                        sleep(TIME_WAIT_OF_BATTERY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        getBatteryThread.start();

        postDataThread = new Thread() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    if (listData.size() > lastDataSize && listData.size() % 5 == 0) {
                        lastDataSize = listData.size();
                        JSONArray ja = new JSONArray();
                        for (Data data :
                                listData) {
                            if (data != null)
                                ja.put(data.toData());
                        }
                        postData(API_TEST_URL, ja);
                        LogUtil.d("PostData", "" + listData.size());
                    }
                }
            }
        };
        postDataThread.start();
    }

    private void stop() {
        getLocationThread.interrupt();
        getBatteryThread.interrupt();
        postDataThread.interrupt();
        listData.clear();
        lastDataSize = 0;
    }

    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Check app have permission get current location of device, if Permission = Granted show runtime popup request
     *
     * @param requestCode request code to validate result
     * @return true if have, false if not
     */
    private boolean checkPermissonLocationRequest(int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /*
    Create a broadcast receiver to listener a Intent
     */
    private BroadcastReceiver mBatteryInforReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        }
    };

    public void postData(String url, JSONArray listDataToJson) {
        // Create a new HttpClient, Header and request data
        HttpParams myParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(myParams, 10 * 1000);
        HttpConnectionParams.setSoTimeout(myParams, 10 * 1000);
        HttpClient httpclient = new DefaultHttpClient(myParams);
        String dataJson = listDataToJson.toString();

        try {

            HttpPost httppost = new HttpPost(url);
            //Request with content type is json
            httppost.setHeader(HTTP.CONTENT_TYPE, "application/json");

            StringEntity se = new StringEntity(dataJson);
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            httppost.setEntity(se);

            HttpResponse response = httpclient.execute(httppost);//execute request and get response
            String responseString = EntityUtils.toString(response.getEntity());
            LogUtil.i("Response", responseString);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Copyright 2016 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android_quick_settings;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.lang.Runtime.*;


import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;


import com.topjohnwu.superuser.Shell;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;


@SuppressLint("Override")
@TargetApi(Build.VERSION_CODES.N)
public class QuickSettingsService
        extends android.service.quicksettings.TileService {

    private static final String SERVICE_STATUS_FLAG = "serviceStatus";
    private static final String PREFERENCES_KEY = "com.google.android_quick_settings";
    protected Shell.Container container;

    @Override
    public void onCreate() {
        super.onCreate();
        // Assign the container with a pre-configured Container
        container = Shell.Config.newContainer();
    }

    /**
     * Called when the tile is added to the Quick Settings.
     *
     * @return TileService constant indicating tile state
     */
    @Override
    public void onTileAdded() {
        updateTile();
    }

    /**
     * Called when this tile begins listening for events.
     */
    @Override
    public void onStartListening() {
        updateTile();

    }

    /**
     * Called when the user taps the tile.
     */

    @Override
    public void onClick() {

        //switch state
        switchState();

        // updateTile(); will be called asynchronously
    }
/*
    *//**
     * Called when this tile moves out of the listening state.
     *//*
    @Override
    public void onStopListening() {
        Log.d("QS", "Stop Listening");
        //do nothing
    }*/

    /**
     * Called when the user removes this tile from Quick Settings.
     */
    @Override
    public void onTileRemoved() {
        Log.d("QS", "Tile removed, charging enabled");
        //enable mcs
        enableMcs();
    }


    private void switchState() {
        //boolean state = getServiceStatus();
        if (mcsActive()) {
            enableCharging();

        } else {
            enableMcs();
        }

        //https://stackoverflow.com/questions/8369718/sleep-function-in-android-program
        //wait one second, then update the tile
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }

            private void runOnUiThread(Runnable runnable) {
                updateTile();
            }
        }).start();


    }

    public boolean mcsActive() {
        // List<String> output = Shell.su("find /dev/block -iname boot").exec().getOut();
        List<String> output = Shell.su("mcs -i").exec().getOut();
        for (String tmp : output) {
            if (tmp.contains("pause_svc=false")) {
                return true;
            }
        }
        return false;
    }

    public void enableCharging() {
        //mcs -e
        Log.d("QS", "Enable Charging called");
        List<String> output = Shell.su("mcs -e").exec().getOut();
    }


    public void enableMcs() {
        //mcs -s
        List<String> output = Shell.su("mcs -s").exec().getOut();
        Log.d("QS", "Enable MCS called");
    }

    /**
     * @return -1 on error
     */
    public Integer getModCapacity() {
        List<String> output = Shell.su("cat /sys/class/power_supply/gb_battery/capacity").exec().getOut();
        for (String tmp : output) {
            if (tmp.contains("No such file or directory")) {
                Log.e("QS", "No battery-mod connected");
                continue;
            }

            try {
                int charge = Integer.parseInt(tmp);
                Log.d("QS", "Mod Charge: " + charge + "%");
                return charge;
            } catch (java.lang.NumberFormatException e) {
                Log.e("QS", "getModCapacity failed", e);
                //nothing
            }
        }
        return null;
    }

//    private void enableCharging(){
//        setChargingState(true);
//    }

//    private void disableCharging(){
//        setChargingState(false);
//    }

    /*private void setChargingState2(boolean enableCharging){
        //doesn't work
        byte value = 1;

        if (enableCharging){
            Log.d("QS", "Setting charging status: enabled");
            value = 1;

        } else {
            Log.d("QS", "Setting charging status: disabled");
            value = 0;
        }

        try {
            getRuntime().exec("su");

            File outfile;

            try {
                outfile=new File("/sys/class/power_supply/battery/charging_enabled");

                if(outfile.exists()){
                    OutputStream os = new FileOutputStream(outfile);
                    os.write(value);
                    os.flush();
                    os.close();

                }
            } catch (Exception e) {
                Log.e("QS", "Could't write charging status");
                Log.e("QS", e.toString());
            }

        } catch (IOException e) {
            Log.d("QS", "Requesting root didn't work");
        }

    }
*/

    private void setChargingState(boolean enableCharging) {
        //https://gist.github.com/rosterloh/c4bd02bed8c5e7bd47c5

        int value = 1;


        if (enableCharging) {
            Log.d("QS", "Setting charging status: enabled");
            value = 1;

        } else {
            Log.d("QS", "Setting charging status: disabled");
            value = 0;
        }
        String command = String.format("echo %d > /sys/class/power_supply/battery/charging_enabled", value);


        try {
            String[] test = new String[]{"su", "-c", command};
            Runtime.getRuntime().exec(test);


        } catch (IOException e) {

            Log.e("QS", "Setting charging status didn't work", e);

        }

    }

    // Changes the appearance of the tile.
    private void updateTile() {

        Tile tile = this.getQsTile();
//        boolean isActive = getServiceStatus();
        boolean isActive = mcsActive();

        Icon newIcon;
        String newLabel;
        int newState;
        Integer capacity = getModCapacity();
        // Change the tile to match the service status.
        if (isActive) {
            if (capacity != null) {
                newLabel = String.format(Locale.US,
                        "%s - %d%%",
                        getString(R.string.mcs_active),
                        capacity);
            } else {
                newLabel = String.format(Locale.US,
                        "%s",
                        getString(R.string.mcs_active));
            }


            newIcon = Icon.createWithResource(getApplicationContext(),
                    R.drawable.battery_60);

            newState = Tile.STATE_ACTIVE;

        } else {

            if (capacity != null) {
                newLabel = String.format(Locale.US,
                        "%s - %d%%",
                        getString(R.string.mcs_inactive),
                        capacity
                );
            } else {
                newLabel = String.format(Locale.US,
                        "%s",
                        getString(R.string.mcs_inactive)
                );
            }

            newIcon = Icon.createWithResource(getApplicationContext(),
                    R.drawable.battery_charging_60);

           /* newIcon =
                    Icon.createWithResource(getApplicationContext(),
                           android.R.drawable.ic_lock_idle_low_battery);*/

            newState = Tile.STATE_INACTIVE;
            //   newState = Tile.STATE_ACTIVE;
        }

        // Change the UI of the tile.
        tile.setLabel(newLabel);
        tile.setIcon(newIcon);
        tile.setState(newState);

        // Need to call updateTile for the tile to pick up changes.
        tile.updateTile();
    }


//    private boolean getServiceStatus() {
//
//        SharedPreferences prefs =
//                getApplicationContext()
//                        .getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
//
//        //boolean isActive = prefs.getBoolean(SERVICE_STATUS_FLAG, false);
//
//        Log.d("QS", "requesting root");
//        boolean isActive = false;
//        try {
//            getRuntime().exec("su");
//
//            File infile;
//            FileInputStream inputStream;
//            byte[] buffer = null;
//            try {
//                infile=new File("/sys/class/power_supply/battery/charging_enabled");
//
//                if(infile.exists()){
//                    InputStream is = new FileInputStream(infile);
//                    byte[] b = new byte[is.available()];
//                    is.read(b);
//                    String fileContent = new String(b);
//                    is.close();
//
//                    //Log.d("QS", fileContent);
//                    if (fileContent.startsWith("1")){
//                        Log.d("QS", "charging is activated");
//                        isActive = true;
//                    } else {
//                        Log.d("QS", "charging is not activated");
//                        isActive = false;
//                    }
//                }
//            } catch (Exception e) {
//                Log.e("QS", "Could't read charging status", e);
//            }
//
//        } catch (IOException e) {
//            Log.e("QS", "Requesting root didn't work", e);
//        }
//
//        //not needed, yet
//        prefs.edit().putBoolean(SERVICE_STATUS_FLAG, isActive).apply();
//
//        return isActive;
//    }


}

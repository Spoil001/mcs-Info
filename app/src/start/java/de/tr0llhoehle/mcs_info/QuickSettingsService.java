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

package de.tr0llhoehle.mcs_info;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;


import com.topjohnwu.superuser.Shell;

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
        if(container == null) {
            container = Shell.Config.newContainer();
        }
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
        Log.d("QS", "Tile removed, mcs enabled");
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
     * @return NULL on error
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

    /**
     * true, when the mod is charging the battery
     *
     * @return NULL on error
     */
    public Boolean getModChargingState() {
        //might want to use /sys/class/power_supply/battery/battery_charging_enabled or charging_enabled
        List<String> output = Shell.su("cat /sys/class/power_supply/gb_battery/current_now").exec().getOut();
        for (String tmp : output) {
            if (tmp.contains("No such file or directory")) {
                Log.e("QS", "No battery-mod connected");
                continue;
            }

            try {
                int current = Integer.parseInt(tmp);
                Log.d("QS", "Mod Current: " + current);
                if (current < 0) {
                    return true;
                } else {
                    return false;
                }
            } catch (java.lang.NumberFormatException e) {
                Log.e("QS", "getModChargingState failed", e);
                //nothing
            }
        }
        return null;
    }


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
        newIcon = Icon.createWithResource(getApplicationContext(),
                R.drawable.battery_alert);
        Boolean charging = Boolean.FALSE.equals(getModChargingState());

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
            newState = Tile.STATE_INACTIVE;
        }

        if (charging && capacity != null) {

            if (capacity <= 14) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_10);
            } else if (capacity <= 24) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_20);
            } else if (capacity <= 34) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_30);
            } else if (capacity <= 44) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_40);
            } else if (capacity <= 54) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_50);
            } else if (capacity <= 64) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_60);
            } else if (capacity <= 74) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_70);
            } else if (capacity <= 84) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_80);
            } else if (capacity <= 94) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_90);
            } else {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_full);
            }
        } else if (capacity != null) {
            if (capacity <= 14) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_10);
            } else if (capacity <= 24) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_20);
            } else if (capacity <= 34) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_30);
            } else if (capacity <= 44) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_40);
            } else if (capacity <= 54) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_50);
            } else if (capacity <= 64) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_60);
            } else if (capacity <= 74) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_70);
            } else if (capacity <= 84) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_80);
            } else if (capacity <= 94) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_90);
            } else if (capacity <= 104) {
                newIcon = Icon.createWithResource(getApplicationContext(),
                        R.drawable.battery_charging_100);
            }
        }

        // Change the UI of the tile.
        tile.setLabel(newLabel);
        tile.setIcon(newIcon);
        tile.setState(newState);

        // Need to call updateTile for the tile to pick up changes.
        tile.updateTile();
    }

}

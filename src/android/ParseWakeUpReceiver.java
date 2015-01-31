/*
 * Copyright (C) 2015 Frosty Elk AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.frostyelk.cordova.parse.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseInstallation;


/**
 * This class starts just before ParsePush and ParseBroadcastReceiver and does the Parse initialization.
 * The ParseWakeUpReceiver must be declared before GcmBroadcastReceiver and ParseBroadcastReceiver in the manifest.
 * The ParseWakeUpReceiver also handles when the device wakes up from sleep and receives android.intent.action.USER_PRESENT
 *
 * Note that is a brutal workaround due to that Parse(Facebook) refuses to fix this. It can probably stop working
 * anytime if the order of Receivers in the manifest are reordered by Android at load time.
 */
public class ParseWakeUpReceiver extends BroadcastReceiver {
    String TAG = "ParseWakeUpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ParseWakeUpReceiver.onReceive");
        Log.d(TAG, "Intent: " + intent.getAction());

        SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences(ParsePlugin.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String appId = sharedPref.getString(ParsePlugin.PREFERENCE_APP_ID, "");
        String clientKey = sharedPref.getString(ParsePlugin.PREFERENCE_CLIENT_KEY, "");

        if (ParsePlugin.isActive()) {
            Log.d(TAG, "Parse Plugin is active, no need to start Parse");
        } else {
            if (!"".equals(appId)) {
                Log.i(TAG, "Parse initialize in ParseWakeUpReceiver");
                Parse.initialize(context.getApplicationContext(), appId, clientKey);
                ParseInstallation.getCurrentInstallation().saveInBackground();
            }
        }
    }
}

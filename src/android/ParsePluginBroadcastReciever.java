/**
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseAnalytics;
import com.parse.ParsePushBroadcastReceiver;

public class ParsePluginBroadcastReciever extends ParsePushBroadcastReceiver {

    private static final String LOGTAG = "ParsePluginReciever";

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.i(LOGTAG, "onPushReceive Intent: " + intent.getAction());

        Bundle pushData = intent.getExtras();

        if (ParsePlugin.isAppForeground()) {
            Log.i(LOGTAG, "App is in foreground");
            pushData.putBoolean("foreground", true);
            ParsePlugin.receivePushData(pushData);
        } else {
            // Let Parse show the notification
            Log.i(LOGTAG, "App is NOT in foreground");
            super.onPushReceive(context, intent);
        }
    }

    @Override
    protected void onPushOpen(Context context, Intent intent) {
        Log.i(LOGTAG, "onPushOpen Intent: " + intent.getAction());

        Bundle pushData = intent.getExtras();

        if (ParsePlugin.isActive()) {
            Log.i(LOGTAG, "App is Active");
            Log.i(LOGTAG, "Resuming current activity");

            // Resume current Activity
            pushData.putBoolean("coldstart", false);
            ParsePlugin.getActivity().startActivity(ParsePlugin.getActivity().getIntent());
        } else {
            Log.i(LOGTAG, "App is not Active");
            Log.i(LOGTAG, "Starting Main activity");

            // Start main application
            pushData.putBoolean("coldstart", true);
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(context.getPackageName());
            context.startActivity(launchIntent);
        }

        if (ParsePlugin.isAppForeground()) {
            pushData.putBoolean("foreground", true);
        } else {
            pushData.putBoolean("foreground", false);
        }

        ParsePlugin.receivePushData(pushData);

        ParseAnalytics.trackAppOpenedInBackground(intent);

    }
}

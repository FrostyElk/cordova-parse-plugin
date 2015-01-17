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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseInstallation;

/**
 * This is a workaround for Parse Push. ParsePush needs a context when the push
 * is received.
 *
 * The Application level is not used. It has been replaced by a nicer workaround. Please
 * see the ParseWakeUpReceiver class.
 */
@SuppressWarnings("UnusedDeclaration")
public class ParsePluginMainApplication extends Application {
    private static final String LOGTAG = "ParsePluginApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOGTAG, "ParsePluginMainApplication onCreate");

        SharedPreferences sharedPref = getSharedPreferences(ParsePlugin.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String appId = sharedPref.getString(ParsePlugin.PREFERENCE_APP_ID, "");
        String clientKey = sharedPref.getString(ParsePlugin.PREFERENCE_CLIENT_KEY, "");

        if (!"".equals(appId)) {
//			Log.i(LOGTAG, "Parse initialize: " + appId + ", " + clientKey);
            Parse.initialize(this, appId, clientKey);
            ParseInstallation.getCurrentInstallation().saveInBackground();
        }
    }
}
/**
Copyright 2014 Frosty Elk AB

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package se.frostyelk.cordova.parse.plugin;

import java.util.Iterator;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.PushService;
import com.parse.SaveCallback;

/**
 * This class represents the native implementation for the Parse Cordova plugin.
 */
public class ParsePlugin extends CordovaPlugin {

	private static final String LOGTAG = "ParsePlugin";
	private static final String ACTION_INITIALIZE = "initialize";
	private static final String ACTION_GET_INSTALLATION_ID = "getInstallationId";
	private static final String ACTION_GET_INSTALLATION_OBJECT_ID = "getInstallationObjectId";
	private static final String ACTION_GET_SUBSCRIPTIONS = "getSubscriptions";
	private static final String ACTION_GET_PENDING_PUSH = "getPendingPush";
	private static final String ACTION_SUBSCRIBE = "subscribe";
	private static final String ACTION_UNSUBSCRIBE = "unsubscribe";

	private static CordovaInterface cordovaInterface;
	private static CordovaWebView webView;
	private static boolean appForeground = false;
	private static JSONObject pushDataJSON;
	private static boolean sendPushDataWhenResumed = false;
	private static boolean sendPushDataAfterColdStart = false;

	public static final String PREFERENCE_APP_ID = "se.frostyelk.cordova.parse.ParseAppId";
	public static final String PREFERENCE_CLIENT_KEY = "se.frostyelk.cordova.parse.ClientKey";
	public static final String SHARED_PREFERENCES = "se.frostyelk.cordova.parse";
	private String appId;
	private String clientKey;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView cordovaWebView) {
		super.initialize(cordova, cordovaWebView);

		webView = cordovaWebView;
		cordovaInterface = cordova;

		appForeground = true;

		Log.i(LOGTAG, "Parse plugin initialize");
	}

	public static boolean isAppForeground() {
		return appForeground;
	}

	public static void receivePushData(Bundle extras) {
		Log.i(LOGTAG, "receivePushData");

		// Convert data to JSON
		if (extras == null) {
			pushDataJSON = new JSONObject();
		} else {
			pushDataJSON = convertBundleToJson(extras);
		}

		// Transfer notification to JS layer
		if (appForeground) {
			// Send Javascript directly
			sendPushDataWhenResumed = false;
			sendPushToWebView(pushDataJSON);
		} else {
			sendPushDataAfterColdStart = extras.getBoolean("coldstart");

			boolean foregroundActual;

			if (sendPushDataAfterColdStart) {
				// Cold start will not get a onResume(), Cordova bug/feature?
				foregroundActual = false;
			} else {
				// After activity start the app will be foreground after
				// onResume()
				foregroundActual = true;
			}

			// Wait for app to resume, then send notification
			sendPushDataWhenResumed = true;

			try {
				pushDataJSON.put("foreground", foregroundActual);
			} catch (JSONException e) {
				Log.i(LOGTAG, "JSON error: " + e.getMessage());
			}
		}
	}

	public static Activity getActivity() {
		return cordovaInterface.getActivity();
	}

	public static boolean isActive() {
		return webView != null;
	}

	public static void sendPushToWebView(JSONObject jsonData) {
		final String url = "javascript:cordova.fireDocumentEvent('onParsePushReceived', " + jsonData.toString() + ");";
		Log.i(LOGTAG, "sendPushToWebView: " + url);

		if (webView != null) {
			webView.post(new Runnable() {

				@Override
				public void run() {
					webView.loadUrl(url);
				}
			});
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(LOGTAG, "onDestroy");
		appForeground = false;
		webView = null;
		final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);
		Log.i(LOGTAG, "onPause");
		appForeground = false;

		SharedPreferences sharedPref = getActivity().getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(PREFERENCE_APP_ID, appId);
		editor.putString(PREFERENCE_CLIENT_KEY, clientKey);
		editor.commit();

	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		Log.i(LOGTAG, "onResume");
		appForeground = true;

		if (sendPushDataWhenResumed) {
			sendPushDataWhenResumed = false;
			sendPushToWebView(pushDataJSON);
		}
	}

	/*
	 * Serializes a bundle to JSON.
	 */
	private static JSONObject convertBundleToJson(Bundle extras) {
		try {
			JSONObject json;
			json = new JSONObject().put("event", "message");

			JSONObject jsondata = new JSONObject();
			Iterator<String> it = extras.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				Object value = extras.get(key);

				// System data from Android
				if (key.equals("from") || key.equals("collapse_key")) {
					json.put(key, value);
				} else if (key.equals("foreground")) {
					json.put(key, extras.getBoolean("foreground"));
				} else if (key.equals("coldstart")) {
					json.put(key, extras.getBoolean("coldstart"));
				} else {
					// Maintain backwards compatibility
					if (key.equals("message") || key.equals("msgcnt") || key.equals("soundname")) {
						json.put(key, value);
					}

					if (value instanceof String) {
						// Try to figure out if the value is another JSON object

						String strValue = (String) value;
						if (strValue.startsWith("{")) {
							try {
								JSONObject json2 = new JSONObject(strValue);
								jsondata.put(key, json2);
							} catch (Exception e) {
								jsondata.put(key, value);
							}
							// Try to figure out if the value is another JSON
							// array
						} else if (strValue.startsWith("[")) {
							try {
								JSONArray json2 = new JSONArray(strValue);
								jsondata.put(key, json2);
							} catch (Exception e) {
								jsondata.put(key, value);
							}
						} else {
							jsondata.put(key, value);
						}
					}
				}
			} // while
			json.put("payload", jsondata);

//			Log.i(LOGTAG, "extrasToJSON: " + json.toString());

			return json;
		} catch (JSONException e) {
			Log.e(LOGTAG, "extrasToJSON: JSON exception");
		}
		return null;
	}

	/**
	 * This is the main method for the Parse Plugin. All API calls go through
	 * here. This method determines the action, and executes the appropriate
	 * call.
	 *
	 * @param action
	 *            The action that the plugin should execute.
	 * @param inputs
	 *            The input parameters for the action.
	 * @param callbackContext
	 *            The callback context.
	 * @return A PluginResult representing the result of the provided action. A
	 *         status of INVALID_ACTION is returned if the action is not
	 *         recognized.
	 */
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		PluginResult result = null;

		if (ACTION_INITIALIZE.equals(action)) {
			result = initialize(callbackContext, args);
		} else if (ACTION_GET_INSTALLATION_ID.equals(action)) {
			result = getInstallationId(callbackContext);
		} else if (ACTION_GET_INSTALLATION_OBJECT_ID.equals(action)) {
			result = getInstallationObjectId(callbackContext);
		} else if (ACTION_GET_SUBSCRIPTIONS.equals(action)) {
			result = getSubscriptions(callbackContext);
		} else if (ACTION_SUBSCRIBE.equals(action)) {
			result = subscribe(args.getString(0), callbackContext);
		} else if (ACTION_UNSUBSCRIBE.equals(action)) {
			result = unsubscribe(args.getString(0), callbackContext);
		} else if (ACTION_GET_PENDING_PUSH.equals(action)) {
			result = getPendingPush(callbackContext);
		} else {
			result = new PluginResult(Status.INVALID_ACTION);
		}

		if (result != null)
			callbackContext.sendPluginResult(result);

		return true;
	}

	private PluginResult initialize(final CallbackContext callbackContext, final JSONArray args) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					appId = args.getString(0);
					clientKey = args.getString(1);
					Parse.initialize(cordova.getActivity(), appId, clientKey);
					ParseInstallation.getCurrentInstallation().save();
					callbackContext.success();
				} catch (JSONException e) {
					callbackContext.error("JSONException: " + e.getMessage());
				} catch (ParseException e) {
					callbackContext.error("ParseException: " + e.getMessage());
				}
			}
		});

		return null;
	}

	private PluginResult getInstallationId(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				String installationId = ParseInstallation.getCurrentInstallation().getInstallationId();
				callbackContext.success(installationId);
			}
		});

		return null;
	}

	private PluginResult getInstallationObjectId(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				String objectId = ParseInstallation.getCurrentInstallation().getObjectId();
				callbackContext.success(objectId);
			}
		});

		return null;
	}

	private PluginResult getSubscriptions(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				Set<String> subscriptions = PushService.getSubscriptions(cordova.getActivity());
				callbackContext.success(subscriptions.toString());
			}
		});

		return null;
	}

	private PluginResult subscribe(final String channel, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				Log.i(LOGTAG, "Subscribe to channel: " + channel);
				ParsePush.subscribeInBackground(channel, new SaveCallback() {
					@Override
					public void done(ParseException e) {
						if (e == null) {
							callbackContext.success();
						} else {
							Log.i(LOGTAG, "Failed to subscribe for push: " + e.getMessage());
							callbackContext.error("Subscribe error: " + e.getMessage());
						}
					}
				});
			}
		});

		return null;
	}

	private PluginResult unsubscribe(final String channel, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				PushService.unsubscribe(cordova.getActivity(), channel);
				callbackContext.success();
			}
		});

		return null;
	}

	private PluginResult getPendingPush(final CallbackContext callbackContext) {
		Log.i(LOGTAG, "getPendingPush");

		if (sendPushDataWhenResumed) {
			Log.i(LOGTAG, "getPendingPush sends data");
			sendPushToWebView(pushDataJSON);
			sendPushDataWhenResumed = false;
		}
		callbackContext.success();

		return null;
	}

}

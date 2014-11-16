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

import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

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
	private static final String ACTION_SUBSCRIBE = "subscribe";
	private static final String ACTION_UNSUBSCRIBE = "unsubscribe";

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		Log.i(LOGTAG, "Parse plugin initialize");
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
					String appId = args.getString(0);
					String clientKey = args.getString(1);
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
				Log.d(LOGTAG, "Subscribe to channel: " + channel);
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

}

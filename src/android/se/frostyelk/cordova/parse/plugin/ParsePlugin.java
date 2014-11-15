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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * This class represents the native implementation for the Parse Cordova plugin.
 */
public class ParsePlugin extends CordovaPlugin {

	private static final String LOGTAG = "ParsePlugin";
	private static final String ACTION_ECHO = "";

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
	public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
		PluginResult result = null;

		if (ACTION_ECHO.equals(action)) {
			JSONObject options = inputs.optJSONObject(0);
			result = executeEcho(options, callbackContext);
		} else {
			result = new PluginResult(Status.INVALID_ACTION);
		}

		if (result != null)
			callbackContext.sendPluginResult(result);

		return true;
	}

	private PluginResult executeEcho(JSONObject options, CallbackContext callbackContext) {
		callbackContext.success();
		return null;
	}

}

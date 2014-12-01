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
/* jshint -W117 */
var exec = require('cordova/exec');

var parseExport = {};

parseExport.echo = function (successCallback, failureCallback) {
	cordova.exec(successCallback, failureCallback, 'ParsePlugin', 'echo');
};

parseExport.initialize = function (appId, clientKey, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, 'ParsePlugin', 'initialize', [appId, clientKey]);
};

parseExport.getInstallationId = function (successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, 'ParsePlugin', 'getInstallationId', []);
};

parseExport.getInstallationObjectId = function (successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, 'ParsePlugin', 'getInstallationObjectId', []);
};

parseExport.getSubscriptions = function (successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, 'ParsePlugin', 'getSubscriptions', []);
};

parseExport.subscribe = function (channel, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, 'ParsePlugin', 'subscribe', [channel]);
};

parseExport.unsubscribe = function (channel, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, 'ParsePlugin', 'unsubscribe', [channel]);
};

parseExport.getPendingPush = function (channel, successCallback, errorCallback) {
	cordova.exec(successCallback, errorCallback, 'ParsePlugin', 'getPendingPush', []);
};

module.exports = parseExport;
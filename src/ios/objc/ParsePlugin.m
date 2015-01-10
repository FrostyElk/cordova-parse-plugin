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

#import "ParsePlugin.h"
#import <Cordova/CDV.h>
#import <Parse/Parse.h>

@implementation ParsePlugin

- (void)initialize:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Parse Plugin initialize");
    CDVPluginResult* pluginResult = nil;
    
    NSString* appId = [command.arguments objectAtIndex:0];
    NSString* clientKey = [command.arguments objectAtIndex:1];
    
    [Parse setApplicationId: appId clientKey:clientKey];
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getInstallationId:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Parse Plugin getInstallationId");
    CDVPluginResult* pluginResult = nil;
    
    NSString* installationId = nil;
    
    installationId = @"123123123";
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:installationId];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getPendingPush:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Parse Plugin getPendingPush");
    CDVPluginResult* pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}


@end
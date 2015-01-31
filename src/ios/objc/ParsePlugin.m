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

@synthesize callbackId;
@synthesize pendingNotifications = _pendingNotifications;

static NSDictionary *coldstartNotification;

NSMutableArray *jsEventQueue;
BOOL canDeliverNotifications = NO;


/* Holds the notifications data */
- (NSMutableArray*)pendingNotifications {
    if(_pendingNotifications == nil) {
        _pendingNotifications = [[NSMutableArray alloc] init];
    }
    return _pendingNotifications;
}

+(void) load
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(checkForColdStartNotification:)
                                                 name:UIApplicationDidFinishLaunchingNotification object:nil];
}

+ (void) checkForColdStartNotification:(NSNotification *)notification
{
    NSLog(@"Parse Plugin checkForColdStartNotification");
    NSDictionary *launchOptions = [notification userInfo];
    NSDictionary *payload = [launchOptions objectForKey: @"UIApplicationLaunchOptionsRemoteNotificationKey"];
    
    if(payload) {
        NSMutableDictionary *extendedPayload = [payload mutableCopy];
        [extendedPayload setObject:[NSNumber numberWithBool:NO] forKey:@"foreground"];
        [extendedPayload setObject:[NSNumber numberWithBool:YES] forKey:@"coldstart"];
        coldstartNotification = extendedPayload;
    }
}


- (void)initialize:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Parse Plugin initialize");
    CDVPluginResult* pluginResult = nil;
    
    NSString* appId = [command.arguments objectAtIndex:0];
    NSString* clientKey = [command.arguments objectAtIndex:1];
    
    [Parse setApplicationId: appId clientKey:clientKey];
    
    PFInstallation *currentInstallation = [PFInstallation currentInstallation];
    [currentInstallation saveInBackground];
    
    // Save the Parse Id and Key for AppDelegate to re-initialize at coldstart
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setObject: appId forKey: @"AppId"];
    [defaults setObject: clientKey forKey: @"ClientKey"];
    
    // Register for Push Notitications iOS 8
    if ([[UIApplication sharedApplication] respondsToSelector:@selector(registerUserNotificationSettings:)]) {
        UIUserNotificationType userNotificationTypes = (UIUserNotificationTypeAlert |
                                                        UIUserNotificationTypeBadge |
                                                        UIUserNotificationTypeSound);
        
        UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:userNotificationTypes categories:nil];
        
        [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
        [[UIApplication sharedApplication] registerForRemoteNotifications];
    } else {
        // Register for Push Notifications before iOS 8
        [[UIApplication sharedApplication] registerForRemoteNotificationTypes:(UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeAlert | UIRemoteNotificationTypeSound)];
    }
    
    self.callbackId = command.callbackId;
    
    canDeliverNotifications = YES;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)getInstallationId:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Parse Plugin getInstallationId");
    
    [self.commandDelegate runInBackground:^{
        CDVPluginResult* pluginResult = nil;
        PFInstallation *currentInstallation = [PFInstallation currentInstallation];
        NSString *installId = currentInstallation.installationId;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:installId];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}


- (void)getPendingPush:(CDVInvokedUrlCommand*)command
{
    NSLog(@"Parse Plugin getPendingPush");
    CDVPluginResult* pluginResult = nil;
    
    [self flushNotificationEventQueue];
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}


- (void)subscribe: (CDVInvokedUrlCommand *)command
{
    NSLog(@"Parse Plugin subscribe");
    
    [self.commandDelegate runInBackground:^{
        CDVPluginResult* pluginResult = nil;
        PFInstallation *currentInstallation = [PFInstallation currentInstallation];
        NSString *channel = [command.arguments objectAtIndex:0];
        [currentInstallation addUniqueObject:channel forKey:@"channels"];
        [currentInstallation saveInBackground];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}


- (void)unsubscribe: (CDVInvokedUrlCommand *)command
{
    NSLog(@"Parse Plugin unsubscribe");
    
    [self.commandDelegate runInBackground:^{
        CDVPluginResult* pluginResult = nil;
        PFInstallation *currentInstallation = [PFInstallation currentInstallation];
        NSString *channel = [command.arguments objectAtIndex:0];
        [currentInstallation removeObject:channel forKey:@"channels"];
        [currentInstallation saveInBackground];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}


- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    NSMutableDictionary *results = [NSMutableDictionary dictionary];
    NSString *token = [[[[deviceToken description] stringByReplacingOccurrencesOfString:@"<"withString:@""]
                        stringByReplacingOccurrencesOfString:@">" withString:@""]
                       stringByReplacingOccurrencesOfString: @" " withString: @""];
    [results setValue:token forKey:@"deviceToken"];
    
#if !TARGET_IPHONE_SIMULATOR
    
    CDVPluginResult* pluginResult = nil;
    
    PFInstallation *currentInstallation = [PFInstallation currentInstallation];
    [currentInstallation setDeviceTokenFromData:deviceToken];
    
    [currentInstallation saveInBackground];
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:token];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    
#endif
}


- (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    NSLog(@"Parse Plugin didFailToRegisterForRemoteNotificationsWithError");
    
    CDVPluginResult* pluginResult = nil;
    NSString *err = [error description];
    NSLog(@"Notification register fail message: %@", err);
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:err];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    
}

- (void)didReceiveRemoteNotificationWithPayload: (NSDictionary *)payload
{
    NSLog(@"Parse Plugin didReceiveRemoteNotificationWithPayload");
    
    [self.pendingNotifications addObject:payload];
    
    
    BOOL receivedInForeground = [[payload objectForKey:@"foreground"] boolValue];
    
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:payload options:NSJSONWritingPrettyPrinted error:nil];
    
    NSString *json = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    
    NSLog(@"Msg: %@", json);
    
    NSString * jsCallBack = [NSString stringWithFormat:@"cordova.fireDocumentEvent('onParsePushReceived', %@);",json];
    
    // Decrement the badge when the push is opened
    PFInstallation *currentInstallation = [PFInstallation currentInstallation];
    NSLog(@"Current installation badge: %d", currentInstallation.badge);
    if (currentInstallation.badge > 0){
        currentInstallation.badge--;
        [currentInstallation saveInBackground];
    }
    
    if(receivedInForeground) {
        NSLog(@"Sending in foreground");
        [self.commandDelegate evalJs:jsCallBack];
    }
    else
    {
        NSLog(@"Put in JS Event Queue for background app");
        if(jsEventQueue == nil)
        {
            jsEventQueue = [[NSMutableArray alloc] init];
        }
        
        [jsEventQueue addObject:jsCallBack];
        [PFAnalytics trackAppOpenedWithRemoteNotificationPayload:payload];
    }
}


- (void) didBecomeActive:(NSNotification *)notification
{
    NSLog(@"Parse Plugin didBecomeActive");
    
    if(canDeliverNotifications)
    {
        [self flushNotificationEventQueue];
    }
    
}

-(void) flushNotificationEventQueue
{
    NSLog(@"Parse Plugin flushNotificationEventQueue");
    
    if(jsEventQueue != nil && [jsEventQueue count] > 0)
    {
        for(NSString *notificationEvent in jsEventQueue)
        {
            [self.commandDelegate evalJs:notificationEvent];
        }
        
        [jsEventQueue removeAllObjects];
    }
}

- (void) pluginInitialize
{
    NSLog(@"Parse Plugin pluginInitialize");
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didBecomeActive:)
                                                 name:UIApplicationDidBecomeActiveNotification object:nil];
    
    if(coldstartNotification)
    {
        NSLog(@"Parse Plugin coldstartNotification");
        
        [self didReceiveRemoteNotificationWithPayload:coldstartNotification];
        coldstartNotification = nil;
    }
}


@end
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

#import "AppDelegate+ParsePushNotification.h"

#import "ParsePlugin.h"
#import <objc/runtime.h>
#import <Parse/Parse.h>


@implementation AppDelegate (ParsePushNotification)

+ (void) load
{
    NSLog(@"AppDelegate+ParsePushNotification load");
    
    Method original, swizzled;
    
    original = class_getInstanceMethod(self, @selector(init));
    swizzled = class_getInstanceMethod(self, @selector(swizzled_init));
    method_exchangeImplementations(original, swizzled);
}

- (AppDelegate *) swizzled_init
{
    NSLog(@"AppDelegate+ParsePushNotification swizzled_init");
    
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(parseTrackAppOpen:)
                                                 name:@"UIApplicationDidFinishLaunchingNotification" object:nil];
    
    // This actually calls the original init method over in AppDelegate. Equivilent to calling super
    // on an overrided method, this is not recursive, although it appears that way.
    return [self swizzled_init];
}

- (void) parseTrackAppOpen:(NSNotification *)notification
{
    NSLog(@"AppDelegate+ParsePushNotification parseTrackAppOpen");
    
    // Restore the Appid and ClientKey that was saved during initialize
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString *clientKey = [defaults objectForKey:@"ClientKey"];
    NSString *appId =  [defaults objectForKey:@"AppId"];
    
    if (clientKey && appId) {
        NSLog(@"Setting new Parse Id in AppDelegate");
        [Parse setApplicationId:appId clientKey:clientKey];
        PFInstallation *currentInstallation = [PFInstallation currentInstallation];
        [currentInstallation saveInBackground];
        
        // Parse Analytics
        if (notification)
        {
            NSDictionary *launchOptions = [notification userInfo];
            [PFAnalytics trackAppOpenedWithLaunchOptions:launchOptions];
        }
    }
    
    
}

- (void) application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    ParsePlugin *pushHandler = [self getCommandInstance:@"ParsePlugin"];
    [pushHandler didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}


- (void) application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    ParsePlugin *pushHandler = [self getCommandInstance:@"ParsePlugin"];
    [pushHandler didFailToRegisterForRemoteNotificationsWithError:error];
}


- (void) application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)payload
{
    
    NSLog(@"AppDelegate+ParsePushNotification didReceiveRemoteNotification");
    UIApplicationState appstate = [[UIApplication sharedApplication] applicationState];
    
    NSMutableDictionary *extendedPayload = [payload mutableCopy];
    [extendedPayload setObject:[NSNumber numberWithBool:(appstate == UIApplicationStateActive)] forKey:@"foreground"];
    
    ParsePlugin *pushHandler = [self getCommandInstance:@"ParsePlugin"];
    [pushHandler didReceiveRemoteNotificationWithPayload:extendedPayload];
}


- (id) getCommandInstance:(NSString*)className
{
    return [self.viewController getCommandInstance:className];
}

@end

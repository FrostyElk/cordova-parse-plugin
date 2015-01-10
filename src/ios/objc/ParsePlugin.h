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

#ifndef _ParsePlugin_h
#define _ParsePlugin_h

#import <Cordova/CDV.h>

@interface ParsePlugin : CDVPlugin

- (void)initialize:(CDVInvokedUrlCommand*)command;
- (void)getInstallationId:(CDVInvokedUrlCommand*)command;
- (void)getPendingPush:(CDVInvokedUrlCommand*)command;

@end



#endif

import Foundation
import Parse


@objc(ParsePlugin) class ParsePlugin : CDVPlugin {
    
    func initialize(command: CDVInvokedUrlCommand) {
        NSLog("Parse Plugin initialize")
        let appId = command.argumentAtIndex(0) as String
        let clientKey = command.argumentAtIndex(1) as String
        
        Parse.setApplicationId(appId, clientKey: clientKey)
        
        PFInstallation.currentInstallation().save()
        
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
    
    func getInstallationId(command: CDVInvokedUrlCommand) {
        NSLog("Parse Plugin getInstallationId")
        let currentInstallaion = PFInstallation.currentInstallation()
        let installationId = currentInstallaion.installationId
        
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: installationId)
        commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
    
    func subscribe (command: CDVInvokedUrlCommand) {
        let channel = command.argumentAtIndex(0) as String
        NSLog("Parse Plugin subscribe to channel: " + channel)
        
        var err: NSError?
        PFPush.subscribeToChannel(channel, error: &err)
        
        var pluginResult : CDVPluginResult
        if let error = err? {
            NSLog("Parse Plugin subscribe to channel failed: " + error.description)
            pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAsString: error.description)
        } else {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        }
        
        commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
    
}

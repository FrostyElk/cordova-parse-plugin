import Foundation
 
@objc(ParsePlugin) class ParsePlugin : CDVPlugin {
  func echo(command: CDVInvokedUrlCommand) {
    var message = command.arguments[0] as String
 
    message = message.uppercaseString
 
    var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: message)
    commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
  }
}

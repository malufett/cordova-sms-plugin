var smsExport = {};

smsExport.sendMessage = function(messageInfo, successCallback, errorCallback) {
    if (messageInfo == null || typeof messageInfo !== 'object') {
    
        if (errorCallback) {
            errorCallback({
                code: "INVALID_INPUT",
                message: "Invalid Input"
            });
        }
       
        return;
    }
           
    var phoneNumber = messageInfo.phoneNumber;
    var textMessage = messageInfo.textMessage || "";
           
    if (! phoneNumber) {
        console.log("Missing Phone Number");
    
        if (errorCallback) {
            errorCallback({
                code: "MISSING_PHONE_NUMBER",
                message: "Missing Phone number"
            });
        }
           
        return;
    }
           
    cordova.exec(successCallback, errorCallback, "Sms", "sendMessage", [phoneNumber, textMessage]);
};

smsExport.readMessage = function(type, successCallback, errorCallback) {              
    cordova.exec(successCallback, errorCallback, "Sms", "readMessage", [type]);
};

smsExport.isWAAvailable = function(successCallback, errorCallback) {              
    cordova.exec(successCallback, errorCallback, "Sms", "isWhatsAppInstalled", []);
};

module.exports = smsExport;

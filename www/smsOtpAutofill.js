var exec = require('cordova/exec');

module.exports = {
    startSmsUserConsent: function(successCallback,errorCallback,options) {
        cordova.exec(successCallback,errorCallback,"SmsOtpAutofill","startSmsUserConsent",[options]);
    }
}

# cordova-plugin-otp-autofill


### Description

This plugin extracts the OTP of required length from the received SMS.
It uses the  [Request one-time consent to read an SMS verification code](https://developers.google.com/identity/sms-retriever/user-consent/request), making it compatible with Android 15.


### Installation

```
cordova plugin add cordova-plugin-otp-autofill

```

### Supported Platforms

- Android

### Usage

```
var options = {
	otpLength: 4,
	delimiter: 'is',
	senderID: 'WAYSMS',
	validateSender: true
}

smsOtpAutofill.startOTPListener(successCallback,errorCallback,options);
```

### Function Parameters

- _successCallback_: This callback function is executed when OTP is extracted successfully.
- _errorCallback_: This callback function is executed if an error occurs (eg. when options are empty or sms permissions are denied).
- _options_: the OTP options


### Example

```	
var options = {
	otpLength: 4,
	delimiter: 'is',
	senderID: 'WAYSMS',
	validateSender: true
}
	
function successCallback(result) {
	//Set the result (OTP value) to the field
	console.log(result);
}


function errorCallback(message) {
	console.log(message);
}

smsOtpAutofill.startOTPListener(successCallback,errorCallback,options);

```


### OTP options

Parameters to customize the retrieval of the OTP.

     { otpLength: 4, delimiter: 'is', senderID: 'WAYSMS'}
     

### Options

- __delimiter__: This text appears just before the OTP in the SMS content. For example, if the SMS content is 'Your verification code is 6367', the delimiter should be set to 'is'. _(String)_

- __length__: This is the length of the OTP extracted from the SMS. For example, if the SMS content is 'Your OTP is 6367', the length should be set to 4. _(Number)_

- __senderID__: This is the sender ID  of the received SMS. For example, if the sender name of the received SMS is 'QP-WAYSMS', then the senderID should be set to 'QP-WAYSMS'. In case the message is sent without an SMS service provider, senderID should be set to the phone number, including the country code of the sender. _(String)_

- __validateSender__: This is the flag used to know if the __senderID__ should be validated. Set to false if no validation is needed. _(Boolean)_



    
    

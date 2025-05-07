package org.apache.cordova.smsotpautofill;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SmsOtpAutofill extends CordovaPlugin {

    private String senderID;
    private String delimiter;
    private int otpLength;
    private boolean validateSender;
    private CallbackContext callbackContext;
    private static final String TAG = "SmsOptAutofillUserConsent";
    ActivityResultLauncher<Intent> smsConsentLauncher;

    @Override
    public void onStart() {
        super.onStart();
        createSmsConsentLauncher();
    }

    @Override
    public boolean execute(String action, JSONArray options, CallbackContext callbackContext){
        this.callbackContext = callbackContext;

        if (action.equals("startSmsUserConsent")) {
            try {
                senderID = options.getJSONObject(0).getString("senderID");
                delimiter = options.getJSONObject(0).getString("delimiter");
                otpLength = options.getJSONObject(0).getInt("otpLength");
                validateSender = options.getJSONObject(0).getBoolean("validateSender");

                startSmsUserConsent();
            } catch (JSONException e){
                e.printStackTrace();
                callbackContext.error("Please enter all of the required options");
            }
            return true;
        }
        return false;
    }

    private String parseOneTimeCode(String message){
        Pattern pattern = Pattern.compile("(?i)" + delimiter + "\\s?(\\d{" +otpLength+ "})");
        Matcher m = pattern.matcher(message);

        String otp = "";
        if(m.find()) {
            otp = m.group(1);
        }

        return otp;
    }

    private void updateCallbackStatus(String result) {

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,result);
        pluginResult.setKeepCallback(false);
        callbackContext.sendPluginResult(pluginResult);
    }

    private void startSmsUserConsent() {
        SmsRetriever.getClient(cordova.getContext()).startSmsUserConsent(this.validateSender && !this.senderID.isEmpty()? this.senderID: null)
                .addOnSuccessListener(aVoid -> {
                    IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
                    ContextCompat.registerReceiver(cordova.getActivity(), smsVerificationReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "Failed to start SMS User Consent", e);
                    callbackContext.error("Failed to start SMS User Consent");
                });
    }

    /**
     * Start activity to show consent dialog to user, activity must be started in
     * 5 minutes, otherwise you'll receive another TIMEOUT intent
     */
    private void createSmsConsentLauncher(){
        //cordova.getActivity().startActivityForResult(consentIntent, SMS_CONSENT_REQUEST);
        //startActivityForResult replaced with the below to avoid using deprecated methods
        smsConsentLauncher = cordova.getActivity().registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        // Get SMS message content
                        String message = result.getData().getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                        // Extract one-time code from the message and complete verification
                        // `message` contains the entire text of the SMS message, so we need
                        // to parse the string.
                        String oneTimeCode = parseOneTimeCode(message);

                        // send otp to cordova
                        updateCallbackStatus(oneTimeCode);
                        Log.i(TAG, "OTP Received: " + message);
                    } else {
                        callbackContext.error("User denied consent");
                    }
                });
    }

    private final BroadcastReceiver smsVerificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                switch (smsRetrieverStatus.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        // Get consent intent
                        Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                        try {
                            smsConsentLauncher.launch(consentIntent);
                        } catch (ActivityNotFoundException e) {
                            callbackContext.error(e.getMessage());
                        }
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        // Time out occurred, handle the error.
                        callbackContext.error("Timeout");
                        break;
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        try {
            cordova.getActivity().unregisterReceiver(smsVerificationReceiver);
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "Receiver not registered or already unregistered", e);
        }
    super.onDestroy();
    }
}

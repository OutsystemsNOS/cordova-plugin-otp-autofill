package org.apache.cordova.smsotpautofill;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsMessage;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsOtpAutofill extends CordovaPlugin {


    public static final String[] permissions = {Manifest.permission.RECEIVE_SMS,Manifest.permission.READ_SMS};
    private static final int REQ_CODE = 0;
    private CountDownTimer countDownTimer;
    private String OTP = "";
    private String senderID;
    private String delimiter;
    private int timeout;
    private int otpLength;
    private boolean validateSender;
    private static final String BroadcastAction = "android.provider.Telephony.SMS_RECEIVED";
    private CallbackContext otpCallbackContext;


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCountDownTimer();
        } else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
            otpCallbackContext.error("SMS permissions have been denied; please enable it in the Settings app to continue.");
        }
    }

    @Override
    public boolean execute(String action, JSONArray options, CallbackContext callbackContext){
        if(action.equals("extractOtp")){

            otpCallbackContext = callbackContext;
            try {
                senderID = options.getJSONObject(0).getString("senderID");
                delimiter = options.getJSONObject(0).getString("delimiter");
                otpLength = options.getJSONObject(0).getInt("otpLength");
                timeout = options.getJSONObject(0).getInt("timeout");
                validateSender = options.getJSONObject(0).getBoolean("validateSender");

                if(!delimiter.isEmpty()) {
                    delimiter += " ";
                }

                checkPermissions();
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
                return true;

            } catch (JSONException e){
                e.printStackTrace();
                otpCallbackContext.error("Please enter all of the required options");
            }

        }
        return false;
    }
    private void checkPermissions() {

        if(cordova.hasPermission(permissions[0]) && cordova.hasPermission(permissions[1])) {
            startCountDownTimer();
        } else {
            cordova.requestPermissions(this,REQ_CODE,permissions);
        }


    }
    private void startCountDownTimer() {

        registerReceiver();
        countDownTimer = new CountDownTimer(timeout* 1000L, 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                unregisterReceiver();
                updateCallbackStatus(otpCallbackContext,"Resend OTP");
            }
        };

        countDownTimer.start();

    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastAction);
        cordova.getActivity().getApplicationContext().registerReceiver(broadcastReceiver,intentFilter);
    }

    private void unregisterReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastAction);
        cordova.getActivity().getApplicationContext().unregisterReceiver(broadcastReceiver);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle bundle = intent.getExtras();
            try
            {
                if (bundle != null)
                {

                    final Object[] pdusObj = (Object[]) bundle.get("pdus");
                    for (int i = 0; i < Objects.requireNonNull(pdusObj).length; i++)
                    {

                        SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                        String message = currentMessage .getDisplayMessageBody();
                        String senderIDInSms = currentMessage.getDisplayOriginatingAddress();
                        Pattern pattern = Pattern.compile(delimiter + "(\\d{" +otpLength+ "})");
                        Matcher m = pattern.matcher(message);

                        if(m.find()) {
                            OTP = m.group(1);
                        }
                        updateOTP(senderIDInSms);
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    private void updateOTP(String senderIDInSms) {

        if(!validateSender || senderID.equals(senderIDInSms)) {
            countDownTimer.cancel();
            unregisterReceiver();
            updateCallbackStatus(otpCallbackContext,OTP);
        }

    }

    private void updateCallbackStatus(CallbackContext callbackContext, String result) {

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK,result);
        pluginResult.setKeepCallback(false);
        callbackContext.sendPluginResult(pluginResult);
    }
}

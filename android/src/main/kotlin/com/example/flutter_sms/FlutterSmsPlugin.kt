package com.example.flutter_sms

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


class FlutterSmsPlugin(registrar: Registrar) : MethodCallHandler {
    private val REQUEST_CODE_SEND_SMS = 205

    var activity: Activity? = null
    private var result: Result? = null

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_sms")
            channel.setMethodCallHandler(FlutterSmsPlugin(registrar))
        }
    }

    init {
        this.activity = registrar.activity()
//    registrar.addActivityResultListener(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        this.result = result
        when (call.method) {
            "sendSMS" -> {
                if (!canSendSMS()) {
                    addSmsNotSupportedError(result)
                    return
                }
                val message = call.argument<String?>("message")
                val recipients = call.argument<String?>("recipients")
                sendSMS(result, recipients, message!!)
                result.success("SMS Sent!")
            }
            "sendBackgroundSMS" -> {
                if (!canSendSMS()) {
                    addSmsNotSupportedError(result)
                    return
                }
                val message = call.argument<String?>("message")
                val recipients = call.argument<String?>("recipients")
                sendBackgroundSMS(result, recipients, message!!)
                result.success("SMS Sent!")
            }
            "canSendSMS" -> result.success(canSendSMS())
            else -> result.notImplemented()
        }
    }

    private fun addSmsNotSupportedError(result: Result) {
        result.error(
                "device_not_capable",
                "The current device is not capable of sending text messages.",
                "A device may be unable to send messages if it does not support messaging or if it is not currently configured to send messages. This only applies to the ability to send text messages via iMessage, SMS, and MMS.")
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private fun canSendSMS(): Boolean {
        if (!activity!!.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            return false
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:")
        val activityInfo = intent.resolveActivityInfo(activity!!.packageManager, intent.flags)
        return !(activityInfo == null || !activityInfo.exported)

    }

    private fun sendSMS(result: Result, phones: String?, message: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phones")
        intent.putExtra("sms_body", message)
        intent.putExtra(Intent.EXTRA_TEXT, message)
        //     intent.putExtra(Intent.EXTRA_STREAM, attachment);
        activity?.startActivityForResult(intent, REQUEST_CODE_SEND_SMS)
    }

    private fun sendBackgroundSMS(result: Result, phones: String?, message: String?) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()

            smsManager.sendTextMessage(phones, null, message, null, null)
        } catch (ex: Exception) {
            ex.printStackTrace()

            addSmsNotSupportedError(result)
        }
    }

//  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
//    if (requestCode == REQUEST_CODE_SEND_SMS && this.result != null) {
//      this.result!!.success("finished")
//      this.result = null
//      return true
//    }
//    return false
//  }
}

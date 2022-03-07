package org.unifiedpush.flutter.connector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.unifiedpush.android.connector.UnifiedPush

class Plugin : ActivityAware, FlutterPlugin, MethodCallHandler {
    private var mContext : Context? = null
    private var mActivity : Activity? = null
    var pluginChannel: MethodChannel? = null

    companion object {
        @JvmStatic
        private fun getDistributors(context: Context,
                                    result: Result?){
            val distributors = UnifiedPush.getDistributors(context)
            result?.success(distributors)
        }

        @JvmStatic
        private fun getDistributor(context: Context,
                                   result: Result?) {
            val distributor = UnifiedPush.getDistributor(context)
            result?.success(distributor)
        }

        @JvmStatic
        private fun saveDistributor(context: Context,
                                    args: ArrayList<*>?,
                                    result: Result?) {
            val distributor = args!![0] as String
            UnifiedPush.saveDistributor(context, distributor)
            result?.success(true)
        }

        @JvmStatic
        private fun registerApp(context: Context,
                                args: ArrayList<*>?,
                                result: Result?) {
            val instance: String = (args?.get(0) ?: "") as String
            Log.d("Plugin", "registerApp: instance=$instance")
            if (instance.isEmpty()) {
                UnifiedPush.registerApp(context)
            } else {
                UnifiedPush.registerApp(context, instance)
            }
            result?.success(true)
        }

        @JvmStatic
        private fun unregister(context: Context,
                               args: ArrayList<*>?,
                               result: Result) {
            val instance: String = (args?.get(0) ?: "") as String
            if (instance.isEmpty()) {
                UnifiedPush.unregisterApp(context)
            } else {
                UnifiedPush.unregisterApp(context, instance)
            }
            result.success(true)
        }

        private fun getAllNativeSharedPrefs(context: Context,
                               args: ArrayList<*>?,
                               result: Result) {
            val prefs = context.getSharedPreferences("UP-lib", Context.MODE_PRIVATE)
            val allPrefs = prefs?.all

            if (allPrefs != null) {
                // MethodCall.Result only supports List and not Set
                val sanitizedAllPrefs = mutableMapOf<String, Any?>()
                for ((k, v) in allPrefs) {
                    if (v is Collection<*>) {
                        val l = mutableListOf<Any?>()
                        l.addAll(v)
                        sanitizedAllPrefs.put(k, l)
                    } else {
                        sanitizedAllPrefs.put(k, v)
                    }
                }
                result.success(sanitizedAllPrefs)
            }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d("Plugin", "onAttachedToEngine")
        mContext = binding.applicationContext
        pluginChannel = MethodChannel(binding.binaryMessenger, PLUGIN_CHANNEL)
        pluginChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d("Plugin", "onDetachedFromEngine")
        pluginChannel?.setMethodCallHandler(null)
        mContext = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d("Plugin", "onAttachedToActivity")
        mActivity = binding.activity
    }

    override fun onDetachedFromActivity() {
        Log.d("Plugin", "onDetachedFromActivity")
        mActivity = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d("Plugin", "onDetachedFromActivityForConfigChanges")
        mActivity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d("Plugin", "onReattachedToActivityForConfigChanges")
        mActivity = binding.activity
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        Log.d("Plugin","Method: ${call.method}")
        val args = call.arguments<ArrayList<*>>()
        // TODO mContext vs mActivity as context ?
        when(call.method) {
            PLUGIN_EVENT_GET_DISTRIBUTORS -> getDistributors(mActivity!!, result)
            PLUGIN_EVENT_GET_DISTRIBUTOR -> getDistributor(mActivity!!, result)
            PLUGIN_EVENT_SAVE_DISTRIBUTOR -> saveDistributor(mActivity!!, args, result)
            PLUGIN_EVENT_REGISTER_APP -> registerApp(mActivity!!, args, result)
            PLUGIN_EVENT_UNREGISTER -> unregister(mActivity!!, args, result)
            PLUGIN_EVENT_GET_ALL_NATIVE_SHARED_PREFS -> getAllNativeSharedPrefs(mActivity!!, args, result)
            else -> result.notImplemented()
        }
    }
}

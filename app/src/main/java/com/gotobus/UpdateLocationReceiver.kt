package com.gotobus

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log

import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import org.json.JSONObject

class UpdateLocationReceiver : BroadcastReceiver() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {


        val baseUrl = intent.getStringExtra("baseUrl")
        val accessToken = intent.getStringExtra("Authorization")

        AndroidNetworking.initialize(context)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                AndroidNetworking.post(baseUrl + "/updateLatLong.php")
                    .setOkHttpClient(NetworkCookies.okHttpClient)
                    .addHeaders("Authorization", accessToken)
                    .addBodyParameter("last_location", location?.latitude.toString() + "," + location?.longitude.toString())
                    .addBodyParameter("bearing", location?.bearing.toString())
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(object : JSONObjectRequestListener {

                        override fun onResponse(response: JSONObject) {
                           // Toast.makeText(context, response.toString(), Toast.LENGTH_LONG).show()
                        }

                        override fun onError(anError: ANError) {

                        }
                    })
                // Got last known location. In some rare situations this can be null.
            }
    }
}

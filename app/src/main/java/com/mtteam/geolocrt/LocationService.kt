package com.mtteam.geolocrt

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.paho.client.mqttv3.MqttException
import java.time.LocalDateTime
import java.util.Date

class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private lateinit var mqttMgr: MqttManager

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        mqttMgr = MqttManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun start() {
        //val mapper = jacksonObjectMapper()

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        locationClient
            .getLocationUpdates(30000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = location.latitude
                val long = location.longitude
                //val locData = LocationData(location.latitude,location.longitude)
                // mqttMgr.publish(TOPIC,mapper.writeValueAsString(locData))
                try {
                    mqttMgr.publish(TOPIC,"Location: ($lat, $long)")
                }catch (e: MqttException ) {
                    e.printStackTrace()
                    mqttMgr.tryReconnect()
                }
                val updatedNotification = notification.setContentText(
                    "Location: ($lat, $long)"
                )
                notificationManager.notify(1, updatedNotification.build())
            }
            .launchIn(serviceScope)
        mqttMgr.connect(BROKER_URL,CLIENT_ID, USR_ID, PWD)


        startForeground(1, notification.build())
    }

    private fun stop() {
        mqttMgr.disconnect()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val BROKER_URL = "ssl://lccac99e.ala.us-east-1.emqxsl.com:8883"
        const val CLIENT_ID = "android9f6h4kS0f1"
        const val USR_ID = "usr1975"
        const val PWD = "Vgy76yjm"
        const val TOPIC = "geoloc/1"
    }
}
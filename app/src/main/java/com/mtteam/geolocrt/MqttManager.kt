package com.mtteam.geolocrt

import android.content.Context
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

class MqttManager(private val context: Context) {
    var client: MqttClient? = null

    fun connect(brokerUrl: String, clientId: String, usr: String, pwd: String) {
        try {
            // Set up the persistence layer
            val persistence = MemoryPersistence()

            // Initialize the MQTT client
            client = MqttClient(brokerUrl, clientId, persistence)

            // Set up the connection options
            val connectOptions = MqttConnectOptions()
            connectOptions.isCleanSession = true
            connectOptions.password = pwd.toCharArray()
            connectOptions.userName = usr
            connectOptions.isAutomaticReconnect = true
            try {
                val caCrtFile = context.resources.openRawResource(R.raw.ca)
                connectOptions.socketFactory = getSingleSocketFactory(caCrtFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }


            // Connect to the broker
            client!!.connect(connectOptions)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    fun getSingleSocketFactory(caCrtFileInputStream: InputStream?): SSLSocketFactory {
        Security.addProvider(BouncyCastleProvider())
        var caCert: X509Certificate? = null
        val bis = BufferedInputStream(caCrtFileInputStream)
        val cf = CertificateFactory.getInstance("X.509")
        while (bis.available() > 0) {
            caCert = cf.generateCertificate(bis) as X509Certificate
        }
        val caKs = KeyStore.getInstance(KeyStore.getDefaultType())
        caKs.load(null, null)
        caKs.setCertificateEntry("cert-certificate", caCert)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(caKs)
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, tmf.trustManagers, null)
        return sslContext.socketFactory
    }

    fun disconnect() {
        try {
            client!!.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String) {
            val mqttMessage = MqttMessage(message.toByteArray())
            client!!.publish(topic, mqttMessage)
    }

    fun subscribe(topic: String?) {
        try {
            client!!.subscribe(topic)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun  tryReconnect()
    {
        if (!client!!.isConnected)
            client!!.reconnect()
    }
}
package com.example.alertnotificationapps

import android.app.*
import android.companion.DeviceId
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import okhttp3.*
import org.json.JSONObject
import android.os.Build
import android.util.Log
import android.os.Handler
import android.os.Looper



class AlertService : Service() {

    private lateinit var ws: WebSocket
    private lateinit var player: MediaPlayer

    override fun onCreate() {
        super.onCreate()
        Log.d("SERVICE", "AlertService CREATED")

        createChannel()
        startForeground(1, foregroundNotif())

        player = MediaPlayer.create(this, R.raw.alarm)

        connectWebSocket()
    }

    private fun sendUiUpdate(deviceId: String, status: String, temperature: Double) {
        val intent = Intent("ALERT_UPDATE").apply {
            setPackage(packageName)
            putExtra("device_id", deviceId)
            putExtra("status", status)
            putExtra("temperature", temperature)
        }
        sendBroadcast(intent)
    }
    private fun reconnect() {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("WS", "Reconnecting WebSocket...")
            connectWebSocket()
        }, 5000) // retry tiap 5 detik
    }



    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://192.168.0.163:8765")
            .build()


        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS", "CONNECTED")
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                Log.e("WS", "DISCONNECTED", t)
                reconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS", "CLOSED")
                reconnect()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)

                    val deviceId = json.getString("device_id")
                    val status = json.getString("status")
                    val temperature = json.getDouble("temperature")

                    // ✅ UI SELALU UPDATE
                    sendUiUpdate(deviceId, status, temperature)

                    // 🔔 ALARM HANYA JIKA BAHAYA
                    if (status == "BAHAYA") {
                        sendAlert(status, temperature)
                    }else if (status == "PERINGATAN"){
                        sendNotificationOnly(deviceId, status, temperature)
                    }

                } catch (e: Exception) {
                    Log.e("WS", "ERROR parsing", e)
                }
            }




        })

    }

    private fun sendAlert(status: String, temperature: Double) {

        val content = "Status: $status\nSuhu: %.1f °C".format(temperature)

        val notif = NotificationCompat.Builder(this, "alert")
            .setContentTitle("🚨 KEBAKARAN COYY")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(2, notif)

        if (!player.isPlaying) {
            player.start()
        }
    }

    private fun sendNotificationOnly(
        deviceId: String,
        status: String,
        temperature: Double
    ) {
        val content = "Device: $deviceId\nStatus: $status\nSuhu: %.1f °C".format(temperature)

        val notif = NotificationCompat.Builder(this, "alert")
            .setContentTitle("⚠️ PERINGATAN")
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(true) // 🔕 tidak ada suara
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(deviceId.hashCode(), notif)
    }




    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "alert",
                "IoT Alert",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.setSound(null, null)

            channel.enableVibration(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }


    private fun foregroundNotif(): Notification =
        NotificationCompat.Builder(this, "alert")
            .setContentTitle("IoT Alert System")
            .setContentText("Monitoring aktif")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null
}
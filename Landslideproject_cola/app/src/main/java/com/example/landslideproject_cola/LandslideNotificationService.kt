package com.example.landslideproject_cola

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Background service ที่ตรวจสอบ notification ใหม่จาก backend
 * และแสดง push notification บน lock screen / status bar
 *
 * วิธีทำงาน:
 *  1. Poll GET /api/notifications/{user_id} ทุก POLL_INTERVAL_MS
 *  2. เปรียบเทียบกับ notification_id ที่เคยเห็นแล้ว (เก็บใน SharedPrefs)
 *  3. ถ้าเจอ notification ใหม่ → โชว์ push notification ทันที
 *  4. บันทึก ID นั้นไว้ ไม่โชว์ซ้ำ
 *
 * Backend logic (server/main.py verify_alert):
 *   Admin approve → INSERT INTO notifications สำหรับ user ที่อยู่ใกล้ → service จับได้แล้วโชว์
 */
class LandslideNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "landslide_alert_channel"
        const val CHANNEL_NAME = "แจ้งเตือนดินถล่ม"
        const val FOREGROUND_NOTIF_ID = 1001
        const val PUSH_NOTIF_BASE_ID = 2000
        const val PREFS_SEEN_IDS = "seen_notification_ids"
        const val TAG = "LandslideService"

        // ===== ปรับได้ =====
        const val POLL_INTERVAL_MS = 30 * 1000L   // ตรวจสอบทุก 30 วินาที

        fun startService(context: Context) {
            val intent = Intent(context, LandslideNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, LandslideNotificationService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sharedPref: SharedPreferencesManager

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        sharedPref = SharedPreferencesManager(this)
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_NOTIF_ID, buildForegroundNotification())

        val userId = sharedPref.getSavedUserId()
        if (userId.isBlank()) {
            Log.d(TAG, "No user ID, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            while (isActive) {
                try {
                    checkNewNotifications(userId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking notifications: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    // ===================== MAIN LOGIC =====================

    /**
     * ดึง notifications จาก backend แล้วเช็คว่ามีอันใหม่ไหม
     * ถ้าใหม่ (ยังไม่เคยแจ้งเตือน) → โชว์ push notification
     */
    private suspend fun checkNewNotifications(userId: String) {
        val notifications = fetchNotificationsFromServer(userId) ?: return
        Log.d(TAG, "Fetched ${notifications.size} notifications for user $userId")

        val seenIds = getSeenNotificationIds()

        for (notif in notifications) {
            val notifId = notif.notification_id
            if (notifId.isBlank() || notifId in seenIds) continue

            // notification ใหม่ที่ยังไม่เคยโชว์ push → โชว์ทันที
            Log.d(TAG, "New notification found: $notifId — ${notif.title}")
            showPushNotification(notif)
            markNotificationSeen(notifId)
        }
    }

    // ===================== API CALL =====================

    private suspend fun fetchNotificationsFromServer(userId: String): List<NotifPayload>? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val url = "${EarthquakeClient.BASE_URL}api/notifications/$userId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "LandslideNanApp/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful || body.isBlank()) {
                    Log.e(TAG, "API error ${response.code}: $body")
                    return@withContext null
                }

                val jsonArray = JSONArray(body)
                val list = mutableListOf<NotifPayload>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        NotifPayload(
                            notification_id = obj.optString("notification_id", ""),
                            title   = obj.optString("title", "แจ้งเตือนดินถล่ม"),
                            message = obj.optString("message", ""),
                            sent_at = obj.optString("sent_at", ""),
                            is_read = obj.optInt("is_read", 0)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                Log.e(TAG, "fetchNotifications error: ${e.message}")
                null
            }
        }

    // ===================== PUSH NOTIFICATION =====================

    private fun showPushNotification(notif: NotifPayload) {
        // เมื่อกด notification → เปิด app ไปหน้า Notifications
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notif.notification_id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notif.title)
            .setContentText(notif.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setColor(0xFFD32F2F.toInt())   // สีแดง
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // โชว์บน lock screen
            .build()

        // ใช้ ID ที่ unique ต่อแต่ละ notification
        val notifAndroidId = PUSH_NOTIF_BASE_ID + (notif.notification_id.hashCode() % 1000)
            .let { if (it < 0) it + 1000 else it }

        try {
            NotificationManagerCompat.from(this).notify(notifAndroidId, notification)
            Log.d(TAG, "Push notification shown for ${notif.notification_id}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission: ${e.message}")
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ระบบแจ้งเตือนดินถล่ม")
            .setContentText("กำลังรับการแจ้งเตือนจากระบบ...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "แจ้งเตือนเมื่อมีความเสี่ยงดินถล่มในพื้นที่ใกล้คุณ"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // ===================== PREFS HELPERS =====================

    private fun getSeenNotificationIds(): Set<String> {
        val prefs = getSharedPreferences(PREFS_SEEN_IDS, Context.MODE_PRIVATE)
        return prefs.getStringSet("ids", emptySet()) ?: emptySet()
    }

    private fun markNotificationSeen(notifId: String) {
        val prefs = getSharedPreferences(PREFS_SEEN_IDS, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("ids", mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet().also { it.add(notifId) }
        prefs.edit().putStringSet("ids", updated).apply()
    }
}

// ===== Data class สำหรับ notification จาก API =====
private data class NotifPayload(
    val notification_id: String,
    val title: String,
    val message: String,
    val sent_at: String,
    val is_read: Int
)

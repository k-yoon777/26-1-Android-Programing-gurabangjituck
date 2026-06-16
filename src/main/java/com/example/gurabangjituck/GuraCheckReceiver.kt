package com.example.gurabangjituck

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class GuraCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getStringExtra("ALARM_TYPE") ?: "FINAL_CHECK"

        if (alarmType == "PRE_ALERT") {
            val interval = intent.getIntExtra("PUSH_INTERVAL", 10)
            sendPushNotification(
                context,
                "🚨 구라방지턱 마감 임박!",
                "약속지 탈출 마감 시간까지 $interval 분 남았습니다! 지금 바로 준비해서 이동하세요."
            )
            return
        }

        Log.d("GuraCheck", "🚨 [예약 기능] 백그라운드 구라 감지 동작함!")
        Toast.makeText(context, "🤖 구라방지턱 위치 검증 시작!", Toast.LENGTH_SHORT).show()

        val prefs = context.getSharedPreferences("GuraPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_scheduled", false)) return

        val savedLat = prefs.getFloat("home_lat", 37.514457f).toDouble()
        val savedLng = prefs.getFloat("home_lng", 127.021008f).toDouble()
        val guardPhone = prefs.getString("guard_phone", "") ?: ""

        // 🌟 [추가 기능 1] 저장된 커스텀 굴욕 메시지 로드
        val customPenaltyMsg = prefs.getString("custom_penalty_message", "외출 마감 시간까지 자택 탈출 실패! 벌칙을 요구하세요.") ?: ""

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            sendPushNotification(context, "🚨 권한 오류", "위치 권한이 차단되어 외출 검증에 실패했습니다.")
            handleFailure(context, prefs, guardPhone, customPenaltyMsg)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLat = location.latitude
                val currentLng = location.longitude

                val results = FloatArray(1)
                Location.distanceBetween(savedLat, savedLng, currentLat, currentLng, results)
                val distanceInMeters = results[0]

                Log.d("GuraCheck", "현재 계산된 거리: ${distanceInMeters}m")

                if (distanceInMeters > 50.0) {
                    // 🌟 [추가 기능 4] 성공 통계 데이터 누적 기록
                    val successCnt = prefs.getInt("stats_success", 0)
                    val totalCnt = prefs.getInt("stats_total_promises", 0)
                    prefs.edit()
                        .putInt("stats_success", successCnt + 1)
                        .putInt("stats_total_promises", totalCnt + 1)
                        .apply()

                    sendPushNotification(
                        context,
                        "🏃‍♂️ 외출 검증 성공 (SAFE)",
                        "집 반경을 무사히 탈출했습니다! (현재 거리: ${distanceInMeters.toInt()}m)"
                    )
                    clearScheduleAndRefresh(context, prefs)
                } else {
                    sendPushNotification(
                        context,
                        "🚨 외출 검증 실패 (구라 적발)",
                        "마감 시간까지 집을 떠나지 않아 감시자에게 굴욕 문자를 전송했습니다."
                    )
                    handleFailure(context, prefs, guardPhone, customPenaltyMsg)
                }
            } else {
                sendPushNotification(context, "🚨 위치 확인 불가", "최근 GPS 위치를 잡지 못해 탈출 실패로 간주합니다.")
                handleFailure(context, prefs, guardPhone, customPenaltyMsg)
            }
        }.addOnFailureListener {
            sendPushNotification(context, "🚨 GPS 에러", "위치 탐색 엔진 구동 실패로 탈출 실패 처리됩니다.")
            handleFailure(context, prefs, guardPhone, customPenaltyMsg)
        }
    }

    private fun handleFailure(context: Context, prefs: SharedPreferences, guardPhone: String, customMessage: String) {
        // 🌟 [추가 기능 4] 실패 통계 데이터 누적 기록
        val failureCnt = prefs.getInt("stats_failure", 0)
        val totalCnt = prefs.getInt("stats_total_promises", 0)
        prefs.edit()
            .putInt("stats_failure", failureCnt + 1)
            .putInt("stats_total_promises", totalCnt + 1)
            .apply()

        sendPenaltySms(context, guardPhone, customMessage)
        clearScheduleAndRefresh(context, prefs)
    }

    private fun clearScheduleAndRefresh(context: Context, prefs: SharedPreferences) {
        prefs.edit().apply {
            putBoolean("is_scheduled", false)
            putString("guard_phone", "")
            putString("selected_time_text", "지정되지 않음")
            putLong("target_time_millis", 0L)
            apply()
        }
        context.sendBroadcast(Intent("com.example.gurabangjituck.UPDATE_UI").apply {
            putExtra("isEscaped", true)
            setPackage(context.packageName)
        })
    }

    private fun sendPushNotification(context: Context, title: String, content: String) {
        val channelId = "gura_notification_channel"
        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "구라방지턱 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "구라방지턱의 외출 감지 및 마감 알림을 제공합니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e("GuraCheck", "🔔 푸시 알림 발송 실패: ${e.message}")
        }
    }

    private fun sendPenaltySms(context: Context, phone: String, customMessage: String) {
        if (phone.isEmpty() || phone == "미등록") return

        val cleanPhone = phone.replace("-", "")

        // 🌟 [추가 기능 1] 40자 입력문구와 고정 프리픽스([구라방지턱])를 조합. 무조건 70자 이내!
        val finalMessage = "[구라방지턱] $customMessage"

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(cleanPhone, null, finalMessage, null, null)
                Log.d("GuraCheck", "📩 감시자 번호 $cleanPhone 로 커스텀 굴욕 문자 발송 완료")
            } catch (e: Exception) {
                Log.e("GuraCheck", "문자 발송 오류: ${e.message}")
            }
        } else {
            Log.e("GuraCheck", "문자 발송 권한이 없습니다.")
        }
    }
}
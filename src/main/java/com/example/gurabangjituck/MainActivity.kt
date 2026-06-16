package com.example.gurabangjituck

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.compose.*
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

const val GEMINI_API_KEY = "AQ.Ab8RN6KwMJAfKFftCtfIhlFORIzMm6Cw_VLfPRtyhrmYOgLy6w"

val TossBlue = Color(0xFF3182F6)
val TossDismissRed = Color(0xFFF04452)
val TossBg = Color(0xFFF2F4F6)
val TossPanelWhite = Color(0xFFFFFFFF)
val TossBadgeBg = Color(0XFFE8F3FF)
val TossTextPrimary = Color(0xFF191F28)
val TossTextSecondary = Color(0xFF4E5968)
val TossTextMuted = Color(0xFF8B95A1)
val TossBorder = Color(0xFFE5E8EB)

class KoreanPhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val formatted = StringBuilder()
        for (i in raw.indices) {
            formatted.append(raw[i])
            if (raw.length <= 10) {
                if (i == 2 || i == 5) if (i != raw.lastIndex) formatted.append("-")
            } else {
                if (i == 2 || i == 6) if (i != raw.lastIndex) formatted.append("-")
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (raw.length <= 10) {
                    if (offset <= 6) return offset + 1; return offset + 2
                } else {
                    if (offset <= 7) return offset + 1; return offset + 2
                }
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (raw.length <= 10) {
                    if (offset <= 7) return offset - 1; return offset - 2
                } else {
                    if (offset <= 8) return offset - 1; return offset - 2
                }
            }
        }
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var locationSource: FusedLocationSource
    private lateinit var sharedPreferences: SharedPreferences
    private var uiUpdateReceiver: BroadcastReceiver? = null
    private val _isEscapedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NaverMapSdk.getInstance(this).client = NaverMapSdk.NcpKeyClient("kd8pfnzizg")
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        sharedPreferences = getSharedPreferences("GuraPrefs", Context.MODE_PRIVATE)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = android.app.NotificationChannel("gura_notification_channel", "구라방지턱 알림", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        uiUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.gurabangjituck.UPDATE_UI") {
                    _isEscapedState.value = intent.getBooleanExtra("isEscaped", false)
                    finish()
                    startActivity(getIntent())
                }
            }
        }
        registerReceiver(uiUpdateReceiver, IntentFilter("com.example.gurabangjituck.UPDATE_UI"), Context.RECEIVER_NOT_EXPORTED)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = TossBlue, background = TossBg, surface = TossPanelWhite)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = launchActivityContext()
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") { DashboardScreen(navController, sharedPreferences, _isEscapedState, context) }
                        composable("settings") { SettingsScreen(navController, locationSource, sharedPreferences, context) }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiUpdateReceiver?.let { unregisterReceiver(it) }
    }

    @Composable
    private fun launchActivityContext(): Context {
        val context = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }
        LaunchedEffect(Unit) {
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) permissionsToRequest.add(Manifest.permission.SEND_SMS)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (permissionsToRequest.isNotEmpty()) permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
        return context
    }
    companion object { private const val LOCATION_PERMISSION_REQUEST_CODE = 1000 }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(items: List<String>, initialIndex: Int, onIndexSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val itemHeight = 46.dp
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LaunchedEffect(lazyListState.isScrollInProgress) { if (!lazyListState.isScrollInProgress) onIndexSelected(lazyListState.firstVisibleItemIndex) }
    Box(modifier = modifier.height(itemHeight * 3), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(itemHeight).background(TossBg, RoundedCornerShape(10.dp)))
        LazyColumn(state = lazyListState, flingBehavior = rememberSnapFlingBehavior(lazyListState), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = itemHeight)) {
            items(items.size) { index ->
                val isSelected = lazyListState.firstVisibleItemIndex == index
                Box(modifier = Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(items[index], fontSize = if (isSelected) 18.sp else 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) TossBlue else TossTextMuted)
                }
            }
        }
    }
}

@Composable
fun SamsungStyleTimePicker(initialHour24: Int, initialMinute: Int, onTimeChanged: (Int, Int) -> Unit) {
    val amPmItems = listOf("오전", "오후")
    val hourItems = (1..12).map { it.toString() }
    val minuteItems = (0..59).map { String.format(Locale.getDefault(), "%02d", it) }
    var selectedAmPm by remember { mutableStateOf(if (initialHour24 < 12) 0 else 1) }
    var selectedHour12 by remember { mutableStateOf(if (initialHour24 % 12 == 0) 12 else initialHour24 % 12) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    LaunchedEffect(selectedAmPm, selectedHour12, selectedMinute) {
        val hour24 = if (selectedAmPm == 0) { if (selectedHour12 == 12) 0 else selectedHour12 } else { if (selectedHour12 == 12) 12 else selectedHour12 + 12 }
        onTimeChanged(hour24, selectedMinute)
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        WheelPicker(items = amPmItems, initialIndex = selectedAmPm, onIndexSelected = { selectedAmPm = it }, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        WheelPicker(items = hourItems, initialIndex = selectedHour12 - 1, onIndexSelected = { selectedHour12 = it + 1 }, modifier = Modifier.weight(1f))
        Text(":", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TossTextSecondary, modifier = Modifier.padding(horizontal = 6.dp))
        WheelPicker(items = minuteItems, initialIndex = selectedMinute, onIndexSelected = { selectedMinute = it }, modifier = Modifier.weight(1f))
    }
}

// ==========================================
// 📺 메인 대시보드 화면
// ==========================================
@Composable
fun DashboardScreen(navController: NavController, prefs: SharedPreferences, isEscapedState: State<Boolean>, context: Context) {
    // 🔥 [추가] AI가 생성한 팩폭 독려 문구를 저장할 상태 변수
    var aiMessage by remember { mutableStateOf("AI가 열심히 뼈 때릴 문구를 생각 중입니다...") }

    var updateTrigger by remember { mutableStateOf(0) }
    val isScheduled = remember(updateTrigger) { prefs.getBoolean("is_scheduled", false) }
    val guardPhone = remember(updateTrigger) { prefs.getString("guard_phone", "") ?: "" }
    val selectedTimeText = remember(updateTrigger) { prefs.getString("selected_time_text", "") ?: "" }
    val customPenaltyMsg = remember(updateTrigger) { prefs.getString("custom_penalty_message", "") ?: "" }
    val targetTimeMillis = remember(updateTrigger) { prefs.getLong("target_time_millis", 0L) }

    val displayPhone = if (isScheduled && guardPhone.isNotEmpty()) guardPhone else "미등록"
    val displayTime = if (isScheduled && selectedTimeText.isNotEmpty() && selectedTimeText != "지정되지 않음") selectedTimeText else "등록된 일정 없음"
    val displayPenalty = if (isScheduled && customPenaltyMsg.isNotEmpty()) customPenaltyMsg else "지정되지 않음"

    val statsTotal = remember(updateTrigger) { prefs.getInt("stats_total_promises", 0) }
    val statsSuccess = remember(updateTrigger) { prefs.getInt("stats_success", 0) }
    val statsFailure = remember(updateTrigger) { prefs.getInt("stats_failure", 0) }
    val reliability = if (statsTotal > 0) (statsSuccess * 100) / statsTotal else 0

    var remainingSeconds by remember { mutableLongStateOf(0L) }
    var initialTotalWindow by remember { mutableLongStateOf(1L) }

    LaunchedEffect(isScheduled, targetTimeMillis, updateTrigger) {
        if (isScheduled && targetTimeMillis > System.currentTimeMillis()) {
            val window = targetTimeMillis - System.currentTimeMillis()
            initialTotalWindow = if (window > 0) window else 1L
            while (true) {
                val diff = targetTimeMillis - System.currentTimeMillis()
                if (diff <= 0) { remainingSeconds = 0L; break }
                remainingSeconds = diff / 1000
                delay(1000)
            }
        } else remainingSeconds = 0L
    }

    // 🔥 [추가] 일정이 등록되었을 때 Gemini API를 호출하는 독립된 로직
    // 🔥 [수정] 복잡한 문자열 비교를 완전히 빼버린 최종 정상 작동 버전
    LaunchedEffect(isScheduled, targetTimeMillis) {
        if (isScheduled && targetTimeMillis > System.currentTimeMillis()) {

            // 💡 진짜 API 키를 잘 넣으셨으니, 단순히 비어있지 않은지만 체크합니다!
            if (GEMINI_API_KEY.isNotBlank()) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-3.1-flash-lite", // 빠르고 가벼운 플래시 모델 사용
                        apiKey = GEMINI_API_KEY
                    )
                    val prompt = "나는 지금 외출 약속을 지키기 위해 집에서 나갈 준비를 하고 있어. 지각하지 않고 제 시간에 집에서 탈출할 수 있도록, 의지를 북돋아주면서도 뼈를 때리는 유쾌하고 짧은(1~2문장) 동기부여 메시지를 친구처럼 반말로 작성해줘."
                    val response = generativeModel.generateContent(prompt)
                    aiMessage = response.text?.trim() ?: "늦지 않게 어서 나가자! 시간 간다!"
                } catch (e: Exception) {
                    // 🔥 [이 한 줄을 추가!] 안드로이드 스튜디오 하단에 진짜 에러 원인을 빨간 글씨로 출력합니다.
                    android.util.Log.e("GeminiDebug", "서버 통신 실패 원인: ${e.localizedMessage}", e)

                    aiMessage = "시간 맞춰 서둘러 준비해! 감시 레이더가 작동 중이야."
                }
            } else {
                aiMessage = "상단의 API 키 설정을 완료하면 실시간 AI 팩폭 문구가 표시됩니다."
            }
        } else {
            aiMessage = ""
        }
    }

    val liveCountdownText = String.format(Locale.getDefault(), "%02d:%02d:%02d", remainingSeconds / 3600, (remainingSeconds % 3600) / 60, remainingSeconds % 60)
    val progressFraction = if (isScheduled && remainingSeconds > 0) remainingSeconds.toFloat() / (initialTotalWindow / 1000f).coerceAtLeast(1f) else 0f
    val alpha by rememberInfiniteTransition(label = "").animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "")

    LaunchedEffect(navController.currentBackStackEntry) { updateTrigger++ }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                // 🌟 메인 화면 좌측 상단 방패 심볼 추가 완료
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TossBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = "앱 로고", tint = Color.White, modifier = Modifier.size(26.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text("구라방지턱", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TossTextPrimary, letterSpacing = (-0.5).sp)
                        Text(if (isScheduled) "실시간 위치 감시 중" else "등록된 감시 일정이 없습니다", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isScheduled) TossBlue else TossTextMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                if (isScheduled) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(100.dp)).background(TossBadgeBg).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Box(modifier = Modifier.size(8.dp).alpha(alpha).background(TossBlue, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("구라감지 활성화", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TossBlue)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = TossPanelWhite)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Analytics, contentDescription = null, tint = TossTextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("나의 신뢰도 통계", color = TossTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        if(statsTotal == 0) Text("아직 기록이 없어요!", fontSize = 12.sp, color = TossTextMuted)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("총 약속", fontSize = 12.sp, color = TossTextSecondary)
                            Text("${statsTotal}회", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TossTextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("탈출 성공", fontSize = 12.sp, color = TossTextSecondary)
                            Text("${statsSuccess}회", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TossBlue)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("구라 적발", fontSize = 12.sp, color = TossTextSecondary)
                            Text("${statsFailure}회", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TossDismissRed)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("신뢰도", fontSize = 12.sp, color = TossTextSecondary)
                            Text("${reliability}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if(reliability >= 80 || statsTotal == 0) TossBlue else TossDismissRed)
                        }
                    }
                }
            }

            if (isScheduled) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = TossPanelWhite)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("집 탈출 마감까지 남은 시간", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TossTextSecondary, letterSpacing = 0.5.sp)
                        Text(liveCountdownText, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = if (remainingSeconds > 0) TossTextPrimary else TossDismissRed, letterSpacing = (-1).sp, modifier = Modifier.padding(vertical = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progressFraction }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(100.dp)), color = TossBlue, trackColor = TossBorder)
                    }
                }
            }

            // 🔥🔥🔥 [4번 AI 관련 추가 내용] 🔥🔥🔥
            if (aiMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp), // 간격 조절을 위해 top padding 추가
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = TossBadgeBg) // 정의해두신 TossBadgeBg 사용
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = "AI 독려",
                            tint = TossBlue, // 정의해두신 TossBlue 사용
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = aiMessage,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TossBlue,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            // 🔥🔥🔥 [여기까지] 🔥🔥🔥

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = TossPanelWhite)) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("나의 약속 감시 설정 현황", color = TossTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (isScheduled) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TossDismissRed.copy(alpha = 0.1f))
                                    .clickable {
                                        cancelAlarm(context)
                                        prefs.edit().apply { putBoolean("is_scheduled", false); putLong("target_time_millis", 0L); apply() }
                                        Toast.makeText(context, "등록된 일정이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                                        updateTrigger++
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = TossDismissRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("일정 취소", fontSize = 12.sp, color = TossDismissRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = TossBg, thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("지정 감시자", color = TossTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(displayPhone, color = TossTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = TossBg, thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("외출 마감 시간", color = TossTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(displayTime, color = TossTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = TossBg, thickness = 1.dp)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                        Text("설정된 굴욕 문구", color = TossTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                        Text(displayPenalty, color = if(isScheduled) TossDismissRed else TossTextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val currentMillis = System.currentTimeMillis()
                    prefs.edit().apply { putBoolean("is_scheduled", true); putString("guard_phone", "010-1234-5678"); putString("selected_time_text", "테스트 스케줄 (5초후 알림, 15초후 검증)"); putString("custom_penalty_message", "저는 아직도 집을 나서지 않았습니다. 사과의 의미로 커피는 제가 살게요!"); putLong("target_time_millis", currentMillis + 15000); putInt("push_interval", 1); apply() }
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val finalPendingIntent = PendingIntent.getBroadcast(context, 999, Intent(context, GuraCheckReceiver::class.java).apply { putExtra("ALARM_TYPE", "FINAL_CHECK") }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val prePendingIntent = PendingIntent.getBroadcast(context, 998, Intent(context, GuraCheckReceiver::class.java).apply { putExtra("ALARM_TYPE", "PRE_ALERT"); putExtra("PUSH_INTERVAL", 1) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    try { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentMillis + 15000, finalPendingIntent); alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currentMillis + 5000, prePendingIntent) }
                    catch (e: SecurityException) { alarmManager.set(AlarmManager.RTC_WAKEUP, currentMillis + 15000, finalPendingIntent); alarmManager.set(AlarmManager.RTC_WAKEUP, currentMillis + 5000, prePendingIntent) }
                    Toast.makeText(context, "통합 알람 테스트 시작!", Toast.LENGTH_SHORT).show()
                    updateTrigger++
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = TossTextSecondary.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Rounded.BugReport, contentDescription = null, tint = TossTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("개발자 통합 듀얼 알람 테스트 실행", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TossTextSecondary)
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(TossBg).padding(horizontal = 24.dp, vertical = 20.dp)) {
            Button(
                onClick = { navController.navigate("settings") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TossBlue)
            ) {
                Icon(if (isScheduled) Icons.Rounded.EditCalendar else Icons.Rounded.AddAlert, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScheduled) "약속 일정 변경하기" else "새 약속 일정 등록하기", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

// ==========================================
// 📺 설정 및 등록 화면
// ==========================================
@OptIn(ExperimentalNaverMapApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, locationSource: FusedLocationSource, prefs: SharedPreferences, context: Context) {
    var savedLat by remember { mutableStateOf(prefs.getFloat("home_lat", 37.514457f)) }
    var savedLng by remember { mutableStateOf(prefs.getFloat("home_lng", 127.021008f)) }
    val homeLatLng = remember(savedLat, savedLng) { LatLng(savedLat.toDouble(), savedLng.toDouble()) }
    val previewCameraPositionState = rememberCameraPositionState { position = com.naver.maps.map.CameraPosition(homeLatLng, 16.0) }
    val dialogCameraPositionState = rememberCameraPositionState { position = com.naver.maps.map.CameraPosition(homeLatLng, 16.0) }
    var showMapDialog by remember { mutableStateOf(false) }

    var rawPhoneNumber by remember { val saved = prefs.getString("guard_phone", "") ?: ""; mutableStateOf(if(saved == "미등록") "" else saved.replace("-", "")) }
    var targetTimeMillis by remember { val saved = prefs.getLong("target_time_millis", 0L); mutableLongStateOf(if (saved <= System.currentTimeMillis()) System.currentTimeMillis() + 3600000 else saved) }
    val selectedTimeText = remember(targetTimeMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = targetTimeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val ampm = if (hour < 12) "오전" else "오후"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        "${cal.get(Calendar.MONTH) + 1}월 ${cal.get(Calendar.DAY_OF_MONTH)}일 $ampm ${String.format(Locale.getDefault(), "%02d:%02d", displayHour, cal.get(Calendar.MINUTE))}"
    }

    var pushInterval by remember { mutableStateOf(prefs.getInt("push_interval", 5)) }
    var customPenaltyMessage by remember { mutableStateOf(prefs.getString("custom_penalty_message", "외출 마감 시간까지 자택 탈출 실패! 밥값을 청구하세요.") ?: "") }

    var showDateTimePickerDialog by remember { mutableStateOf(false) }
    var dialogActiveTab by remember { mutableStateOf("date") }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetTimeMillis)
    var chosenHour24 by remember { mutableStateOf(Calendar.getInstance().apply { timeInMillis = targetTimeMillis }.get(Calendar.HOUR_OF_DAY)) }
    var chosenMinute by remember { mutableStateOf(Calendar.getInstance().apply { timeInMillis = targetTimeMillis }.get(Calendar.MINUTE)) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 110.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(40.dp).background(TossPanelWhite, RoundedCornerShape(12.dp))) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = TossTextSecondary)
                }
                Text(text = "약속 일정 등록", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TossTextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(40.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                Text("실패 시 어떤 분께 메시지를 보낼까요?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TossTextSecondary, modifier = Modifier.padding(start = 4.dp))
                OutlinedTextField(
                    value = rawPhoneNumber, onValueChange = { input -> val clean = input.filter { it.isDigit() }; if (clean.length <= 11) rawPhoneNumber = clean },
                    placeholder = { Text("010-0000-0000", color = TossTextMuted.copy(alpha = 0.45f)) }, leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = TossTextMuted) },
                    trailingIcon = { if (rawPhoneNumber.isNotEmpty()) IconButton(onClick = { rawPhoneNumber = "" }) { Icon(Icons.Rounded.Clear, contentDescription = null, tint = TossTextMuted) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), visualTransformation = KoreanPhoneNumberVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TossBlue, unfocusedBorderColor = Color.Transparent, focusedContainerColor = TossPanelWhite, unfocusedContainerColor = TossPanelWhite)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                Text("언제까지 집 밖을 나서야 하나요?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TossTextSecondary, modifier = Modifier.padding(start = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TossPanelWhite).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(36.dp).background(TossBadgeBg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = TossBlue, modifier = Modifier.size(18.dp))
                        }
                        Text(selectedTimeText, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TossTextPrimary)
                    }
                    Text("일정 설정", fontSize = 12.sp, color = TossBlue, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(TossBadgeBg).clickable { dialogActiveTab = "date"; showDateTimePickerDialog = true }.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                Text("언제부터 푸시 알림을 받을까요?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TossTextSecondary, modifier = Modifier.padding(start = 4.dp))
                val intervals = listOf(5, 10, 30, 60)
                val labels = listOf("출발 5분 전", "10분 전", "30분 전", "1시간 전")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervals.forEachIndexed { index, minutes ->
                        val isSelected = pushInterval == minutes
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) TossBlue else TossPanelWhite).clickable { pushInterval = minutes }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                            Text(labels[index], fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, color = if (isSelected) Color.White else TossTextSecondary)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("실패 시 보낼 '굴욕 메시지' 작성", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TossTextSecondary)
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(TossBadgeBg).clickable {
                            val randomMessages = listOf(
                                "저 또 지각했습니다. 제 뺨을 시원하게 쳐주세요.",
                                "외출 마감 대실패! 스타벅스 한 잔 바치겠습니다.",
                                "자택 탈출에 실패했습니다. 저는 의지박약입니다.",
                                "오늘 약속 대지각 확정! 지각비 드릴게요ㅠㅠ",
                                "숨쉬듯 구라치다 적발되었습니다. 오늘 커피는 제가 쏩니다.",
                                "집에서 안 나갔습니다. 구라방지턱에 걸렸습니다."
                            )
                            customPenaltyMessage = randomMessages.random()
                        }.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = TossBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("랜덤 생성", fontSize = 11.sp, color = TossBlue, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedTextField(
                    value = customPenaltyMessage,
                    onValueChange = { if (it.length <= 40) customPenaltyMessage = it },
                    placeholder = { Text("예: 저 또 지각했습니다. 제 뺨을 때려주세요.", color = TossTextMuted.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TossBlue, unfocusedBorderColor = Color.Transparent, focusedContainerColor = TossPanelWhite, unfocusedContainerColor = TossPanelWhite)
                )
                Text(text = "현재 ${customPenaltyMessage.length} / 40자 (SMS 전송 제한 보호)", fontSize = 11.sp, color = if (customPenaltyMessage.length >= 40) TossDismissRed else TossTextMuted, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(end = 4.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                Text("나의 집 위치를 설정해 주세요. (반경 50m)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TossTextSecondary, modifier = Modifier.padding(start = 4.dp))
                Card(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, TossBorder)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NaverMap(modifier = Modifier.fillMaxSize(), cameraPositionState = previewCameraPositionState, properties = MapProperties(locationTrackingMode = LocationTrackingMode.NoFollow), uiSettings = MapUiSettings(isLocationButtonEnabled = false, isZoomControlEnabled = false, isScrollGesturesEnabled = false, isZoomGesturesEnabled = false, isTiltGesturesEnabled = false, isRotateGesturesEnabled = false)) { CircleOverlay(center = homeLatLng, radius = 50.0, color = Color(0x1A3182F6), outlineColor = TossBlue, outlineWidth = 2.dp) }
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.02f)).clickable { dialogCameraPositionState.position = com.naver.maps.map.CameraPosition(homeLatLng, 16.0); showMapDialog = true })
                        Box(modifier = Modifier.align(Alignment.Center).size(48.dp).background(TossPanelWhite.copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Home, contentDescription = null, tint = TossBlue, modifier = Modifier.size(28.dp))
                        }
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).clip(RoundedCornerShape(100.dp)).background(TossBadgeBg).padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Map, contentDescription = null, tint = TossBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("터치하여 지도 펼치기", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TossBlue)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(TossBg).padding(horizontal = 24.dp, vertical = 20.dp)) {
            Button(
                onClick = {
                    if (rawPhoneNumber.isEmpty()) { Toast.makeText(context, "감시자 연락처를 먼저 입력해 주세요.", Toast.LENGTH_SHORT).show(); return@Button }
                    if (targetTimeMillis <= System.currentTimeMillis()) { Toast.makeText(context, "외출 마감 시간은 미래여야 합니다.", Toast.LENGTH_SHORT).show(); return@Button }
                    if (customPenaltyMessage.isEmpty()) { customPenaltyMessage = "외출 마감 시간까지 자택 탈출 실패! 벌칙을 요구하세요." }

                    val formatted = StringBuilder()
                    for (i in rawPhoneNumber.indices) { formatted.append(rawPhoneNumber[i]); if (rawPhoneNumber.length <= 10) { if (i == 2 || i == 5) if (i != rawPhoneNumber.lastIndex) formatted.append("-") } else { if (i == 2 || i == 6) if (i != rawPhoneNumber.lastIndex) formatted.append("-") } }
                    prefs.edit().apply { putBoolean("is_scheduled", true); putString("guard_phone", formatted.toString()); putString("selected_time_text", selectedTimeText); putString("custom_penalty_message", customPenaltyMessage); putInt("push_interval", pushInterval); putLong("target_time_millis", targetTimeMillis); apply() }
                    scheduleAlarm(context, targetTimeMillis, pushInterval)
                    Toast.makeText(context, "감시 일정이 등록되었습니다!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = TossBlue)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("저장 및 감시 시작하기", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }

    if (showMapDialog) {
        Dialog(onDismissRequest = { showMapDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { showMapDialog = false })
                Card(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).align(Alignment.BottomCenter).clickable(enabled = false) { },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = TossBg)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NaverMap(modifier = Modifier.fillMaxSize(), cameraPositionState = dialogCameraPositionState, locationSource = locationSource, properties = MapProperties(locationTrackingMode = LocationTrackingMode.NoFollow), uiSettings = MapUiSettings(isLocationButtonEnabled = true, isZoomControlEnabled = false)) { CircleOverlay(center = dialogCameraPositionState.position.target, radius = 50.0, color = Color(0x1A3182F6), outlineColor = TossBlue, outlineWidth = 2.dp) }
                        Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 24.dp)) { Icon(Icons.Rounded.Place, contentDescription = null, tint = TossDismissRed, modifier = Modifier.size(48.dp)) }
                        Row(modifier = Modifier.fillMaxWidth().background(TossPanelWhite.copy(alpha = 0.9f)).padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("지도를 움직여 집 위치를 골라주세요", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TossTextPrimary)
                            IconButton(onClick = { showMapDialog = false }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Close, contentDescription = null, tint = TossTextSecondary) }
                        }
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp)) {
                            Button(onClick = { val c = dialogCameraPositionState.position.target; prefs.edit().apply { putFloat("home_lat", c.latitude.toFloat()); putFloat("home_lng", c.longitude.toFloat()); apply() }; savedLat = c.latitude.toFloat(); savedLng = c.longitude.toFloat(); previewCameraPositionState.position = com.naver.maps.map.CameraPosition(c, 16.0); showMapDialog = false }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = TossBlue), shape = RoundedCornerShape(16.dp)) { Text("이 위치를 집으로 지정하기", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
                        }
                    }
                }
            }
        }
    }

    if (showDateTimePickerDialog) {
        ModalBottomSheet(onDismissRequest = { showDateTimePickerDialog = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = TossPanelWhite) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(TossBg, RoundedCornerShape(16.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("설정 중인 마감 일시", fontSize = 11.sp, color = TossTextSecondary, fontWeight = FontWeight.SemiBold)
                    Text(selectedTimeText, fontSize = 16.sp, color = TossBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                }
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TossBg).padding(4.dp)) {
                    Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if(dialogActiveTab == "date") TossPanelWhite else Color.Transparent).clickable { dialogActiveTab = "date" }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DateRange, contentDescription = null, tint = if(dialogActiveTab == "date") TossBlue else TossTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("날짜 선택", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if(dialogActiveTab == "date") TossBlue else TossTextSecondary)
                    }
                    Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if(dialogActiveTab == "time") TossPanelWhite else Color.Transparent).clickable { dialogActiveTab = "time" }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = if(dialogActiveTab == "time") TossBlue else TossTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("시간 선택", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if(dialogActiveTab == "time") TossBlue else TossTextSecondary)
                    }
                }
                if (dialogActiveTab == "date") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { targetTimeMillis = Calendar.getInstance().timeInMillis; dialogActiveTab = "time" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = TossBadgeBg, contentColor = TossBlue)) { Text("오늘", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        Button(onClick = { targetTimeMillis = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis; dialogActiveTab = "time" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = TossBadgeBg, contentColor = TossBlue)) { Text("내일", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                    DatePicker(state = datePickerState, title = null, headline = null, showModeToggle = false, colors = DatePickerDefaults.colors(containerColor = TossPanelWhite, selectedDayContainerColor = TossBlue, selectedDayContentColor = Color.White, todayContentColor = TossBlue, todayDateBorderColor = TossBlue))
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10, 30, 60).forEach { mins -> Button(onClick = { val cal = Calendar.getInstance().apply { timeInMillis = targetTimeMillis }; cal.add(Calendar.MINUTE, mins); targetTimeMillis = cal.timeInMillis; chosenHour24 = cal.get(Calendar.HOUR_OF_DAY); chosenMinute = cal.get(Calendar.MINUTE) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = TossBg, contentColor = TossTextPrimary)) { Text("+${mins}분", fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SamsungStyleTimePicker(initialHour24 = chosenHour24, initialMinute = chosenMinute, onTimeChanged = { h, m -> chosenHour24 = h; chosenMinute = m })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDateTimePickerDialog = false }) { Text("취소", color = TossTextSecondary, fontWeight = FontWeight.SemiBold) }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (dialogActiveTab == "date") {
                        Button(onClick = { dialogActiveTab = "time" }, colors = ButtonDefaults.buttonColors(containerColor = TossBlue), shape = RoundedCornerShape(12.dp)) {
                            Text("다음", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    else {
                        Button(
                            onClick = { val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis() }; val res = Calendar.getInstance().apply { set(Calendar.YEAR, utc.get(Calendar.YEAR)); set(Calendar.MONTH, utc.get(Calendar.MONTH)); set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH)); set(Calendar.HOUR_OF_DAY, chosenHour24); set(Calendar.MINUTE, chosenMinute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }; targetTimeMillis = res.timeInMillis; showDateTimePickerDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TossBlue), shape = RoundedCornerShape(12.dp)
                        ) { Text("선택 완료", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }
        }
    }
}

fun scheduleAlarm(context: Context, targetTimeMillis: Long, pushIntervalMinutes: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val finalPendingIntent = PendingIntent.getBroadcast(context, 999, Intent(context, GuraCheckReceiver::class.java).apply { putExtra("ALARM_TYPE", "FINAL_CHECK") }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val preAlertTimeMillis = targetTimeMillis - (pushIntervalMinutes * 60 * 1000)
    val prePendingIntent = PendingIntent.getBroadcast(context, 998, Intent(context, GuraCheckReceiver::class.java).apply { putExtra("ALARM_TYPE", "PRE_ALERT"); putExtra("PUSH_INTERVAL", pushIntervalMinutes) }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    try { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMillis, finalPendingIntent); if (preAlertTimeMillis > System.currentTimeMillis()) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preAlertTimeMillis, prePendingIntent) }
    catch (e: SecurityException) { alarmManager.set(AlarmManager.RTC_WAKEUP, targetTimeMillis, finalPendingIntent); if (preAlertTimeMillis > System.currentTimeMillis()) alarmManager.set(AlarmManager.RTC_WAKEUP, preAlertTimeMillis, prePendingIntent) }
}

fun cancelAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    PendingIntent.getBroadcast(context, 999, Intent(context, GuraCheckReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { alarmManager.cancel(it); it.cancel() }
    PendingIntent.getBroadcast(context, 998, Intent(context, GuraCheckReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { alarmManager.cancel(it); it.cancel() }
}
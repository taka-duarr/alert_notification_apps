
package com.example.alertnotificationapps

import android.annotation.SuppressLint
import android.content.*
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Context
import com.example.alertnotificationapps.model.SensorData
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.background
import android.os.PowerManager
import android.net.Uri
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource

//ssssSSS


class MainActivity : ComponentActivity() {
    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestIgnoreBatteryOptimization()


        // ✅ STATE HARUS DI DALAM setContent
        setContent {

            var statusText by remember { mutableStateOf("Monitoring aktif") }
            var tempText by remember { mutableStateOf("-- °C") }
            val sensors = remember {
                mutableStateMapOf<String, SensorData>()
            }


            // ✅ REGISTER RECEIVER DI COMPOSE LIFECYCLE
            DisposableEffect(Unit) {

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val id = intent?.getStringExtra("device_id") ?: return
                        val status = intent.getStringExtra("status") ?: return
                        val temp = intent.getDoubleExtra("temperature", 0.0)

                        sensors[id] = SensorData(
                            deviceId = id,
                            status = status,
                            temperature = temp
                        )
                    }

                }

                val filter = IntentFilter("ALERT_UPDATE")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(
                        receiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    registerReceiver(receiver, filter)
                }


                onDispose {
                    unregisterReceiver(receiver)
                }
            }

            AppUI(sensors = sensors)

        }

        // START SERVICE
        val intent = Intent(this, AlertService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }







    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI(sensors: Map<String, SensorData>) {
    MaterialTheme {
        val alertCount = sensors.values.count { it.status == "BAHAYA" }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {} // kosongkan
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1e1f22))
                    .padding(padding)
            ) {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MonitoringHeader(
                        buildingName = "Gedung A Monitoring",
                        floors = "${sensors.size} Floors Active",
                        alertCount = alertCount
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (sensors.isEmpty()) {
                        EmptyState(Modifier.fillMaxSize())
                    } else {
                        SensorList(
                            sensors = sensors,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

//anjayyyy
@Composable
fun MonitoringHeader(
    buildingName: String,
    floors: String,
    alertCount: Int
) {

    // Warna badge mengikuti jumlah alert
    val badgeColor = if (alertCount > 0) Color(0xFF8B1E1E) else Color(0xFF2E2E2E)
    val textColor = if (alertCount > 0) Color.White else Color.LightGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Ikon Gedung
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2C2F48), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🏢",
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nama Gedung & Informasi lantai
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    buildingName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    floors,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Badge Alert dinamis
            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(30))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$alertCount Alerts",
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Menambahkan ikon untuk visual yang lebih baik
            Text(
                text = "📡",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                "Menunggu data sensor…",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                "Pastikan sensor & server aktif",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SensorList(
    sensors: Map<String, SensorData>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,  // ✅ DITAMBAHKAN untuk spacing dari top bar
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sensors.values.toList()) { sensor ->
            SensorCard(sensor)
        }
    }
}

@Composable
fun SensorCard(sensor: SensorData) {

    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (sensor.status) {
        "BAHAYA" -> Color(0xFFFF4C4C)
        "PERINGATAN" -> Color(0xFFFFB74D)
        else -> Color(0xFF66DD77)
    }

    val surfaceDark = Color(0xFF1A1A1A)
    val cardDark = Color(0xFF222224)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardDark),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Indicator bulat merah/hijau/kuning
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(statusColor, shape = RoundedCornerShape(50))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = sensor.deviceId,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = R.drawable.corporate_fare_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("Temp", "${"%.1f".format(sensor.temperature)}°C", Color(0xFF4A90E2))
                SummaryItem("Smoke", "50%", Color(0xFFC8C8C8))
                SummaryItem("Alerts", if (sensor.status == "BAHAYA") "1" else "0", Color(0xFFFF6B6B))
            }

            // EXPANDED DETAIL
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(surfaceDark, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {

                    Text(
                        "Detail Ruangan",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // GRID 2x2 RUANGAN
                    DetailGrid()

                    Spacer(modifier = Modifier.height(16.dp))

                    // LEGEND
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(Color(0xFF66DD77), "Normal")
                        LegendItem(Color(0xFFFFB74D), "Warning")
                        LegendItem(Color(0xFFFF4C4C), "Fire Alert")
                    }
                }
            }
        }
    }
}
@Composable
fun SummaryItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
@Composable
fun DetailGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoomCard("A - 201", Color(0xFFFF4C4C))
            RoomCard("A - 202", Color(0xFF66DD77))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoomCard("A - 203", Color(0xFFFFB74D))
            RoomCard("A - 204", Color(0xFF66DD77))
        }
    }
}
@Composable
fun RoomCard(room: String, statusColor: Color) {
    Column(
        modifier = Modifier
            .background(Color(0xFF121212), RoundedCornerShape(14.dp))
            .border(
                BorderStroke(1.dp, statusColor.copy(alpha = 0.8f)),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            room,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Temp : 23°C", color = Color.White)
        Text("Smoke : 45%", color = Color.White)
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White)
    }
}




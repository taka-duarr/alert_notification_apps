
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "IoT Alert System",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                )
            }
        ) { padding ->
            if (sensors.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            } else {
                SensorList(
                    sensors = sensors,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
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
    val (color, icon, label) = when (sensor.status) {
        "BAHAYA" -> Triple(
            MaterialTheme.colorScheme.error,
            "🚨",
            "BAHAYA"
        )
        "PERINGATAN" -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "⚠️",
            "PERINGATAN"
        )
        else -> Triple(
            MaterialTheme.colorScheme.primary,
            "✅",
            "AMAN"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,  // Lebih halus
            pressedElevation = 8.dp
        ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar dengan corner radius
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(56.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(3.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.deviceId,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    Text(
                        text = label,
                        color = color,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${"%.1f".format(sensor.temperature)}°C",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = when (sensor.status) {
                        "BAHAYA" -> MaterialTheme.colorScheme.error
                        "PERINGATAN" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                Text(
                    "Temperature",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



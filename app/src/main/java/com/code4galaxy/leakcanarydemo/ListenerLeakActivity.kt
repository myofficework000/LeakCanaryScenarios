package com.code4galaxy.leakcanarydemo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.code4galaxy.leakcanarydemo.ui.theme.LeakCanaryDemoTheme

/**
 * ---------------------------------------------------------------------------------------------
 * 🎧 SCENARIO 2: UNREGISTERED LISTENER / SINGLETON OBSERVER LEAK
 * ---------------------------------------------------------------------------------------------
 *
 * 🚨 THE BUG:
 * Registering an Activity or Fragment as a listener to a long-lived Singleton (e.g. LocationManager,
 * SensorManager, EventBus, Custom SDK) without unregistering upon Activity destruction.
 * The Singleton holds a strong reference to `this` (Activity) in its listener collection.
 *
 * 🔍 LEAKCANARY DIAGNOSIS:
 * LeakCanary detects that [ListenerLeakActivity] has leaked because:
 *   SensorManagerSingleton.listeners -> ArrayList -> element [0] -> ListenerLeakActivity instance
 *
 * ✅ THE FIX:
 * Always match `registerListener(this)` with `unregisterListener(this)` in symmetric lifecycle callbacks
 * (e.g. `onCreate()`/`onDestroy()` or `onStart()`/`onStop()`).
 * ---------------------------------------------------------------------------------------------
 */
class ListenerLeakActivity : ComponentActivity(), SensorEventListener {

    companion object {
        private const val TAG = "ListenerLeakActivity"
    }

    private var isLeakEnabled by mutableStateOf(true)
    private var lastEmittedData by mutableStateOf("No data emitted yet")
    private var listenerCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register Activity as a listener to the Singleton
        SensorManagerSingleton.registerListener(this)
        listenerCount = SensorManagerSingleton.getListenerCount()

        setContent {
            LeakCanaryDemoTheme {
                ListenerLeakScreen(
                    isLeakEnabled = isLeakEnabled,
                    lastEmittedData = lastEmittedData,
                    listenerCount = listenerCount,
                    onLeakToggle = { enabled ->
                        isLeakEnabled = enabled
                    },
                    onEmitMockData = {
                        val mockData = "Sensor Ping #${System.currentTimeMillis() % 1000}"
                        SensorManagerSingleton.emitMockData(mockData)
                    },
                    onFinish = { finish() }
                )
            }
        }
    }

    override fun onSensorDataChanged(value: String) {
        Log.d(TAG, "Received sensor data: $value")
        lastEmittedData = value
        Toast.makeText(this, "Event: $value", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called. isLeakEnabled=$isLeakEnabled")

        // ✅ FIX: Unregister listener in onDestroy if leak mode is OFF
        if (!isLeakEnabled) {
            SensorManagerSingleton.unregisterListener(this)
            Log.d(TAG, "✅ FIXED: Unregistered listener from Singleton in onDestroy()")
        } else {
            Log.w(
                TAG,
                "🚨 LEAKING: Listener remains registered in Singleton after onDestroy()!"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenerLeakScreen(
    isLeakEnabled: Boolean,
    lastEmittedData: String,
    listenerCount: Int,
    onLeakToggle: (Boolean) -> Unit,
    onEmitMockData: () -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Singleton Listener Leak Demo") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Mode Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLeakEnabled) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLeakEnabled) "🚨 Leak Mode: ENABLED" else "✅ Fix Mode: ENABLED",
                            fontWeight = FontWeight.Bold,
                            color = if (isLeakEnabled) Color(0xFFC62828) else Color(0xFF2E7D32),
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isLeakEnabled)
                                "Activity stays in Singleton's listener list after onDestroy()"
                            else
                                "Activity unregisters from Singleton in onDestroy()",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLeakEnabled,
                        onCheckedChange = onLeakToggle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live State Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Singleton State:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Registered Listeners Count: $listenerCount", fontSize = 14.sp)
                    Text("Latest Received Event: $lastEmittedData", fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onEmitMockData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Emit Mock Event from Singleton")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Code Explanation Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Code Comparison:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = """
                            |// 🚨 LEAKING CODE:
                            |override fun onCreate(...) {
                            |    SensorManagerSingleton.registerListener(this)
                            |}
                            |// onDestroy() does NOT unregister!
                            |
                            |// ✅ FIXED CODE:
                            |override fun onDestroy() {
                            |    super.onDestroy()
                            |    SensorManagerSingleton.unregisterListener(this)
                            |}
                        """.trimMargin(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        color = Color(0xFFD4D4D4)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions to test LeakCanary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔍 How to Test with LeakCanary:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Keep 'Leak Mode: ENABLED'\n" +
                                "2. Click 'Finish Activity & Trigger Leak'\n" +
                                "3. Navigating back destroys the Activity, but SensorManagerSingleton still holds `this`.\n" +
                                "4. LeakCanary will detect this retained Activity in ~5 seconds!",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLeakEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLeakEnabled) "Finish Activity & Trigger Leak" else "Finish Activity (Clean)")
            }
        }
    }
}

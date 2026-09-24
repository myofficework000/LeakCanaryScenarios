package com.code4galaxy.leakcanarydemo

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * 📌 SCENARIO 1: STATIC REFERENCE MEMORY LEAK
 * ---------------------------------------------------------------------------------------------
 *
 * 🚨 THE BUG:
 * Storing an [Activity] or [Context] instance in a `companion object` (static variable) holds a
 * strong reference in the JVM ClassLoader. When the Activity is destroyed (e.g. back navigation or
 * screen rotation), Garbage Collector CANNOT reclaim it because the static field still references it!
 *
 * 🔍 LEAKCANARY DIAGNOSIS:
 * LeakCanary detects that [StaticLeakActivity] has leaked because:
 *   StaticLeakActivity.companion.leakedActivity -> StaticLeakActivity instance
 *
 * ✅ THE FIX:
 * 1. Clear the static reference on destroy: `leakedActivity = null` in [onDestroy].
 * 2. Or avoid static references to Context/Activity altogether (use Application Context if needed).
 * 3. Or wrap in a [java.lang.ref.WeakReference].
 * ---------------------------------------------------------------------------------------------
 */
class StaticLeakActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StaticLeakActivity"

        // 🚨 LEAK: Static reference holding Activity context indefinitely
        var leakedActivity: Context? = null
    }

    private var isLeakEnabled by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Assign static reference
        leakedActivity = this
        Log.d(TAG, "Static reference set to $this")

        setContent {
            LeakCanaryDemoTheme {
                StaticLeakScreen(
                    isLeakEnabled = isLeakEnabled,
                    onLeakToggle = { enabled ->
                        isLeakEnabled = enabled
                        if (!enabled) {
                            // If user turns off leak mode, clear static ref immediately
                            leakedActivity = null
                            Log.d(TAG, "Leak mode disabled. Cleared static reference.")
                        } else {
                            leakedActivity = this@StaticLeakActivity
                            Log.d(TAG, "Leak mode enabled. Set static reference.")
                        }
                    },
                    onFinish = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called. isLeakEnabled=$isLeakEnabled")

        // ✅ FIX: Clear static reference in onDestroy if leak mode is OFF
        if (!isLeakEnabled) {
            leakedActivity = null
            Log.d(TAG, "✅ FIXED: Static reference cleared in onDestroy()")
        } else {
            Log.w(TAG, "🚨 LEAKING: Static reference retains Activity after onDestroy()!")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticLeakScreen(
    isLeakEnabled: Boolean,
    onLeakToggle: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Static Reference Leak Demo") },
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
                                "Companion object holds strong Activity reference after onDestroy()"
                            else
                                "Static reference is cleared in onDestroy() allowing GC",
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
                            |companion object {
                            |    var leakedActivity: Context? = null
                            |}
                            |
                            |override fun onCreate(...) {
                            |    leakedActivity = this // Held forever in JVM!
                            |}
                            |
                            |// ✅ FIXED CODE:
                            |override fun onDestroy() {
                            |    super.onDestroy()
                            |    leakedActivity = null // Clears reference
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
                                "2. Click 'Finish Activity & Go Back' below\n" +
                                "3. LeakCanary will observe this destroyed Activity.\n" +
                                "4. In ~5 seconds, LeakCanary will trigger GC and show a notification with the full leak trace!",
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

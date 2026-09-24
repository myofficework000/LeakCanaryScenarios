package com.code4galaxy.leakcanarydemo

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
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.lifecycleScope
import com.code4galaxy.leakcanarydemo.ui.theme.LeakCanaryDemoTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ---------------------------------------------------------------------------------------------
 * 🚀 SCENARIO 4: UNCANCELLED COROUTINE / GLOBALSCOPE LEAK
 * ---------------------------------------------------------------------------------------------
 *
 * 🚨 THE BUG:
 * Launching long-running asynchronous coroutines using `GlobalScope` or an unmanaged [CoroutineScope]
 * that accesses Activity properties or references `this`.
 * `GlobalScope` coroutines operate at the application level and outlive Activity lifecycles.
 *
 * 🔍 LEAKCANARY DIAGNOSIS:
 * LeakCanary detects that [CoroutineLeakActivity] has leaked because:
 *   StandaloneCoroutine -> Continuation -> CoroutineLeakActivity$startCoroutineTask$1 -> CoroutineLeakActivity
 *
 * ✅ THE FIX:
 * Use structured concurrency! Launch coroutines using [lifecycleScope] (or [androidx.lifecycle.ViewModel.viewModelScope]).
 * `lifecycleScope` automatically cancels all active coroutines when the Activity receives `ON_DESTROY`.
 * ---------------------------------------------------------------------------------------------
 */
class CoroutineLeakActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CoroutineLeakActivity"
    }

    private var isLeakEnabled by mutableStateOf(true)
    private var coroutineStatus by mutableStateOf("No coroutine running")
    /** The job currently used by the selected demonstration mode. */
    private var runningJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Launch coroutine task upon creation
        startCoroutineTask()

        setContent {
            LeakCanaryDemoTheme {
                CoroutineLeakScreen(
                    isLeakEnabled = isLeakEnabled,
                    coroutineStatus = coroutineStatus,
                    onLeakToggle = { enabled ->
                        isLeakEnabled = enabled
                        // A mode change must replace (and therefore cancel) the previous job.
                        // Without this, turning Fix Mode on after opening the screen would leave
                        // the already-started GlobalScope job retaining this Activity.
                        startCoroutineTask()
                    },
                    onRestartTask = {
                        startCoroutineTask()
                    },
                    onFinish = { finish() }
                )
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startCoroutineTask() {
        runningJob?.cancel()

        if (isLeakEnabled) {
            // 🚨 LEAK: GlobalScope launches coroutine that runs independently of Activity lifecycle
            runningJob = GlobalScope.launch(Dispatchers.Main) {
                coroutineStatus = "GlobalScope job running loop..."
                Log.d(TAG, "🚨 GlobalScope coroutine started on Activity: ${this@CoroutineLeakActivity}")

                for (i in 1..60) {
                    delay(1000L)
                    val statusMsg = "GlobalScope Coroutine Ping $i/60s on $this"
                    Log.d(TAG, statusMsg)
                    coroutineStatus = statusMsg
                }
            }
        } else {
            // ✅ FIX: Use lifecycleScope, which automatically cancels when Activity is destroyed!
            runningJob = lifecycleScope.launch(Dispatchers.Main) {
                coroutineStatus = "lifecycleScope job running loop..."
                Log.d(TAG, "✅ lifecycleScope coroutine started")

                for (i in 1..60) {
                    delay(1000L)
                    val statusMsg = "lifecycleScope Coroutine Ping $i/60s"
                    Log.d(TAG, statusMsg)
                    coroutineStatus = statusMsg
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called. isLeakEnabled=$isLeakEnabled")

        if (!isLeakEnabled) {
            runningJob?.cancel()
            Log.d(TAG, "✅ FIXED: lifecycleScope automatically cancels active jobs on onDestroy()")
        } else {
            Log.w(
                TAG,
                "🚨 LEAKING: GlobalScope job is still active in background after onDestroy()!"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoroutineLeakScreen(
    isLeakEnabled: Boolean,
    coroutineStatus: String,
    onLeakToggle: (Boolean) -> Unit,
    onRestartTask: () -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GlobalScope Coroutine Leak Demo") },
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
                                "GlobalScope.launch { ... } continues running after onDestroy()"
                            else
                                "lifecycleScope.launch { ... } automatically cancels on onDestroy()",
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

            // Coroutine State Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Coroutine Job Status:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(coroutineStatus, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRestartTask,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restart Coroutine Loop")
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
                            |GlobalScope.launch {
                            |    // Outlives Activity lifecycle!
                            |    delay(60_000L)
                            |    updateUi(this@Activity)
                            |}
                            |
                            |// ✅ FIXED CODE:
                            |lifecycleScope.launch {
                            |    // Cancelled automatically on onDestroy()
                            |    delay(60_000L)
                            |    updateUi(this@Activity)
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
                                "3. The GlobalScope coroutine keeps running in background.\n" +
                                "4. LeakCanary detects that the active Continuation holds a reference to the destroyed Activity!",
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

package com.code4galaxy.leakcanarydemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
 * ⏱️ SCENARIO 3: HANDLER / POSTDELAYED RUNNABLE LEAK
 * ---------------------------------------------------------------------------------------------
 *
 * 🚨 THE BUG:
 * Posting a delayed [Runnable] using a [Handler] keeps a reference to the [Message] in the main
 * thread's [Looper] queue. The Runnable implicitly captures a reference to the enclosing
 * [Activity] class. As long as the message remains in the queue (e.g. 60 seconds), GC cannot collect
 * the Activity!
 *
 * 🔍 LEAKCANARY DIAGNOSIS:
 * LeakCanary detects that [HandlerLeakActivity] has leaked because:
 *   MessageQueue.mMessages -> Message.callback -> HandlerLeakActivity$postDelayedTask$1 -> HandlerLeakActivity
 *
 * ✅ THE FIX:
 * Call `handler.removeCallbacksAndMessages(null)` in `onDestroy()` to cancel and purge all pending
 * messages and runnables associated with this Handler.
 * ---------------------------------------------------------------------------------------------
 */
class HandlerLeakActivity : ComponentActivity() {

    companion object {
        private const val TAG = "HandlerLeakActivity"
        private const val DELAY_MS = 60_000L // 60 seconds delay
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isLeakEnabled by mutableStateOf(true)
    private var taskStatus by mutableStateOf("No delayed task running")

    // Anonymous Runnable holding reference to HandlerLeakActivity
    private val delayedRunnable = Runnable {
        val msg = "60-Second Delayed Task executed on Activity: $this"
        Log.d(TAG, msg)
        taskStatus = msg
        Toast.makeText(this, "Task completed!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Automatically start the delayed task upon entering screen
        postDelayedTask()

        setContent {
            LeakCanaryDemoTheme {
                HandlerLeakScreen(
                    isLeakEnabled = isLeakEnabled,
                    taskStatus = taskStatus,
                    onLeakToggle = { enabled ->
                        isLeakEnabled = enabled
                    },
                    onPostTaskAgain = {
                        postDelayedTask()
                    },
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun postDelayedTask() {
        handler.removeCallbacks(delayedRunnable)
        handler.postDelayed(delayedRunnable, DELAY_MS)
        taskStatus = "Task posted! Waiting in Looper queue for 60 seconds..."
        Log.d(TAG, "Posted Runnable to Handler with 60s delay")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called. isLeakEnabled=$isLeakEnabled")

        // ✅ FIX: Remove pending callbacks in onDestroy if leak mode is OFF
        if (!isLeakEnabled) {
            handler.removeCallbacksAndMessages(null)
            Log.d(TAG, "✅ FIXED: Removed pending Handler callbacks in onDestroy()")
        } else {
            Log.w(
                TAG,
                "🚨 LEAKING: Handler Runnable remains in Looper queue after onDestroy()!"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandlerLeakScreen(
    isLeakEnabled: Boolean,
    taskStatus: String,
    onLeakToggle: (Boolean) -> Unit,
    onPostTaskAgain: () -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Handler PostDelayed Leak Demo") },
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
                                "Delayed message sits in Looper queue for 60s holding Activity reference"
                            else
                                "handler.removeCallbacksAndMessages(null) called in onDestroy()",
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

            // Task State Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Handler State:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(taskStatus, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onPostTaskAgain,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Post 60s Delayed Task Again")
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
                            |val handler = Handler(Looper.getMainLooper())
                            |handler.postDelayed({
                            |    // Runnable retains Activity reference
                            |    doSomething(this)
                            |}, 60_000L)
                            |// Destroying Activity leaves task in Looper queue!
                            |
                            |// ✅ FIXED CODE:
                            |override fun onDestroy() {
                            |    super.onDestroy()
                            |    handler.removeCallbacksAndMessages(null)
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
                                "3. The 60-second delayed Runnable stays in the main Looper queue.\n" +
                                "4. LeakCanary detects that the Looper Message holds the destroyed Activity!",
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

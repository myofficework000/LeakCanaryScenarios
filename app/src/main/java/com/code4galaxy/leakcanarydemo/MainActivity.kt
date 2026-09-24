package com.code4galaxy.leakcanarydemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.code4galaxy.leakcanarydemo.ui.theme.LeakCanaryDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LeakCanaryDemoTheme {
                MainDashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LeakCanary Demo",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Card explaining LeakCanary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🐤 What is LeakCanary?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "LeakCanary is an open-source memory leak detection library for Android. " +
                                "It automatically monitors destroyed Activities & Fragments. When an object remains " +
                                "in memory for >5 seconds after destruction, LeakCanary dumps the heap, " +
                                "identifies the retain chain, and alerts you via notification!",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Interactive Memory Leak Scenarios",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a scenario to test leaking vs. fixed code paths:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scenario 1: Static Leak
            ScenarioCard(
                icon = "📌",
                title = "1. Static Reference Leak",
                description = "Companion object holds a strong Activity/Context reference.",
                tags = listOf("Static Field", "ClassLoader"),
                onClick = {
                    context.startActivity(Intent(context, StaticLeakActivity::class.java))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scenario 2: Singleton Listener Leak
            ScenarioCard(
                icon = "🎧",
                title = "2. Unregistered Singleton Listener",
                description = "Activity registers on Singleton but forgets to unregister on destroy.",
                tags = listOf("Singleton", "Observer Pattern"),
                onClick = {
                    context.startActivity(Intent(context, ListenerLeakActivity::class.java))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scenario 3: Handler Leak
            ScenarioCard(
                icon = "⏱️",
                title = "3. Handler / PostDelayed Runnable",
                description = "Delayed Runnable sitting in main thread's Looper queue.",
                tags = listOf("Handler", "Looper Message"),
                onClick = {
                    context.startActivity(Intent(context, HandlerLeakActivity::class.java))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scenario 4: Coroutine Leak
            ScenarioCard(
                icon = "🚀",
                title = "4. GlobalScope Coroutine Leak",
                description = "GlobalScope job capturing Activity reference without cancellation.",
                tags = listOf("Coroutines", "Structured Concurrency"),
                onClick = {
                    context.startActivity(Intent(context, CoroutineLeakActivity::class.java))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Footer info about checking LeakCanary output
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Checking Leak Canary Output:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Enter a scenario with Leak Mode ENABLED.\n" +
                                "2. Tap 'Finish Activity & Trigger Leak' to destroy the screen.\n" +
                                "3. Run the nativeDebug variant: this demo dumps after one retained Activity, so a result appears in about 5 seconds.\n" +
                                "4. Open the notification or the 'Leaks' launcher icon. For Android Studio's Profiler, run studioDebug instead; Studio owns that analysis.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ScenarioCard(
    icon: String,
    title: String,
    description: String,
    tags: List<String>,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

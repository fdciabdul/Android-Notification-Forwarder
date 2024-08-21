package com.example.notifier

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.notifier.ui.theme.MyApplicationTheme
import java.util.Calendar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: NotificationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this).get(NotificationViewModel::class.java)

        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver,
            IntentFilter("com.example.myapplication.NOTIFICATION_POSTED")
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        startForegroundService()

        setContent {
            MyApplicationTheme {
                MainActivityContent(viewModel)
            }
        }
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("package")
            val title = intent?.getStringExtra("title")
            val text = intent?.getStringExtra("text")

            viewModel.addNotification(packageName, title, text)
        }
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun requestNotificationPermission() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Notification permission is required for this app.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
    }
}

@Composable
fun MainActivityContent(viewModel: NotificationViewModel) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ChangeServerDialog(
            onDismiss = { showDialog = false },
            onSave = { newUrl ->
                val sharedPreferences = context.getSharedPreferences("myAppPrefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("server_url", newUrl)
                    apply()
                }
                Toast.makeText(context, "Server URL updated", Toast.LENGTH_SHORT).show()
                showDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 16.dp),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Change Server", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppTitle("E-WALLET - BANK NOTIF FORWARDER")
            Spacer(modifier = Modifier.height(20.dp))
            NotificationPermissionButton(context = LocalContext.current)
            Spacer(modifier = Modifier.height(20.dp))
            ServerUrlInput()
            Spacer(modifier = Modifier.height(20.dp))
            NotificationList(viewModel)
            Spacer(modifier = Modifier.height(20.dp))
            Footer()
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeServerDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var serverUrl by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = { onSave(serverUrl.text) }) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSecondary)
            }
        },
        title = { Text("Change Server URL", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column {
                TextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("New Server URL", color = MaterialTheme.colorScheme.onBackground) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
}



@Composable
fun AppTitle(title: String) {
    Text(
        text = title,
        fontSize = 24.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,  // Ensure text color matches dark mode
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun NotificationPermissionButton(context: Context) {
    val notificationListenerEnabled = remember {
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).contains(context.packageName)
    }

    if (!notificationListenerEnabled) {
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
        ) {
            Text("Allow Notification Access", fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerUrlInput() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("myAppPrefs", Context.MODE_PRIVATE)
    val savedServerUrl = sharedPreferences.getString("server_url", "")

    if (savedServerUrl.isNullOrEmpty()) {
        var serverUrl by remember { mutableStateOf(TextFieldValue("")) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Enter Server URL", color = MaterialTheme.colorScheme.onBackground) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )

            Button(
                onClick = {
                    with(sharedPreferences.edit()) {
                        putString("server_url", serverUrl.text)
                        apply()
                    }
                    // Display a message to the user
                    Toast.makeText(context, "Server URL saved", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text("Save URL", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun NotificationList(viewModel: NotificationViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications.size) { index ->
            val notification = notifications[index]
            NotificationItem(
                packageName = notification.packageName,
                title = notification.title,
                text = notification.text
            )
        }
    }
}

@Composable
fun NotificationItem(
    packageName: String?,
    title: String?,
    text: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Package: $packageName", fontSize = 12.sp, textAlign = TextAlign.Left, color = MaterialTheme.colorScheme.onBackground)
        Text("Title: $title", fontSize = 16.sp, textAlign = TextAlign.Left, color = MaterialTheme.colorScheme.onBackground)
        Text("Text: $text", fontSize = 14.sp, textAlign = TextAlign.Left, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun Footer() {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    Text(
        text = "Copyright imtaqin $year",
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,  // Ensure text color matches dark mode
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )
}

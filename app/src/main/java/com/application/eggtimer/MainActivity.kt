package com.application.eggtimer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.application.eggtimer.ui.theme.EggTimerTheme

class MainActivity : ComponentActivity() {
    private var timerService: TimerService? = null
    private var isBound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true
            timerService?.setAppInForeground(true)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        
        val serviceIntent = Intent(this, TimerService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            EggTimerTheme {
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted -> hasNotificationPermission = isGranted }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isBound) {
                        EggTimerApp(
                            modifier = Modifier.padding(innerPadding),
                            timerService = timerService
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        timerService?.setAppInForeground(true)
    }

    override fun onPause() {
        super.onPause()
        timerService?.setAppInForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Egg Timer Channel"
            val descriptionText = "Notifications for Egg Timer"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("EGG_TIMER_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun EggTimerApp(modifier: Modifier = Modifier, timerService: TimerService?) {
    val context = LocalContext.current
    val timerState by timerService?.timerState?.collectAsState() ?: remember { mutableStateOf(TimerState()) }
    var selectedType by remember { mutableStateOf<EggType?>(null) }
    var showInstructions by remember { mutableStateOf(false) }
    
    val sharedPrefs = remember { context.getSharedPreferences("EggTimerPrefs", Context.MODE_PRIVATE) }
    var selectedRingtoneUri by remember { 
        mutableStateOf(sharedPrefs.getString("ringtone_uri", null)?.let { Uri.parse(it) }) 
    }
    var ringtoneName by remember {
        mutableStateOf(
            selectedRingtoneUri?.let { uri ->
                try {
                    RingtoneManager.getRingtone(context, uri).getTitle(context)
                } catch (e: Exception) {
                    "Default Alarm"
                }
            } ?: "Default Alarm"
        )
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            selectedRingtoneUri = uri
            uri?.let {
                sharedPrefs.edit().putString("ringtone_uri", it.toString()).apply()
                ringtoneName = RingtoneManager.getRingtone(context, it).getTitle(context)
            } ?: run {
                ringtoneName = "Default Alarm"
            }
        }
    }

    // Update selectedType when timer state changes externally (like from reset)
    LaunchedEffect(timerState.totalTime) {
        if (selectedType == null || selectedType?.seconds != timerState.totalTime) {
            selectedType = EggType.entries.find { it.seconds == timerState.totalTime }
        }
    }

    val displayTime = if (timerState.totalTime == 0 && selectedType != null) selectedType!!.seconds else timerState.timeLeft
    val progress by animateFloatAsState(
        targetValue = if (timerState.totalTime > 0) timerState.timeLeft.toFloat() / timerState.totalTime else 0f,
        label = "TimerProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Egg Timer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = { showInstructions = !showInstructions }) {
                Text(text = if (showInstructions) "Hide Steps" else "Steps")
            }
        }

        AnimatedVisibility(visible = showInstructions) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "How to boil the perfect egg:", fontWeight = FontWeight.Bold)
                    Text(text = "1. Place eggs in saucepan.\n2. Cover with 1 inch water.\n3. Bring to boil.\n4. Simmer and start timer.\n5. Use ice bath after.")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            CircularProgressIndicator(
                progress = { if (timerState.totalTime > 0) timerState.timeLeft.toFloat() / timerState.totalTime else 0f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(displayTime),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = selectedType?.title ?: "Select Egg")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Sound: $ringtoneName", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri)
            }
            ringtonePickerLauncher.launch(intent)
        }) {
            Text(text = "Change Alarm Sound")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            EggType.entries.forEach { type ->
                ElevatedCard(
                    modifier = Modifier.weight(1f).clickable { 
                        if (!timerState.isRunning) {
                            selectedType = type
                            val intent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_RESET
                                putExtra(TimerService.EXTRA_TIME, type.seconds)
                            }
                            context.startService(intent)
                        }
                    },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selectedType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                        Image(painter = painterResource(id = type.drawableId), contentDescription = type.title, modifier = Modifier.size(60.dp))
                        Text(text = type.title, fontWeight = if (selectedType == type) FontWeight.ExtraBold else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    selectedType?.let {
                        val intent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_START
                            putExtra(TimerService.EXTRA_TIME, it.seconds)
                            putExtra(TimerService.EXTRA_RINGTONE_URI, selectedRingtoneUri?.toString())
                        }
                        context.startService(intent)
                    }
                },
                enabled = !timerState.isRunning && selectedType != null && !timerState.isAlarming,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Start")
            }
            Button(
                onClick = {
                    val intent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STOP }
                    context.startService(intent)
                },
                enabled = timerState.isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Stop")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val intent = Intent(context, TimerService::class.java).apply {
                        action = TimerService.ACTION_RESET
                        putExtra(TimerService.EXTRA_TIME, selectedType?.seconds ?: 0)
                    }
                    context.startService(intent)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text(text = "Reset")
            }
            
            if (timerState.isAlarming) {
                Button(
                    onClick = {
                        val intent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_STOP_ALARM }
                        context.startService(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Stop Alarm")
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

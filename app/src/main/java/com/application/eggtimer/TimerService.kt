package com.application.eggtimer

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TimerState(
    val timeLeft: Int = 0,
    val totalTime: Int = 0,
    val isRunning: Boolean = false,
    val isAlarming: Boolean = false,
    val alarmUri: Uri? = null
)

class TimerService : Service() {

    private val binder = TimerBinder()
    private var timerJob: Job? = null
    private var ringtone: Ringtone? = null

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val time = intent.getIntExtra(EXTRA_TIME, 0)
                val uriString = intent.getStringExtra(EXTRA_RINGTONE_URI)
                val alarmUri = uriString?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                startTimer(time, alarmUri)
            }
            ACTION_STOP -> stopTimer()
            ACTION_RESET -> {
                val time = intent.getIntExtra(EXTRA_TIME, -1)
                resetTimer(time)
            }
            ACTION_STOP_ALARM -> stopAlarm()
        }
        return START_STICKY
    }

    fun setAppInForeground(inForeground: Boolean) {
        // Keeping notification always visible as per requirement
    }

    private fun startTimer(time: Int, alarmUri: Uri) {
        timerJob?.cancel()
        
        _timerState.value = _timerState.value.copy(
            totalTime = time, 
            timeLeft = time, 
            isRunning = true, 
            isAlarming = false, 
            alarmUri = alarmUri
        )
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (_timerState.value.timeLeft > 0 && _timerState.value.isRunning) {
                delay(1000)
                _timerState.value = _timerState.value.copy(timeLeft = _timerState.value.timeLeft - 1)
                updateNotification()
            }
            if (_timerState.value.isRunning && _timerState.value.timeLeft == 0) {
                _timerState.value = _timerState.value.copy(isRunning = false, isAlarming = true)
                playAlarm()
                updateNotification()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
        updateNotification()
    }

    private fun resetTimer(newTime: Int) {
        timerJob?.cancel()
        stopAlarm()
        val timeToSet = if (newTime != -1) newTime else _timerState.value.totalTime
        _timerState.value = _timerState.value.copy(
            timeLeft = timeToSet, 
            totalTime = timeToSet, 
            isAlarming = false,
            isRunning = false
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun playAlarm() {
        ringtone?.stop()
        val uri = _timerState.value.alarmUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()
    }

    private fun stopAlarm() {
        ringtone?.stop()
        _timerState.value = _timerState.value.copy(isAlarming = false)
        if (_timerState.value.isRunning) {
            updateNotification()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun updateNotification() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, createNotification())
            } catch (e: SecurityException) {
                // Should be covered by checkSelfPermission
            }
        }
    }

    private fun createNotification(): Notification {
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, TimerReceiver::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getBroadcast(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopAlarmIntent = Intent(this, TimerReceiver::class.java).apply { action = ACTION_STOP_ALARM }
        val stopAlarmPendingIntent = PendingIntent.getBroadcast(this, 2, stopAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, "EGG_TIMER_CHANNEL")
            .setContentTitle("Egg Timer")
            .setSmallIcon(R.drawable.soft_boiled_egg) 
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)

        if (_timerState.value.isAlarming) {
            builder.setContentText("Your egg is ready!")
            builder.addAction(R.drawable.soft_boiled_egg, "Stop Alarm", stopAlarmPendingIntent)
        } else {
            builder.setContentText("Time left: ${formatTime(_timerState.value.timeLeft)}")
            if (_timerState.value.isRunning) {
                builder.addAction(R.drawable.soft_boiled_egg, "Stop", stopPendingIntent)
            }
        }

        return builder.build()
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.application.eggtimer.START"
        const val ACTION_STOP = "com.application.eggtimer.STOP"
        const val ACTION_RESET = "com.application.eggtimer.RESET"
        const val ACTION_STOP_ALARM = "com.application.eggtimer.STOP_ALARM"
        const val EXTRA_TIME = "EXTRA_TIME"
        const val EXTRA_RINGTONE_URI = "EXTRA_RINGTONE_URI"
    }
}

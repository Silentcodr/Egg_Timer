package com.application.eggtimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, TimerService::class.java)
        serviceIntent.action = intent.action
        context.startService(serviceIntent)
    }
}
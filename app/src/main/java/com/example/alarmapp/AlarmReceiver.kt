package com.example.alarmapp

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action)
        {
            SNOOZE_ACTION -> snoozeAlarm(context, intent)
            STOP_ACTION -> stopAlarm(context)
            else -> triggerAlarm(context, intent)
        }
    }



    private fun triggerAlarm(context: Context, intent: Intent) {
        val alarmTime = intent.getStringExtra("ALARM_TIME") ?: ""
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI") ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        mp.setAudioStreamType(AudioManager.STREAM_ALARM)
        mp.apply {
            setDataSource(context, Uri.parse(ringtoneUri.toString()))
            prepare()
            start()
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(context, alarmTime)
        notificationManager.notify(NOTIFICATION_ID, notification)

        Handler(Looper.getMainLooper()).postDelayed({
            stopAlarm(context)
        }, 60*1000)
    }



    private fun buildNotification(context: Context, alarmTime: String): android.app.Notification {
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = SNOOZE_ACTION
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, 1, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = STOP_ACTION
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.baseline_alarm_24)
            .setContentTitle("Alarm")
            .setContentText("Your $alarmTime alarm is ringing")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.baseline_snooze_24, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.baseline_alarm_off_24, "Stop", stopPendingIntent)
            .build()
    }



    private fun snoozeAlarm(context: Context, intent: Intent) {
        stopAlarm(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }.timeInMillis

        val snoozeIntent = Intent(context, AlarmReceiver::class.java)
        snoozeIntent.putExtra("ALARM_TIME", "Snoozed")
        snoozeIntent.putExtra("RINGTONE_URI", intent.getStringExtra("RINGTONE_URI"))

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }



    private fun stopAlarm(context: Context) {
        if (mp.isPlaying)
        {
            mp.stop()
            mp.release()
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }



    companion object
    {
        private val mp = MediaPlayer()
    }
}
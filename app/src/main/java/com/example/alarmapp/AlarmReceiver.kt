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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        //CONTEXT REPRESENTS THE CURRENT STATE OF THE APPLICATION.
        //DETERMINES WHAT ACTION WAS RECEIVED AND ACCORDINGLY TAKES THE APPROPRIATE ACTION.
        when (intent.action)
        {
            SNOOZE_ACTION -> {
                snoozeAlarm(context, intent)
                Toast.makeText(context, "Alarm snoozed!", Toast.LENGTH_SHORT).show()
            }
            STOP_ACTION -> {
                stopAlarm(context)
                Toast.makeText(context, "Alarm stopped!", Toast.LENGTH_SHORT).show()
            }
            else -> triggerAlarm(context, intent)
        }
    }




    private fun triggerAlarm(context: Context, intent: Intent) {
        val alarmTime = intent.getStringExtra("ALARM_TIME") ?: ""
        val ringtoneUri = intent.getStringExtra("RINGTONE_URI") ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)


        //ACCESS THE ALARM VOLUME OF THE DEVICE. OTHERWISE THE MEDIA VOLUME WILL BE USED.
        mp.setAudioStreamType(AudioManager.STREAM_ALARM)

        /*APPLIES THE CHOSEN RINGTONE URI OR DEFAULT ALARM RINGTONE URI TO THE MEDIA PLAYER,
        PREPARES AND STARTS THE MEDIA PLAYER. */
        mp.apply {
            setDataSource(context, Uri.parse(ringtoneUri.toString()))
            prepare()
            start()
        }


        //FETCH THE SYSTEM LEVEL NOTIFICATION SERVICE.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //buildNotification() METHOD IS USED TO CONSTRUCT AND RETURN A NOTIFICATION OBJECT.
        val notification = buildNotification(context, alarmTime)

        //DISPLAY THE NOTIFICATION.
        notificationManager.notify(NOTIFICATION_ID, notification)


        //STOP THE ALARM AFTER IT RINGS FOR 60 SECONDS.
        Handler(Looper.getMainLooper()).postDelayed({
            stopAlarm(context)
        }, 60*1000)
    }




    private fun buildNotification(context: Context, alarmTime: String): android.app.Notification {
        //NOTIFY THE "AlarmReceiver" TO PERFORM THE SNOOZE ACTION.
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = SNOOZE_ACTION
        }
        /*PENDING INTENT GRANTS ANOTHER COMPONENT (HERE THE "alarmManager") PERMISSION TO EXECUTE THE "snoozeIntent"
        AT A FUTURE TIME, EVEN WHEN THE APP IS CLOSED. */
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, 1, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = STOP_ACTION
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        /*NOTIFICATION IS DISPLAYED WITH A SNOOZE AND STOP BUTTON.
        CLICKING THE SNOOZE BUTTON WILL MAKE THE PENDING INTENT EXECUTE THE "snoozeIntent"
         */
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
        //FIRST STOP THE ALARM.
        stopAlarm(context)


        //THEN ADD 5 MINUTES TO THE TIME AT WHICH THE SNOOZE BUTTON WAS CLICKED.
        val snoozeTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }.timeInMillis


        //SET THE NEW ALARM.
        val snoozeIntent = Intent(context, AlarmReceiver::class.java)
        snoozeIntent.putExtra("ALARM_TIME", "Snoozed")
        snoozeIntent.putExtra("RINGTONE_URI", intent.getStringExtra("RINGTONE_URI"))

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, snoozeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)

        //val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //notificationManager.cancel(NOTIFICATION_ID)
    }




    private fun stopAlarm(context: Context) {
        //STOP THE MEDIA PLAYER AND RELEASE THE MEMORY SPACE OCCUPIED BY IT.
        if (mp.isPlaying)
        {
            mp.stop()
            mp.release()
        }


        //CANCEL THE ALARM NOTIFICATION.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }




    companion object
    {
        //INITIALIZE THE MEDIA PLAYER AS A COMPANION OBJECT, INITIALIZING IT IN THE CLASS DOESN'T WORK.
        private val mp = MediaPlayer()
    }
}
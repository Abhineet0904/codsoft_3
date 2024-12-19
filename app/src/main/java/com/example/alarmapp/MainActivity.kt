package com.example.alarmapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat

@SuppressLint("NotifyDataSetChanged", "SimpleDateFormat")
@RequiresApi(Build.VERSION_CODES.O, Build.VERSION_CODES.TIRAMISU)
class MainActivity : AppCompatActivity() {
    private lateinit var timeDisplay: TextView
    private lateinit var dateDisplay: TextView

    private val alarms = mutableListOf<Alarm>()
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmList: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var ringtonePicker: ActivityResultLauncher<Intent>
    private lateinit var selectedAlarm: Alarm
    private lateinit var addAlarmButton: FloatingActionButton

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //ASK FOR USER PERMISSION TO SEND NOTIFICATIONS IF THIS PERMISSION HAS NOT BEEN GIVEN
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                ,1
            )
        }

        /*CREATE A NOTIFICATION CHANNEL AT THE TIME OF OPENING THE APP.
        THIS METHOD IS PREFERABLE AND MORE EFFICIENT THAN CREATING THE NOTIFICATION CHANNEL IN THE ALARM RECEIVER,
        SO THAT THE CHANNEL IS NOT CREATED EVERYTIME AN ALARM GOES OFF*/
        createNotification(this)


        timeDisplay = findViewById(R.id.textView1)
        dateDisplay = findViewById(R.id.textView2)
        displayTimeAndDate()                                //DISPLAY CURRENT DATE AND TIME


        displayScheduledAlarms()                            //DISPLAY ALL THE ALARMS THAT HAVE BEEN SCHEDULED BEFORE


        //ACTIVITY LAUNCHER USER TO CHANGE THE RINGTONE AFTER AN ALARM HAS BEEN SET
        ringtonePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null)
            {
                val uri = it.data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri != null)
                {
                    val updatedAlarm = Alarm(
                        selectedAlarm.time,
                        uri.toString(),
                        true
                    )
                    cancelAlarm(selectedAlarm)                        //DISABLE THE OLD ALARM
                    alarms.remove(selectedAlarm)                      //DELETE THE OLD ALARM FROM THE ALARM LIST
                    setAlarm(updatedAlarm)                            //SET THE NEW ALARM WHICH HAS THE USER CHOSEN RINGTONE
                    alarmAdapter.notifyDataSetChanged()               //NOTIFY THE ALARM ADAPTER THAT THERE HAS BEEN SOME CHANGE IN THE RECYCLERVIEW THAT DISPLAYS THE ALARMS
                }
            }
        }


        alarmList = findViewById(R.id.alarmList)
        alarmAdapter = AlarmAdapter(alarms, this) {alarm, action ->
            //HANDLES THE TASKS TO BE PERFORMED WHEN THE USER CLICKS ON ANY OF THE ALARMS
            handleAlarmAction(alarm, action)
        }
        alarmList.layoutManager = LinearLayoutManager(this)
        alarmList.adapter = this.alarmAdapter


        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        addAlarmButton = findViewById(R.id.addAlarm)
        addAlarmButton.setOnClickListener {
            showTimePicker(null)
        }
    }




    private fun createNotification(context: Context) {

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notification for alarm"
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }




    private fun displayTimeAndDate() {
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                val currentTime = Calendar.getInstance().time
                timeDisplay.text = SimpleDateFormat("HH:mm:ss").format(currentTime)

                val currentDate = java.util.Date()
                dateDisplay.text = SimpleDateFormat("EEE, dd MMM yyyy").format(currentDate)

                Handler(Looper.getMainLooper()).postDelayed(this,1000)
            }
        })
    }




    private fun displayScheduledAlarms() {

        //RETRIEVES THE SHARED PREFERENCES
        val sharedPreferences = getSharedPreferences("Alarms scheduled", Context.MODE_PRIVATE)

        //FETCHES THE SAVED ALARMS' JSON STRING FROM THE RETRIEVED SHARED PREFERENCES
        val alarmJSON = sharedPreferences.getString("Alarms", null)

        //CHECKS IF THE ALARM DATA EXISTS, OR THERE IS ATLEAST ONE ALARM STORED IN THE SHARED PREFERENCES
        if (alarmJSON != null)
        {
            /*"Gson" IS A JAVA/KOTLIN LIBRARY DEVELOPED BY GOOGLE, THAT HELPS IN CONVERTING
            JAVA/KOTLIN OBJECTS TO JSON AND VICE-VERSA.
            "TypeToken" IS A HELPER CLASS IN THE "Gson" LIBRARY

             */
            val type = object : TypeToken<MutableList<Alarm>>() {}.type
            alarms.clear()

            //Gson is a Kotlin library that helps in converting Kotlin objects to JSON and vice-versa
            alarms.addAll(Gson().fromJson(alarmJSON, type))
            //THE ALARMS ARE DISPLAYED IN ASCENDING ORDER OF THEIR TIME
            alarms.sortBy { it.time }
        }
    }




    private fun showTimePicker(oldAlarm: Alarm?) {

        val currentTime = Calendar.getInstance().time
        val currentHour = SimpleDateFormat("HH").format(currentTime)
        //val currentMinute = SimpleDateFormat("mm").format(currentTime)

        val timePicker = MaterialTimePicker.Builder()
            .setTitleText("Select time")
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour.toInt()+1)
            //.setMinute(currentMinute.toInt())
            .build()
        timePicker.show(supportFragmentManager,"alarmPicker")

        timePicker.addOnPositiveButtonClickListener {

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            /*IF THIS FUNCTION HAS BEEN CALLED WHEN USER IS SETTING A NEW ALARM, THEN ELSE BLOCK IS EXECUTED.
            IF THIS FUNCTION HAS BEEN CALLED WHEN THE USER IS CHANGING THE TIME OF AN ALARM,
            THEN THE OLD ALARM IS DISABLED AND DELETED, AND A NEW ALARM WITH THE SPECIFIED CHANGES IS SET*/
            if (oldAlarm != null)
            {
                val updatedAlarm = Alarm(
                    calendar.timeInMillis,
                    oldAlarm.ringtone,
                    oldAlarm.isEnabled
                )
                setAlarm(updatedAlarm)
                cancelAlarm(oldAlarm)
                alarms.remove(oldAlarm)
                alarmAdapter.notifyDataSetChanged()
                updatePreferences()
            }
            else
            {
                val alarm = Alarm(
                    calendar.timeInMillis,
                    RingtoneManager.getDefaultUri((RingtoneManager.TYPE_ALARM)).toString(),
                    true
                )
                setAlarm(alarm)
            }
        }
    }




    private fun setAlarm(alarm: Alarm) {

        /*IF THE TIME FOR WHICH YOU ARE SETTING THE ALARM HAS ALREADY PASSED,
        IT SETS THE ALARM FOT THAT TIME BUT FOR THE NEXT DAY*/
        if (alarm.time < System.currentTimeMillis()) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = alarm.time

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            alarm.time = calendar.timeInMillis

        }

        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("ALARM_TIME", SimpleDateFormat("HH:mm").format(alarm.time).toString())
        intent.putExtra("RINGTONE_URI", alarm.ringtone)

        val pendingIntent = PendingIntent.getBroadcast(this, alarm.time.toInt(),
            intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.time, pendingIntent)
        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarm.time, 24*60*60*1000, pendingIntent)
        //alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarm.time, 24*60*60*1000, pendingIntent)

        //AFTER SETTING THE ALARM, SAVE IT AND DISPLAY IT IN THE RECYCLERVIEW
        saveAlarms(alarm)

        Toast.makeText(this, "Alarm set for ${SimpleDateFormat("HH:mm")
            .format(alarm.time)}", Toast.LENGTH_SHORT).show()
    }




    private fun saveAlarms(alarm: Alarm) {
        alarms.add(alarm)
        updatePreferences()
        alarms.sortBy { it.time }
        alarmAdapter.notifyDataSetChanged()
    }




    private fun updatePreferences() {
        val sharedPreferences = getSharedPreferences("Alarms scheduled", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val alarmJSON = Gson().toJson(alarms)
        editor.putString("Alarms", alarmJSON)
        editor.apply()
    }




    private fun handleAlarmAction(alarm: Alarm, action: AlarmAction) {

        when (action) {
            AlarmAction.CHANGE_TIME ->
            {
                showTimePicker(alarm)
            }
            AlarmAction.CHANGE_RINGTONE ->
            {
                selectedAlarm = alarm
                val ringtoneIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                }
                ringtonePicker.launch(ringtoneIntent)

            }
            AlarmAction.TOGGLE ->
            {
                /*WHEN USER SWITCHES THE TOGGLE ON/OFF,
                UPDATE THE "isEnabled" ATTRIBUTE OF THE "Alarm" CLASS TO THE OPPOSITE OF IT'S ORIGINAL VALUE*/
                alarm.isEnabled = !alarm.isEnabled
                if (alarm.isEnabled)
                {
                    alarms.remove(alarm)
                    setAlarm(alarm)
                }
                else
                {
                    cancelAlarm(alarm)
                }
                updatePreferences()
            }
            AlarmAction.DELETE ->
            {
                cancelAlarm(alarm)
                alarms.remove(alarm)
                alarmAdapter.notifyDataSetChanged()
                updatePreferences()
            }
        }
    }




    private fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.time.toInt(),
            intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }
}
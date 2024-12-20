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

        //ASK FOR USER PERMISSION TO SEND NOTIFICATIONS IF THIS PERMISSION HAS NOT BEEN GIVEN.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                ,1
            )
        }

        /*CREATE A NOTIFICATION CHANNEL AT THE TIME OF OPENING THE APP.
        HERE THE CHANNEL IS NOT CREATED EVERYTIME AN ALARM GOES OFF.
        THIS METHOD IS PREFERABLE AND MORE EFFICIENT THAN CREATING THE NOTIFICATION CHANNEL IN THE ALARM RECEIVER,
        IN WHICH THE NOTIFICATION CHANNEL WILL BE CREATED EVERYTIME AN ALARM GOES OFF. */
        createNotification(this)


        timeDisplay = findViewById(R.id.textView1)
        dateDisplay = findViewById(R.id.textView2)
        displayTimeAndDate()


        //DISPLAYS ALL THE ALARMS THAT HAVE BEEN SCHEDULED BEFORE.
        displayScheduledAlarms()


        //ACTIVITY LAUNCHER USER TO CHANGE THE RINGTONE AFTER AN ALARM HAS BEEN SET.
        ringtonePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null)
            {
                val uri = it.data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri != null)
                {
                    /*CREATE A NEW ALARM WITH SAME TIME AND STATUS, BUT THE RINGTONE WILL BE
                    THE ONE THAT WAS JUST CHOSEN BY THE USER. */
                    val updatedAlarm = Alarm(
                        selectedAlarm.time,
                        uri.toString(),
                        true
                    )

                    //DISABLE THE OLD ALARM.
                    cancelAlarm(selectedAlarm)

                    //DELETE THE OLD ALARM FROM THE ALARM LIST.
                    alarms.remove(selectedAlarm)

                    //SET THE NEW ALARM WHICH HAS THE RINGTONE CHOSEN BY THE USER.
                    setAlarm(updatedAlarm)

                    //NOTIFY THE ALARM ADAPTER THAT THERE HAS BEEN A CHANGE IN THE RECYCLERVIEW THAT DISPLAYS ALL THE SCHEDULED ALARMS.
                    alarmAdapter.notifyDataSetChanged()
                }
            }
        }


        alarmList = findViewById(R.id.alarmList)
        alarmAdapter = AlarmAdapter(alarms, this) {alarm, action ->
            //HANDLES THE TASKS TO BE PERFORMED WHEN THE USER CLICKS ON ANY OF THE ALARMS.
            handleAlarmAction(alarm, action)
        }
        alarmList.layoutManager = LinearLayoutManager(this)
        alarmList.adapter = this.alarmAdapter


        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        addAlarmButton = findViewById(R.id.addAlarm)
        addAlarmButton.setOnClickListener {
            showTimePicker(null)
            //ELSE BLOCK OF "showTimePicker()" METHOD WILL BE EXECUTED AFTER THE USER PICKS A TIME.
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

        //DISPLAY THE CURRENT DATE AND TIME, AND UPDATES IT EVERY SECOND OR EVERY 1000 MILLISECONDS.
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

        /*RETRIEVES THE SHARED PREFERENCES. "Context.MODE_PRIVATE" ENSURES THAT THIS PREFERENCE FILE
        REMAINS PRIVATE TO THIS APP AND CANNOT BE ACCESSED BY OTHER APPS. */
        val sharedPreferences = getSharedPreferences("Alarms scheduled", Context.MODE_PRIVATE)


        //FETCHES THE SAVED ALARM'S JSON STRING FROM THE RETRIEVED SHARED PREFERENCES.
        val alarmJSON = sharedPreferences.getString("Alarms", null)


        //CHECKS IF THE ALARM DATA EXISTS, OR THERE IS ATLEAST ONE ALARM STORED IN THE SHARED PREFERENCES.
        if (alarmJSON != null)
        {
            /*"Gson" IS A JAVA/KOTLIN LIBRARY DEVELOPED BY GOOGLE, THAT HELPS IN CONVERTING
            JAVA/KOTLIN OBJECTS TO JSON AND VICE-VERSA.

            "TypeToken" IS A HELPER CLASS IN THE "Gson" LIBRARY. Gson IS UNABLE TO HANDLE GENERIC TYPES LIKE
            "MutableList<Alarm>", SO TypeToken IS USED TO PROVIDE NECESSARY TYPE INFORMATION TO Gson AT RUNTIME.

            THE "type" VARIABLE WILL HOLD THE TYPE OBJECT CORRESPONDING TO THE MutableList<Alarm>. THIS IS
            NECESSARY FOR Gson TO CORRECTLY DESERIALIZE THE JSON STRING INTO A LIST OF "Alarm" OBJECTS.

            "object : TypeToken<MutableList<Alarm>>() {}" CREATES AN ANONYMOUS OBJECT.
            ".type PROVIDES A Type OBJECT." */
            val type = object : TypeToken<MutableList<Alarm>>() {}.type


            //CLEAR THE "Alarm" LIST TO ENSURE THAT IT DOESN'T CONTAIN ANY UNINTENDED OR UPDATED ALARM DATA
            alarms.clear()


            /*"Gson().fromJSON(alarmJSON, type))" IS USED TO CONVERT THE "alarmJSON" STRING INTO AN OBJECT SPECIFIED
            BY THE "type" VARIABLE, i.e. "MutableList<Alarm>". THIS PROCESS IS CALLED DESERIALIZATION.
            "Gson().fromJson(alarmJSON, MutableList<Alarm>)" WILL NOT WORK AT RUNTIME.

            THE DESERIALIZED LIST OF ALARMS IS THEN ADDED TO THE "alarms" LIST.*/
            alarms.addAll(Gson().fromJson(alarmJSON, type))


            //THE ALARMS ARE DISPLAYED IN ASCENDING ORDER OF THEIR TIME.
            alarms.sortBy { it.time }
        }
    }




    private fun showTimePicker(oldAlarm: Alarm?) {

        val currentTime = Calendar.getInstance().time
        val currentHour = SimpleDateFormat("HH").format(currentTime)

        val timePicker = MaterialTimePicker.Builder()
            .setTitleText("Select time")
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour.toInt()+1)
            .build()
        timePicker.show(supportFragmentManager,"alarmPicker")

        timePicker.addOnPositiveButtonClickListener {

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timePicker.hour)
                set(Calendar.MINUTE, timePicker.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            /*IF THE "showTimePicker()" METHOD HAS BEEN CALLED WHEN USER IS SETTING A NEW ALARM,
            THEN THE ELSE BLOCK IS EXECUTED.
            IF THIS FUNCTION HAS BEEN CALLED WHEN THE USER IS CHANGING THE TIME OF AN ALARM,
            THEN THE OLD ALARM IS DISABLED AND DELETED, AND A NEW ALARM WITH THE UPDATED TIME IS SET. */
            if (oldAlarm != null)
            {
                val updatedAlarm = Alarm(
                    calendar.timeInMillis,
                    oldAlarm.ringtone,
                    oldAlarm.isEnabled
                )
                cancelAlarm(oldAlarm)
                alarms.remove(oldAlarm)
                setAlarm(updatedAlarm)
                //alarmAdapter.notifyDataSetChanged()
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
        IT SETS THE ALARM FOT THAT TIME BUT FOR THE NEXT DAY. */
        if (alarm.time < System.currentTimeMillis()) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = alarm.time

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            alarm.time = calendar.timeInMillis

        }

        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("ALARM_TIME", SimpleDateFormat("HH:mm").format(alarm.time).toString())
        intent.putExtra("RINGTONE_URI", alarm.ringtone)


        /*PENDING INTENT GRANTS ANOTHER COMPONENT (HERE THE "alarmManager") PERMISSION TO EXECUTE THE "intent"
        AT A FUTURE TIME, EVEN WHEN THE APP IS CLOSED.
        "PendingIntent.getBroadcast()" IS USED TO SEND A BROADCAST.
        "alarm.time.toInt()" ACTS LIKE A UNIQUE REQUEST CODE THAT IDENTIFIES THE PENDING INTENT. IT IS
        REQUIRED TO DISTINGUISH BETWEEN DIFFERENT ALARMS.
        THE "intent" WILL BE EXECUTED WHEN THE "pendingIntent" IS TRIGGERED.
        "PendingIntent.FLAG_IMMUTABLE" MEANS THE pendingIntent CANNOT BE MODIFIED AFTER CREATION.
         */
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.time.toInt(),
            intent, PendingIntent.FLAG_IMMUTABLE)


        /*"alarmManager" IS AN INSTANCE OF THE ALARM MANAGER, WHICH IS USED TO SCHEDULE OPERATIONS AT A SPECIFIC TIME.
        "setExactAndAllowWhileIdle()" SCHEDULES THE ALARM TO GO OFF AT THE EXACT SPECIFIED TIME, EVEN IF THE DEVICE
        IS IDLE OR HAS LOW POWER.
        "AlarmManager.RTC_WAKEUP" TRIGGERS THE ALARM BASED ON THE REAL TIME CLOCK AND WAKES THE DEVICE IF IT IS
        ASLEEP AT THE TIME OF THE ALARM GOING OFF.
        "alarm.time" IS THE TIME IN MILLISECONDS AT WHICH THE "pendingIntent" SHOULD TRIGGERS.
         */
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.time, pendingIntent)


        //AFTER SETTING THE ALARM, SAVE IT AND DISPLAY IT IN THE RECYCLERVIEW.
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

        //RETRIEVES THE SHARED PREFERENCES OF THE NAME "Alarms Scheduled".
        val sharedPreferences = getSharedPreferences("Alarms scheduled", Context.MODE_PRIVATE)

        //RETURNS AN OBJECT THAT ALLOWS US TO MODIFY THE CONTENTS OF THE SHARED PREFERENCES FILE.
        val editor = sharedPreferences.edit()

        //"Gson().toJson(alarms)" CONVERTS THE "alarms" list into a JSON STRING.
        val alarmJSON = Gson().toJson(alarms)

        //THE JSON STRING IS STORED IN THE SHARED PREFERENCES FILE USING THE "Alarms" key.
        editor.putString("Alarms", alarmJSON)
        editor.apply()
    }




    private fun handleAlarmAction(alarm: Alarm, action: AlarmAction) {

        when (action) {
            AlarmAction.CHANGE_TIME ->
            {
                showTimePicker(alarm)
                //IF BLOCK OF "showTimePicker()" METHOD WILL BE EXECUTED AFTER THE USER PICKS A TIME.
            }
            AlarmAction.CHANGE_RINGTONE ->
            {
                selectedAlarm = alarm
                val ringtoneIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {

                    //ENSURE THAT ONLY THE ALARM TONES ARE DISPLAYED IN THE PICKER.
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")

                    //ENSURE THAT RINGTONES WITH VALID URI CAN ONLY BE PICKED.
                    putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, RingtoneManager.getValidRingtoneUri(this@MainActivity))
                }
                ringtonePicker.launch(ringtoneIntent)

            }
            AlarmAction.TOGGLE ->
            {
                /*WHEN USER SWITCHES THE TOGGLE ON/OFF,
                UPDATE THE "isEnabled" ATTRIBUTE OF THE "Alarm" OBJECT TO THE OPPOSITE OF IT'S ORIGINAL VALUE. */
                alarm.isEnabled = !alarm.isEnabled
                if (alarm.isEnabled)
                {
                    //IF A DISABLED ALARM IS ENABLED AGAIN, DELETE IT AND SET IT AGAIN.
                    alarms.remove(alarm)
                    setAlarm(alarm)
                }
                else
                {
                    //DISABLE THE ALARM.
                    cancelAlarm(alarm)
                    updatePreferences()
                }

                /*WHETHER THE ALARM IS ENABLED OR DISABLED, THE SHARED PREFERENCES THAT STORES
                THESE ALARMS ARE UPDATED WITH THAT ALARM'S NEW STATUS. */
            }
            AlarmAction.DELETE ->
            {
                cancelAlarm(alarm)
                alarms.remove(alarm)
                updatePreferences()
                alarmAdapter.notifyDataSetChanged()
            }
        }
    }




    private fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(this, AlarmReceiver::class.java)

        /*CREATES A PENDING INTENT THAT MATCHES WITH THE PENDING INTENT OF THAT ALARM,
        BY USING SAME ID ("alarm.time.toInt()") FOR THIS PENDING INTENT AS THE OLD ONE. */
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.time.toInt(),
            intent, PendingIntent.FLAG_IMMUTABLE)

        //CANCELS THE ALARM'S PENDING INTENT.
        alarmManager.cancel(pendingIntent)
    }
}

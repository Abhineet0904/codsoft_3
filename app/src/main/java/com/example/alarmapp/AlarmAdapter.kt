package com.example.alarmapp

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.DateFormat
import android.media.RingtoneManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("UseSwitchCompatOrMaterialCode", "SimpleDateFormat")
class AlarmAdapter (
    private var alarms: MutableList<Alarm>,
    private val context: Context,
    private val onAlarmAction: (Alarm, AlarmAction) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {


    //BINDS INDIVIDUAL ALARM DATA TO THE RESPECTIVE ITEM IN THE RECYCLERVIEW.
    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val alarmTime: TextView = itemView.findViewById(R.id.alarmItem)
        private val alarmRingtone: TextView = itemView.findViewById(R.id.alarmRingtone)
        private val alarmSwitch: Switch = itemView.findViewById(R.id.alarmState)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.alarmDelete)


        //TAKES AN "Alarm" OBJECT AND POPULATE THE UI ELEMENTS OF A SINGLE RECYCLERVIEW ITEM.
        fun bind(alarm: Alarm)
        {
            //FORMATS THE "time" PROPERTY OF THE "Alarm" OBJECT INTO THE 24 HOUR TIME FORMAT.
            alarmTime.text = SimpleDateFormat("HH:mm").format(Date(alarm.time))


            //CONVERTS THE RINGTONE URI STRING INTO THE RINGTONE TITLE.
            alarmRingtone.text = RingtoneManager.getRingtone(context, Uri.parse(alarm.ringtone)).getTitle(context)


            //MAKES THE SWITCH REFLECT THE ALARM STATUS (THE "isEnabled" PROPERTY OF THE "Alarm" OBJECT).
            alarmSwitch.isChecked = alarm.isEnabled


            //CALLS THE "onAlarmAction()" CALLBACK WITH "CHANGE_TIME" ACTION WHEN THE USER CLICKS THE ALARM TIME.
            alarmTime.setOnClickListener {
                onAlarmAction(alarm, AlarmAction.CHANGE_TIME)
            }


            //TRIGGERS WHEN THE USER CLICKS THE RINGTONE OF THE ALARM.
            alarmRingtone.setOnClickListener {
                onAlarmAction(alarm, AlarmAction.CHANGE_RINGTONE)
            }


            //TRIGGERS WHEN THE ALARM STATE IS TOGGLED.
            alarmSwitch.setOnCheckedChangeListener { _, _ ->
                onAlarmAction(alarm, AlarmAction.TOGGLE)
            }


            //TRIGGERS WHEN THE DELETE BUTTON IS CLICKED.
            deleteButton.setOnClickListener {
                onAlarmAction(alarm,AlarmAction.DELETE)
            }
        }
    }



    /*CREATES AND INITIALIZES A "ViewHolder" OBJECT FOR EACH RECYCLERVIEW ITEM, AND
    INFLATES THE "item_alarm" LAYOUT FILE FOR DEFINING THE UI OF EACH RECYCLERVIEW ITEM. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent,false)
        return AlarmViewHolder(view)
    }



    //RETURNS THE TOTAL NO. OF ALARMS IN THE LIST.
    override fun getItemCount(): Int {
        return alarms.size
    }



    //PASSES THE "Alarm" OBJECT AT THE CURRENT POSITION TO THE "AlarmViewHolder" FOR BINDING.
    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.bind(alarm)
    }
}
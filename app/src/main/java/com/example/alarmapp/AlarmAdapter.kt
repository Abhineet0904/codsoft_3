package com.example.alarmapp

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
import java.util.Date

class AlarmAdapter (private var alarms: MutableList<Alarm>,
    private val context: Context,
    private val onAlarmAction: (Alarm, AlarmAction) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {


    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarmTime: TextView = itemView.findViewById(R.id.alarmItem)
        val alarmRingtone: TextView = itemView.findViewById(R.id.alarmRingtone)
        val alarmSwitch: Switch = itemView.findViewById(R.id.alarmState)
        val deleteButton: ImageButton = itemView.findViewById(R.id.alarmDelete)


        fun bind(alarm: Alarm)
        {
            alarmTime.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(alarm.time))
            alarmRingtone.text = RingtoneManager.getRingtone(context, Uri.parse(alarm.ringtone)).getTitle(context)
            alarmSwitch.isChecked = alarm.isEnabled

            alarmTime.setOnClickListener {
                onAlarmAction(alarm, AlarmAction.CHANGE_TIME)
            }


            alarmRingtone.setOnClickListener {
                onAlarmAction(alarm, AlarmAction.CHANGE_RINGTONE)
            }


            alarmSwitch.setOnCheckedChangeListener { _, _ ->
                onAlarmAction(alarm, AlarmAction.TOGGLE)
            }


            deleteButton.setOnClickListener {
                onAlarmAction(alarm,AlarmAction.DELETE)
            }
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent,false)
        return AlarmViewHolder(view)
    }



    override fun getItemCount(): Int {
        return alarms.size
    }



    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.bind(alarm)
    }
}
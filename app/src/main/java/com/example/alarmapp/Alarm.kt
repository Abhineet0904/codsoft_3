package com.example.alarmapp

data class Alarm(
    var time: Long,
    var ringtone: String,
    var isEnabled: Boolean
)
package ru.itmo.dws.calendar.service.notification.model

data class EmailNotificationPayload(
    val to: String,
    val subject: String,
    val body: String,
) : NotificationPayload

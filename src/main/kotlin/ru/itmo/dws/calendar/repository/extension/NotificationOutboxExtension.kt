package ru.itmo.dws.calendar.repository.extension

import ru.itmo.dws.calendar.model.NotificationOutbox

interface NotificationOutboxExtension {
    fun insert(notificationOutbox: NotificationOutbox): Long
    fun update(notificationOutbox: NotificationOutbox): Long
}

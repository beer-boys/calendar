package ru.itmo.dws.calendar.provider

@Suppress("ForbiddenComment")
interface CalendarProvider {

    // todo replace with own model
    fun getCalendars(
        username: String,
    ): Map<String, String>

    fun getCalendarById(
        username: String,
        calendarId: String,
    ): String

    fun getEventsByCalendarId(
        username: String,
        calendarId: String,
        timeMin: java.time.ZonedDateTime? = null,
        timeMax: java.time.ZonedDateTime? = null
    ): String

    fun getEventByEventIdAndCalendarId(
        username: String,
        calendarId: String,
        eventId: String
    ): String
}

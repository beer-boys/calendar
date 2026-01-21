package ru.itmo.dws.calendar.dto.google

import com.google.api.client.util.DateTime

data class CreateEventRequest(
    val start: EventDateTime,
    val end: EventDateTime,
    val anyoneCanAddSelf: Boolean?,
    val attachments: List<FileUrl>?,
    val attendees: List<Attendee>?,
    val birthdayProperties: BirthdayProperties?,
    val colorId: String?,
    val description: String?,
    // 'birthday' | 'default' | 'focusTime' | 'fromGmail' | 'outOfOffice' | 'workingLocation'
    val eventType: String?,
    val gadget: Gadget?,
    val guestsCanInviteOthers: Boolean?,
    val guestsCanModify: Boolean?,
    val guestsCanSeeOtherGuests: Boolean?,
    // characters allowed in the ID are those used in base32hex encoding, i.e. lowercase letters a-v and digits 0-9, see section 3.1.2 in RFC2938
    // the length of the ID must be between 5 and 1024 characters
    // the ID must be unique per calendar
    val id: String?,
    val location: String?,
    val originationStartTime: EventDateTime?,
    val recurrence: List<String>?,
    val reminders: Reminders?,
    val sequence: Int?,
    val source: Source?,
    // 'confirmed' | 'tentative' | 'cancelled'
    val status: String?,
    val summary: String?,
    // 'opaque' | 'transparent'
    val transparency: String?,
    // 'default' | 'public' | 'private' | 'confidential'
    val visibility: String?,
    val workingLocationProperties: WorkingLocationProperties?
)

data class EventDateTime(
    val date: DateTime?,
    val dateTime: DateTime?,
    val timeZone: String
)

fun EventDateTime.toGoogleEventDateTime(): com.google.api.services.calendar.model.EventDateTime {
    return com.google.api.services.calendar.model.EventDateTime()
        .setDate(this.date)
        .setDateTime(this.dateTime)
        .setTimeZone(this.timeZone)
}

data class FileUrl(
    val fileUrl: String,
)

data class Attendee(
    val additionalGuests: Int?,
    val comment: String?,
    val displayName: String?,
    val email: String,
    val optional: Boolean?,
    val resource: Boolean?,
    // 'needsAction' | 'declined' | 'tentative' | 'accepted'
    val responseStatus: String?,
)

data class BirthdayProperties(
    // 'anniversary' | 'birthday' | 'custom' | 'other' | 'self'
    val type: String,
)

data class Gadget(
    val display: String,
    val height: String,
    val iconLink: String,
    val link: String,
)

data class Reminders(
    val useDefault: Boolean,
    val overrides: List<RemindersInner>,
)

data class RemindersInner(
    val method: String,
    val minutes: Int,
)

data class Source(
    val title: String,
    val url: String,
    val workingLocationProperties: WorkingLocationProperties,
)

data class WorkingLocationProperties(
    val customLocation: WorkingLocationCustomLocation,
    val homeOffice: Any,
    val workingLocationOfficeLocation: WorkingLocationOfficeLocation
)

data class WorkingLocationCustomLocation(
    val label: String,
)

data class WorkingLocationOfficeLocation(
    val buildingId: String,
    val deskId: String,
    val floorId: String,
    val floorSectionId: String,
    val label: String,
)

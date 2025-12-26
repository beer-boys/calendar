package ru.itmo.dws.calendar.core.domain.valueobject.room

enum class RoomFeature {
    PROJECTOR,
    WHITEBOARD,
    VIDEO_CONFERENCING,
    TV,
    SPEAKERPHONE,
    HDMI,
    AIR_CONDITIONING,
    ;

    companion object {
        fun fromString(value: String): RoomFeature? {
            return entries.firstOrNull { value.uppercase() == it.name }
        }
    }
}

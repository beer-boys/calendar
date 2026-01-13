package ru.itmo.dws.calendar.model.types

data class JsonbString(val value: String) {
    init {
        require(value.isNotBlank()) { "JsonbString cannot be blank (use '{}')" }
    }
    companion object {
        val EMPTY = JsonbString("{}")
    }
}

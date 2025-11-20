package ru.itmo.dws.calendar.exception

class RegisterFailException(
    val login: String
) : RuntimeException("Пользователь с логином $login уже существует")
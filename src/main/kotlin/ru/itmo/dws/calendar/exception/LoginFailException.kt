package ru.itmo.dws.calendar.exception

class LoginFailException(
    login: String
) : RuntimeException("Не удалось авторизовать пользователя с логином $login")

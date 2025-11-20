package ru.itmo.dws.calendar.service

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ExampleService {

    @GetMapping
    fun test(): String {
        return "test"
    }
}
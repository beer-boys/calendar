package ru.itmo.dws.calendar.service.notification.email

import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(private val mailSender: JavaMailSenderImpl) {

    @Suppress("UseCheckOrError")
    private val from: String = mailSender.javaMailProperties["from"] as? String
        ?: throw IllegalStateException("smtp server login not set")

    fun sendEmail(to: String, subject: String, body: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setFrom(from)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(body, true)

        mailSender.send(message)
    }
}

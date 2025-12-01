package ru.itmo.dws.calendar.core.service

data class RescheduleProposalConfig(
    val maxAlternativeDays: Int = 3,
    val maxProposedSlots: Int = 5,
    val proposalExpirationSeconds: Long = 3600
) {
    init {
        require(maxAlternativeDays > 0) { "maxAlternativeDays must be positive" }
        require(maxProposedSlots > 0) { "maxProposedSlots must be positive" }
        require(proposalExpirationSeconds > 0) { "proposalExpirationSeconds must be positive" }
    }

    companion object {
        fun default(): RescheduleProposalConfig = RescheduleProposalConfig()
    }
}

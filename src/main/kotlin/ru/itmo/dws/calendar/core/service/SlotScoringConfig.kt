package ru.itmo.dws.calendar.core.service

import java.time.Duration

data class SlotScoringConfig(
    val baseScore: Int = 100,
    val preferredTimeDeviationThresholds: PreferredTimeDeviationThresholds = PreferredTimeDeviationThresholds(),
    val windowBoundaryThreshold: Double = 0.1,
    val windowBoundaryPenalty: Int = 10,
    val minimumBreakDuration: Duration = Duration.ofMinutes(15),
    val shortBreakPenalty: Int = 10
) {
    data class PreferredTimeDeviationThresholds(
        val majorDeviationMinutes: Long = 120,
        val majorDeviationPenalty: Int = 30,
        val moderateDeviationMinutes: Long = 60,
        val moderateDeviationPenalty: Int = 15,
        val minorDeviationMinutes: Long = 30,
        val minorDeviationPenalty: Int = 5
    )

    companion object {
        fun default(): SlotScoringConfig = SlotScoringConfig()
    }
}

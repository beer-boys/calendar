package ru.itmo.dws.calendar.core.port.input

import ru.itmo.dws.calendar.core.domain.model.HabitConflict
import ru.itmo.dws.calendar.core.domain.model.HabitRescheduleProposal
import ru.itmo.dws.calendar.core.domain.model.HabitRescheduleResult
import ru.itmo.dws.calendar.core.domain.model.UserRescheduleDecision
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface HabitRescheduleProposalUseCase {

    fun generateProposal(conflict: HabitConflict): HabitRescheduleProposal

    fun applyDecision(proposalId: String, decision: UserRescheduleDecision): HabitRescheduleResult

    fun getActiveProposals(userId: UserId): List<HabitRescheduleProposal>

    fun getProposal(proposalId: String): HabitRescheduleProposal?

    fun cancelProposal(proposalId: String)
}

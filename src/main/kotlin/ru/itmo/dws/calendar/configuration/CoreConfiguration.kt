package ru.itmo.dws.calendar.configuration

import java.time.ZoneId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.itmo.dws.calendar.core.port.input.HabitManagementUseCase
import ru.itmo.dws.calendar.core.port.output.FocusTimeRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.port.output.MeetingRepository
import ru.itmo.dws.calendar.core.service.ConflictDetectionService
import ru.itmo.dws.calendar.core.service.EventSlotFinder
import ru.itmo.dws.calendar.core.service.HabitManagementService
import ru.itmo.dws.calendar.core.service.provider.FocusTimeEventProvider
import ru.itmo.dws.calendar.core.service.provider.HabitEventProvider
import ru.itmo.dws.calendar.core.service.provider.MeetingEventProvider
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider

@Configuration
class CoreConfiguration {

    @Bean
    fun eventSlotFinder(): EventSlotFinder {
        return EventSlotFinder()
    }

    @Bean
    fun habitEventProvider(habitRepository: HabitRepository): HabitEventProvider {
        return HabitEventProvider(habitRepository)
    }

    @Bean
    fun meetingEventProvider(meetingRepository: MeetingRepository): MeetingEventProvider {
        return MeetingEventProvider(meetingRepository)
    }

    @Bean
    fun focusTimeEventProvider(focusTimeRepository: FocusTimeRepository): FocusTimeEventProvider {
        return FocusTimeEventProvider(focusTimeRepository)
    }

    @Bean
    fun eventProviders(
        habitEventProvider: HabitEventProvider,
        meetingEventProvider: MeetingEventProvider,
        focusTimeEventProvider: FocusTimeEventProvider
    ): List<SchedulableEventProvider> {
        return listOf(habitEventProvider, meetingEventProvider, focusTimeEventProvider)
    }

    @Bean
    fun conflictDetectionService(
        eventProviders: List<SchedulableEventProvider>
    ): ConflictDetectionService {
        return ConflictDetectionService(eventProviders)
    }

    @Bean
    fun habitManagementUseCase(
        habitRepository: HabitRepository,
        eventProviders: List<SchedulableEventProvider>,
        eventSlotFinder: EventSlotFinder,
        conflictDetectionService: ConflictDetectionService
    ): HabitManagementUseCase {
        return HabitManagementService(
            habitRepository = habitRepository,
            eventProviders = eventProviders,
            eventSlotFinder = eventSlotFinder,
            conflictDetectionService = conflictDetectionService,
            zoneId = ZoneId.systemDefault()
        )
    }
}

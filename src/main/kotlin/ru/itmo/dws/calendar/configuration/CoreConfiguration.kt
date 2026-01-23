package ru.itmo.dws.calendar.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.ZoneId
import java.util.Optional
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.itmo.dws.calendar.configuration.properties.HabitHorizonProperties
import ru.itmo.dws.calendar.core.port.input.CalendarFeedUseCase
import ru.itmo.dws.calendar.core.port.input.HabitManagementUseCase
import ru.itmo.dws.calendar.core.port.output.CalendarProvider
import ru.itmo.dws.calendar.core.port.output.FocusTimeRepository
import ru.itmo.dws.calendar.core.port.output.HabitOccurrenceRepository
import ru.itmo.dws.calendar.core.port.output.HabitRepository
import ru.itmo.dws.calendar.core.port.output.InternalEventProvider
import ru.itmo.dws.calendar.core.port.output.MeetingRepository
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomBookingProvider
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomProvider
import ru.itmo.dws.calendar.core.service.ConflictDetectionService
import ru.itmo.dws.calendar.core.service.EventSlotFinder
import ru.itmo.dws.calendar.core.service.HabitHorizonExtensionService
import ru.itmo.dws.calendar.core.service.HabitManagementService
import ru.itmo.dws.calendar.core.service.HabitOccurrenceConflictResolutionService
import ru.itmo.dws.calendar.core.service.HabitSchedulingService
import ru.itmo.dws.calendar.core.service.HabitSyncService
import ru.itmo.dws.calendar.core.service.MeetingRoomService
import ru.itmo.dws.calendar.core.service.feed.CalendarFeedService
import ru.itmo.dws.calendar.core.service.feed.HabitOccurrenceEventProvider
import ru.itmo.dws.calendar.core.service.provider.DatabaseMeetingRoomBookingProvider
import ru.itmo.dws.calendar.core.service.provider.DatabaseMeetingRoomProvider
import ru.itmo.dws.calendar.core.service.provider.FocusTimeEventProvider
import ru.itmo.dws.calendar.core.service.provider.HabitEventProvider
import ru.itmo.dws.calendar.core.service.provider.HabitOccurrenceSchedulableEventProvider
import ru.itmo.dws.calendar.core.service.provider.MeetingEventProvider
import ru.itmo.dws.calendar.core.service.provider.SchedulableEventProvider
import ru.itmo.dws.calendar.repository.MeetingRoomBookingRepository
import ru.itmo.dws.calendar.repository.MeetingRoomRepository
import ru.itmo.dws.calendar.service.util.ClockService

@Configuration
@Suppress("TooManyFunctions")
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
    fun habitOccurrenceSchedulableEventProvider(
        habitOccurrenceRepository: HabitOccurrenceRepository,
        habitRepository: HabitRepository
    ): HabitOccurrenceSchedulableEventProvider {
        return HabitOccurrenceSchedulableEventProvider(habitOccurrenceRepository, habitRepository)
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
        habitOccurrenceSchedulableEventProvider: HabitOccurrenceSchedulableEventProvider,
        meetingEventProvider: MeetingEventProvider,
        focusTimeEventProvider: FocusTimeEventProvider
    ): List<SchedulableEventProvider> {
        return listOf(habitOccurrenceSchedulableEventProvider, meetingEventProvider, focusTimeEventProvider)
    }

    @Bean
    fun conflictDetectionService(
        eventProviders: List<SchedulableEventProvider>,
        calendarProvider: Optional<CalendarProvider>
    ): ConflictDetectionService {
        return ConflictDetectionService(eventProviders, calendarProvider.orElse(null))
    }

    @Bean
    fun habitSchedulingService(
        eventProviders: List<SchedulableEventProvider>,
        eventSlotFinder: EventSlotFinder,
        calendarProvider: Optional<CalendarProvider>
    ): HabitSchedulingService {
        return HabitSchedulingService(
            eventProviders = eventProviders,
            eventSlotFinder = eventSlotFinder,
            calendarProvider = calendarProvider.orElse(null),
            zoneId = ZoneId.systemDefault()
        )
    }

    @Bean
    fun habitSyncService(
        occurrenceRepository: HabitOccurrenceRepository,
        calendarProvider: Optional<CalendarProvider>
    ): HabitSyncService {
        return HabitSyncService(
            occurrenceRepository = occurrenceRepository,
            calendarProvider = calendarProvider.orElse(null)
        )
    }

    @Bean
    fun habitManagementUseCase(
        habitRepository: HabitRepository,
        occurrenceRepository: HabitOccurrenceRepository,
        eventProviders: List<SchedulableEventProvider>,
        eventSlotFinder: EventSlotFinder,
        conflictDetectionService: ConflictDetectionService,
        habitSchedulingService: HabitSchedulingService,
        habitSyncService: HabitSyncService
    ): HabitManagementUseCase {
        return HabitManagementService(
            habitRepository = habitRepository,
            occurrenceRepository = occurrenceRepository,
            eventProviders = eventProviders,
            eventSlotFinder = eventSlotFinder,
            conflictDetectionService = conflictDetectionService,
            habitSchedulingService = habitSchedulingService,
            habitSyncService = habitSyncService,
            zoneId = ZoneId.systemDefault()
        )
    }

    @Bean
    fun habitHorizonExtensionService(
        habitRepository: HabitRepository,
        occurrenceRepository: HabitOccurrenceRepository,
        habitSyncService: HabitSyncService,
        eventProviders: List<SchedulableEventProvider>,
        eventSlotFinder: EventSlotFinder,
        habitHorizonProperties: HabitHorizonProperties
    ): HabitHorizonExtensionService {
        return HabitHorizonExtensionService(
            habitRepository = habitRepository,
            occurrenceRepository = occurrenceRepository,
            habitSyncService = habitSyncService,
            eventProviders = eventProviders,
            eventSlotFinder = eventSlotFinder,
            horizonWeeks = habitHorizonProperties.planningWeeks,
            zoneId = ZoneId.systemDefault()
        )
    }

    @Bean
    fun habitOccurrenceEventProvider(
        habitRepository: HabitRepository,
        occurrenceRepository: HabitOccurrenceRepository
    ): HabitOccurrenceEventProvider {
        return HabitOccurrenceEventProvider(
            habitRepository = habitRepository,
            occurrenceRepository = occurrenceRepository,
            zoneId = ZoneId.systemDefault()
        )
    }

    @Bean
    fun internalEventProviders(
        habitOccurrenceEventProvider: HabitOccurrenceEventProvider
    ): List<InternalEventProvider> {
        return listOf(habitOccurrenceEventProvider)
    }

    @Bean
    fun calendarFeedUseCase(
        calendarProvider: Optional<CalendarProvider>,
        internalEventProviders: List<InternalEventProvider>
    ): CalendarFeedUseCase {
        return CalendarFeedService(
            calendarProvider = calendarProvider.orElse(null),
            eventProviders = internalEventProviders
        )
    }

    @Bean
    fun meetingRoomProvider(
        repository: MeetingRoomRepository,
        objectMapper: ObjectMapper,
    ): MeetingRoomProvider {
        return DatabaseMeetingRoomProvider(repository, objectMapper)
    }

    @Bean
    fun meetingRoomBookingProvider(
        repository: MeetingRoomBookingRepository,
        meetingRoomProvider: MeetingRoomProvider,
    ): MeetingRoomBookingProvider {
        return DatabaseMeetingRoomBookingProvider(repository, meetingRoomProvider)
    }

    @Bean
    fun meetingRoomService(
        meetingRoomProvider: MeetingRoomProvider,
        meetingRoomBookingProvider: MeetingRoomBookingProvider,
        clockService: ClockService,
    ): MeetingRoomService {
        return MeetingRoomService(
            meetingRoomProvider,
            meetingRoomBookingProvider,
            slotStep = Duration.ofMinutes(15),
            defaultZone = clockService.offset()
        )
    }

    @Bean
    fun habitOccurrenceConflictResolutionService(
        habitOccurrenceRepository: HabitOccurrenceRepository,
        habitRepository: HabitRepository,
        conflictDetectionService: ConflictDetectionService,
        eventSlotFinder: EventSlotFinder,
        habitSyncService: HabitSyncService,
        eventProviders: List<SchedulableEventProvider>
    ): HabitOccurrenceConflictResolutionService {
        return HabitOccurrenceConflictResolutionService(
            habitOccurrenceRepository = habitOccurrenceRepository,
            habitRepository = habitRepository,
            conflictDetectionService = conflictDetectionService,
            eventSlotFinder = eventSlotFinder,
            habitSyncService = habitSyncService,
            eventProviders = eventProviders,
            zoneId = ZoneId.systemDefault()
        )
    }
}

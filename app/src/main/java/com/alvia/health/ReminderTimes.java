package com.alvia.health;
import java.time.*;
import java.util.*;

final class ReminderTimes {
    static long[] forAppointment(long time, ZoneId zone) {
        ZonedDateTime appointment = Instant.ofEpochMilli(time).atZone(zone);
        // Calendar day reminders stay on the requested day even for early appointments.
        ZonedDateTime sameDay = appointment.toLocalDate().atTime(8,0).atZone(zone);
        if (!sameDay.isBefore(appointment)) sameDay = appointment.minusMinutes(30);
        if (!sameDay.toLocalDate().equals(appointment.toLocalDate())) sameDay = appointment;
        return new long[]{appointment.toLocalDate().minusDays(1).atTime(18,0).atZone(zone).toInstant().toEpochMilli(), sameDay.toInstant().toEpochMilli(), time-3600000L};
    }
}

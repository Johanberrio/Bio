package com.alvia.health;
import java.time.*;
public class ReminderTimesTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        ZoneId zone=ZoneId.of("America/Bogota");
        ZonedDateTime a=ZonedDateTime.of(2026,10,2,14,30,0,0,zone);
        long[] times=ReminderTimes.forAppointment(a.toInstant().toEpochMilli(),zone);
        check(Instant.ofEpochMilli(times[0]).atZone(zone).equals(a.minusDays(1).withHour(18).withMinute(0)),"Previous calendar day at 18:00");
        check(Instant.ofEpochMilli(times[1]).atZone(zone).equals(a.withHour(8).withMinute(0)),"Same day at 08:00");
        check(times[2]==a.minusHours(1).toInstant().toEpochMilli(),"One hour before");
        ZonedDateTime early=a.withHour(0).withMinute(10);
        check(Instant.ofEpochMilli(ReminderTimes.forAppointment(early.toInstant().toEpochMilli(),zone)[1]).atZone(zone).toLocalDate().equals(early.toLocalDate()),"Early appointment same-day reminder stays on same day");
        ZoneId dst=ZoneId.of("America/New_York");
        ZonedDateTime transition=ZonedDateTime.of(2026,3,8,14,0,0,0,dst);
        check(Instant.ofEpochMilli(ReminderTimes.forAppointment(transition.toInstant().toEpochMilli(),dst)[0]).atZone(dst).getHour()==18,"DST reminder retains calendar hour");
        System.out.println("PASS: five reminder scheduling checks");
    }
}

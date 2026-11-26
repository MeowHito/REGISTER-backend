package com.actionth.membership.utils;

import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AgeGroupUtilsRaceAgeTest {

    private static final ZoneOffset BKK = ZoneOffset.ofHours(7);

    private static OffsetDateTime d(int y, int m, int day) {
        return OffsetDateTime.of(y, m, day, 0, 0, 0, 0, BKK);
    }

    private static Event event(String method, OffsetDateTime eventDate) {
        Event e = new Event();
        e.setAgeCalcMethod(method);
        e.setEventDate(eventDate);
        return e;
    }

    @Test
    void birthYearCountsTheRaceYear() {
        Event e = event("BIRTH_YEAR", d(2027, 1, 10));
        assertEquals(27, AgeGroupUtils.raceAge(d(2000, 12, 31), e, null, d(2026, 11, 26)));
    }

    @Test
    void exactDateCountsCompletedYearsOnRaceDay() {
        Event e = event("EXACT_DATE", d(2027, 1, 10));
        assertEquals(26, AgeGroupUtils.raceAge(d(2000, 12, 31), e, null, d(2026, 11, 26)));
        assertEquals(27, AgeGroupUtils.raceAge(d(2000, 1, 10), e, null, d(2026, 11, 26)));
        assertEquals(26, AgeGroupUtils.raceAge(d(2000, 1, 11), e, null, d(2026, 11, 26)));
    }

    @Test
    void distanceDateWinsOverEventDate() {
        Event e = event("BIRTH_YEAR", d(2026, 12, 30));
        EventType et = new EventType();
        et.setEventDate(d(2027, 1, 2));
        assertEquals(27, AgeGroupUtils.raceAge(d(2000, 6, 1), e, et, d(2026, 11, 26)));
    }

    @Test
    void unsetMethodKeepsTheOriginalRegistrationYearRule() {
        Event e = event(null, d(2027, 1, 10));
        assertEquals(26, AgeGroupUtils.raceAge(d(2000, 12, 31), e, null, d(2026, 11, 26)));
        // birth date stored as UTC midnight-ish still reads as the Bangkok calendar date
        assertEquals(26, AgeGroupUtils.raceAge(OffsetDateTime.of(1999, 12, 31, 18, 0, 0, 0, ZoneOffset.UTC), e, null, d(2026, 11, 26)));
    }

    @Test
    void normalizeRejectsUnknownValues() {
        assertEquals("EXACT_DATE", AgeGroupUtils.normalizeAgeCalcMethod(" exact_date "));
        assertNull(AgeGroupUtils.normalizeAgeCalcMethod("SOMETHING"));
        assertNull(AgeGroupUtils.normalizeAgeCalcMethod(null));
    }
}

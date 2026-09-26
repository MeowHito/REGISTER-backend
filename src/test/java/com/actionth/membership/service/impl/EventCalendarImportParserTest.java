package com.actionth.membership.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.actionth.membership.service.impl.EventCalendarImportServiceImpl.ParsedPage;

/** Pure-parsing checks against the markup EventON renders on joggingandrunning.com. */
class EventCalendarImportParserTest {

    private static final String PAGE = "<script type=\"application/ld+json\">{\"@context\": \"http://schema.org\",\"@type\": \"Event\","
            + "\"name\": \"บ่อน้ำร้อนฟันรัน\",\"startDate\": \"2026-10-25T12:47+7:00\",\"endDate\": \"2026-10-25T12:47+7:00\"}</script>"
            + "<p class=\"desc_trig_outter\"><a data-gmap_status=\"null\" data-exlk=\"1\"  target=\"_blank\" rel=\"noopener noreferrer\""
            + "style=\"background-color: #4bb5d8;\" id=\"evc_1\" class=\"desc_trig gmaponload sin_val evcal_list_a\" data-ux_val=\"2\""
            + " href=\"https://script.google.com/macros/s/abc/exec\" target=\"_blank\"  ><span class='evoet_title evcal_event_title'>บ่อน้ำร้อนฟันรัน</span>"
            + "<span class='evcal_event_subtitle' >📌บ่อน้ำร้อนทุ่งนุ้ย จ.สตูล    🏃‍♀️‍➡️ประเภท   5.5/10.5    👥ผู้จัด    บ้านทุ่งนุ้ย    📞เบอร์โทร. 081-234-5678</span>"
            + "</a></p>";

    @Test
    void parsesDateSubtitleAndRegistrationLink() {
        ParsedPage p = EventCalendarImportServiceImpl.parsePage(PAGE);
        assertEquals(LocalDate.of(2026, 10, 25), p.eventDate);
        assertEquals("บ่อน้ำร้อนทุ่งนุ้ย จ.สตูล", p.location);
        assertEquals("5.5/10.5", p.distance);
        assertEquals("บ้านทุ่งนุ้ย", p.organizer);
        assertEquals("081-234-5678", p.phone);
        assertEquals("https://script.google.com/macros/s/abc/exec", p.externalLink);
    }

    @Test
    void subtitleWithoutPhoneOrOrganizer() {
        ParsedPage p = new ParsedPage();
        EventCalendarImportServiceImpl.parseSubtitle("📌ณ สวนหลวง ร.9  ประตู 4   🏃‍♀️‍➡️ประเภท   5/10   👥ผู้จัด   บริษัท มดยักษ์ใหญ่ จำกัด    📞เบอร์โทร.", p);
        assertEquals("ณ สวนหลวง ร.9 ประตู 4", p.location);
        assertEquals("5/10", p.distance);
        assertEquals("บริษัท มดยักษ์ใหญ่ จำกัด", p.organizer);
        assertEquals("", p.phone);
    }

    @Test
    void guessesEventTypeFromDistances() {
        assertEquals("Mini Marathon", EventCalendarImportServiceImpl.guessEventType("5/10", List.of("งานวิ่ง")));
        assertEquals("Marathon", EventCalendarImportServiceImpl.guessEventType("10.5 / 21.1 / 42.195", List.of()));
        assertEquals("Half Marathon", EventCalendarImportServiceImpl.guessEventType("21K", List.of()));
        assertEquals("Fun Run", EventCalendarImportServiceImpl.guessEventType("3.5", List.of()));
        assertEquals("Trail", EventCalendarImportServiceImpl.guessEventType("12/25", List.of("งานวิ่งเทรล")));
        assertNull(EventCalendarImportServiceImpl.guessEventType("", List.of("งานวิ่ง")));
    }

    @Test
    void appendsFullBangkokNameForProvinceFilter() {
        List<String> provinces = List.of("กรุงเทพมหานคร", "สตูล");
        assertEquals("สวนลุมพินี กรุงเทพฯ (กรุงเทพมหานคร)",
                EventCalendarImportServiceImpl.normaliseLocation("สวนลุมพินี กรุงเทพฯ", provinces));
        assertEquals("บ่อน้ำร้อน จ.สตูล", EventCalendarImportServiceImpl.normaliseLocation("บ่อน้ำร้อน จ.สตูล", provinces));
    }

    @Test
    void extraDetailKeepsDistancesOnly() {
        ParsedPage p = new ParsedPage();
        p.distance = "5/10";
        p.organizer = "บริษัท มดยักษ์ใหญ่ จำกัด";
        p.phone = "081-234-5678";
        assertEquals("ประเภท: 5/10 · งานวิ่งฟรี",
                EventCalendarImportServiceImpl.buildExtraDetail(p, List.of("งานวิ่งฟรี")));
    }

    @Test
    void parsesLooseIsoAndGmt() {
        assertEquals(LocalDate.of(2027, 1, 31), EventCalendarImportServiceImpl.parseLooseDate("2027-1-31T13:17+7:00"));
        assertEquals("2026-09-25T12:49:47Z", EventCalendarImportServiceImpl.parseGmt("2026-09-25T12:49:47").toString());
    }
}

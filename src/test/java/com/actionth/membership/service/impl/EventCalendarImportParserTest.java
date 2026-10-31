package com.actionth.membership.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.actionth.membership.service.impl.EventCalendarImportServiceImpl.Card;

/** Pure-parsing checks against the markup of patrunning.com's monthly calendar pages. */
class EventCalendarImportParserTest {

    private static final String PAGE = "<body class=\"pr-next pr-next--month\">"
            + "<details class=\"dayblock dayblock--fold dayblock--ongoing\" id=\"d-ongoing\"><summary><h2>เริ่มก่อนหน้านี้</h2></summary>"
            + "<div class=\"pr-cards pr-cards--2\"><article class=\"pr-card pr-card--row is-text is-free\"><div class=\"pr-card__body\">"
            + "<h3 class=\"pr-card__title\"><a href=\"https://patrunning.com/events/%e0%b8%8b%e0%b8%b4%e0%b8%95%e0%b8%b5%e0%b9%89%e0%b8%a3%e0%b8%b1%e0%b8%99/\">"
            + "ซิตี้รัน มันส์ ฟัน เว่อร์ (ซีซั่น 5) Presented by TOYOTA (🎁สมัครฟรี! ไม่มีค่าใช้จ่าย) 📢เปิดรับสมัคร วันที่ 26 ตุลาคม 2569</a></h3>"
            + "<div class=\"pr-card__row\"><span class=\"pr-chip-sm\">ซิตี้รัน</span></div>"
            + "<div class=\"pr-card__meta\"><span>31 ต.ค. – 13 ธ.ค. 2026</span><span aria-hidden=\"true\">·</span><strong>กรุงเทพมหานคร</strong>"
            + "<span class=\"pr-card__venue\">ทุกเสาร์-อาทิตย์  Week1 - Week4</span></div>"
            + "<div class=\"pr-card__cta\"><a class=\"pr-btn pr-btn--sm\" href=\"https://www.facebook.com/amazingthailandcityrun\" target=\"_blank\">สมัคร</a></div>"
            + "</div></article></div></details>"
            + "<section class=\"dayblock\" id=\"d-2026-12-05\" aria-labelledby=\"d-2026-12-05-h\"><h2>วันเสาร์ที่ 5 ธ.ค. 2026 <small>2 งาน</small></h2>"
            + "<div class=\"pr-cards pr-cards--2\"><article class=\"pr-card pr-card--row is-text\"><div class=\"pr-card__body\">"
            + "<h3 class=\"pr-card__title\"><a href=\"https://patrunning.com/events/shanghai-marathon-2026-2/\">Shanghai Marathon 2026</a></h3>"
            + "<div class=\"pr-card__row\"><span class=\"pr-chip-sm\">5K</span><span class=\"pr-chip-sm\">42.195K</span></div>"
            + "<div class=\"pr-card__meta\"><span>5–6 ธ.ค. 2026</span><span aria-hidden=\"true\">·</span><strong>China</strong>"
            + "<span class=\"pr-card__venue\">Jing&#039;an District, Shanghai, China</span></div>"
            + "<div class=\"pr-card__cta\"><a class=\"pr-btn pr-btn--sm\" href=\"https://www.ahotu.com/event/shanghai-marathon\" target=\"_blank\">สมัคร</a></div>"
            + "</div></article>"
            + "<article class=\"pr-card pr-card--row is-text\"><div class=\"pr-card__body\">"
            + "<h3 class=\"pr-card__title\"><a href=\"https://patrunning.com/events/buriram-marathon-2027/\">Buriram Marathon 2027</a></h3>"
            + "<div class=\"pr-card__row\"><span class=\"pr-chip-sm\">10K</span></div>"
            + "<div class=\"pr-card__meta\"><span>5 ธ.ค. 2026</span><span aria-hidden=\"true\">·</span><strong>บุรีรัมย์</strong>"
            + "<span class=\"pr-card__venue\">ณ สนามช้างอารีนา จ.บุรีรัมย์</span></div>"
            + "<div class=\"pr-card__cta\"><a class=\"pr-btn pr-btn--sm\" href=\"https://patrunning.com/hotel-ad/26981/\" target=\"_blank\">สมัคร</a></div>"
            + "</div></article></div></section></body>";

    @Test
    void parsesCardsWithDayBlockDates() {
        List<Card> cards = EventCalendarImportServiceImpl.parseMonth(PAGE);
        assertEquals(3, cards.size());

        Card shanghai = cards.get(1);
        assertEquals("shanghai-marathon-2026-2", shanghai.sourceId);
        assertEquals("Shanghai Marathon 2026", shanghai.title);
        assertEquals(LocalDate.of(2026, 12, 5), shanghai.eventDate);
        assertEquals(List.of("5K", "42.195K"), shanghai.distances);
        assertEquals("China", shanghai.province);
        assertEquals("Jing'an District, Shanghai, China", shanghai.venue);
        assertEquals("https://www.ahotu.com/event/shanghai-marathon", shanghai.registrationLink);
        assertFalse(shanghai.free);
    }

    @Test
    void ongoingCardTakesItsStartDateFromTheText() {
        Card cityRun = EventCalendarImportServiceImpl.parseMonth(PAGE).get(0);
        assertEquals("ซิตี้รัน", cityRun.sourceId);
        assertEquals("ซิตี้รัน มันส์ ฟัน เว่อร์ (ซีซั่น 5) Presented by TOYOTA", cityRun.title);
        assertEquals(LocalDate.of(2026, 10, 31), cityRun.eventDate);
        assertTrue(cityRun.free);
        assertEquals("ประเภท: ซิตี้รัน · งานวิ่งฟรี", EventCalendarImportServiceImpl.buildExtraDetail(cityRun));
    }

    @Test
    void ignoresLinksBackIntoTheSourceSite() {
        Card buriram = EventCalendarImportServiceImpl.parseMonth(PAGE).get(2);
        assertNull(buriram.registrationLink);
        assertEquals("https://patrunning.com/events/buriram-marathon-2027/", buriram.pageUrl);
    }

    @Test
    void parsesThaiDateLines() {
        assertEquals(LocalDate.of(2026, 12, 5), EventCalendarImportServiceImpl.parseThaiStartDate("5 ธ.ค. 2026"));
        assertEquals(LocalDate.of(2026, 12, 4), EventCalendarImportServiceImpl.parseThaiStartDate("4–6 ธ.ค. 2026"));
        assertEquals(LocalDate.of(2026, 6, 6), EventCalendarImportServiceImpl.parseThaiStartDate("6 มิ.ย. – 8 พ.ย. 2026"));
        assertEquals(LocalDate.of(2026, 7, 1),
                EventCalendarImportServiceImpl.parseThaiStartDate("1 ก.ค. 2026 – 31 ธ.ค. 2027"));
        assertEquals(LocalDate.of(2026, 12, 30), EventCalendarImportServiceImpl.parseThaiStartDate("30 ธ.ค. – 2 ม.ค. 2027"));
        assertEquals(LocalDate.of(2026, 3, 1), EventCalendarImportServiceImpl.parseThaiStartDate("1 มี.ค. 2569"));
        assertNull(EventCalendarImportServiceImpl.parseThaiStartDate("เร็วๆ นี้"));
    }

    @Test
    void guessesEventTypeFromDistances() {
        assertEquals("Mini Marathon", EventCalendarImportServiceImpl.guessEventType("5K / 10K", List.of()));
        assertEquals("Marathon", EventCalendarImportServiceImpl.guessEventType("10.5K / 21.1K / 42.195K", List.of()));
        assertEquals("Half Marathon", EventCalendarImportServiceImpl.guessEventType("21K", List.of()));
        assertEquals("Fun Run", EventCalendarImportServiceImpl.guessEventType("3.5K", List.of()));
        assertEquals("Trail", EventCalendarImportServiceImpl.guessEventType("12K / 25K", List.of("เทรล")));
        assertNull(EventCalendarImportServiceImpl.guessEventType("ซิตี้รัน", List.of()));
    }

    @Test
    void locationCarriesTheProvinceForTheFilter() {
        List<String> provinces = List.of("กรุงเทพมหานคร", "บุรีรัมย์", "ลำพูน");
        assertEquals("สวนรถไฟ · กรุงเทพมหานคร",
                EventCalendarImportServiceImpl.buildLocation("สวนรถไฟ", "กรุงเทพมหานคร", provinces));
        assertEquals("ณ เทศบาลตำบลก้อ อำเภอลี้ จังหวัดลำพูน",
                EventCalendarImportServiceImpl.buildLocation("ณ เทศบาลตำบลก้อ อำเภอลี้ จังหวัดลำพูน", "ลำพูน", provinces));
        assertEquals("บุรีรัมย์", EventCalendarImportServiceImpl.buildLocation(null, "บุรีรัมย์", provinces));
        assertEquals("สวนลุมพินี กรุงเทพฯ (กรุงเทพมหานคร)",
                EventCalendarImportServiceImpl.normaliseLocation("สวนลุมพินี กรุงเทพฯ", provinces));
    }

    @Test
    void titleDropsPromoNotes() {
        assertEquals("Nan EcoTrail สวรรค์บนดอย STAGE1",
                EventCalendarImportServiceImpl.cleanTitle("Nan Eco​Trail​ สวรรค์​บนดอย STAGE1(🎁วิ่งเทรลฟรี !) 📢เปิดรับสมัคร"));
        assertEquals("KRAM & CRAFT",
                EventCalendarImportServiceImpl.cleanTitle("KRAM &amp; CRAFT (🎁สมัครฟรี!) 📢เปิดรับสมัคร"));
        assertEquals("Pong Yaeng Trail 2026 ปีที่ 12", EventCalendarImportServiceImpl.cleanTitle("Pong Yaeng Trail 2026 ปีที่ 12"));
    }

    @Test
    void longThaiUrlsAreStoredDecodedInsteadOfCut() {
        String encoded = "https://patrunning.com/events/" + "%e0%b8%a7%e0%b8%b4%e0%b9%88%e0%b8%87".repeat(12) + "/";
        String fitted = EventCalendarImportServiceImpl.fitUrl(encoded, null);
        assertEquals("https://patrunning.com/events/" + "วิ่ง".repeat(12) + "/", fitted);
        assertEquals("https://x.th/a", EventCalendarImportServiceImpl.fitUrl(null, "https://x.th/a"));
    }
}

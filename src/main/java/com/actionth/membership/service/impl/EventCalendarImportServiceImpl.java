package com.actionth.membership.service.impl;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.actionth.membership.constant.NotificationType;
import com.actionth.membership.dto.EventCalendarImportResult;
import com.actionth.membership.dto.EventCalendarImportStatus;
import com.actionth.membership.model.AppConfig;
import com.actionth.membership.model.EventCalendar;
import com.actionth.membership.repository.AppConfigRepository;
import com.actionth.membership.repository.EventCalendarRepository;
import com.actionth.membership.repository.MasterDataRepository;
import com.actionth.membership.service.EventCalendarImportService;
import com.actionth.membership.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Imports races from patrunning.com's monthly calendar ({@code /ปฏิทินวิ่งรายเดือน/yyyy-MM/}).
 *
 * <p>Each month page server-renders every race of that month as a card: title + link to the
 * race's page on patrunning (whose slug is our {@code sourceId}), distance chips, a date line,
 * province, venue, the organizer's registration link and an {@code is-free} class. Cards sit in
 * {@code <section class="dayblock" id="d-yyyy-MM-dd">} blocks; races that started in an earlier
 * month sit in a {@code d-ongoing} block and carry their dates only as text. Trail races are
 * not marked on the card, so the {@code ?pr_type=trail-run} filter of each month is read too.
 *
 * <p>A run reads every month from this one to the horizon (two requests a month), then upserts
 * by (source, sourceId). Rows go live at once; an admin's later reject is never overwritten.
 * When every page was read, rows of this source inside the window that the source no longer
 * lists (cancelled or merged races) are removed.
 */
@Slf4j
@Service
public class EventCalendarImportServiceImpl implements EventCalendarImportService {

    static final String CFG_LAST_RUN = "eventCalendarImport.lastRun";
    static final String CFG_HORIZON = "eventCalendarImport.horizonMonths";
    static final int MIN_HORIZON = 1;
    static final int MAX_HORIZON = 36;

    private static final ZoneId SOURCE_ZONE = ZoneId.of("Asia/Bangkok");
    private static final String MONTH_PATH = "ปฏิทินวิ่งรายเดือน";
    private static final int COLUMN_MAX = 255;

    private static final Pattern P_BLOCK = Pattern
            .compile("<(?:section|details) class=\"dayblock[^\"]*\" id=\"d-([\\w-]+)\"");
    private static final Pattern P_ARTICLE = Pattern.compile("<article class=\"pr-card([^\"]*)\">(.*?)</article>",
            Pattern.DOTALL);
    private static final Pattern P_TITLE = Pattern
            .compile("class=\"pr-card__title\"><a href=\"([^\"]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern P_CHIP = Pattern.compile("class=\"pr-chip-sm\">(.*?)</span>", Pattern.DOTALL);
    private static final Pattern P_META = Pattern.compile("class=\"pr-card__meta\">(.*?)</div>", Pattern.DOTALL);
    private static final Pattern P_META_DATE = Pattern.compile("^<span>(.*?)</span>", Pattern.DOTALL);
    private static final Pattern P_META_PROVINCE = Pattern.compile("<strong>(.*?)</strong>", Pattern.DOTALL);
    private static final Pattern P_META_VENUE = Pattern.compile("class=\"pr-card__venue\">(.*?)</span>",
            Pattern.DOTALL);
    private static final Pattern P_CTA = Pattern.compile("class=\"pr-card__cta\"><a[^>]*href=\"([^\"]+)\"");
    private static final Pattern P_EVENT_SLUG = Pattern.compile("/events/([^/?#]+)");
    private static final Pattern P_DAY_ID = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    /** One end of a date line: "31", "31 ต.ค.", "31 ต.ค. 2026". */
    private static final Pattern P_DATE_PART = Pattern
            .compile("(\\d{1,2})\\s*([\\u0E00-\\u0E7F]{1,3}\\.[\\u0E00-\\u0E7F]{1,2}\\.)?\\s*(\\d{4})?");
    private static final String[] THAI_MONTHS = { "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.",
            "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค." };
    private static final Pattern P_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern P_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern P_WS = Pattern.compile("\\s+");

    private final EventCalendarRepository eventCalendarRepository;
    private final AppConfigRepository appConfigRepository;
    private final MasterDataRepository masterDataRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final RestTemplate http;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    /** Counters of the run in progress, read by the status endpoint while the job works. */
    private volatile EventCalendarImportResult currentRun;

    @Value("${app.event-calendar-import.enabled:true}")
    private boolean enabled;
    @Value("${app.event-calendar-import.source:patrunning.com}")
    private String source;
    @Value("${app.event-calendar-import.base-url:https://patrunning.com}")
    private String baseUrl;
    /** Default horizon (months ahead) until an admin picks one in the back office. */
    @Value("${app.event-calendar-import.horizon-months:12}")
    private int defaultHorizonMonths;
    /** Pause between requests to the source site. */
    @Value("${app.event-calendar-import.request-delay-ms:400}")
    private long requestDelayMs;
    /** Upper bound of races saved per run, a guard against a page that suddenly lists far too much. */
    @Value("${app.event-calendar-import.max-events-per-run:2000}")
    private int maxEventsPerRun;

    public EventCalendarImportServiceImpl(EventCalendarRepository eventCalendarRepository,
            AppConfigRepository appConfigRepository, MasterDataRepository masterDataRepository,
            NotificationService notificationService, ObjectMapper objectMapper, RestTemplateBuilder builder) {
        this.eventCalendarRepository = eventCalendarRepository;
        this.appConfigRepository = appConfigRepository;
        this.masterDataRepository = masterDataRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.http = builder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(30))
                .defaultHeader(HttpHeaders.USER_AGENT, "ActionRegisterCalendarSync/1.0 (+https://action.in.th)")
                .build();
    }

    // ------------------------------------------------------------------ status

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean requestStop() {
        if (!running.get()) {
            return false;
        }
        stopRequested.set(true);
        log.info("EventCalendar import: stop requested by an admin");
        return true;
    }

    @Override
    public EventCalendarImportStatus getStatus() {
        return EventCalendarImportStatus.builder()
                .enabled(enabled)
                .running(running.get())
                .stopping(running.get() && stopRequested.get())
                .horizonMonths(readHorizon())
                .source(source)
                .sourceUrl(baseUrl)
                .lastRun(readLastRun().orElse(null))
                .currentRun(running.get() ? currentRun : null)
                .pendingCount(eventCalendarRepository.countBySourceAndIsApprovedIsNull(source))
                .build();
    }

    // ------------------------------------------------------------------ sync

    @Override
    public EventCalendarImportResult sync() {
        return sync(null, false);
    }

    @Override
    public EventCalendarImportResult sync(Integer horizonMonths, boolean clearFirst) {
        if (!enabled) {
            throw new IllegalStateException("การดึงปฏิทินงานวิ่งถูกปิดไว้ (app.event-calendar-import.enabled=false)");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("กำลังดึงข้อมูลอยู่ กรุณารอให้รอบก่อนหน้าเสร็จก่อน");
        }
        int horizon = clampHorizon(horizonMonths == null ? readHorizon() : horizonMonths);
        EventCalendarImportResult result = EventCalendarImportResult.builder()
                .mode("MONTHLY")
                .startedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .horizonMonths(horizon)
                .build();
        currentRun = result;
        stopRequested.set(false);
        try {
            if (horizonMonths != null) {
                writeConfig(CFG_HORIZON, String.valueOf(horizon));
            }
            if (clearFirst) {
                long removed = clearImportedRows();
                result.setCleared((int) Math.min(removed, Integer.MAX_VALUE));
            }
            log.info("EventCalendar import started: source={} horizon={}m clearFirst={}", source, horizon,
                    clearFirst);
            runImport(horizon, result);
        } catch (Exception e) {
            log.error("EventCalendar import aborted", e);
            result.setError(abbreviate(e.getClass().getSimpleName() + ": " + e.getMessage(), 80));
        } finally {
            result.setFinishedAt(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            result.setStopped(stopRequested.getAndSet(false));
            writeLastRun(result);
            currentRun = null;
            running.set(false);
        }
        log.info("EventCalendar import finished: {}", result);
        if (result.getCreated() > 0) {
            notificationService.notifyAdmins(NotificationType.EVENT_CALENDAR_IMPORTED,
                    "ดึงปฏิทินงานวิ่งจาก " + source,
                    "มีงานใหม่ " + result.getCreated() + " รายการขึ้นปฏิทินแล้ว",
                    "/eventCalendarList");
        }
        return result;
    }

    @Override
    public long clearImported() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("กำลังดึงข้อมูลอยู่ กรุณารอให้รอบก่อนหน้าเสร็จก่อน");
        }
        try {
            return clearImportedRows();
        } finally {
            running.set(false);
        }
    }

    /** Every imported row, whichever site it came from, so rows of a retired source go too. */
    private long clearImportedRows() {
        long removed = eventCalendarRepository.deleteImported();
        log.info("EventCalendar import: cleared {} imported rows", removed);
        return removed;
    }

    private void runImport(int horizonMonths, EventCalendarImportResult result) throws InterruptedException {
        List<String> provinces = loadProvinceNames();
        YearMonth first = YearMonth.now(SOURCE_ZONE);
        YearMonth last = YearMonth.from(LocalDate.now(SOURCE_ZONE).plusMonths(horizonMonths));
        LocalDate windowStart = first.atDay(1);
        LocalDate windowEnd = last.atEndOfMonth();

        // 1. Read every month first: the total is known before saving starts, and rows the source
        // dropped may only be removed when no page was missed.
        Map<String, Card> cards = new LinkedHashMap<>();
        boolean complete = true;
        for (YearMonth ym = first; !ym.isAfter(last) && !stopRequested.get(); ym = ym.plusMonths(1)) {
            try {
                List<Card> month = parseMonth(fetch(monthUri(ym, null)));
                Thread.sleep(requestDelayMs);
                Set<String> trail = new HashSet<>();
                parseMonth(fetch(monthUri(ym, "trail-run"))).forEach(c -> trail.add(c.sourceId));
                Thread.sleep(requestDelayMs);
                for (Card c : month) {
                    c.trail = trail.contains(c.sourceId);
                    // A race spanning months is listed again as "ongoing"; its first sighting wins.
                    cards.putIfAbsent(c.sourceId, c);
                }
                log.info("EventCalendar import: {} lists {} races ({} trail)", ym, month.size(), trail.size());
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                complete = false;
                result.setFailed(result.getFailed() + 1);
                log.warn("EventCalendar import: month {} failed: {}", ym, e.toString());
            }
        }
        if (stopRequested.get()) {
            complete = false;
        }

        List<Card> inWindow = cards.values().stream()
                .filter(c -> c.eventDate != null && !c.eventDate.isBefore(windowStart)
                        && !c.eventDate.isAfter(windowEnd))
                .limit(maxEventsPerRun)
                .toList();
        result.setSkipped(cards.size() - inWindow.size());
        result.setTotal(inWindow.size());

        // 2. Upsert.
        OffsetDateTime seenAt = OffsetDateTime.now();
        Set<String> seen = new HashSet<>();
        for (Card card : inWindow) {
            if (stopRequested.get()) {
                complete = false;
                break;
            }
            result.setListed(result.getListed() + 1);
            seen.add(card.sourceId);
            try {
                upsert(card, provinces, seenAt, result);
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                log.warn("EventCalendar import: race {} failed: {}", card.sourceId, e.toString());
            }
        }

        // 3. Drop what the source no longer lists in the window we just read in full.
        if (complete && !inWindow.isEmpty()) {
            List<EventCalendar> gone = eventCalendarRepository
                    .findBySourceAndEventDateBetween(source, windowStart.atStartOfDay(SOURCE_ZONE).toOffsetDateTime(),
                            windowEnd.plusDays(1).atStartOfDay(SOURCE_ZONE).toOffsetDateTime().minusNanos(1))
                    .stream()
                    .filter(row -> !seen.contains(row.getSourceId()))
                    .toList();
            if (!gone.isEmpty()) {
                eventCalendarRepository.deleteAll(gone);
                result.setRemoved(gone.size());
                log.info("EventCalendar import: removed {} races the source no longer lists", gone.size());
            }
        }
    }

    private void upsert(Card card, List<String> provinces, OffsetDateTime seenAt, EventCalendarImportResult result) {
        Optional<EventCalendar> existing = eventCalendarRepository.findBySourceAndSourceId(source, card.sourceId);
        EventCalendar row = existing.orElseGet(() -> EventCalendar.builder()
                .source(source)
                .sourceId(card.sourceId)
                // The source is curated by its own team, so its races go straight onto the calendar;
                // an admin can still reject one, and that decision survives later runs.
                .isApproved(true)
                .build());
        String before = fingerprint(row);
        row.setEventName(abbreviate(card.title, COLUMN_MAX));
        row.setEventDate(card.eventDate.atStartOfDay(SOURCE_ZONE).toOffsetDateTime());
        row.setLocation(abbreviate(buildLocation(card.venue, card.province, provinces), COLUMN_MAX));
        row.setEventType(guessEventType(String.join(" / ", card.distances),
                card.trail ? List.of("เทรล") : List.of()));
        row.setExtraDetail(abbreviate(buildExtraDetail(card), COLUMN_MAX));
        row.setLink(fitUrl(card.registrationLink, card.pageUrl));
        // Submitter name / phone / email are for people who file an event themselves; an
        // imported row is identified by `source` instead.
        row.setSourceUrl(fitUrl(card.pageUrl, null));
        if (existing.isPresent() && before.equals(fingerprint(row))) {
            return;
        }
        row.setSourceUpdatedAt(seenAt);
        eventCalendarRepository.save(row);
        if (existing.isPresent()) {
            result.setUpdated(result.getUpdated() + 1);
        } else {
            result.setCreated(result.getCreated() + 1);
        }
    }

    private static String fingerprint(EventCalendar r) {
        return String.join("\u0001", Objects.toString(r.getEventName()),
                Objects.toString(r.getEventDate() == null ? null : r.getEventDate().toLocalDate()),
                Objects.toString(r.getLocation()), Objects.toString(r.getEventType()),
                Objects.toString(r.getExtraDetail()), Objects.toString(r.getLink()),
                Objects.toString(r.getSourceUrl()));
    }

    // ------------------------------------------------------------------ source access

    URI monthUri(YearMonth ym, String type) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(MONTH_PATH, ym.toString())
                .path("/");
        if (type != null) {
            b.queryParam("pr_type", type);
        }
        return b.encode().build().toUri();
    }

    private String fetch(URI uri) {
        String html = http.getForObject(uri, String.class);
        // An empty month still renders the month template; without it the layout has changed and
        // reading "no races" from it would wipe the month.
        if (html == null || !html.contains("pr-next--month")) {
            throw new IllegalStateException("Unexpected page from " + uri);
        }
        return html;
    }

    // ------------------------------------------------------------------ page parsing

    static final class Card {
        String sourceId;
        String pageUrl;
        String title;
        LocalDate eventDate;
        List<String> distances = new ArrayList<>();
        String province;
        String venue;
        String registrationLink;
        boolean free;
        boolean trail;
    }

    static List<Card> parseMonth(String html) {
        TreeMap<Integer, String> blocks = new TreeMap<>();
        Matcher m = P_BLOCK.matcher(html);
        while (m.find()) {
            blocks.put(m.start(), m.group(1));
        }
        List<Card> cards = new ArrayList<>();
        m = P_ARTICLE.matcher(html);
        while (m.find()) {
            Map.Entry<Integer, String> block = blocks.floorEntry(m.start());
            Card c = parseCard(m.group(1), m.group(2), block == null ? null : block.getValue());
            if (c != null) {
                cards.add(c);
            }
        }
        return cards;
    }

    /** @param blockId "2026-12-05" for a day block, "ongoing" for races that started earlier. */
    static Card parseCard(String classes, String body, String blockId) {
        Matcher t = P_TITLE.matcher(body);
        if (!t.find()) {
            return null;
        }
        Card c = new Card();
        c.pageUrl = HtmlUtils.htmlUnescape(t.group(1)).trim();
        Matcher slug = P_EVENT_SLUG.matcher(c.pageUrl);
        if (!slug.find()) {
            return null;
        }
        c.sourceId = abbreviate(decodeUrl(slug.group(1)), COLUMN_MAX);
        c.title = cleanTitle(t.group(2));
        c.free = classes.contains("is-free");

        Matcher chip = P_CHIP.matcher(body);
        while (chip.find()) {
            String d = text(chip.group(1));
            if (!d.isEmpty()) {
                c.distances.add(d);
            }
        }

        String dateText = null;
        Matcher meta = P_META.matcher(body);
        if (meta.find()) {
            String mh = meta.group(1);
            Matcher x = P_META_DATE.matcher(mh);
            dateText = x.find() ? text(x.group(1)) : null;
            x = P_META_PROVINCE.matcher(mh);
            c.province = x.find() ? text(x.group(1)) : null;
            x = P_META_VENUE.matcher(mh);
            c.venue = x.find() ? text(x.group(1)) : null;
        }
        if (blockId != null && P_DAY_ID.matcher(blockId).matches()) {
            c.eventDate = LocalDate.parse(blockId);
        } else if (dateText != null) {
            c.eventDate = parseThaiStartDate(dateText);
        }

        Matcher cta = P_CTA.matcher(body);
        if (cta.find()) {
            String href = HtmlUtils.htmlUnescape(cta.group(1)).trim();
            // Links back into patrunning itself are its own ads (e.g. hotel rooms), not registration.
            if (href.startsWith("http") && !hostOf(href).endsWith("patrunning.com")) {
                c.registrationLink = href;
            }
        }
        return c;
    }

    /** "5 ธ.ค. 2026", "5–6 ธ.ค. 2026", "31 ต.ค. – 13 ธ.ค. 2026", "1 ก.ค. 2026 – 31 ธ.ค. 2027" → start date. */
    static LocalDate parseThaiStartDate(String text) {
        if (text == null) {
            return null;
        }
        String[] ends = text.split("\\s*[–—-]\\s*", 2);
        int[] start = datePart(ends[0]);
        int[] end = ends.length > 1 ? datePart(ends[1]) : null;
        if (start == null) {
            return null;
        }
        int month = start[1] > 0 ? start[1] : end == null ? 0 : end[1];
        int year = start[2] > 0 ? start[2] : end == null ? 0 : end[2];
        if (month == 0 || year == 0) {
            return null;
        }
        // "30 ธ.ค. – 2 ม.ค. 2027" starts in the year before the one written.
        if (start[2] == 0 && end != null && end[1] > 0 && month > end[1]) {
            year--;
        }
        try {
            return LocalDate.of(year, month, start[0]);
        } catch (Exception e) {
            return null;
        }
    }

    /** @return {day, month or 0, year or 0}, or null when no day is found. */
    private static int[] datePart(String s) {
        Matcher m = P_DATE_PART.matcher(s.trim());
        if (!m.lookingAt()) {
            return null;
        }
        int month = 0;
        if (m.group(2) != null) {
            for (int i = 0; i < THAI_MONTHS.length; i++) {
                if (THAI_MONTHS[i].equals(m.group(2))) {
                    month = i + 1;
                    break;
                }
            }
        }
        int year = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        if (year > 2400) {
            year -= 543; // Buddhist era
        }
        return new int[] { Integer.parseInt(m.group(1)), month, year };
    }

    /** Maps the source's free-text distances onto our eventType options. */
    static String guessEventType(String distance, List<String> categoryNames) {
        String cats = String.join(" ", categoryNames);
        String d = distance == null ? "" : distance;
        if (cats.contains("เทรล") || d.contains("เทรล") || d.toLowerCase().contains("trail")) {
            return "Trail";
        }
        double max = -1;
        Matcher m = P_NUMBER.matcher(d);
        while (m.find()) {
            try {
                max = Math.max(max, Double.parseDouble(m.group(1)));
            } catch (NumberFormatException ignored) {
                // not a distance
            }
        }
        if (max >= 42) {
            return "Marathon";
        }
        if (max >= 21) {
            return "Half Marathon";
        }
        if (max >= 10) {
            return "Mini Marathon";
        }
        if (max > 0) {
            return "Fun Run";
        }
        return null;
    }

    /**
     * Venue plus province. The public calendar filters by province with
     * {@code location LIKE %stateLocal%}, so the province's full name must be in the text.
     */
    static String buildLocation(String venue, String province, List<String> provinces) {
        String v = venue == null ? "" : venue;
        String p = province == null ? "" : province;
        String loc;
        if (v.isEmpty()) {
            loc = p;
        } else if (p.isEmpty() || v.contains(p)) {
            loc = v;
        } else {
            loc = v + " · " + p;
        }
        return normaliseLocation(loc, provinces);
    }

    static String normaliseLocation(String location, List<String> provinces) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String loc = location;
        boolean hasProvince = provinces.stream().anyMatch(loc::contains);
        if (!hasProvince && (loc.contains("กรุงเทพ") || loc.contains("กทม"))) {
            loc = loc + " (กรุงเทพมหานคร)";
        }
        return loc;
    }

    /** Distances and the free-race flag only; organizer contact details are deliberately left out. */
    static String buildExtraDetail(Card c) {
        List<String> parts = new ArrayList<>();
        if (!c.distances.isEmpty()) {
            parts.add("ประเภท: " + String.join(" / ", c.distances));
        }
        if (c.free) {
            parts.add("งานวิ่งฟรี");
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    private List<String> loadProvinceNames() {
        List<String> names = new ArrayList<>();
        try {
            for (Map<String, Object> row : masterDataRepository.getAllCountryState()) {
                Object local = row.get("stateLocal");
                if (local != null && !local.toString().isBlank()) {
                    names.add(local.toString());
                }
            }
        } catch (Exception e) {
            log.warn("EventCalendar import: could not load provinces: {}", e.toString());
        }
        return names;
    }

    /**
     * Card title → plain race name. The source appends notes such as "(🎁สมัครฟรี!) 📢เปิดรับสมัคร
     * วันที่ …" after the name; the free flag is kept separately, so everything from the first
     * 🎁 / 📢 note on is cut.
     */
    static String cleanTitle(String rendered) {
        if (rendered == null) {
            return null;
        }
        String s = cleanText(HtmlUtils.htmlUnescape(P_TAG.matcher(rendered).replaceAll(" ")).replace("​", ""));
        int cut = s.length();
        for (String marker : new String[] { "(🎁", "🎁", "📢" }) {
            int i = s.indexOf(marker);
            if (i > 0) {
                cut = Math.min(cut, i);
            }
        }
        String name = cleanText(s.substring(0, cut));
        return name.isEmpty() ? s : name;
    }

    private static String text(String html) {
        return cleanText(HtmlUtils.htmlUnescape(P_TAG.matcher(html).replaceAll(" ")));
    }

    static String cleanText(String s) {
        if (s == null) {
            return null;
        }
        return P_WS.matcher(s.replace(' ', ' ')).replaceAll(" ").trim();
    }

    static String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }

    /** Percent-encoded Thai → readable text (browsers accept either), "+" kept literal. */
    static String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Fits a URL into a varchar(255) without cutting it: an encoded Thai URL is often too long
     * as-is but fits once decoded. Falls back to {@code fallback} when neither form fits.
     */
    static String fitUrl(String url, String fallback) {
        if (url == null) {
            return fallback == null ? null : fitUrl(fallback, null);
        }
        if (url.length() <= COLUMN_MAX) {
            return url;
        }
        String decoded = decodeUrl(url);
        if (decoded.length() <= COLUMN_MAX && !decoded.contains(" ")) {
            return decoded;
        }
        return fallback == null ? null : fitUrl(fallback, null);
    }

    private static String hostOf(String url) {
        try {
            String host = URI.create(url.replace(" ", "%20")).getHost();
            return host == null ? "" : host.toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------------ appConfig state

    static int clampHorizon(int months) {
        return Math.max(MIN_HORIZON, Math.min(MAX_HORIZON, months));
    }

    private int readHorizon() {
        return appConfigRepository.findFirstByName(CFG_HORIZON)
                .map(AppConfig::getValue)
                .filter(v -> v != null && v.matches("\\d+"))
                .map(v -> clampHorizon(Integer.parseInt(v)))
                .orElse(clampHorizon(defaultHorizonMonths));
    }

    private Optional<EventCalendarImportResult> readLastRun() {
        return appConfigRepository.findFirstByName(CFG_LAST_RUN)
                .map(AppConfig::getValue)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> {
                    try {
                        return objectMapper.readValue(v, EventCalendarImportResult.class);
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    private void writeLastRun(EventCalendarImportResult result) {
        try {
            // appConfig.value is varchar(255): keep the JSON compact and the error short.
            String json = objectMapper.writeValueAsString(result);
            if (json.length() > COLUMN_MAX) {
                result.setError(abbreviate(result.getError(), 30));
                json = objectMapper.writeValueAsString(result);
            }
            writeConfig(CFG_LAST_RUN, abbreviate(json, COLUMN_MAX));
        } catch (Exception e) {
            log.warn("EventCalendar import: could not persist last-run summary: {}", e.toString());
        }
    }

    private void writeConfig(String name, String value) {
        AppConfig cfg = appConfigRepository.findFirstByName(name)
                .orElseGet(() -> AppConfig.builder().name(name).build());
        cfg.setValue(value);
        appConfigRepository.save(cfg);
    }
}

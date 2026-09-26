package com.actionth.membership.service.impl;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Imports races from joggingandrunning.com (WordPress + EventON).
 *
 * <p>Two source endpoints are used:
 * <ul>
 * <li>{@code /wp-json/wp/v2/ajde_events} — the public REST listing. It gives id, title, link,
 * category and modified time, but <b>not</b> the race date or venue.</li>
 * <li>each event's own page — its JSON-LD {@code Event} block carries {@code startDate}, and the
 * subtitle line ("📌venue 🏃ประเภท 10/21 👥ผู้จัด … 📞เบอร์โทร …") carries venue, distances,
 * organizer and phone. The card link on that page is the organizer's registration link when
 * the site has one.</li>
 * </ul>
 *
 * <p>The listing is walked in ascending {@code modified} order and the last processed modified
 * time is kept in {@code appConfig} as a watermark, so a nightly run only touches what changed.
 * Rows are upserted by (source, sourceId); admin approval decisions are never overwritten.
 */
@Slf4j
@Service
public class EventCalendarImportServiceImpl implements EventCalendarImportService {

    static final String CFG_WATERMARK = "eventCalendarImport.watermark";
    static final String CFG_LAST_RUN = "eventCalendarImport.lastRun";
    static final String CFG_HORIZON = "eventCalendarImport.horizonMonths";
    static final int MIN_HORIZON = 1;
    static final int MAX_HORIZON = 36;

    private static final ZoneId SOURCE_ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter WP_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int PAGE_SIZE = 100;
    private static final int COLUMN_MAX = 255;

    private static final Pattern P_START_DATE = Pattern.compile("\"startDate\"\\s*:\\s*\"([^\"]+)\"");
    /** EventON writes "2027-1-31T13:17+7:00" — single-digit month/day, non-standard offset. */
    private static final Pattern P_LOOSE_ISO = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})T(\\d{1,2}):(\\d{2})");
    private static final Pattern P_DATA_TIME = Pattern.compile("data-time=\"(\\d{9,11})-\\d+\"");
    private static final Pattern P_SUBTITLE = Pattern
            .compile("class=['\"]evcal_event_subtitle['\"][^>]*>(.*?)</span>", Pattern.DOTALL);
    private static final Pattern P_ANCHOR = Pattern.compile("<a\\b[^>]*>", Pattern.DOTALL);
    private static final Pattern P_HREF = Pattern.compile("href=\"([^\"]+)\"");
    private static final Pattern P_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern P_EMOJI = Pattern.compile("[\\p{So}\\p{Cs}\\u200D\\uFE0F\\u20E3]");
    private static final Pattern P_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern P_WS = Pattern.compile("\\s+");

    private static final String[] SUBTITLE_MARKERS = { "ประเภท", "ผู้จัด", "เบอร์โทร" };

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
    @Value("${app.event-calendar-import.source:joggingandrunning.com}")
    private String source;
    @Value("${app.event-calendar-import.base-url:https://www.joggingandrunning.com}")
    private String baseUrl;
    /** First run: only posts the source published within this many months are listed. */
    @Value("${app.event-calendar-import.backfill-months:12}")
    private int backfillMonths;
    /** Default horizon (months ahead) until an admin picks one in the back office. */
    @Value("${app.event-calendar-import.horizon-months:12}")
    private int defaultHorizonMonths;
    /** Pause between requests to the source site so a backfill doesn't hammer it. */
    @Value("${app.event-calendar-import.request-delay-ms:400}")
    private long requestDelayMs;
    /** Upper bound per run; whatever is left continues on the next run via the watermark. */
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
                .watermark(readWatermark().orElse(null))
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
                .startedAt(OffsetDateTime.now())
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
            Optional<OffsetDateTime> watermark = readWatermark();
            result.setMode(watermark.isPresent() ? "INCREMENTAL" : "BACKFILL");
            log.info("EventCalendar import started: mode={} source={} horizon={}m clearFirst={} watermark={}",
                    result.getMode(), source, horizon, clearFirst, watermark.orElse(null));
            runImport(watermark.orElse(null), horizon, result);
        } catch (Exception e) {
            log.error("EventCalendar import aborted", e);
            result.setError(abbreviate(e.getClass().getSimpleName() + ": " + e.getMessage(), 80));
        } finally {
            result.setFinishedAt(OffsetDateTime.now());
            result.setStopped(stopRequested.getAndSet(false));
            writeLastRun(result);
            currentRun = null;
            running.set(false);
        }
        log.info("EventCalendar import finished: {}", result);
        if (result.getCreated() > 0) {
            notificationService.notifyAdmins(NotificationType.EVENT_CALENDAR_IMPORTED,
                    "ดึงปฏิทินงานวิ่งจาก " + source,
                    "มีงานใหม่ " + result.getCreated() + " รายการรออนุมัติ",
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

    private long clearImportedRows() {
        long removed = eventCalendarRepository.deleteBySource(source);
        appConfigRepository.findFirstByName(CFG_WATERMARK).ifPresent(appConfigRepository::delete);
        log.info("EventCalendar import: cleared {} imported rows and reset the watermark", removed);
        return removed;
    }

    private void runImport(OffsetDateTime watermark, int horizonMonths, EventCalendarImportResult result)
            throws InterruptedException {
        Map<Integer, String> categories = fetchCategories();
        List<String> provinces = loadProvinceNames();
        LocalDate today = LocalDate.now(SOURCE_ZONE);
        LocalDate horizon = today.plusMonths(horizonMonths);

        // The watermark may only move up to just before the earliest failed item, so the next run
        // retries it; this keeps working whatever order the listing is walked in.
        OffsetDateTime maxOk = null;
        OffsetDateTime earliestFailed = null;
        int processed = 0;
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages && processed < maxEventsPerRun && !stopRequested.get()) {
            ResponseEntity<String> listing = http.exchange(listingUri(watermark, page), HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), String.class);
            String totalHeader = listing.getHeaders().getFirst("X-WP-TotalPages");
            if (totalHeader != null) {
                totalPages = Integer.parseInt(totalHeader.trim());
            }
            String totalCount = listing.getHeaders().getFirst("X-WP-Total");
            if (totalCount != null) {
                result.setTotal(Math.min(Integer.parseInt(totalCount.trim()), maxEventsPerRun));
            }
            JsonNode items = readJson(listing.getBody());
            if (items == null || !items.isArray()) {
                throw new IllegalStateException("Unexpected listing response from " + source);
            }

            for (JsonNode item : items) {
                if (processed >= maxEventsPerRun || stopRequested.get()) {
                    break;
                }
                processed++;
                result.setListed(result.getListed() + 1);
                OffsetDateTime modified = parseGmt(item.path("modified_gmt").asText(null));
                boolean ok = false;
                try {
                    ok = importOne(item, categories, provinces, today, horizon, result);
                } catch (Exception e) {
                    log.warn("EventCalendar import: event {} failed: {}", item.path("id").asText(), e.toString());
                }
                if (!ok) {
                    result.setFailed(result.getFailed() + 1);
                    if (modified != null && (earliestFailed == null || modified.isBefore(earliestFailed))) {
                        earliestFailed = modified;
                    }
                } else if (modified != null && (maxOk == null || modified.isAfter(maxOk))) {
                    maxOk = modified;
                }
                if (processed % 50 == 0) {
                    log.info("EventCalendar import progress: {}/{} created={} updated={} skipped={} failed={}",
                            result.getListed(), result.getTotal(), result.getCreated(), result.getUpdated(),
                            result.getSkipped(), result.getFailed());
                }
                Thread.sleep(requestDelayMs);
            }
            page++;
        }

        OffsetDateTime newWatermark = maxOk;
        if (earliestFailed != null && (newWatermark == null || !earliestFailed.isAfter(newWatermark))) {
            newWatermark = earliestFailed.minusSeconds(1);
        }
        if (newWatermark != null && (watermark == null || newWatermark.isAfter(watermark))) {
            writeWatermark(newWatermark);
        }
    }

    /** @return true when the event was created, updated or deliberately skipped; false on failure. */
    private boolean importOne(JsonNode item, Map<Integer, String> categories, List<String> provinces,
            LocalDate today, LocalDate horizon, EventCalendarImportResult result) {
        String sourceId = item.path("id").asText(null);
        String pageUrl = item.path("link").asText(null);
        if (sourceId == null || pageUrl == null || !"publish".equals(item.path("status").asText("publish"))) {
            return true;
        }
        String title = cleanText(HtmlUtils.htmlUnescape(item.path("title").path("rendered").asText("")));
        List<String> categoryNames = new ArrayList<>();
        for (JsonNode t : item.path("event_type")) {
            String name = categories.get(t.asInt());
            if (name != null) {
                categoryNames.add(name);
            }
        }

        String html = http.getForObject(URI.create(pageUrl), String.class);
        if (html == null) {
            return false;
        }
        ParsedPage parsed = parsePage(html);
        if (parsed.eventDate == null) {
            log.warn("EventCalendar import: no start date on {}", pageUrl);
            return false;
        }

        Optional<EventCalendar> existing = eventCalendarRepository.findBySourceAndSourceId(source, sourceId);
        boolean inWindow = !parsed.eventDate.isBefore(today) && !parsed.eventDate.isAfter(horizon);
        if (existing.isEmpty() && !inWindow) {
            result.setSkipped(result.getSkipped() + 1);
            return true;
        }

        EventCalendar row = existing.orElseGet(() -> EventCalendar.builder()
                .source(source)
                .sourceId(sourceId)
                .isApproved(null)
                .build());
        row.setEventName(abbreviate(title.isBlank() ? parsed.title : title, COLUMN_MAX));
        row.setEventDate(parsed.eventDate.atStartOfDay(SOURCE_ZONE).toOffsetDateTime());
        row.setLocation(abbreviate(normaliseLocation(parsed.location, provinces), COLUMN_MAX));
        row.setEventType(guessEventType(parsed.distance, categoryNames));
        row.setExtraDetail(abbreviate(buildExtraDetail(parsed, categoryNames), COLUMN_MAX));
        row.setLink(abbreviate(parsed.externalLink != null ? parsed.externalLink : pageUrl, COLUMN_MAX));
        // Submitter name / phone / email are for people who file an event themselves; an
        // imported row is identified by `source` instead.
        row.setSourceUrl(abbreviate(pageUrl, COLUMN_MAX));
        row.setSourceUpdatedAt(parseGmt(item.path("modified_gmt").asText(null)));
        eventCalendarRepository.save(row);

        if (existing.isPresent()) {
            result.setUpdated(result.getUpdated() + 1);
        } else {
            result.setCreated(result.getCreated() + 1);
        }
        return true;
    }

    // ------------------------------------------------------------------ source access

    private URI listingUri(OffsetDateTime watermark, int page) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/wp-json/wp/v2/ajde_events")
                .queryParam("per_page", PAGE_SIZE)
                .queryParam("page", page)
                // Backfill walks newest posts first so upcoming races show up within minutes;
                // incremental runs walk oldest change first.
                .queryParam("orderby", watermark != null ? "modified" : "date")
                .queryParam("order", watermark != null ? "asc" : "desc")
                .queryParam("status", "publish")
                .queryParam("_fields", "id,link,modified_gmt,title,event_type,status");
        if (watermark != null) {
            // WP compares against post_modified in the site's own timezone; an hour of overlap
            // costs a few re-fetches and guards against clock drift.
            LocalDateTime local = watermark.minusHours(1).atZoneSameInstant(SOURCE_ZONE).toLocalDateTime();
            b.queryParam("modified_after", local.format(WP_LOCAL));
        } else {
            LocalDateTime cutoff = LocalDateTime.now(SOURCE_ZONE).minusMonths(backfillMonths);
            b.queryParam("after", cutoff.format(WP_LOCAL));
        }
        return b.build(true).toUri();
    }

    private Map<Integer, String> fetchCategories() {
        Map<Integer, String> map = new HashMap<>();
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl).path("/wp-json/wp/v2/event_type")
                    .queryParam("per_page", 100).queryParam("_fields", "id,name").build(true).toUri();
            JsonNode terms = readJson(http.getForObject(uri, String.class));
            if (terms != null) {
                for (JsonNode t : terms) {
                    map.put(t.path("id").asInt(), HtmlUtils.htmlUnescape(t.path("name").asText("")));
                }
            }
        } catch (Exception e) {
            log.warn("EventCalendar import: could not load categories: {}", e.toString());
        }
        return map;
    }

    private JsonNode readJson(String body) {
        if (body == null) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON from " + source, e);
        }
    }

    // ------------------------------------------------------------------ page parsing

    static final class ParsedPage {
        String title;
        LocalDate eventDate;
        String location;
        String distance;
        String organizer;
        String phone;
        String externalLink;
    }

    static ParsedPage parsePage(String html) {
        ParsedPage p = new ParsedPage();

        Matcher m = P_START_DATE.matcher(html);
        if (m.find()) {
            p.eventDate = parseLooseDate(m.group(1));
        }
        if (p.eventDate == null) {
            m = P_DATA_TIME.matcher(html);
            if (m.find()) {
                long epoch = Long.parseLong(m.group(1));
                p.eventDate = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(epoch), SOURCE_ZONE)
                        .toLocalDate();
            }
        }

        m = P_SUBTITLE.matcher(html);
        if (m.find()) {
            parseSubtitle(cleanText(HtmlUtils.htmlUnescape(P_TAG.matcher(m.group(1)).replaceAll(" "))), p);
        }

        m = P_ANCHOR.matcher(html);
        while (m.find()) {
            String tag = m.group();
            if (tag.contains("desc_trig") && tag.contains("data-exlk=\"1\"")) {
                Matcher h = P_HREF.matcher(tag);
                if (h.find()) {
                    String href = HtmlUtils.htmlUnescape(h.group(1)).trim();
                    if (href.startsWith("http")) {
                        p.externalLink = href;
                    }
                }
                break;
            }
        }
        return p;
    }

    /** "📌venue จ.สตูล 🏃ประเภท 5.5 👥ผู้จัด บ้านทุ่งนุ้ย 📞เบอร์โทร. 08x" → fields. */
    static void parseSubtitle(String text, ParsedPage p) {
        String t = cleanText(P_EMOJI.matcher(text).replaceAll(" "));
        TreeMap<Integer, String> markers = new TreeMap<>();
        for (String marker : SUBTITLE_MARKERS) {
            int i = t.indexOf(marker);
            if (i >= 0) {
                markers.put(i, marker);
            }
        }
        p.location = cleanText(markers.isEmpty() ? t : t.substring(0, markers.firstKey()));
        Integer[] starts = markers.keySet().toArray(new Integer[0]);
        for (int i = 0; i < starts.length; i++) {
            String marker = markers.get(starts[i]);
            int from = starts[i] + marker.length();
            int to = i + 1 < starts.length ? starts[i + 1] : t.length();
            String value = cleanText(t.substring(from, to).replaceFirst("^[\\s.:：]+", ""));
            switch (marker) {
                case "ประเภท" -> p.distance = value;
                case "ผู้จัด" -> p.organizer = value;
                case "เบอร์โทร" -> p.phone = value;
                default -> {
                }
            }
        }
    }

    static LocalDate parseLooseDate(String raw) {
        Matcher m = P_LOOSE_ISO.matcher(raw);
        if (!m.find()) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)));
        } catch (Exception e) {
            return null;
        }
    }

    static OffsetDateTime parseGmt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw, WP_LOCAL).atOffset(ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
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
     * The public calendar filters by province with {@code location LIKE %stateLocal%}, so make sure
     * the province's full name is in the text when the source only abbreviated it.
     */
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
    static String buildExtraDetail(ParsedPage p, List<String> categoryNames) {
        List<String> parts = new ArrayList<>();
        if (p.distance != null && !p.distance.isBlank()) {
            parts.add("ประเภท: " + p.distance);
        }
        for (String c : categoryNames) {
            if (c.contains("ฟรี")) {
                parts.add("งานวิ่งฟรี");
                break;
            }
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

    static String cleanText(String s) {
        if (s == null) {
            return null;
        }
        return P_WS.matcher(s.replace(' ', ' ')).replaceAll(" ").trim();
    }

    static String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
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

    private Optional<OffsetDateTime> readWatermark() {
        return appConfigRepository.findFirstByName(CFG_WATERMARK)
                .map(AppConfig::getValue)
                .filter(v -> v != null && !v.isBlank())
                .map(OffsetDateTime::parse);
    }

    private void writeWatermark(OffsetDateTime value) {
        writeConfig(CFG_WATERMARK, value.toString());
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

package com.actionth.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.actionth.membership.constant.Constants;
import com.actionth.membership.model.AppConfig;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventCondition;
import com.actionth.membership.model.EventDetail;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.EventSelectionField;
import com.actionth.membership.model.EventSelectionOption;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.Menu;
import com.actionth.membership.model.PaymentType;
import com.actionth.membership.model.Permission;
import com.actionth.membership.model.Pricing;
import com.actionth.membership.model.Role;
import com.actionth.membership.model.ShirtSize;
import com.actionth.membership.model.ShirtType;
import com.actionth.membership.model.User;
import com.actionth.membership.constant.SelectionType;
import com.actionth.membership.repository.AppConfigRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.MenuRepository;
import com.actionth.membership.repository.PermissionRepository;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.repository.PaymentWebhookLogRepository;
import com.actionth.membership.repository.RoleRepository;
import com.actionth.membership.repository.UserRepository;
import com.actionth.membership.constant.PaymentProvider;
import com.actionth.membership.constant.WebhookDescription;
import com.actionth.membership.constant.WebhookLogType;
import com.actionth.membership.constant.WebhookReasonType;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.PaymentWebhookLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AppConfigRepository appConfigRepository;
	private final MenuRepository menuRepository;
	private final PermissionRepository permissionRepository;
	private final EventRepository eventRepository;
	private final JdbcTemplate jdbcTemplate;
	private final PaymentWebhookLogRepository webhookLogRepository;
	private final OrderRepository orderRepository;

	@Value("${app.env}")
	private String appEnv;

	@Value("${app.seed-user-password}")
	private String seedUserPassword;

	@Override
	public void run(String... args) {
		if (menuRepository.count() == 0) {
			List<Menu> menus = List.of(
					Menu.builder().title("dashboard").path("/backoffice/dashboard").icon("DashboardOutlined")
							.isDisplay(true).isNoti(false).position(0).build(),
					Menu.builder().title("historyList").path("/backoffice/historyList").icon("HistoryOutlined")
							.isDisplay(true).isNoti(false).position(1).build(),
					Menu.builder().title("eventList").path("/backoffice/eventList").icon("CalendarOutlined")
							.isDisplay(true).isNoti(false).position(2).build(),
					Menu.builder().title("couponList").path("/backoffice/couponList").icon("GiftOutlined")
							.isDisplay(true).isNoti(false).position(3).build(),
					Menu.builder().title("announcementList").path("/backoffice/announcementList").icon("SoundOutlined")
							.isDisplay(true).isNoti(true).badgeKey("announcement").position(4).build(),
					Menu.builder().title("eventCalendarList").path("/backoffice/eventCalendarList")
							.icon("NotificationOutlined").isDisplay(true).isNoti(true).badgeKey("eventCalendar")
							.position(5).build(),
					Menu.builder().title("contractList").path("/backoffice/contractList").icon("FileDoneOutlined")
							.isDisplay(true).isNoti(false).position(6).build(),
					Menu.builder().title("reportList").path("/backoffice/reportList").icon("BarChartOutlined")
							.isDisplay(true).isNoti(false).position(7).build(),
					Menu.builder().title("setting").path("/backoffice/setting").icon("SettingOutlined").isDisplay(true)
							.isNoti(false).position(8).build(),
					Menu.builder().title("operations").path("/backoffice/operations").icon("ToolOutlined").isDisplay(true)
							.isNoti(false).position(9).build());
			menuRepository.saveAll(menus);
		}

		if (roleRepository.count() == 0) {
			Role admin = roleRepository.save(Role.builder().roleType("admin").role("admin").active(true).build());
			Role organizer = roleRepository
					.save(Role.builder().roleType("organizer").role("organizer").active(true).build());
			Role guest = roleRepository.save(Role.builder().roleType("guest").role("guest").active(true).build());

			List<Menu> menus = menuRepository.findAll();
			Map<String, Menu> menuMap = menus.stream().collect(Collectors.toMap(Menu::getTitle, m -> m));

			List<Permission> allPermissions = new ArrayList<>();
			allPermissions.addAll(buildPermissions(admin, Constants.ADMIN_MENU, menuMap));
			allPermissions.addAll(buildPermissions(organizer, Constants.ORGANIZER_MENU, menuMap));
			allPermissions.addAll(buildPermissions(guest, Constants.GUEST_MENU, menuMap));

			permissionRepository.saveAll(allPermissions);
		}

		if (userRepository.count() == 0) {
			List<User> users = List.of(
					User.builder().firstName("Test").lastName("Admin").email("testAdmin@dione.zone")
						.password(passwordEncoder.encode(seedUserPassword))
						.role(roleRepository.findByRole("admin")
								.orElseThrow(() -> new IllegalArgumentException("Role not found: admin")))
						.active(true).isApprover(true).build(),
					User.builder().firstName("Test").lastName("Organizer").email("testOrganizer@dione.zone")
						.password(passwordEncoder.encode(seedUserPassword))
						.role(roleRepository.findByRole("organizer")
								.orElseThrow(() -> new IllegalArgumentException("Role not found: organizer")))
						.active(true).isApprover(true).build(),
					User.builder().firstName("Test").lastName("Guest").email("testGuest@dione.zone")
						.password(passwordEncoder.encode(seedUserPassword))
							.role(roleRepository.findByRole("guest")
									.orElseThrow(() -> new IllegalArgumentException("Role not found: guest")))
							.active(true).isApprover(true).build());
			userRepository.saveAll(users);
		}

		if (appConfigRepository.count() == 0) {
			AppConfig data = new AppConfig();
			data.setName("providerSealPath");
			data.setValue(Constants.PROVIDER_SEAL_PATH);
			appConfigRepository.save(data);
		}

		if (!"PROD".equals(appEnv) && eventRepository.count() == 0) {
			User organizer = userRepository.findByEmail("testOrganizer@dione.zone").orElse(null);
			List<Event> events = new ArrayList<>();

			// ==========================================
			// Event 1: Chiang Mai Trail 2026 (UTC+7)
			// ==========================================
			Event cmTrail = Event.builder()
				.name("เชียงใหม่ เทรล 2026")
				.eventDate(OffsetDateTime.of(2026, 11, 15, 0, 0, 0, 0, ZoneOffset.of("+07:00")))
				.organizerName("Trail Lovers")
				.location("ดอยอินทนนท์")
				.description("วิ่งผจญภัยในเส้นทางธรรมชาติ")
				.logoUrl("https://placehold.co/400x400/000000/FFFFFF/png?text=CM+Trail")
				.type("Trail")
				.link("cm-trail-2026")
				.pictureUrl("https://placehold.co/800x400/000000/FFFFFF/png?text=Chiang+Mai+Mountains")
				.prefixPath("cm-trail-2026")
				.startRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+07:00")))
				.endRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+07:00")).plusMonths(2))
				.shippingFee(new BigDecimal("60.00"))
				.isDraft(false)
				.organizer(organizer)
				.generalInfoTitle("ข้อมูลทั่วไป")
				.eventTypeTitle("ประเภทการแข่งขัน")
				.eventPrimaryColor("#008000") // Green
				.eventSecondaryColor("#FFFFFF")
				.eventFontColor("#000000")
				.uuid(UUID.randomUUID().toString())
				.createdBy(organizer)
				.build();

			// Conditions
			cmTrail.getEventConditions().add(EventCondition.builder().event(cmTrail).description("ผู้เข้าแข่งขันต้องมีอายุอย่างน้อย 18 ปี").build());
			cmTrail.getEventConditions().add(EventCondition.builder().event(cmTrail).description("ไม่สามารถขอคืนเงิน").build());
			
			// Details
			cmTrail.getEventDetails().add(EventDetail.builder().event(cmTrail).type("General").title("การรับอุปกรณ์").detail("<p>รับที่ห้างเมย่า 14 พ.ย.</p>").position(1).build());
			cmTrail.getEventDetails().add(EventDetail.builder().event(cmTrail).type("Reward").title("รางวัล").detail("<ul><li>เหรียญ</li><li>เสื้อ Finisher</li></ul>").position(2).build());

			// Types & Pricing
			EventType trail50k = EventType.builder().event(cmTrail).name("50 กม. อัลตร้า").eventDate(cmTrail.getEventDate()).quota(500).price(new BigDecimal("2500.00")).isNoShirt(false).build();
			EventType trail20k = EventType.builder().event(cmTrail).name("20 กม. ฟันรัน").eventDate(cmTrail.getEventDate()).quota(1000).price(new BigDecimal("1500.00")).isNoShirt(false).build();
			
			PaymentType cmCredit = PaymentType.builder().event(cmTrail).name("Credit Card").endDate(cmTrail.getEndRegistrationDate()).build();
			cmTrail.getPaymentTypes().add(cmCredit);
			
			Pricing cmP1 = Pricing.builder().eventType(trail50k).paymentType(cmCredit).price(new BigDecimal("2500.00")).quota(500).build();
			trail50k.getPricing().add(cmP1); cmCredit.getPricing().add(cmP1);
			Pricing cmP2 = Pricing.builder().eventType(trail20k).paymentType(cmCredit).price(new BigDecimal("1500.00")).quota(1000).build();
			trail20k.getPricing().add(cmP2); cmCredit.getPricing().add(cmP2);

			cmTrail.getEventTypes().add(trail50k);
			cmTrail.getEventTypes().add(trail20k);

			// Shirt
			ShirtType cmShirt = ShirtType.builder().event(cmTrail).name("เสื้อแข่ง").description("Dry-fit").build();
			cmShirt.getShirtSizes().add(ShirtSize.builder().shirtType(cmShirt).name("M").chestSize(new BigDecimal("38")).lengthSize(new BigDecimal("27")).build());
			cmTrail.getShirtTypes().add(cmShirt);
			
			// Selection
			EventSelectionField cmMed = EventSelectionField.builder().event(cmTrail).title("Medical Condition?").type(SelectionType.SINGLE).required(true).build();
			cmMed.getOptions().add(EventSelectionOption.builder().selectionField(cmMed).value("No").build());
			cmTrail.getSelectionFields().add(cmMed);

			events.add(cmTrail);

			// ==========================================
			// Event 2: Bangkok Marathon 2026 (UTC+7)
			// ==========================================
			Event bkkRun = Event.builder()
				.name("กรุงเทพมาราธอน 2026")
				.eventDate(OffsetDateTime.of(2026, 12, 10, 0, 0, 0, 0, ZoneOffset.of("+07:00"))) // midnight
				.organizerName("BKK Sports Authority")
				.location("Grand Palace, Bangkok")
				.description("The biggest city run in Thailand.")
				.logoUrl("https://placehold.co/400x400/0000FF/FFFFFF/png?text=BKK+Run")
				.type("Marathon")
				.link("bkk-marathon-2026")
				.pictureUrl("https://placehold.co/800x400/0000FF/FFFFFF/png?text=City+Lights")
				.prefixPath("bkk-marathon-2026")
				.startRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+07:00")))
				.endRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+07:00")).plusMonths(3))
				.shippingFee(new BigDecimal("50.00"))
				.isDraft(false)
				.organizer(organizer)
				.generalInfoTitle("Event Info")
				.eventTypeTitle("Categories")
				.eventPrimaryColor("#0000FF") // Blue
				.eventSecondaryColor("#FFFFFF")
				.eventFontColor("#333333")
				.uuid(UUID.randomUUID().toString())
				.createdBy(organizer)
				.build();

			// Conditions
			bkkRun.getEventConditions().add(EventCondition.builder().event(bkkRun).description("Cut-off time: 6 hours for Full Marathon").build());
			
			// Details
			bkkRun.getEventDetails().add(EventDetail.builder().event(bkkRun).type("General").title("Route").detail("<p>Start at Grand Palace, finish at Democracy Monument.</p>").position(1).build());

			// Types
			EventType bkk42k = EventType.builder().event(bkkRun).name("Full Marathon 42.195 km").eventDate(bkkRun.getEventDate()).quota(2000).price(new BigDecimal("1200.00")).isNoShirt(false).build();
			EventType bkk10k = EventType.builder().event(bkkRun).name("Mini Marathon 10 km").eventDate(bkkRun.getEventDate().plusHours(2)).quota(5000).price(new BigDecimal("600.00")).isNoShirt(false).build();

			PaymentType bkkQr = PaymentType.builder().event(bkkRun).name("QR Payment").endDate(bkkRun.getEndRegistrationDate()).build();
			bkkRun.getPaymentTypes().add(bkkQr);

			Pricing bkkP1 = Pricing.builder().eventType(bkk42k).paymentType(bkkQr).price(new BigDecimal("1200.00")).quota(2000).build();
			bkk42k.getPricing().add(bkkP1); bkkQr.getPricing().add(bkkP1);
			Pricing bkkP2 = Pricing.builder().eventType(bkk10k).paymentType(bkkQr).price(new BigDecimal("600.00")).quota(5000).build();
			bkk10k.getPricing().add(bkkP2); bkkQr.getPricing().add(bkkP2);

			bkkRun.getEventTypes().add(bkk42k);
			bkkRun.getEventTypes().add(bkk10k);
			
			// Shirt
			ShirtType bkkShirt = ShirtType.builder().event(bkkRun).name("Singlet").description("City Run Theme").build();
			bkkShirt.getShirtSizes().add(ShirtSize.builder().shirtType(bkkShirt).name("L").chestSize(new BigDecimal("40")).lengthSize(new BigDecimal("28")).build());
			bkkRun.getShirtTypes().add(bkkShirt);

			// Permissions
			bkkRun.getEventPermissions().add(EventPermission.builder().event(bkkRun).user(organizer).role("editor").canRead(true).canUpdate(true).canDelete(false).build());

			events.add(bkkRun);

			// ==========================================
			// Event 3: New York City Marathon 2026 (UTC-5)
			// ==========================================
			Event nyMarathon = Event.builder()
				.name("New York City Marathon 2026")
				.eventDate(OffsetDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneOffset.of("-05:00"))) // midnight EST
				.organizerName("NY Road Runners")
				.location("Staten Island to Central Park, NYC")
				.description("The world's largest marathon course.")
				.logoUrl("https://placehold.co/400x400/FFA500/000000/png?text=NYC")
				.type("Marathon")
				.link("ny-marathon-2026")
				.pictureUrl("https://placehold.co/800x400/FFA500/000000/png?text=Statue+of+Liberty")
				.prefixPath("ny-marathon-2026")
				.startRegistrationDate(OffsetDateTime.now(ZoneOffset.of("-05:00")))
				.endRegistrationDate(OffsetDateTime.now(ZoneOffset.of("-05:00")).plusMonths(6))
				.shippingFee(new BigDecimal("150.00")) // International shipping
				.isDraft(false)
				.organizer(organizer)
				.generalInfoTitle("Race Details")
				.eventTypeTitle("Entry Types")
				.eventPrimaryColor("#FFA500") // Orange
				.eventSecondaryColor("#000000")
				.eventFontColor("#FFFFFF")
				.uuid(UUID.randomUUID().toString())
				.createdBy(organizer)
				.build();

			// Conditions
			nyMarathon.getEventConditions().add(EventCondition.builder().event(nyMarathon).description("Must pick up bib in person at Expo.").build());

			// Details
			nyMarathon.getEventDetails().add(EventDetail.builder().event(nyMarathon).type("General").title("The Course").detail("<p>Runs through all five boroughs.</p>").position(1).build());

			// Types
			EventType nyFull = EventType.builder().event(nyMarathon).name("Marathon Entry").eventDate(nyMarathon.getEventDate()).quota(50000).price(new BigDecimal("300.00")).isNoShirt(false).build();
			
			PaymentType nyCard = PaymentType.builder().event(nyMarathon).name("Credit Card (USD)").endDate(nyMarathon.getEndRegistrationDate()).build();
			nyMarathon.getPaymentTypes().add(nyCard);

			Pricing nyP1 = Pricing.builder().eventType(nyFull).paymentType(nyCard).price(new BigDecimal("300.00")).quota(1000).build();
			nyFull.getPricing().add(nyP1); nyCard.getPricing().add(nyP1);

			nyMarathon.getEventTypes().add(nyFull);

			// Shirt
			ShirtType nyShirt = ShirtType.builder().event(nyMarathon).name("Technical Tee").description("Long sleeve").build();
			nyShirt.getShirtSizes().add(ShirtSize.builder().shirtType(nyShirt).name("M").chestSize(new BigDecimal("38")).lengthSize(new BigDecimal("27")).build());
			nyMarathon.getShirtTypes().add(nyShirt);

			// Selection
			EventSelectionField nyHotel = EventSelectionField.builder().event(nyMarathon).title("Need Hotel Booking?").type(SelectionType.SINGLE).required(false).build();
			nyHotel.getOptions().add(EventSelectionOption.builder().selectionField(nyHotel).value("Yes").build());
			nyHotel.getOptions().add(EventSelectionOption.builder().selectionField(nyHotel).value("No").build());
			nyMarathon.getSelectionFields().add(nyHotel);

			// Permissions
			nyMarathon.getEventPermissions().add(EventPermission.builder().event(nyMarathon).user(organizer).role("editor").canRead(true).canUpdate(true).canDelete(false).build());

			events.add(nyMarathon);

			// ==========================================
			// Event 4: Berlin Marathon 2026 (UTC+2) -> Testing European Time
			// ==========================================
			Event berlinMarathon = Event.builder()
				.name("Berlin Marathon 2026")
				.eventDate(OffsetDateTime.of(2026, 9, 27, 0, 0, 0, 0, ZoneOffset.of("+02:00"))) // CEST
				.organizerName("SCC Events")
				.location("Brandenburg Gate, Berlin")
				.description("Fastest course in the world.")
				.logoUrl("https://placehold.co/400x400/000000/FFFF00/png?text=Berlin")
				.type("Marathon")
				.link("berlin-marathon-2026")
				.pictureUrl("https://placehold.co/800x400/000000/FFFF00/png?text=Brandenburg+Gate")
				.prefixPath("berlin-marathon-2026")
				.startRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+02:00")))
				.endRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+02:00")).plusMonths(5))
				.shippingFee(new BigDecimal("20.00")) // Euro
				.isDraft(false)
				.organizer(organizer)
				.generalInfoTitle("Informationen") // German title
				.eventTypeTitle("Wettbewerb")
				.eventPrimaryColor("#009EE0") // Blue
				.eventSecondaryColor("#FFFFFF")
				.eventFontColor("#000000")
				.uuid(UUID.randomUUID().toString())
				.createdBy(organizer)
				.build();

			// Conditions
			berlinMarathon.getEventConditions().add(EventCondition.builder().event(berlinMarathon).description("Time limit: 6:15 hours.").build());

			// Types
			EventType berlinFull = EventType.builder().event(berlinMarathon).name("Marathon").eventDate(berlinMarathon.getEventDate()).quota(40000).price(new BigDecimal("200.00")).isNoShirt(false).build();
			
			PaymentType berlinCard = PaymentType.builder().event(berlinMarathon).name("Credit Card (Euro)").endDate(berlinMarathon.getEndRegistrationDate()).build();
			berlinMarathon.getPaymentTypes().add(berlinCard);
			
			Pricing berlinP1 = Pricing.builder().eventType(berlinFull).paymentType(berlinCard).price(new BigDecimal("200.00")).quota(2000).build();
			berlinFull.getPricing().add(berlinP1); berlinCard.getPricing().add(berlinP1);

			berlinMarathon.getEventTypes().add(berlinFull);
			
			ShirtType berlinShirt = ShirtType.builder().event(berlinMarathon).name("Adidas Tee").description("Performance").build();
			berlinShirt.getShirtSizes().add(ShirtSize.builder().shirtType(berlinShirt).name("L").chestSize(new BigDecimal("42")).lengthSize(new BigDecimal("29")).build());
			berlinMarathon.getShirtTypes().add(berlinShirt);

			// Permissions
			berlinMarathon.getEventPermissions().add(EventPermission.builder().event(berlinMarathon).user(organizer).role("editor").canRead(true).canUpdate(true).canDelete(false).build());

			events.add(berlinMarathon);

			// ==========================================
			// Event 5: Tokyo Marathon 2026 (UTC+9) -> Testing Asian Time
			// ==========================================
			Event tokyoMarathon = Event.builder()
				.name("Tokyo Marathon 2026")
				.eventDate(OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.of("+09:00"))) // JST
				.organizerName("Tokyo Marathon Foundation")
				.location("Tokyo Metropolitan Gov Building")
				.description("The Day We Unite.")
				.logoUrl("https://placehold.co/400x400/FF0000/FFFFFF/png?text=Tokyo")
				.type("Marathon")
				.link("tokyo-marathon-2026")
				.pictureUrl("https://placehold.co/800x400/FF0000/FFFFFF/png?text=Tokyo+Tower")
				.prefixPath("tokyo-marathon-2026")
				.startRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+09:00")))
				.endRegistrationDate(OffsetDateTime.now(ZoneOffset.of("+09:00")).plusMonths(4))
				.shippingFee(new BigDecimal("0.00"))
				.isDraft(false)
				.organizer(organizer)
				.generalInfoTitle("General Info")
				.eventTypeTitle("Race Category")
				.eventPrimaryColor("#FF0000") // Red
				.eventSecondaryColor("#FFFFFF")
				.eventFontColor("#000000")
				.uuid(UUID.randomUUID().toString())
				.createdBy(organizer)
				.build();

			// Conditions
			tokyoMarathon.getEventConditions().add(EventCondition.builder().event(tokyoMarathon).description("No costumes allowed that block others.").build());

			// Types
			EventType tokyoFull = EventType.builder().event(tokyoMarathon).name("Marathon").eventDate(tokyoMarathon.getEventDate()).quota(38000).price(new BigDecimal("180.00")).isNoShirt(false).build();
			
			PaymentType tokyoCard = PaymentType.builder().event(tokyoMarathon).name("Credit Card (Yen)").endDate(tokyoMarathon.getEndRegistrationDate()).build();
			tokyoMarathon.getPaymentTypes().add(tokyoCard);
			
			Pricing tokyoP1 = Pricing.builder().eventType(tokyoFull).paymentType(tokyoCard).price(new BigDecimal("180.00")).quota(1500).build();
			tokyoFull.getPricing().add(tokyoP1); tokyoCard.getPricing().add(tokyoP1);

			tokyoMarathon.getEventTypes().add(tokyoFull);

			ShirtType tokyoShirt = ShirtType.builder().event(tokyoMarathon).name("Tokyo Tee").description("Limited Edition").build();
			tokyoShirt.getShirtSizes().add(ShirtSize.builder().shirtType(tokyoShirt).name("S").chestSize(new BigDecimal("36")).lengthSize(new BigDecimal("26")).build());
			tokyoMarathon.getShirtTypes().add(tokyoShirt);

			// Permissions
			tokyoMarathon.getEventPermissions().add(EventPermission.builder().event(tokyoMarathon).user(organizer).role("editor").canRead(true).canUpdate(true).canDelete(false).build());

			events.add(tokyoMarathon);
			// ==========================================

			eventRepository.saveAll(events);
		}

		migrateWebhookLogs();
	}

	@SuppressWarnings("unchecked")
	private void migrateWebhookLogs() {
		if (webhookLogRepository.count() > 0) {
			return;
		}

		// Check if old tables exist
		boolean scbTableExists = tableExists("scbWebhookLog");
		boolean anomalyTableExists = tableExists("paymentWebhookAnomalyLog");

		if (!scbTableExists && !anomalyTableExists) {
			return;
		}

		ObjectMapper mapper = new ObjectMapper();
		List<PaymentWebhookLog> allLogs = new ArrayList<>();

		// Collect webhookLogIds referenced by anomaly table to avoid duplicate migration
		Set<Long> anomalyWebhookLogIds = new HashSet<>();
		if (anomalyTableExists && scbTableExists) {
			List<Map<String, Object>> refs = jdbcTemplate.queryForList(
					"SELECT `webhookLogId` FROM `paymentWebhookAnomalyLog` WHERE `webhookLogId` IS NOT NULL");
			for (Map<String, Object> ref : refs) {
				if (ref.get("webhookLogId") != null) {
					anomalyWebhookLogIds.add(((Number) ref.get("webhookLogId")).longValue());
				}
			}
		}

		// Convert scbWebhookLog rows (skip those already covered by anomaly table)
		if (scbTableExists) {
			List<Map<String, Object>> scbRows = jdbcTemplate.queryForList("SELECT * FROM `scbWebhookLog`");
			for (Map<String, Object> row : scbRows) {
				// Skip rows already referenced by anomaly table (will be migrated with proper reasonType)
				Long scbId = row.get("id") != null ? ((Number) row.get("id")).longValue() : null;
				if (scbId != null && anomalyWebhookLogIds.contains(scbId)) {
					continue;
				}

				String orderNo = row.get("billPaymentRef1") != null ? row.get("billPaymentRef1").toString() : null;
				String transactionId = row.get("transactionId") != null ? row.get("transactionId").toString() : null;
				String payloadJson = row.get("payloadJson") != null ? row.get("payloadJson").toString() : null;
				OffsetDateTime receivedDateTime = row.get("timestamp") != null
						? ((LocalDateTime) row.get("timestamp")).atOffset(ZoneOffset.UTC)
						: OffsetDateTime.now();

				// Detect provider from payload content
				String provider = PaymentProvider.SCB;
				Double amount = null;
				String currency = "THB";

				if (payloadJson != null) {
					try {
						Map<String, Object> payload = mapper.readValue(payloadJson, Map.class);
						if (payload.containsKey("merchantID") || payload.containsKey("invoiceNo")) {
							// 2C2P payload: has merchantID, invoiceNo, tranRef, respCode
							provider = PaymentProvider.TWO_C2P;
							if (payload.get("amount") != null) {
								amount = ((Number) payload.get("amount")).doubleValue();
							}
							if (payload.get("currencyCode") != null) {
								currency = payload.get("currencyCode").toString();
							}
						} else if (payload.containsKey("billPaymentRef1") || payload.containsKey("qrId")) {
							// SCB payload: has billPaymentRef1, payeeProxyId, or qrId
							provider = PaymentProvider.SCB;
							if (payload.get("amount") != null) {
								amount = Double.parseDouble(payload.get("amount").toString());
							}
							if (payload.get("currencyCode") != null) {
								// SCB uses numeric currency code (764 = THB)
								currency = "THB";
							}
						}
					} catch (Exception e) {
						// fallback: keep defaults
					}
				}

				Orders order = (orderNo != null)
						? orderRepository.findByOrderNo(orderNo).orElse(null) : null;

				if (order != null && amount == null) {
					amount = order.getTotalAmountWithFee();
				}

				// Determine logType: for 2C2P, check respCode to detect anomalies
				String logType = WebhookLogType.WEBHOOK;
				String reasonType = null;

				if (PaymentProvider.TWO_C2P.equals(provider) && payloadJson != null) {
					try {
						Map<String, Object> pl = mapper.readValue(payloadJson, Map.class);
						String respCode = pl.get("respCode") != null ? pl.get("respCode").toString().trim() : null;
						if (respCode != null && WebhookReasonType.is2c2pAnomaly(respCode)) {
							logType = WebhookLogType.ANOMALY;
							reasonType = WebhookReasonType.build2c2pReasonType(respCode);
						}
					} catch (Exception e) {
						// fallback: keep WEBHOOK
					}
				}

				var log = new PaymentWebhookLog();
				log.setLogType(logType);
				log.setPaymentProvider(provider);
				log.setPayloadJson(payloadJson);
				log.setTransactionId(transactionId);
				log.setOrderNo(orderNo);
				log.setReasonType(reasonType);
				log.setAmount(amount);
				log.setCurrency(currency);
				log.setReceivedDateTime(receivedDateTime);

				if (WebhookLogType.ANOMALY.equals(logType) && reasonType != null) {
					log.setDescription(WebhookDescription.buildDescription(provider, reasonType, orderNo));
				} else {
					log.setDescription(WebhookDescription.formatWebhook(provider));
				}

				if (order != null) {
					log.setOrderId(order.getId());
					log.setPaymentStatusAtWebhookTime(order.getPaymentStatus());
				}

				allLogs.add(log);
			}
		}

		// Convert paymentWebhookAnomalyLog → ANOMALY rows
		if (anomalyTableExists) {
			String anomalySql = scbTableExists
					? "SELECT a.*, w.`payloadJson` AS `webhookPayload` FROM `paymentWebhookAnomalyLog` a LEFT JOIN `scbWebhookLog` w ON a.`webhookLogId` = w.`id`"
					: "SELECT a.*, NULL AS `webhookPayload` FROM `paymentWebhookAnomalyLog` a";

			List<Map<String, Object>> anomalyRows = jdbcTemplate.queryForList(anomalySql);
			for (Map<String, Object> row : anomalyRows) {
				String orderNo = row.get("orderNo") != null ? row.get("orderNo").toString() : null;
				Integer orderId = row.get("orderId") != null ? ((Number) row.get("orderId")).intValue() : null;
				String provider = row.get("paymentProvider") != null ? row.get("paymentProvider").toString() : null;
				String transactionId = row.get("transactionId") != null ? row.get("transactionId").toString() : null;
				String reasonType = row.get("reasonType") != null ? row.get("reasonType").toString() : null;
				String paymentStatus = row.get("paymentStatusAtWebhookTime") != null ? row.get("paymentStatusAtWebhookTime").toString() : null;
				String payload = row.get("webhookPayload") != null ? row.get("webhookPayload").toString() : null;
				Double amount = row.get("amount") != null ? ((Number) row.get("amount")).doubleValue() : null;
				String currency = row.get("currency") != null ? row.get("currency").toString() : null;
				OffsetDateTime receivedDateTime = row.get("receivedDateTime") != null
						? ((LocalDateTime) row.get("receivedDateTime")).atOffset(ZoneOffset.UTC)
						: OffsetDateTime.now();

				Orders order = (orderNo != null)
						? orderRepository.findByOrderNo(orderNo).orElse(null) : null;

				var log = new PaymentWebhookLog();
				log.setLogType(WebhookLogType.ANOMALY);
				log.setPaymentProvider(provider);
				log.setPayloadJson(payload);
				log.setTransactionId(transactionId);
				log.setOrderNo(orderNo);
				log.setOrderId(orderId);
				log.setPaymentStatusAtWebhookTime(paymentStatus);
				log.setReasonType(reasonType);
				log.setAmount(amount);
				log.setCurrency(currency);
				log.setReceivedDateTime(receivedDateTime);

				if (reasonType != null) {
					log.setDescription(WebhookDescription.buildDescription(
							provider, reasonType, orderNo));
				}

				allLogs.add(log);
			}
		}

		// Sort all collected logs by receivedDateTime so IDs reflect chronological order
		allLogs.sort((a, b) -> {
			OffsetDateTime dtA = a.getReceivedDateTime();
			OffsetDateTime dtB = b.getReceivedDateTime();
			if (dtA == null && dtB == null) return 0;
			if (dtA == null) return 1;
			if (dtB == null) return -1;
			return dtA.compareTo(dtB);
		});

		webhookLogRepository.saveAll(allLogs);
	}

	private boolean tableExists(String tableName) {
		try {
			jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Integer.class);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private List<Permission> buildPermissions(Role role, List<String> menuTitles, Map<String, Menu> menuMap) {
		List<Permission> permissions = new ArrayList<>();
		for (String menuKey : menuTitles) {
			Menu menu = menuMap.get(menuKey);
			if (menu != null) {
				permissions.add(Permission.builder()
						.active(true)
						.menu(menu)
						.role(role)
						.canRead(true)
						.canCreate(true)
						.canUpdate(true)
						.canDelete(true)
						.build());
			}
		}
		return permissions;
	}
}

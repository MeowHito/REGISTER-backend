package com.actionth.membership.controller;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.Contact;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.User;
import com.actionth.membership.model.UserToken;
import com.actionth.membership.model.PagingData.Search;
import com.actionth.membership.model.dto.EventDto;
import com.actionth.membership.model.dto.EventViewDto;
import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.model.dto.AvailablePricingResponse;
import com.actionth.membership.model.dto.EventTypeAvailabilityResponse;
import com.actionth.membership.model.request.ContactDTO;
import com.actionth.membership.model.request.EventCalendarDTO;
import com.actionth.membership.model.request.GeneralRequest;
import com.actionth.membership.model.request.SocialAuthDTORequest;
import com.actionth.membership.model.request.UserForgotDTORequest;
import com.actionth.membership.model.request.UserRegisterDTORequest;
import com.actionth.membership.model.request.UserTokenDTORequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.ContactService;
import com.actionth.membership.service.EmailService;
import com.actionth.membership.service.EventCalendarService;
import com.actionth.membership.service.EventService;
import com.actionth.membership.service.UserTokenService;
import com.actionth.membership.service.LineAuthService;
import com.actionth.membership.service.PricingService;
import com.actionth.membership.service.UserService;
import com.actionth.membership.service.GoogleAuthService;
import com.actionth.membership.service.FacebookAuthService;
import com.actionth.membership.service.SliderService;
import com.actionth.membership.service.SystemAnnouncementService;
import com.actionth.membership.model.request.SliderDTO;
import com.actionth.membership.model.request.SystemAnnouncementDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/public-api")
public class PublicAPIController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private LineAuthService lineAuthService;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private FacebookAuthService facebookAuthService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventCalendarService eventCalendarService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private AWSService awsService;

    @Autowired
    private SliderService sliderService;

    @Autowired
    private SystemAnnouncementService systemAnnouncementService;

    @Value("${app.env}")
    private String appEnv;

    @GetMapping("/slider/active")
    public Response<List<SliderDTO>> getActiveSliders() {
        return new Response<>(sliderService.findAllActive(), "Success", true);
    }

    @GetMapping("/system-announcements/active")
    public Response<List<SystemAnnouncementDTO>> getActiveSystemAnnouncements() {
        return new Response<>(systemAnnouncementService.findAllActive(), "Success", true);
    }

    @GetMapping("/pricing/checkQuota/{pricingId}")
    public Response<Boolean> checkPricingQuota(@PathVariable String pricingId) {
        return new Response<>(pricingService.checkQuota(pricingId), "Check quota successfully", true);
    }

    @PostMapping("/event/getAllEvents")
    public Response<Page<EventViewDto>> getAllEvents(@RequestBody GeneralRequest request) {
        PagingData paging = request.getPaging();
        if (paging == null) {
            request.setPaging(PagingData.builder().search(List.of(
                    Search.builder().searchField("isDraft").searchText("false").searchType("BOOLEAN")
                            .build()))
                    .build());
        } else {
            List<Search> search = paging.getSearch();
            search.add(Search.builder().searchField("isDraft").searchText("false").searchType("BOOLEAN")
                    .build());
            request.setPaging(paging);
        }

        request.setActive(true);
        return new Response<>(
                eventService.findAll(request),
                "Events retrieved successfully", true);
    }

    @GetMapping("/event/{uuid}")
    public Response<EventDto> getEventByLinkOrUuid(@PathVariable String uuid) {
        return new Response<>(eventService.getEventByLinkOrUuid(uuid), "Events retrieved successfully", true);
    }

    @PostMapping("/event/getExternalEvents")
    public Response<Page<EventCalendarDTO>> getApprovedEvents(@RequestBody GeneralRequest generalRequest) {
        Page<EventCalendarDTO> dtos = eventCalendarService.getApprovedEvents(generalRequest);
        return new Response<>(dtos, "Public events fetched", true);
    }

    @PostMapping("/eventCalendar")
    public Response<Void> createEventCalendar(@RequestBody EventCalendarDTO eventCalendarDTO) {
        eventCalendarService.createEventCalendar(eventCalendarDTO);
        emailService.sendEventCalendarConfirmMail(
                eventCalendarDTO.getEmail(),
                eventCalendarDTO.getEventName());
        return new Response<>(null, "Event submitted for approval", true);
    }

    @GetMapping("/event/dates")
    public ResponseEntity<List<String>> getAllEventDatesInMonth(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime date) {
        List<OffsetDateTime> dates = eventService.getAllEventDatesInMonth(date);
        List<String> formatted = dates.stream()
                .map(d -> d.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .toList();

        return ResponseEntity.ok(formatted);
    }

    @PostMapping("/register")
    public Response<Object> createUser(@RequestBody UserRegisterDTORequest userDto,
            HttpServletResponse servletResponse) {
        UserDto user = userService.createUser(userDto);

        boolean isProd = !"DEV".equalsIgnoreCase(Optional.ofNullable(appEnv).orElse("DEV"));
        ResponseCookie jwtCookie = ResponseCookie
                .from("access_token", userService.getToken(user))
                .httpOnly(true)
                .secure(isProd)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return new Response<>(Map.of("user", user), "Login successful.", true);
    }

    @PostMapping("/sendContactEmail")
    public Response<Boolean> sendContactEmail(@RequestBody ContactDTO contactDTO) {
        if (contactDTO.getEmail() != null) {

            Contact contactData = contactService.createContact(contactDTO);

            if (contactData == null) {
                throw new BusinessException("Failed to create contact.");
            }
            emailService.sendContactCustomerEmail(contactData);
            emailService.sendContactAdminEmail(contactData);
            return new Response<>(true, "Contact created and emails sent.", true);
        } else {
            return new Response<>(false, "The email is available.", true);
        }
    }

    @PostMapping("/checkUserEmail")
    public Response<Boolean> checkEmailExists(@RequestBody UserForgotDTORequest userDTO) {
        try {
            User user = userService.findByEmail(userDTO.getEmail());
            
            if (user != null) {
                UserToken userToken = new UserToken();
                userToken.setUser(user);
                UserToken tokenData = userTokenService.createUserToken(userToken);

                if (tokenData != null) {
                    String tokenUuid = tokenData.getUuid();

                    String firstName = user.getFirstName();
                    String lastName = user.getLastName();
                    String username = "";
                    if (firstName != null) {
                        username = firstName;
                        if (lastName != null && !lastName.isEmpty()) {
                            username += " " + lastName;
                        }
                    }

                    emailService.sendResetPasswordMail(userDTO.getEmail(), username, tokenUuid);
                }
            }
        } catch (ResourceNotFoundException e) {
            // User not found - intentionally suppress to prevent user enumeration
        }

        return new Response<>(true, "Password reset link has been sent.", true);
    }

    @PostMapping("/login")
    public Response<Object> login(
            @RequestBody SocialAuthDTORequest socialAuthDTORequest, HttpServletResponse servletResponse) {
        String type = socialAuthDTORequest.getType();
        String token = socialAuthDTORequest.getToken();

        if (type == null || type.isEmpty()) {
            return new Response<>(null, "ประเภทการเข้าสู่ระบบไม่ถูกต้อง", false);
        }

        if (token == null || token.isEmpty()) {
            return new Response<>(null, "ไม่พบ Token กรุณาลองใหม่อีกครั้ง", false);
        }

        String email = null;
        try {
            switch (type.toLowerCase()) {
                case "line":
                    email = resolveLineEmail(token);
                    break;
                case "google":
                    email = resolveGoogleEmail(token);
                    break;
                case "facebook":
                    email = resolveFacebookEmail(token);
                    break;
                default:
                    return new Response<>(null, "ประเภทการเข้าสู่ระบบไม่รองรับ: " + type, false);
            }
        } catch (SocialLoginException e) {
            return new Response<>(null, e.getMessage(), false);
        } catch (Exception e) {
            return new Response<>(null, "เกิดข้อผิดพลาดในการยืนยันตัวตน กรุณาลองใหม่อีกครั้ง", false);
        }

        if (email == null || email.isEmpty()) {
            return new Response<>(null, "ไม่พบอีเมลจากบัญชีที่เลือก กรุณาใช้บัญชีที่มีอีเมลที่ยืนยันแล้ว", false);
        }

        return processUserLogin(email, servletResponse);
    }

    private String resolveLineEmail(String token) {
        try {
            String lineResponse = lineAuthService.verifyIdToken(token);
            if (lineResponse == null || lineResponse.isEmpty()) {
                throw new SocialLoginException("ไม่สามารถยืนยัน LINE Token ได้ กรุณาลองใหม่");
            }
            Map<String, Object> lineData = new ObjectMapper().readValue(lineResponse,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            return (String) lineData.get("email");
        } catch (SocialLoginException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialLoginException("ไม่สามารถเข้าสู่ระบบด้วย LINE ได้ กรุณาลองใหม่อีกครั้ง");
        }
    }

    private String resolveGoogleEmail(String token) {
        try {
            Map<String, Object> tokenResponse = googleAuthService.exchangeCodeForToken(token);
            String idTokenGoogle = (String) tokenResponse.get("id_token");
            if (idTokenGoogle == null || idTokenGoogle.isEmpty()) {
                throw new SocialLoginException("ไม่พบข้อมูลจาก Google กรุณาลองใหม่อีกครั้ง");
            }
            Map<String, Object> googleData = new ObjectMapper().readValue(
                    new String(java.util.Base64.getDecoder().decode(idTokenGoogle.split("\\.")[1])),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            return (String) googleData.get("email");
        } catch (SocialLoginException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("redirect_uri_mismatch")) {
                throw new SocialLoginException("การตั้งค่า Google OAuth ไม่ถูกต้อง (redirect_uri) กรุณาติดต่อผู้ดูแลระบบ");
            }
            if (msg.contains("invalid_grant") || msg.contains("400")) {
                throw new SocialLoginException("รหัสยืนยันตัวตนของ Google หมดอายุ กรุณาลองใหม่อีกครั้ง");
            }
            throw new SocialLoginException("ไม่สามารถเข้าสู่ระบบด้วย Google ได้ กรุณาลองใหม่อีกครั้ง");
        }
    }

    private String resolveFacebookEmail(String token) {
        try {
            if (!facebookAuthService.validateAccessToken(token)) {
                throw new SocialLoginException("Facebook Token ไม่ถูกต้องหรือหมดอายุ กรุณาลองใหม่");
            }
            return facebookAuthService.getEmailFromFacebook(token);
        } catch (SocialLoginException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialLoginException("ไม่สามารถเข้าสู่ระบบด้วย Facebook ได้ กรุณาลองใหม่อีกครั้ง");
        }
    }

    private static class SocialLoginException extends RuntimeException {
        SocialLoginException(String message) { super(message); }
    }

    @GetMapping("/validateUserToken")
    public Response<Boolean> validateUserToken(@RequestParam String id) {
        boolean isValid = userTokenService.existsByUuidAndActiveWithinOneDay(id);
        String message = isValid ? "The token is valid and active." : "The token is invalid or expired.";
        return new Response<>(isValid, message, true);
    }

    @PutMapping("/updateUserToken")
    public Response<UserToken> updateUserToken(@RequestBody UserTokenDTORequest userTokenDTO) {
        return new Response<>(userTokenService.updateUserToken(userTokenDTO), "User updated successfully", true);
    }

    @GetMapping(path = "/getPublicUrl")
    public ResponseEntity<Response<String>> getPublicUrl(
            @RequestParam("prefix") String prefix,
            @RequestParam("key") String key,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic) {
        String url = awsService.getSharedUrl(prefix, key, isPublic);

        Response<String> response = new Response<>();
        response.setData(url);
        response.setMessage("success");
        response.setSuccess(true);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/pricing/available/{eventTypeId}")
    public Response<AvailablePricingResponse> getAvailablePricing(@PathVariable String eventTypeId) {
        AvailablePricingResponse pricing = pricingService.getAvailablePricingByEventType(eventTypeId);
        return new Response<>(pricing, "Success", true);
    }

    @GetMapping("/event/{eventId}/types/availability")
    public Response<List<EventTypeAvailabilityResponse>> getEventTypesAvailability(@PathVariable String eventId) {
        List<EventTypeAvailabilityResponse> availability = eventService.getEventTypesAvailability(eventId);
        return new Response<>(availability, "Success", true);
    }

    private Response<Object> processUserLogin(String email, HttpServletResponse servletResponse) {
        try {
            UserDto user = userService.getByEmail(email);
            boolean isProd = !"DEV".equalsIgnoreCase(Optional.ofNullable(appEnv).orElse("DEV"));
            ResponseCookie jwtCookie = ResponseCookie
                    .from("access_token", userService.getToken(user))
                    .httpOnly(true)
                    .secure(isProd)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(14))
                    .build();
            servletResponse.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            return new Response<>(null, "Login successful.", true);
        } catch (ResourceNotFoundException e) {
            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            return new Response<>(data, "Redirecting to register.", false);
        }
    }

}

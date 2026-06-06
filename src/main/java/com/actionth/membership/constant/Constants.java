package com.actionth.membership.constant;

import java.util.List;

public class Constants {

    private Constants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String DATA_NOT_FOUND_TH = "ไม่พบข้อมูล";
    public static final String ERROR_INVALID_TOKEN = "Invalid token";
    public static final String ERROR_SERVER = "Server error";
    public static final String USER_NOT_FOUND = "ไม่พบผู้ใช้ในระบบ/รหัสผ่านไม่ถูกต้อง";
    public static final String PROVIDER_SEAL_PATH = "/stamp.png";

    public static final List<String> ADMIN_MENU = List.of("eventList", "dashboard", "contractList", "announcementList", "eventCalendarList", "couponList", "reportList", "historyList", "setting", "operations");
    public static final List<String> ORGANIZER_MENU = List.of("eventList", "dashboard", "contractList", "announcementList", "eventCalendarList", "couponList", "historyList", "setting");
    public static final List<String> GUEST_MENU = List.of("historyList", "setting");
}
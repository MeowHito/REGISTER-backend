package com.actionth.membership.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.actionth.membership.model.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Which fields the registration form asks for, per event. The organizer can hide a field, make
 * it optional or require it; anything not configured keeps the platform default below. Name,
 * birth date, gender and email are always required and are not listed here. The frontend keeps
 * the same list in {@code StreamlinedRegistration/fieldConfig.js}.
 */
public final class RegistrationFieldConfig {

    public static final String HIDDEN = "HIDDEN";
    public static final String OPTIONAL = "OPTIONAL";
    public static final String REQUIRED = "REQUIRED";

    /** Configurable field -> default mode. Order is the order they appear on the form. */
    public static final Map<String, String> DEFAULTS;

    static {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("pictureUrl", OPTIONAL);
        d.put("firstNameEn", REQUIRED);
        d.put("lastNameEn", REQUIRED);
        d.put("idNo", REQUIRED);
        d.put("phone", REQUIRED);
        d.put("province", REQUIRED);
        d.put("nationality", REQUIRED);
        d.put("bloodType", REQUIRED);
        d.put("healthIssues", OPTIONAL);
        d.put("emergencyContact", REQUIRED);
        d.put("emergencyRelation", REQUIRED);
        d.put("emergencyPhone", REQUIRED);
        d.put("teamClub", OPTIONAL);
        DEFAULTS = Map.copyOf(d);
    }

    public static final List<String> FIELDS = List.copyOf(new LinkedHashMap<>(DEFAULTS).keySet());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RegistrationFieldConfig() {
    }

    /** Effective mode of every configurable field for the event (defaults filled in). */
    public static Map<String, String> resolve(Event event) {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        String json = event != null ? event.getFieldConfig() : null;
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            Map<String, String> stored = MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
            });
            stored.forEach((k, v) -> {
                if (DEFAULTS.containsKey(k) && v != null) {
                    String mode = v.trim().toUpperCase();
                    if (HIDDEN.equals(mode) || OPTIONAL.equals(mode) || REQUIRED.equals(mode)) {
                        result.put(k, mode);
                    }
                }
            });
        } catch (Exception ignored) {
            // an unreadable config behaves like no config
        }
        return result;
    }

    public static boolean isRequired(Map<String, String> config, String field) {
        return REQUIRED.equals(config.get(field));
    }
}

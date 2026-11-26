package com.actionth.membership.utils;

import com.actionth.membership.model.AgeGroup;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.OrderDetail;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;

@Slf4j
public class AgeGroupUtils {

    private AgeGroupUtils() {
        // Utility class
    }

    /**
     * Resolves the age group text for a participant based on their birth date and event type age groups.
     * Returns formatted age group like "(กลุ่มอายุ 20-30 ปี ชาย)" or "ไม่มีการแข่งขันกลุ่มอายุ" if no match.
     *
     * @param participant The order detail containing participant information
     * @return Formatted age group text
     */
    public static String resolveAgeGroup(OrderDetail participant) {
        if (participant.getEventType() != null &&
                participant.getEventType().getAgeGroups() != null) {

            try {
                Integer age = participant.getAge();
                if (age == null && participant.getBirthDate() != null) {
                    age = calculateAge(participant.getBirthDate());
                }

                if (age == null) {
                    log.debug("[AgeGroup] No age available for participant");
                    return "ไม่มีการแข่งขันกลุ่มอายุ";
                }

                String gender = participant.getGender();
                log.debug("[AgeGroup] Resolving for participant: age={}, gender={}", age, gender);

                for (AgeGroup ageGroup : participant.getEventType().getAgeGroups()) {
                    String gGender = ageGroup.getGender() != null ? ageGroup.getGender().name() : "";
                    if (gender != null && gGender.equalsIgnoreCase(gender)) {
                        int min = ageGroup.getMinAge() != null ? ageGroup.getMinAge() : Integer.MIN_VALUE;
                        int max = ageGroup.getMaxAge() != null ? ageGroup.getMaxAge() : Integer.MAX_VALUE;

                        if (age >= min && age <= max) {
                            String result = formatAgeGroup(ageGroup.getMinAge(), ageGroup.getMaxAge(), gender);
                            log.debug("[AgeGroup] Matched: {}", result);
                            return result;
                        }
                    }
                }
                log.debug("[AgeGroup] No matching age group found");
            } catch (Exception e) {
                log.warn("[AgeGroup] Error resolving age group for participant: {}",
                        participant.getUuid(), e);
            }
        } else {
            log.debug("[AgeGroup] Missing eventType/ageGroups");
        }
        return "ไม่มีการแข่งขันกลุ่มอายุ";
    }

    /**
     * Formats age group information into Thai text format.
     * Examples:
     * - Both min and max: "(กลุ่มอายุ 20-30 ปี ชาย)"
     * - Only min: "(กลุ่มอายุ 20 ปีขึ้นไป ชาย)"
     * - Only max: "(กลุ่มอายุไม่เกิน 30 ปี ชาย)"
     *
     * @param minAge Minimum age (can be null)
     * @param maxAge Maximum age (can be null)
     * @param gender Gender ("MALE", "FEMALE", etc.)
     * @return Formatted age group text
     */
    public static String formatAgeGroup(Integer minAge, Integer maxAge, String gender) {
        log.debug("[AgeGroup formatAgeGroup] Input - minAge={}, maxAge={}, gender={}", minAge, maxAge, gender);

        String genderText = "";
        if (gender != null) {
            if ("MALE".equalsIgnoreCase(gender)) {
                genderText = " ชาย";
            } else if ("FEMALE".equalsIgnoreCase(gender)) {
                genderText = " หญิง";
            } else {
                genderText = " " + gender;
            }
        }

        String result;
        if (minAge != null && maxAge != null) {
            result = "(กลุ่มอายุ " + minAge + "-" + maxAge + " ปี" + genderText + ")";
        } else if (minAge != null) {
            result = "(กลุ่มอายุ " + minAge + " ปีขึ้นไป" + genderText + ")";
        } else if (maxAge != null) {
            result = "(กลุ่มอายุไม่เกิน " + maxAge + " ปี" + genderText + ")";
        } else {
            result = "";
        }

        log.debug("[AgeGroup formatAgeGroup] Output - {}", result);
        return result;
    }

    public static final String AGE_BIRTH_YEAR = "BIRTH_YEAR";
    public static final String AGE_EXACT_DATE = "EXACT_DATE";

    /** Only the two known methods are stored; anything else means "the original rule" (null). */
    public static String normalizeAgeCalcMethod(String method) {
        if (method == null) return null;
        String m = method.trim().toUpperCase();
        return AGE_BIRTH_YEAR.equals(m) || AGE_EXACT_DATE.equals(m) ? m : null;
    }

    /**
     * A runner's race age under the event's {@code ageCalcMethod}, mirrored by the registration
     * page's {@code raceAge} in StreamlinedRegistration/utils.js:
     * <ul>
     *   <li>{@code BIRTH_YEAR}: race year minus birth year</li>
     *   <li>{@code EXACT_DATE}: completed years on race day</li>
     *   <li>null (events from before the option): {@code legacyRef}'s year minus birth year</li>
     * </ul>
     * Race day is the distance's own date, else the event's, else today. Dates are read in Bangkok time.
     */
    public static int raceAge(OffsetDateTime birthDate, Event event, EventType eventType, OffsetDateTime legacyRef) {
        if (birthDate == null) return 0;
        LocalDate birth = birthDate.atZoneSameInstant(ExportDateTimeUtils.BANGKOK_ZONE).toLocalDate();
        String method = event != null ? normalizeAgeCalcMethod(event.getAgeCalcMethod()) : null;
        if (method == null) {
            OffsetDateTime ref = legacyRef != null ? legacyRef : OffsetDateTime.now();
            return ref.atZoneSameInstant(ExportDateTimeUtils.BANGKOK_ZONE).getYear() - birth.getYear();
        }
        OffsetDateTime raceDay = eventType != null && eventType.getEventDate() != null ? eventType.getEventDate()
                : event.getEventDate() != null ? event.getEventDate() : OffsetDateTime.now();
        LocalDate race = raceDay.atZoneSameInstant(ExportDateTimeUtils.BANGKOK_ZONE).toLocalDate();
        if (AGE_EXACT_DATE.equals(method)) {
            return Math.max(0, Period.between(birth, race).getYears());
        }
        return race.getYear() - birth.getYear();
    }

    /**
     * Calculates age from birth date.
     *
     * @param birthDate The birth date
     * @return Age in years
     */
    public static int calculateAge(OffsetDateTime birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return OffsetDateTime.now().getYear() - birthDate.getYear();
    }

    /**
     * Calculates age from birth date using a reference date.
     *
     * @param birthDate The birth date
     * @param refDate The reference date (e.g. eventType.eventDate or event.eventDate)
     * @return Age in years at refDate
     */
    public static int calculateAgeAtRefDate(OffsetDateTime birthDate, OffsetDateTime refDate) {
        if (birthDate == null) {
            return 0;
        }
        OffsetDateTime ref = (refDate != null) ? refDate : OffsetDateTime.now();
        return ref.getYear() - birthDate.getYear();
    }

    /**
     * Resolve reference date for age calculation.
     * Priority:
     * 1) orderDetail.createdTime
     * 2) eventType.eventDate
     * 3) event.eventDate
     * 4) now (fallback)
     */
    private static OffsetDateTime resolveAgeRefDate(OrderDetail od) {
        if (od == null) return OffsetDateTime.now();

        if (od.getCreatedTime() != null)
            return od.getCreatedTime();

        EventType et = od.getEventType();
        if (et != null && et.getEventDate() != null)
            return et.getEventDate();

        if (et != null && et.getEvent() != null && et.getEvent().getEventDate() != null) {
            return et.getEvent().getEventDate();
        }

        return OffsetDateTime.now();
    }

    /**
     * Finds matching age group from a list based on age and gender.
     *
     * @param age Age in years
     * @param gender Gender string
     * @param ageGroups List of age groups to search
     * @return Matching AgeGroup or null if not found
     */
    public static AgeGroup findMatchingAgeGroup(int age, String gender, List<AgeGroup> ageGroups) {
        if (ageGroups == null || gender == null) {
            return null;
        }

        for (AgeGroup ageGroup : ageGroups) {
            String gGender = ageGroup.getGender() != null ? ageGroup.getGender().name() : "";
            if (gGender.equalsIgnoreCase(gender)) {
                int min = ageGroup.getMinAge() != null ? ageGroup.getMinAge() : Integer.MIN_VALUE;
                int max = ageGroup.getMaxAge() != null ? ageGroup.getMaxAge() : Integer.MAX_VALUE;

                if (age >= min && age <= max) {
                    return ageGroup;
                }
            }
        }
        return null;
    }

    /**
     * Returns age group "code" based on participant birthDate and event/eventType date.
     * Examples: "20-30", "0-11", "41+"
     * Returns null if cannot resolve (missing data or no matching group).
     */
    public static String resolveAgeGroupCode(OrderDetail od) {
        if (od == null || od.getEventType() == null) {
            return null;
        }

        EventType eventType = od.getEventType();
        if (eventType.getAgeGroups() == null || eventType.getAgeGroups().isEmpty()) {
            return null;
        }

        OffsetDateTime refDate = resolveAgeRefDate(od);

        Integer ageObj = od.getAge();

        if (ageObj == null) {
            if (od.getBirthDate() == null)
                return null;
            ageObj = calculateAgeAtRefDate(od.getBirthDate(), refDate);
        }

        int age = ageObj;

        AgeGroup.Gender gender = normalizeGender(od.getGender());

        return eventType.getAgeGroups().stream()
                .filter(ag -> gender == null || ag.getGender() == null || ag.getGender() == gender)
                .filter(ag -> (ag.getMinAge() == null || age >= ag.getMinAge())
                        && (ag.getMaxAge() == null || age <= ag.getMaxAge()))
                .findFirst()
                .map(AgeGroupUtils::formatAgeGroupCode)
                .orElse(null);
    }

    private static AgeGroup.Gender normalizeGender(String gender) {
        if (gender == null) return null;

        String g = gender.trim();
        if (g.isEmpty()) return null;

        if ("male".equalsIgnoreCase(g)) return AgeGroup.Gender.male;
        if ("female".equalsIgnoreCase(g)) return AgeGroup.Gender.female;

        return null;
    }

    private static String formatAgeGroupCode(AgeGroup ag) {
        if (ag == null) return null;

        Integer min = ag.getMinAge();
        Integer max = ag.getMaxAge();

        if (min != null && max != null) return min + "-" + max;
        if (min == null && max != null) return "0-" + max;
        if (min != null) return min + "+";
        return null;
    }
}

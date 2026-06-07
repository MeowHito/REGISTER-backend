package thaibaht;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Drop-in replacement for the legacy {@code thaibaht.jar} that was referenced by
 * build.gradle but missing from this repository.
 *
 * Converts a monetary amount into its Thai-language reading
 * (e.g. 1234.50 -> "หนึ่งพันสองร้อยสามสิบสี่บาทห้าสิบสตางค์").
 *
 * Public API kept identical to the original library: {@code new ThaiBaht().getText(amount)}.
 * Used by PaymentWebhookService (receipt e-mails) and summary_finance_report.jrxml.
 */
public class ThaiBaht {

    private static final String[] DIGITS = {
            "", "หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก", "เจ็ด", "แปด", "เก้า"
    };
    // index = position from the right within a 6-digit group (0 = units)
    private static final String[] UNITS = {
            "", "สิบ", "ร้อย", "พัน", "หมื่น", "แสน"
    };

    public String getText(Double amount) {
        if (amount == null) return "";
        return getText(BigDecimal.valueOf(amount));
    }

    public String getText(double amount) {
        return getText(BigDecimal.valueOf(amount));
    }

    public String getText(Number amount) {
        if (amount == null) return "";
        return getText(new BigDecimal(amount.toString()));
    }

    public String getText(String amount) {
        if (amount == null || amount.trim().isEmpty()) return "";
        return getText(new BigDecimal(amount.trim()));
    }

    public String getText(BigDecimal amount) {
        if (amount == null) return "";

        boolean negative = amount.signum() < 0;
        amount = amount.abs().setScale(2, RoundingMode.HALF_UP);

        BigDecimal integerPart = amount.setScale(0, RoundingMode.FLOOR);
        int satang = amount.subtract(integerPart).movePointRight(2).intValueExact();

        StringBuilder sb = new StringBuilder();
        if (negative) sb.append("ลบ");

        sb.append(readNumber(integerPart.toBigInteger().toString())).append("บาท");

        if (satang == 0) {
            sb.append("ถ้วน");
        } else {
            sb.append(readNumber(Integer.toString(satang))).append("สตางค์");
        }
        return sb.toString();
    }

    /** Reads a non-negative integer string into Thai text (handles arbitrary length via "ล้าน" grouping). */
    private String readNumber(String number) {
        number = stripLeadingZeros(number);
        if (number.equals("0")) return "ศูนย์";

        int len = number.length();
        if (len > 6) {
            String high = number.substring(0, len - 6);
            String low = number.substring(len - 6);
            return readNumber(high) + "ล้าน" + readGroup(low);
        }
        return readGroup(number);
    }

    /** Reads a 1-to-6 digit group (leading zeros allowed); returns "" when the group is zero. */
    private String readGroup(String number) {
        number = stripLeadingZeros(number);
        if (number.equals("0")) return "";

        StringBuilder sb = new StringBuilder();
        int len = number.length();
        for (int i = 0; i < len; i++) {
            int digit = number.charAt(i) - '0';
            int pos = len - i - 1; // 0 = units, 1 = tens, ...
            if (digit == 0) continue;

            if (pos == 0 && digit == 1 && len > 1) {
                sb.append("เอ็ด");          // ...๑ -> เอ็ด (e.g. 21 = ยี่สิบเอ็ด)
            } else if (pos == 1 && digit == 2) {
                sb.append("ยี่").append(UNITS[pos]);   // 2 in tens -> ยี่สิบ
            } else if (pos == 1 && digit == 1) {
                sb.append(UNITS[pos]);      // 1 in tens -> สิบ (no หนึ่ง)
            } else {
                sb.append(DIGITS[digit]).append(UNITS[pos]);
            }
        }
        return sb.toString();
    }

    private String stripLeadingZeros(String number) {
        int i = 0;
        while (i < number.length() - 1 && number.charAt(i) == '0') i++;
        return number.substring(i);
    }
}

package com.actionth.membership.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.FinanceSummaryDTO;
import com.actionth.membership.model.dto.FinanceSummaryTotalDTO;
import com.actionth.membership.model.dto.PageWithSummary;
import com.actionth.membership.model.dto.RegistrantSummaryDTO;
import com.actionth.membership.model.dto.RevenueDetailSummaryDTO;
import com.actionth.membership.model.dto.RevenueSummaryDTO;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.service.SummaryReportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SummaryReportServiceImpl implements SummaryReportService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    private BigDecimal safeBigDecimal(Object value) {
        return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
    }

    public String optString(Object value) {
        return value != null ? value.toString() : "";
    }

    private String formatDecimal(Object value) {
        if (value == null)
            return "0.00";
        try {
            BigDecimal decimal = value instanceof BigDecimal bd
                    ? bd
                    : new BigDecimal(value.toString());
            return decimal.setScale(2).toString();
        } catch (Exception e) {
            return "0.00"; // fallback ถ้าแปลงไม่สำเร็จ
        }
    }

    private String formatDateTime(Object value) {
        if (value == null)
            return "";

        if (value instanceof Timestamp tm) {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(tm);
        } else if (value instanceof String str) {
            try {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                OffsetDateTime dateTime = OffsetDateTime.parse(str, inputFormatter);
                return dateTime.format(outputFormatter);
            } catch (Exception e) {
                return value.toString();
            }
        }
        return value.toString();
    }

    private BigDecimal getServiceFeeRate(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty())
            return BigDecimal.ZERO;

        if ("QRCODE".equalsIgnoreCase(paymentMethod)) {
            return new BigDecimal("0.03");
        }

        return new BigDecimal("0.05");
    }

    @Override
    public List<Map<String, Object>> getSummarizeOrderFinance(String eventUuid, String eventName,
            OffsetDateTime startDate, OffsetDateTime endDate) {
        String[] columns = { "ลำดับ", "ประเภทงานวิ่ง", "ราคาค่าสมัคร", "จำนวนคนสมัคร (คน)", "จำนวนเงินสมัคร (THB)" };
        List<Map<String, Object>> results = orderRepository.summarizeOrderFinance(eventUuid, startDate, endDate);
        List<Map<String, Object>> formatData = new ArrayList<>();

        Map<String, Object> datas = new HashMap<>();
        datas.put("sheetName", "Summary");
        datas.put("columns", columns);
        datas.put("preHeader", List.of("ชื่ออีเว้นท์", eventName));

        List<List<String>> sheetData = new ArrayList<>();

        int index = 1;
        for (Map<String, Object> result : results) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(index++));
            row.add(optString(result.get("name")));
            row.add(formatDecimal(result.get("unitPrice")));
            row.add(optString(result.get("qty")));
            row.add(formatDecimal(result.get("total")));
            sheetData.add(row);
        }

        datas.put("datas", sheetData);
        formatData.add(datas);

        return formatData;
    }

    @Override
    public PageWithSummary<FinanceSummaryDTO, FinanceSummaryTotalDTO> getFinanceSummary(
            String eventUuid, OffsetDateTime startDate, OffsetDateTime endDate, PagingData pagingData) {

        List<Map<String, Object>> rawList = orderRepository.summarizeOrderFinance(eventUuid, startDate, endDate);
        List<Map<String, Object>> summaryList = orderRepository.summarizeOrderFinanceMaster(eventUuid, startDate,
                endDate);

        List<FinanceSummaryDTO> dtoList = rawList.stream().map(map -> new FinanceSummaryDTO(
                (String) map.get("name"),
                safeBigDecimal(map.get("unitPrice")),
                safeBigDecimal(map.get("qty")),
                safeBigDecimal(map.get("total")))).toList();

        Map<String, Object> summaryMap = summaryList.isEmpty() ? Map.of() : summaryList.get(0);

        FinanceSummaryTotalDTO summary = new FinanceSummaryTotalDTO(
                safeBigDecimal(summaryMap.get("totalDiscountCoupon")),
                safeBigDecimal(summaryMap.get("totalDiscountShirt")),
                safeBigDecimal(summaryMap.get("totalShippingFee")),
                safeBigDecimal(summaryMap.get("totalAmount")),
                safeBigDecimal(summaryMap.get("totalNetAmount")),
                safeBigDecimal(summaryMap.get("totalServiceFee")),
                safeBigDecimal(summaryMap.get("totalAmountWithFee")));

        Page<FinanceSummaryDTO> page = new PageImpl<>(dtoList);

        return new PageWithSummary<>(page, summary);
    }

    @Override
    public Page<RevenueSummaryDTO> getSummarizeOrderRevenue(OffsetDateTime startDate, OffsetDateTime endDate,
            PagingData pagingData) {

        List<Map<String, Object>> rawData = orderRepository.summarizeRevenue(startDate, endDate);

        // Map จาก Map → DTO
        List<RevenueSummaryDTO> dtoList = rawData.stream()
                .map(row -> {

                    BigDecimal registrationFee = safeBigDecimal(row.get("registrationFee"));
                    BigDecimal shippingFee = safeBigDecimal(row.get("shippingFee"));
                    String paymentMethod = optString(row.get("paymentMethod"));
                    BigDecimal serviceFeeRate = getServiceFeeRate(paymentMethod);
                    BigDecimal serviceFee = registrationFee.multiply(serviceFeeRate);
                    BigDecimal total = registrationFee.add(serviceFee);
                    BigDecimal totalWithShipping = total.add(shippingFee);

                    RevenueSummaryDTO dto = new RevenueSummaryDTO();
                    dto.setContractNo((String) row.get("contractNo"));
                    dto.setEventName((String) row.get("eventName"));
                    dto.setPaymentMethod(paymentMethod);
                    dto.setRegistrationFee(registrationFee);
                    dto.setServiceFee(serviceFee);
                    dto.setTotal(total);
                    dto.setShippingFee(shippingFee);
                    dto.setTotalWithShipping(totalWithShipping);
                    return dto;
                })
                .toList();

        // Create Pageable จาก pagingData
        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize());

        // ตัดรายการตาม page
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtoList.size());
        List<RevenueSummaryDTO> pagedList = dtoList.subList(start, end);

        return new PageImpl<>(pagedList, pageable, dtoList.size());
    }

    @Override
    public Page<RevenueDetailSummaryDTO> getRevenueDetailSummary(String eventUuid, OffsetDateTime startDate,
            OffsetDateTime endDate, PagingData pagingData) {
        String sortField = switch (Optional.ofNullable(pagingData.getSortField()).orElse("")) {
            case "eventName" -> "order.event.name";
            case "orderId" -> "order.orderNo";
            case "transactionId" -> "order.scbTransactionId";
            case "paymentDateTime" -> "order.paymentDateTime";
            case "fullName" -> "firstName";
            case "eventTypeName" -> "eventType.name";
            case "paymentStatus" -> "order.paymentStatus";
            case "paymentMethod" -> "order.paymentMethod";
            case "registrationDateTime" -> "order.createdTime";
            default -> null;
        };

        Sort sort;
        if (sortField == null) {
            sort = Sort.by(Sort.Direction.DESC, "order.paymentDateTime");
        } else {
            Sort.Direction sortDirection = "DESC".equalsIgnoreCase(pagingData.getSortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(sortDirection, sortField);
        }

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        Specification<OrderDetail> spec = (root, query, cb) -> {
            query.distinct(true);

            Join<OrderDetail, Orders> order = root.join("order", JoinType.LEFT);
            Join<OrderDetail, EventType> eventType = root.join("eventType", JoinType.LEFT);
            Join<Orders, Event> event = order.join("event", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("active"), true));
            predicates.add(cb.equal(order.get("paymentStatus"), "SUCCESS"));

            if (eventUuid != null) {
                predicates.add(cb.equal(event.get("uuid"), eventUuid));
            }

            if (startDate != null && endDate != null) {
                predicates.add(cb.between(order.get("createdTime"), startDate, endDate));
            }

            if (pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
                String field = pagingData.getSearchField().toLowerCase();
                String text = "%" + pagingData.getSearchText().toLowerCase() + "%";

                switch (field) {
                    case "eventname" -> predicates.add(cb.like(event.get("name"), text));
                    case "orderid" -> predicates.add(cb.like(order.get("orderNo"), text));
                    case "transactionid" -> predicates.add(cb.like(order.get("scbTransactionId"), text));
                    case "fullname" -> predicates
                            .add(cb.like(cb.concat(root.get("firstName"), cb.concat(" ", root.get("lastName"))), text));
                    case "eventtypename" -> predicates.add(cb.like(eventType.get("name"), text));
                    case "paymentstatus" -> predicates.add(cb.like(order.get("paymentStatus"), text));
                    case "paymentmethod" -> predicates.add(cb.like(order.get("paymentMethod"), text));
                    default -> predicates.add(cb.conjunction());
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OrderDetail> result = orderDetailRepository.findAll(spec, pageable);

        return result.map(od -> {
            BigDecimal price = safeBigDecimal(od.getPrice());
            BigDecimal discountCoupon = safeBigDecimal(od.getCouponDiscount());
            BigDecimal discountShirt = safeBigDecimal(od.getDiscountShirt());
            BigDecimal shippingFee = safeBigDecimal(od.getShippingFee());
            BigDecimal netPrice = price.subtract(discountCoupon).subtract(discountShirt).add(shippingFee);
            String paymentMethod = od.getOrder().getPaymentMethod();
            BigDecimal serviceFeeRate = getServiceFeeRate(paymentMethod);
            BigDecimal serviceFee = netPrice.multiply(serviceFeeRate);
            BigDecimal total = netPrice.add(serviceFee);

            return new RevenueDetailSummaryDTO(
                    od.getUuid(),
                    od.getOrder().getEvent().getName(),
                    od.getOrder().getOrderNo(),
                    od.getOrder().getScbTransactionId(),
                    formatDateTime(od.getOrder().getPaymentDateTime()),
                    od.getFirstName() + " " + Optional.ofNullable(od.getLastName()).orElse(""),
                    price,
                    discountCoupon,
                    discountShirt,
                    shippingFee,
                    serviceFee,
                    total,
                    netPrice,
                    od.getEventType().getName(),
                    od.getOrder().getPaymentStatus(),
                    paymentMethod,
                    formatDateTime(od.getOrder().getCreatedTime()));
        });
    }

    @Override
    public Page<RegistrantSummaryDTO> getSummarizeRegistrant(String eventUuid, OffsetDateTime startDate,
            OffsetDateTime endDate, PagingData pagingData) {
        String sortField = switch (Optional.ofNullable(pagingData.getSortField()).orElse("")) {
            case "eventName" -> "order.event.name";
            case "orderId" -> "order.orderNo";
            case "transactionId" -> "order.scbTransactionId";
            case "fullName" -> "firstName";
            case "eventTypeName" -> "eventType.name";
            case "paymentStatus" -> "order.paymentStatus";
            case "paymentMethod" -> "order.paymentMethod";
            case "registrationDateTime" -> "order.createdTime";
            case "paymentDateTime" -> "order.paymentDateTime";
            default -> null;
        };

        Sort sort;
        if (sortField == null) {
            sort = Sort.by(Sort.Direction.DESC, "order.paymentDateTime");
        } else {
            Sort.Direction sortDirection = "DESC".equalsIgnoreCase(pagingData.getSortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(sortDirection, sortField);
        }

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        Specification<OrderDetail> spec = (root, query, cb) -> {
            query.distinct(true);

            Join<OrderDetail, Orders> order = root.join("order", JoinType.LEFT);
            Join<OrderDetail, EventType> eventType = root.join("eventType", JoinType.LEFT);
            Join<Orders, Event> event = order.join("event", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("active"), true));

            if (eventUuid != null) {
                predicates.add(cb.equal(event.get("uuid"), eventUuid));
            }

            if (startDate != null && endDate != null) {
                predicates.add(cb.between(order.get("createdTime"), startDate, endDate));
            }

            if (pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
                String field = pagingData.getSearchField().toLowerCase();
                String text = "%" + pagingData.getSearchText().toLowerCase() + "%";

                switch (field) {
                    case "eventname" -> predicates.add(cb.like(event.get("name"), text));
                    case "orderid" -> predicates.add(cb.like(order.get("orderNo"), text));
                    case "transactionid" -> predicates.add(cb.like(order.get("scbTransactionId"), text));
                    case "fullname" -> predicates
                            .add(cb.like(cb.concat(root.get("firstName"), cb.concat(" ", root.get("lastName"))), text));
                    case "eventtypename" -> predicates.add(cb.like(eventType.get("name"), text));
                    case "paymentstatus" -> predicates.add(cb.like(order.get("paymentStatus"), text));
                    case "paymentmethod" -> predicates.add(cb.like(order.get("paymentMethod"), text));
                    default -> predicates.add(cb.conjunction()); // ไม่ทำอะไร
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OrderDetail> result = orderDetailRepository.findAll(spec, pageable);

        return result.map(od -> new RegistrantSummaryDTO(
                od.getUuid(),
                od.getOrder().getEvent().getName(),
                od.getOrder().getOrderNo(),
                od.getOrder().getScbTransactionId(),
                formatDateTime(od.getOrder().getPaymentDateTime()),
                od.getFirstName() + " " + Optional.ofNullable(od.getLastName()).orElse(""),
                safeBigDecimal(od.getPrice()),
                safeBigDecimal(od.getCouponDiscount()),
                safeBigDecimal(od.getDiscountShirt()),
                safeBigDecimal(od.getShippingFee()),
                safeBigDecimal(od.getPrice())
                        .subtract(safeBigDecimal(od.getCouponDiscount()))
                        .subtract(safeBigDecimal(od.getDiscountShirt()))
                        .add(safeBigDecimal(od.getShippingFee())),
                od.getEventType().getName(),
                od.getOrder().getPaymentStatus(),
                od.getOrder().getPaymentMethod(),
                formatDateTime(od.getOrder().getCreatedTime())));
    }

    @Override
    public List<Map<String, Object>> getSummarizeOrderRegistrant(String eventUuid, String eventName,
            OffsetDateTime startDate, OffsetDateTime endDate) {
        String[] columns = { "order id", "ID Payment (transaction id)", "วันที่เวลาชำระเงินสำเร็จ",
                "ชื่อ-นามสกุลผู้ลงทะเบียน", "ค่าสมัคร", "ส่วนลดคูปอง", "ส่วนลดไม่รับเสื้อ", "ค่าจัดส่ง",
                "ยอดสุทธิ", "ประเภท", "สถานะ", "ช่องทางชำระเงิน", "วันที่ลงทะเบียน" };

        List<Map<String, Object>> results = orderRepository.summarizeRegistrant(eventUuid, startDate, endDate);
        List<Map<String, Object>> formatData = new ArrayList<>();

        Map<String, Object> datas = new HashMap<>();
        datas.put("sheetName", "Summary");
        datas.put("columns", columns);
        datas.put("preHeader", List.of("ชื่ออีเว้นท์", eventName));

        List<List<String>> sheetData = new ArrayList<>();

        for (Map<String, Object> result : results) {
            List<String> row = new ArrayList<>();

            BigDecimal registrationFee = safeBigDecimal(result.get("registrationFee"));
            BigDecimal discountCoupon = safeBigDecimal(result.get("discountCoupon"));
            BigDecimal discountShirt = safeBigDecimal(result.get("discountShirt"));
            BigDecimal shippingFee = safeBigDecimal(result.get("shippingFee"));
            BigDecimal totalAmount = registrationFee.subtract(discountCoupon).subtract(discountShirt).add(shippingFee);

            row.add(optString(result.get("orderId")));
            row.add(optString(result.get("transactionId")));
            row.add(formatDateTime(result.get("paymentDateTime")));
            row.add(optString(result.get("fullName")));
            row.add(formatDecimal(result.get("registrationFee")));
            row.add(formatDecimal(result.get("discountCoupon")));
            row.add(formatDecimal(result.get("discountShirt")));
            row.add(formatDecimal(result.get("shippingFee")));
            row.add(formatDecimal(totalAmount));
            row.add(optString(result.get("eventTypeName")));
            row.add(optString(result.get("paymentStatus")));
            row.add(optString(result.get("paymentMethod")));
            row.add(formatDateTime(result.get("registrationDateTime")));
            sheetData.add(row);
        }

        datas.put("datas", sheetData);
        formatData.add(datas);

        return formatData;
    }

    @Override
    public List<Map<String, Object>> getSummarizeRevenue(OffsetDateTime startDate, OffsetDateTime endDate) {
        String[] columns = { "เลขที่สัญญา", "อีเว้นท์", "ช่องทางรับชำระเงิน", "ค่าสมัครวิ่ง",
                "ค่าธรรมเนียมงานวิ่งที่เก็บเข้าระบบ Action", "ยอดรวม", "ค่าส่ง", "ยอดรวม + ค่าส่ง" };

        List<Map<String, Object>> results = orderRepository.summarizeRevenue(startDate, endDate);
        List<Map<String, Object>> formatData = new ArrayList<>();

        Map<String, Object> datas = new HashMap<>();
        datas.put("sheetName", "Summary");
        datas.put("columns", columns);

        List<List<String>> sheetData = new ArrayList<>();

        for (Map<String, Object> result : results) {
            BigDecimal registrationFee = safeBigDecimal(result.get("registrationFee"));
            BigDecimal shippingFee = safeBigDecimal(result.get("shippingFee"));
            String paymentMethod = optString(result.get("paymentMethod"));
            BigDecimal serviceFeeRate = getServiceFeeRate(paymentMethod);
            BigDecimal serviceFee = registrationFee.multiply(serviceFeeRate);
            BigDecimal total = registrationFee.add(serviceFee);
            BigDecimal totalWithShipping = total.add(shippingFee);

            List<String> dataRow = new ArrayList<>();
            dataRow.add(optString(result.get("contractNo")));
            dataRow.add(optString(result.get("eventName")));
            dataRow.add(paymentMethod);
            dataRow.add(formatDecimal(registrationFee));
            dataRow.add(formatDecimal(serviceFee));
            dataRow.add(formatDecimal(total));
            dataRow.add(formatDecimal(shippingFee));
            dataRow.add(formatDecimal(totalWithShipping));

            sheetData.add(dataRow);
        }

        datas.put("datas", sheetData);
        formatData.add(datas);

        return formatData;
    }

    @Override
    public List<Map<String, Object>> getSummarizeRevenueDetail(String eventUuid, String eventName,
            OffsetDateTime startDate, OffsetDateTime endDate) {
        String[] columns = { "order id", "ID Payment (transaction id)", "วันที่เวลาชำระเงินสำเร็จ",
                "ชื่อ-นามสกุลผู้ลงทะเบียน", "ค่าสมัคร", "ส่วนลดคูปอง", "ส่วนลดไม่รับเสื้อ", "ค่าจัดส่ง", "ยอดสุทธิ",
                "ค่าธรรมเนียม", "รวมค่าธรรมเนียม", "ประเภท", "ช่องทางชำระเงิน", "วันที่ลงทะเบียน" };

        List<Map<String, Object>> results = orderRepository.summarizeRevenueDetail(eventUuid, startDate, endDate);
        List<Map<String, Object>> formatData = new ArrayList<>();

        Map<String, Object> datas = new HashMap<>();
        datas.put("sheetName", "Summary");
        datas.put("columns", columns);
        datas.put("preHeader", List.of("ชื่ออีเว้นท์", eventName));

        List<List<String>> sheetData = new ArrayList<>();

        for (Map<String, Object> result : results) {

            BigDecimal price = safeBigDecimal(result.get("price"));
            BigDecimal discountCoupon = safeBigDecimal(result.get("discountCoupon"));
            BigDecimal discountShirt = safeBigDecimal(result.get("discountShirt"));
            BigDecimal shippingFee = safeBigDecimal(result.get("shippingFee"));
            BigDecimal netPrice = price.subtract(discountCoupon).subtract(discountShirt).add(shippingFee);
            String paymentMethod = optString(result.get("paymentMethod"));
            BigDecimal serviceFeeRate = getServiceFeeRate(paymentMethod);
            BigDecimal serviceFee = netPrice.multiply(serviceFeeRate);
            BigDecimal total = netPrice.add(serviceFee);
            List<String> row = new ArrayList<>();
            row.add(optString(result.get("orderId")));
            row.add(optString(result.get("transactionId")));
            row.add(formatDateTime(result.get("paymentDateTime")));
            row.add(optString(result.get("fullName")));
            row.add(formatDecimal(result.get("price")));
            row.add(formatDecimal(result.get("discountCoupon")));
            row.add(formatDecimal(result.get("discountShirt")));
            row.add(formatDecimal(result.get("shippingFee")));
            row.add(formatDecimal(netPrice));
            row.add(formatDecimal(serviceFee));
            row.add(formatDecimal(total));
            row.add(optString(result.get("eventTypeName")));
            row.add(optString(result.get("paymentMethod")));
            row.add(formatDateTime(result.get("registrationDateTime")));
            sheetData.add(row);
        }

        datas.put("datas", sheetData);
        formatData.add(datas);

        return formatData;
    }
}

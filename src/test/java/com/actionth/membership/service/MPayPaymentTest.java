package com.actionth.membership.service;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import com.actionth.membership.config.MPayConfig;
import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.constant.WebhookReasonType;
import com.actionth.membership.model.Orders;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.repository.PaymentWebhookLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/** mPAY signature and the webhook/enquiry settle rules, without Spring or a gateway. */
class MPayPaymentTest {

    private static final String SECRET = "b983ab7b986w636f8e5a6e2999wu633"; // sample from the spec

    private MPayPaymentService mPay;
    private PaymentWebhookService webhook;
    private OrderRepository orderRepository;
    private OrderService orderService;
    private Orders order;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        MPayConfig config = new MPayConfig();
        ReflectionTestUtils.setField(config, "channelSecret", SECRET);

        orderRepository = mock(OrderRepository.class);
        orderService = mock(OrderService.class);
        RedissonClient redisson = mock(RedissonClient.class);
        RBucket<Object> bucket = mock(RBucket.class);
        when(bucket.setIfAbsent(any())).thenReturn(true);
        when(redisson.getBucket(anyString())).thenReturn(bucket);
        DistributedLockService lock = mock(DistributedLockService.class);
        when(lock.executeWithLock(anyString(), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        // Runs the success e-mail callback inline, like a real transaction would.
        TransactionTemplate tx = mock(TransactionTemplate.class);
        doAnswer(inv -> {
            ((java.util.function.Consumer<TransactionStatus>) inv.getArgument(0)).accept(mock(TransactionStatus.class));
            return null;
        }).when(tx).executeWithoutResult(any());

        webhook = new PaymentWebhookService(mock(PaymentWebhookLogRepository.class), orderRepository, orderService,
                mock(EmailService.class), mock(AWSService.class), new ObjectMapper(), redisson, lock, tx);
        mPay = new MPayPaymentService(config, new ObjectMapper(), redisson, mock(AppErrorLogService.class), webhook);

        order = new Orders();
        order.setOrderNo("OR261102000001");
        order.setPaymentStatus(PaymentStatus.PENDING.toString());
        order.setPaymentMethod("qrcode");
        order.setTotalAmountWithFee(1030.00);
        when(orderRepository.findByOrderNo("OR261102000001")).thenReturn(Optional.of(order));
    }

    @Test
    void signsBodyPlusNonceAsHexHmacSha256() {
        // Expected value computed independently with Python's hmac module.
        String body = "{\"order_id\":\"OR261102000001ABCDEF\"}";
        assertEquals("ca5a8cd3122fc70ef6f74beddad304228c22d0b5cc84017ac57af2e2cb284167", mPay.sign(body, "nonce-1"));
        assertTrue(mPay.verifySignature(body, "nonce-1",
                "CA5A8CD3122FC70EF6F74BEDDAD304228C22D0B5CC84017AC57AF2E2CB284167"));
        assertFalse(mPay.verifySignature(body + " ", "nonce-1", mPay.sign(body, "nonce-1")));
        assertFalse(mPay.verifySignature(body, null, mPay.sign(body, "nonce-1")));
    }

    @Test
    void successfulPaymentSettlesTheOrderFromTheAttemptId() {
        Map<String, Object> result = webhook.processMPayResult(
                notice("OR261102000001K7Q2ZP", "SUCCESS", "PAYMENT", "SOF_PROMPTPAY", 1030.00), "{}", "WEBHOOK");

        assertEquals("SUCCESS", result.get("status"));
        verify(orderService).updatePaymentStatus("OR261102000001", "T1", PaymentStatus.SUCCESS);
    }

    @Test
    void amountMismatchGoesToReviewInsteadOfSuccess() {
        Map<String, Object> result = webhook.processMPayResult(
                notice("OR261102000001K7Q2ZP", "SUCCESS", "PAYMENT", "SOF_PROMPTPAY", 1000.00), "{}", "WEBHOOK");

        assertEquals("REVIEW", result.get("status"));
        assertEquals(PaymentStatus.REVIEW.toString(), order.getPaymentStatus());
        assertEquals(WebhookReasonType.AMOUNT_MISMATCH, order.getReviewReason());
        verify(orderService, never()).updatePaymentStatus(anyString(), anyString(), eq(PaymentStatus.SUCCESS));
    }

    @Test
    void methodMismatchGoesToReview() {
        Map<String, Object> result = webhook.processMPayResult(
                notice("OR261102000001K7Q2ZP", "SUCCESS", "PAYMENT", "SOF_CC", 1030.00), "{}", "WEBHOOK");

        assertEquals("REVIEW", result.get("status"));
        assertEquals(WebhookReasonType.PAYMENT_METHOD_MISMATCH, order.getReviewReason());
    }

    @Test
    void failedAttemptLeavesOrderPendingForRetry() {
        Map<String, Object> result = webhook.processMPayResult(
                notice("OR261102000001K7Q2ZP", "FAIL", "PAYMENT", "SOF_PROMPTPAY", 1030.00), "{}", "WEBHOOK");

        assertEquals("FAILED", result.get("status"));
        assertEquals(PaymentStatus.PENDING.toString(), order.getPaymentStatus());
    }

    @Test
    void refundNoticeNeverTouchesTheOrder() {
        order.setPaymentStatus(PaymentStatus.SUCCESS.toString());
        webhook.processMPayResult(
                notice("OR261102000001K7Q2ZP", "SUCCESS", "REFUND", "SOF_PROMPTPAY", 1030.00), "{}", "WEBHOOK");

        assertEquals(PaymentStatus.SUCCESS.toString(), order.getPaymentStatus());
        verify(orderService, never()).updatePaymentStatus(anyString(), anyString(), any());
    }

    private static Map<String, Object> notice(String orderId, String status, String txnType, String sof, double amount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order_id", orderId);
        m.put("txn_id", "T1");
        m.put("txn_type", txnType);
        m.put("status", status);
        m.put("sof_type", sof);
        m.put("amount", amount);
        m.put("currency", "THB");
        m.put("ref_1", "OR261102000001");
        return m;
    }
}

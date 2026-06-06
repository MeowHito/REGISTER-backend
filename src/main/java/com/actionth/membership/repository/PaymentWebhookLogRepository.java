package com.actionth.membership.repository;

import com.actionth.membership.model.PaymentWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {

    List<PaymentWebhookLog> findByTransactionId(String transactionId);

    List<PaymentWebhookLog> findByOrderNo(String orderNo);

    List<PaymentWebhookLog> findByOrderId(Integer orderId);

    List<PaymentWebhookLog> findByReasonType(String reasonType);

    List<PaymentWebhookLog> findByPaymentProvider(String paymentProvider);

    List<PaymentWebhookLog> findByLogType(String logType);

    List<PaymentWebhookLog> findByPaymentProviderAndLogType(String paymentProvider, String logType);

    List<PaymentWebhookLog> findByOrderNoAndLogType(String orderNo, String logType);
}

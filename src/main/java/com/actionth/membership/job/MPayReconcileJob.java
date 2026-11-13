package com.actionth.membership.job;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.service.MPayPaymentService;

import lombok.extern.slf4j.Slf4j;

/**
 * Safety net for a lost mPAY webhook: a runner who pays and then closes the browser is only
 * settled by the webhook, and the nightly overdue job would otherwise mark that paid order
 * FAILED. Every few minutes this asks mPAY about each PENDING order that started an mPAY
 * payment and settles it through the same checks as the webhook.
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class MPayReconcileJob implements Job {

    /** Orders that went overdue a little while ago are still checked once more. */
    private static final long GRACE_MINUTES = 60;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MPayPaymentService mPayPaymentService;

    @Override
    public void execute(JobExecutionContext context) {
        if (!mPayPaymentService.isActive()) {
            return;
        }
        List<String> orderNos = orderRepository.findPendingOrderNosDueAfter(OffsetDateTime.now().minusMinutes(GRACE_MINUTES));
        int checked = 0;
        int settled = 0;
        for (String orderNo : orderNos) {
            if (!mPayPaymentService.hasAttempts(orderNo)) {
                continue;
            }
            checked++;
            try {
                Map<String, Object> result = mPayPaymentService.enquireAndSettle(orderNo);
                if (Boolean.TRUE.equals(result.get("settled"))) {
                    settled++;
                    log.info("[mPAY reconcile] Settled orderNo={} without a webhook", orderNo);
                }
            } catch (Exception e) {
                log.warn("[mPAY reconcile] orderNo={} failed: {}", orderNo, e.getMessage());
            }
        }
        if (checked > 0) {
            log.info("[mPAY reconcile] checked={} settled={}", checked, settled);
        }
    }
}

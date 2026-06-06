package com.actionth.membership.job;

import java.time.OffsetDateTime;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.repository.CouponRepository;
import com.actionth.membership.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Quartz Job to update overdue payments and release coupons from failed orders.
 * Runs daily at midnight (00:00:00).
 */
@Slf4j
@Component
public class UpdateOverduePaymentsJob implements Job {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting UpdateOverduePaymentsJob at {}", OffsetDateTime.now());
            
            orderRepository.updateOverduePayments(OffsetDateTime.now());
            couponRepository.releaseCouponsFromFailedOrders();
            
            log.info("UpdateOverduePaymentsJob completed successfully");
        } catch (Exception e) {
            log.error("Error executing UpdateOverduePaymentsJob", e);
            throw new JobExecutionException("Failed to update overdue payments", e);
        }
    }
}

package com.actionth.membership.event;

import com.actionth.membership.model.Orders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import java.util.Objects;

/**
 * Spots payment-status transitions on {@link Orders} no matter which code path
 * made them (SCB / 2C2P webhooks, inquiry jobs, review resolution, test mode,
 * free orders), so notifications don't have to be wired into each one.
 * Hibernate builds this through Spring's bean container, hence the injection.
 */
public class OrderStatusListener {

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostLoad
    public void remember(Orders order) {
        order.setLoadedPaymentStatus(order.getPaymentStatus());
    }

    @PostPersist
    @PostUpdate
    public void publishIfChanged(Orders order) {
        String previous = order.getLoadedPaymentStatus();
        String current = order.getPaymentStatus();
        if (!Objects.equals(previous, current) && current != null && publisher != null) {
            publisher.publishEvent(new OrderStatusChangedEvent(order.getId(), previous, current));
        }
        order.setLoadedPaymentStatus(current);
    }
}

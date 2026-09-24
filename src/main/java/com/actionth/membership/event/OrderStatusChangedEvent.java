package com.actionth.membership.event;

/** An order moved into a new payment status (published by {@link OrderStatusListener}). */
public record OrderStatusChangedEvent(Integer orderId, String previousStatus, String newStatus) {
}

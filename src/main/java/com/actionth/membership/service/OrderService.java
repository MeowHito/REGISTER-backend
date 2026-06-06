package com.actionth.membership.service;

import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.OrderDto;
import com.actionth.membership.model.dto.OrderUpdateResponse;
import com.actionth.membership.model.request.OrderRequest;
import com.actionth.membership.model.request.OrderUpdateRequest;

public interface OrderService {

    OrderDto createOrder(OrderRequest orderRequest);

    void updatePaymentStatus(String orderNo, String refNo, PaymentStatus paymentStatus);

    Orders findByToken(String token);

    Orders findByUuid(String uuid);

    boolean updatePaymentMethod(Integer orderId, String paymentMethod);

    OrderUpdateResponse updateOrderPayment(OrderUpdateRequest request);

}

package com.ticketApi.payment.gateway;

import com.ticketApi.payment.entity.PaymentStatus;

public record PaymentGatewayResult(PaymentStatus status, String referencia) {
}

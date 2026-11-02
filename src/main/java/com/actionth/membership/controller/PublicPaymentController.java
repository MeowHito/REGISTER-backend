package com.actionth.membership.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.response.Response;
import com.actionth.membership.service.PaymentGatewayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/public-api")
@RequiredArgsConstructor
public class PublicPaymentController {

    private static final Pattern ORDER_NO = Pattern.compile("OR\\d{12}");

    private final PaymentGatewayService paymentGatewayService;

    @Value("${app.fe-url}")
    private String feUrl;

    /** Which gateway and methods the payment page should offer right now. */
    @GetMapping("/payment/options")
    public Response<Map<String, Object>> paymentOptions() {
        return new Response<>(paymentGatewayService.paymentOptions(), "Success", true);
    }

    /**
     * Where mPAY sends the runner back after the card / bank / wallet page. Some gateways return
     * with a POST, which a static SPA host cannot answer, so this bounces to the result page.
     * The outcome is never taken from here: the result page asks the backend, which trusts
     * only the signed webhook or its own enquiry.
     */
    @RequestMapping(value = "/mpay/return", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<Void> mpayReturn(@RequestParam(required = false) String orderNo) {
        String base = feUrl.endsWith("/") ? feUrl.substring(0, feUrl.length() - 1) : feUrl;
        String target = base + "/registrationPaymentResult";
        if (orderNo != null && ORDER_NO.matcher(orderNo.trim()).matches()) {
            target += "?invoiceNo=" + URLEncoder.encode(orderNo.trim(), StandardCharsets.UTF_8);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(target));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
}

package com.loft.hotel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class PayHereService {

    @Value("${payhere.merchant-id}")
    private String merchantId;

    @Value("${payhere.merchant-secret}")
    private String merchantSecret;

    @Value("${payhere.checkout-url}")
    private String checkoutUrl;

    public String getMerchantId() {
        return merchantId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    // Generate the hash sent to PayHere during checkout.
    // Amount must match the amount sent in the checkout form,
    // formatted to two decimal places (e.g. "60000.00").
    public String checkoutHash(String orderId, String amount, String currency) {
        validateConfiguration();

        if (orderId == null || amount == null || currency == null) {
            throw new IllegalArgumentException(
                    "Order ID, amount, and currency are required."
            );
        }

        return md5(
                merchantId + orderId + amount + currency + md5(merchantSecret)
        );
    }

    // Verify the signature of a notification received from PayHere.
    public boolean verifyNotify(
            String merchantIdIn,
            String orderId,
            String amount,
            String currency,
            String statusCode,
            String md5sig
    ) {
        validateConfiguration();

        if (merchantIdIn == null
                || orderId == null
                || amount == null
                || currency == null
                || statusCode == null
                || md5sig == null) {
            return false;
        }

        if (!merchantId.equals(merchantIdIn)
                || !md5sig.matches("(?i)[0-9a-f]{32}")) {
            return false;
        }

        String localSignature = md5(
                merchantIdIn + orderId + amount + currency
                        + statusCode + md5(merchantSecret)
        );

        return MessageDigest.isEqual(
                localSignature.getBytes(StandardCharsets.US_ASCII),
                md5sig.toUpperCase(Locale.ROOT)
                        .getBytes(StandardCharsets.US_ASCII)
        );
    }

    // Ensure PayHere credentials are configured.
    private void validateConfiguration() {
        if (merchantId == null || merchantId.isBlank()
                || merchantSecret == null || merchantSecret.isBlank()) {
            throw new IllegalStateException(
                    "PayHere merchant credentials are not configured."
            );
        }
    }

    // Generate an uppercase MD5 hexadecimal string.
    private String md5(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder(32);

            for (byte b : digest) {
                result.append(String.format(Locale.ROOT, "%02X", b & 0xFF));
            }

            return result.toString();

        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate MD5 hash.", e);
        }
    }
}

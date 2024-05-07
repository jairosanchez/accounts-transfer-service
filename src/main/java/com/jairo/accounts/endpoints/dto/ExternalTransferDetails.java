package com.jairo.accounts.endpoints.dto;

import java.math.BigDecimal;

public record ExternalTransferDetails(BigDecimal amount, String status, String address) {
}

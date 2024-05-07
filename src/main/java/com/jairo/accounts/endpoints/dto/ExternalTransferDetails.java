package com.jairo.accounts.endpoints.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExternalTransferDetails(UUID transferId, BigDecimal amount, String status, String address) {
}

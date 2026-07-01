package com.eduardo.financialcontrol.auth.dto;

import java.time.OffsetDateTime;

public record TokenResponse(String token, OffsetDateTime expiraEm) {}

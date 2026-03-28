package com.f1predict.league.dto;

import java.util.UUID;

public record LeagueResponse(
    UUID id,
    String name,
    String visibility,
    String inviteCode,  // only present for PRIVATE leagues when requester is admin
    UUID adminUserId,
    long memberCount
) {}

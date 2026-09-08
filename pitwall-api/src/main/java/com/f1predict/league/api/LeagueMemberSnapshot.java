package com.f1predict.league.api;

import java.util.UUID;

public record LeagueMemberSnapshot(UUID userId, int catchUpPoints) {}

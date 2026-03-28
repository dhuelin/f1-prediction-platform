package com.f1predict.league.dto;

public record JoinLeagueRequest(
    String inviteCode  // required for PRIVATE leagues, null for PUBLIC
) {}

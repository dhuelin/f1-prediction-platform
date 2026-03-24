package com.f1predict.f1data.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class OpenF1ClientTest {
    static WireMockServer wireMock = new WireMockServer(0);
    OpenF1Client client;

    @BeforeAll static void startWireMock() { wireMock.start(); }
    @AfterAll static void stopWireMock() { wireMock.stop(); }

    @BeforeEach void setUp() {
        wireMock.resetAll();
        client = new OpenF1Client("http://localhost:" + wireMock.port());
    }

    @Test
    void fetchSessions_parsesSessionListCorrectly() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/sessions"))
            .withQueryParam("year", equalTo("2025"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    [{"session_key":9158,"session_name":"Race","session_type":"Race",
                      "date_start":"2025-03-16T15:00:00+00:00","year":2025}]
                    """)));

        var sessions = client.fetchSessions(2025);
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).sessionKey()).isEqualTo(9158);
        assertThat(sessions.get(0).sessionType()).isEqualTo("Race");
    }

    @Test
    void fetchSessions_returnsEmptyList_whenNoSessions() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/sessions"))
            .withQueryParam("year", equalTo("2025"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));

        var sessions = client.fetchSessions(2025);
        assertThat(sessions).isEmpty();
    }

    @Test
    void fetchLivePositions_parsesPositionsCorrectly() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/position"))
            .withQueryParam("session_key", equalTo("9158"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    [{"driver_number":1,"position":3,"date":"2025-03-16T15:05:00.000000+00:00"},
                     {"driver_number":4,"position":1,"date":"2025-03-16T15:05:01.000000+00:00"}]
                    """)));

        var positions = client.fetchLivePositions(9158);
        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).driverNumber()).isEqualTo(1);
        assertThat(positions.get(0).position()).isEqualTo(3);
    }
}

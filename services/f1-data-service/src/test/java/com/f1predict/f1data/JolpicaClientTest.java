package com.f1predict.f1data;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.f1predict.f1data.client.JolpicaClient;
import org.junit.jupiter.api.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class JolpicaClientTest {
    static WireMockServer wireMock = new WireMockServer(0);
    JolpicaClient client;

    @BeforeAll static void startWireMock() { wireMock.start(); }
    @AfterAll static void stopWireMock() { wireMock.stop(); }

    @BeforeEach void setUp() {
        wireMock.resetAll();
        client = new JolpicaClient("http://localhost:" + wireMock.port());
    }

    @Test
    void fetchCalendar_parsesRaceListCorrectly() {
        wireMock.stubFor(get(urlEqualTo("/ergast/f1/2025.json"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"MRData":{"RaceTable":{"season":"2025","Races":[
                        {"round":"1","raceName":"Bahrain Grand Prix",
                         "Circuit":{"circuitName":"Bahrain International Circuit",
                         "Location":{"country":"Bahrain"}},
                         "date":"2025-03-16","time":"15:00:00Z"}
                    ]}}}
                    """)));

        var races = client.fetchCalendar(2025);
        assertThat(races).hasSize(1);
        assertThat(races.get(0).raceName()).isEqualTo("Bahrain Grand Prix");
        assertThat(races.get(0).circuit().location().country()).isEqualTo("Bahrain");
        assertThat(races.get(0).isSprintWeekend()).isFalse();
    }

    @Test
    void fetchCalendar_detectsSprintWeekend() {
        wireMock.stubFor(get(urlEqualTo("/ergast/f1/2025.json"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"MRData":{"RaceTable":{"season":"2025","Races":[
                        {"round":"5","raceName":"Miami Grand Prix",
                         "Circuit":{"circuitName":"Miami International Autodrome",
                         "Location":{"country":"USA"}},
                         "date":"2025-05-04","time":"19:00:00Z",
                         "Sprint":{"date":"2025-05-03","time":"19:00:00Z"}}
                    ]}}}
                    """)));

        var races = client.fetchCalendar(2025);
        assertThat(races).hasSize(1);
        assertThat(races.get(0).isSprintWeekend()).isTrue();
    }

    @Test
    void fetchRaceResults_parsesResultsCorrectly() {
        wireMock.stubFor(get(urlEqualTo("/ergast/f1/2025/1/results.json"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"MRData":{"RaceTable":{"Races":[
                        {"Results":[
                            {"position":"1","Driver":{"code":"VER"},"status":"Finished","FastestLap":{"rank":"1"}},
                            {"position":"2","Driver":{"code":"NOR"},"status":"Finished"}
                        ]}
                    ]}}}
                    """)));

        var results = client.fetchRaceResults(2025, 1);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).driver().code()).isEqualTo("VER");
        assertThat(results.get(0).isFastestLap()).isTrue();
        assertThat(results.get(1).isFastestLap()).isFalse();
    }
}

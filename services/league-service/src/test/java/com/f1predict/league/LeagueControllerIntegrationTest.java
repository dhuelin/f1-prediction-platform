package com.f1predict.league;

import com.f1predict.league.repository.LeagueMemberRepository;
import com.f1predict.league.repository.LeagueRepository;
import com.f1predict.league.repository.ScoringConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LeagueControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "5672");
    }

    @MockBean
    RabbitTemplate rabbitTemplate;

    @Autowired MockMvc mockMvc;
    @Autowired LeagueRepository leagueRepository;
    @Autowired LeagueMemberRepository memberRepository;
    @Autowired ScoringConfigRepository configRepository;

    private final UUID adminId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        configRepository.deleteAll();
        memberRepository.deleteAll();
        leagueRepository.deleteAll();
    }

    @Test
    void createPublicLeague_returns201() throws Exception {
        mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Test League","visibility":"PUBLIC"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test League"))
            .andExpect(jsonPath("$.visibility").value("PUBLIC"))
            .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void createLeague_duplicateName_returns409() throws Exception {
        mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Dupe League"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", UUID.randomUUID())
                .content("""
                    {"name":"Dupe League"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void createPrivateLeague_returnsInviteCodeToAdmin() throws Exception {
        mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Private League","visibility":"PRIVATE"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.inviteCode").isNotEmpty());
    }

    @Test
    void joinPublicLeague_returns201() throws Exception {
        // Create league
        String response = mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Join Test League"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String leagueId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/leagues/" + leagueId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", memberId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.memberCount").value(2));
    }

    @Test
    void joinLeague_alreadyMember_returns409() throws Exception {
        String response = mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Dupe Member League"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String leagueId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/leagues/" + leagueId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId))
            .andExpect(status().isConflict());
    }

    @Test
    void getScoringConfig_returnsDefaults() throws Exception {
        String response = mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Config League"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String leagueId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/leagues/" + leagueId + "/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.predictionDepth").value(10))
            .andExpect(jsonPath("$.exactPositionPoints").value(10));
    }

    @Test
    void updateConfig_nonAdmin_returns403() throws Exception {
        String response = mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Config403 League"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String leagueId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/leagues/" + leagueId + "/config?nextRace=2")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", UUID.randomUUID())
                .content("""
                    {"predictionDepth":5,"exactPositionPoints":8,"inRangePoints":1}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_nonAdmin_returns403() throws Exception {
        String response = mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Remove403 League"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String leagueId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(delete("/leagues/" + leagueId + "/members/" + memberId)
                .header("X-User-Id", UUID.randomUUID()))
            .andExpect(status().isForbidden());
    }

    @Test
    void transferAdmin_updatesAdminUserId() throws Exception {
        // Create league
        String response = mockMvc.perform(post("/leagues")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", adminId)
                .content("""
                    {"name":"Transfer League"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String leagueId = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Add new admin as member first
        mockMvc.perform(post("/leagues/" + leagueId + "/join")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", memberId))
            .andExpect(status().isCreated());

        // Transfer admin
        mockMvc.perform(post("/leagues/" + leagueId + "/admin/transfer/" + memberId)
                .header("X-User-Id", adminId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.adminUserId").value(memberId.toString()));
    }
}

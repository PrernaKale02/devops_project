package com.prerna.sponsorship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.prerna.sponsorship.model.Sponsorship;
import com.prerna.sponsorship.repository.SponsorshipRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class ChildSponsorshipApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SponsorshipRepository repository;

    @BeforeEach
    void clearData() {
        repository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

        @Test
        void landingPageLinksToSponsorshipManagement() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/sponsorships")));
        }

        @Test
        void sponsorshipCrudSearchDashboardAndStatusWorkflow() throws Exception {
        Sponsorship sponsorship = new Sponsorship();
        sponsorship.setChildId("CH-001");
        sponsorship.setChildName("Asha");
        sponsorship.setSponsorName("Ravi");
        sponsorship.setStatus(Sponsorship.Status.PENDING);
        sponsorship.setStartDate(LocalDate.of(2026, 1, 1));
        sponsorship = repository.save(sponsorship);

        mockMvc.perform(get("/sponsorships").param("search", "CH-001"))
            .andExpect(status().isOk())
            .andExpect(view().name("sponsorships"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Asha")));

        mockMvc.perform(get("/sponsorships/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Pending")));

        mockMvc.perform(post("/sponsorships/{id}/status", sponsorship.getId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("status", "ACTIVE"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/sponsorships"));

        org.junit.jupiter.api.Assertions.assertEquals(
            Sponsorship.Status.ACTIVE,
            repository.findById(sponsorship.getId()).orElseThrow().getStatus());
        }
}

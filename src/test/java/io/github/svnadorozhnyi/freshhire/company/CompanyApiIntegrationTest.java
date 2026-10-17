package io.github.svnadorozhnyi.freshhire.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.svnadorozhnyi.freshhire.TestcontainersConfiguration;
import java.net.URI;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CompanyApiIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final String ACME_BODY = """
            {"name": "Acme", "atsType": "GREENHOUSE", "boardToken": "acme"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void cleanDatabase() {
        companyRepository.deleteAll();
    }

    @Test
    void createdCompanyCanBeFetchedByLocation() throws Exception {
        String location = mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACME_BODY))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION);

        assertThat(location).isNotNull();
        mockMvc.perform(get(URI.create(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createReturnsConflictForDuplicateAtsTypeAndBoardToken() throws Exception {
        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACME_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACME_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivatedCompanyIsExcludedFromActiveList() throws Exception {
        Company company = companyRepository.save(new Company("Acme", AtsType.GREENHOUSE, "acme", NOW));

        mockMvc.perform(post("/api/v1/companies/{id}/deactivate", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/companies").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/companies").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(company.getId()));
    }

    @Test
    void listReturnsBadRequestForUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/v1/companies").param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown sort property: foo"));
    }

    @Test
    void databaseRejectsDuplicateAtsTypeAndBoardToken() {
        companyRepository.saveAndFlush(new Company("Acme", AtsType.GREENHOUSE, "acme", NOW));

        assertThatThrownBy(
                        () -> companyRepository.saveAndFlush(new Company("Acme copy", AtsType.GREENHOUSE, "acme", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(
                        ConstraintViolationException.class,
                        violation -> assertThat(violation.getConstraintName())
                                .isEqualToIgnoringCase(CompanyService.UNIQUE_ATS_TYPE_BOARD_TOKEN));
    }

    @Test
    void updateReturnsConflictWhenPairBelongsToAnotherCompany() throws Exception {
        companyRepository.save(new Company("Acme", AtsType.GREENHOUSE, "acme", NOW));
        Company other = companyRepository.save(new Company("Other", AtsType.LEVER, "other", NOW));

        mockMvc.perform(put("/api/v1/companies/{id}", other.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACME_BODY))
                .andExpect(status().isConflict());
    }
}

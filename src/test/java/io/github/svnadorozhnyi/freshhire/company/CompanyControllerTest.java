package io.github.svnadorozhnyi.freshhire.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.svnadorozhnyi.freshhire.common.PageResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final String VALID_BODY = """
            {"name": "Acme", "atsType": "GREENHOUSE", "boardToken": "acme"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @Test
    void createReturnsCreatedWithLocationHeader() throws Exception {
        when(companyService.create(any(CreateCompanyRequestDto.class))).thenReturn(companyResponse());

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/api/v1/companies/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void createReturnsBadRequestWhenNameIsBlank() throws Exception {
        String body = """
                {"name": "", "atsType": "GREENHOUSE", "boardToken": "acme"}
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        verify(companyService, never()).create(any());
    }

    @Test
    void createReturnsBadRequestWhenAtsTypeIsUnknown() throws Exception {
        String body = """
                {"name": "Acme", "atsType": "WORKDAY", "boardToken": "acme"}
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("atsType"))
                .andExpect(jsonPath("$.errors[0].message").value("must be one of: GREENHOUSE, LEVER"));
        verify(companyService, never()).create(any());
    }

    @Test
    void createReturnsBadRequestWhenJsonIsMalformed() throws Exception {
        String body = """
                {"name": "Acme", "atsType": "GREENHOUSE"
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Malformed JSON request body"))
                .andExpect(jsonPath("$.errors").doesNotExist());
        verify(companyService, never()).create(any());
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            255, 201
            256, 400
            """)
    void createValidatesMaximumNameLength(int nameLength, int expectedStatus) throws Exception {
        when(companyService.create(any(CreateCompanyRequestDto.class))).thenReturn(companyResponse());
        String body = """
                {"name": "%s", "atsType": "GREENHOUSE", "boardToken": "acme"}
                """.formatted("a".repeat(nameLength));

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    @ParameterizedTest
    @ValueSource(strings = {"acme/jobs", "acme?x=1", "..", "ac me", "acme%2F"})
    void createReturnsBadRequestWhenBoardTokenHasUnsafeCharacters(String boardToken) throws Exception {
        String body = """
                {"name": "Acme", "atsType": "GREENHOUSE", "boardToken": "%s"}
                """.formatted(boardToken);

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("boardToken"));
        verify(companyService, never()).create(any());
    }

    @Test
    void createStripsSurroundingWhitespaceFromBoardToken() throws Exception {
        when(companyService.create(any(CreateCompanyRequestDto.class))).thenReturn(companyResponse());
        String body = """
                {"name": "Acme", "atsType": "GREENHOUSE", "boardToken": "  acme_inc  "}
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(companyService).create(new CreateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "acme_inc"));
    }

    @Test
    void createReturnsConflictWhenCompanyAlreadyExists() throws Exception {
        when(companyService.create(any(CreateCompanyRequestDto.class)))
                .thenThrow(new DuplicateCompanyException(AtsType.GREENHOUSE, "acme"));

        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getByIdReturnsCompany() throws Exception {
        when(companyService.getById(1L)).thenReturn(companyResponse());

        mockMvc.perform(get("/api/v1/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"))
                .andExpect(jsonPath("$.atsType").value("GREENHOUSE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getByIdReturnsNotFoundWhenCompanyIsMissing() throws Exception {
        when(companyService.getById(42L)).thenThrow(new CompanyNotFoundException(42L));

        mockMvc.perform(get("/api/v1/companies/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Company not found with id: 42"));
    }

    @Test
    void listUsesDefaultPageSizeAndSortById() throws Exception {
        when(companyService.list(isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(companyResponse()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Acme"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(companyService).list(isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void listCapsPageSizeAtConfiguredMaximum() throws Exception {
        when(companyService.list(isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

        mockMvc.perform(get("/api/v1/companies").param("size", "1000")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(companyService).list(isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listPassesActiveFilterToService() throws Exception {
        when(companyService.list(eq(false), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/companies").param("active", "false")).andExpect(status().isOk());

        verify(companyService).list(eq(false), any(Pageable.class));
    }

    @Test
    void updateReturnsUpdatedCompany() throws Exception {
        when(companyService.update(eq(1L), any(UpdateCompanyRequestDto.class))).thenReturn(companyResponse());

        mockMvc.perform(put("/api/v1/companies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsBadRequestWhenBoardTokenIsBlank() throws Exception {
        String body = """
                {"name": "Acme", "atsType": "GREENHOUSE", "boardToken": ""}
                """;

        mockMvc.perform(put("/api/v1/companies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field").value(everyItem(is("boardToken"))));
        verify(companyService, never()).update(any(), any());
    }

    @Test
    void updateReturnsNotFoundWhenCompanyIsMissing() throws Exception {
        when(companyService.update(eq(42L), any(UpdateCompanyRequestDto.class)))
                .thenThrow(new CompanyNotFoundException(42L));

        mockMvc.perform(put("/api/v1/companies/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void deactivateReturnsDeactivatedCompany() throws Exception {
        when(companyService.deactivate(1L)).thenReturn(companyResponse(false));

        mockMvc.perform(post("/api/v1/companies/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(false));

        verify(companyService).deactivate(1L);
    }

    @Test
    void activateReturnsActivatedCompany() throws Exception {
        when(companyService.activate(1L)).thenReturn(companyResponse(true));

        mockMvc.perform(post("/api/v1/companies/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(true));

        verify(companyService).activate(1L);
    }

    @Test
    void activateReturnsNotFoundWhenCompanyIsMissing() throws Exception {
        when(companyService.activate(42L)).thenThrow(new CompanyNotFoundException(42L));

        mockMvc.perform(post("/api/v1/companies/42/activate")).andExpect(status().isNotFound());
    }

    private static CompanyResponseDto companyResponse() {
        return companyResponse(true);
    }

    private static CompanyResponseDto companyResponse(boolean active) {
        return new CompanyResponseDto(1L, "Acme", AtsType.GREENHOUSE, "acme", active, null, null, NOW, NOW);
    }
}

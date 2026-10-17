package io.github.svnadorozhnyi.freshhire.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.svnadorozhnyi.freshhire.common.PageResponse;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-01-01T00:00:00Z");
    private static final Long COMPANY_ID = 1L;

    @Mock
    private CompanyRepository companyRepository;

    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        companyService = new CompanyService(companyRepository, new CompanyMapperImpl(), clock);
    }

    @Test
    void createsCompanyWhenAtsTypeAndBoardTokenAreUnique() {
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.GREENHOUSE, "acme"))
                .thenReturn(false);
        when(companyRepository.saveAndFlush(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponseDto response =
                companyService.create(new CreateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "acme"));

        assertThat(response.name()).isEqualTo("Acme");
        assertThat(response.atsType()).isEqualTo(AtsType.GREENHOUSE);
        assertThat(response.boardToken()).isEqualTo("acme");
        assertThat(response.active()).isTrue();
    }

    @Test
    void throwsDuplicateWhenAtsTypeAndBoardTokenAlreadyExist() {
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.GREENHOUSE, "acme"))
                .thenReturn(true);

        assertThatThrownBy(() -> companyService.create(new CreateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "acme")))
                .isInstanceOf(DuplicateCompanyException.class);
        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    void setsCreatedAtAndUpdatedAtFromClock() {
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.GREENHOUSE, "acme"))
                .thenReturn(false);
        when(companyRepository.saveAndFlush(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyResponseDto response =
                companyService.create(new CreateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "acme"));

        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void createThrowsDuplicateWhenConcurrentInsertViolatesUniqueConstraint() {
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.GREENHOUSE, "acme"))
                .thenReturn(false);
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenThrow(integrityViolation(CompanyService.UNIQUE_ATS_TYPE_BOARD_TOKEN));

        assertThatThrownBy(() -> companyService.create(new CreateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "acme")))
                .isInstanceOf(DuplicateCompanyException.class);
    }

    @Test
    void createRethrowsIntegrityViolationOfOtherConstraints() {
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.GREENHOUSE, "acme"))
                .thenReturn(false);
        DataIntegrityViolationException violation = integrityViolation("ck_companies_ats_type");
        when(companyRepository.saveAndFlush(any(Company.class))).thenThrow(violation);

        assertThatThrownBy(() -> companyService.create(new CreateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "acme")))
                .isSameAs(violation);
    }

    @Test
    void updateThrowsDuplicateWhenConcurrentUpdateViolatesUniqueConstraint() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(existingCompany()));
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.LEVER, "acme-lever"))
                .thenReturn(false);
        doThrow(integrityViolation(CompanyService.UNIQUE_ATS_TYPE_BOARD_TOKEN))
                .when(companyRepository)
                .flush();

        assertThatThrownBy(() -> companyService.update(
                        COMPANY_ID, new UpdateCompanyRequestDto("Acme", AtsType.LEVER, "acme-lever")))
                .isInstanceOf(DuplicateCompanyException.class);
    }

    @Test
    void throwsNotFoundWhenCompanyDoesNotExist() {
        when(companyRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getById(42L)).isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void getByIdReturnsCompany() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(existingCompany()));

        CompanyResponseDto response = companyService.getById(COMPANY_ID);

        assertThat(response.id()).isEqualTo(COMPANY_ID);
        assertThat(response.name()).isEqualTo("Acme");
    }

    @Test
    void updateChangesPairWhenNewPairIsFree() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(existingCompany()));
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.LEVER, "acme-lever"))
                .thenReturn(false);

        CompanyResponseDto response =
                companyService.update(COMPANY_ID, new UpdateCompanyRequestDto("Acme", AtsType.LEVER, "acme-lever"));

        assertThat(response.atsType()).isEqualTo(AtsType.LEVER);
        assertThat(response.boardToken()).isEqualTo("acme-lever");
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void updateThrowsNotFoundWhenCompanyDoesNotExist() {
        when(companyRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        companyService.update(42L, new UpdateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "acme")))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void updateSkipsDuplicateCheckWhenPairIsUnchanged() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(existingCompany()));

        CompanyResponseDto response =
                companyService.update(COMPANY_ID, new UpdateCompanyRequestDto("Acme Inc", AtsType.GREENHOUSE, "acme"));

        assertThat(response.name()).isEqualTo("Acme Inc");
        assertThat(response.updatedAt()).isEqualTo(NOW);
        verify(companyRepository, never()).existsByAtsTypeAndBoardToken(any(), any());
    }

    @Test
    void updateThrowsDuplicateWhenNewPairIsTaken() {
        Company company = existingCompany();
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));
        when(companyRepository.existsByAtsTypeAndBoardToken(AtsType.GREENHOUSE, "taken"))
                .thenReturn(true);

        assertThatThrownBy(() -> companyService.update(
                        COMPANY_ID, new UpdateCompanyRequestDto("Acme", AtsType.GREENHOUSE, "taken")))
                .isInstanceOf(DuplicateCompanyException.class);
        assertThat(company.getBoardToken()).isEqualTo("acme");
    }

    @Test
    void deactivateSetsActiveToFalse() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(existingCompany()));

        CompanyResponseDto response = companyService.deactivate(COMPANY_ID);

        assertThat(response.active()).isFalse();
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void activateSetsActiveToTrue() {
        Company company = existingCompany();
        company.deactivate(EARLIER);
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        CompanyResponseDto response = companyService.activate(COMPANY_ID);

        assertThat(response.active()).isTrue();
        assertThat(response.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void listWithoutFilterReturnsAllCompanies() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Company> page = new PageImpl<>(List.of(existingCompany()), pageable, 1);
        when(companyRepository.findAll(pageable)).thenReturn(page);

        PageResponse<CompanyResponseDto> response = companyService.list(null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        verify(companyRepository, never()).findAllByActive(anyBoolean(), any());
    }

    @Test
    void listWithActiveFilterUsesFilteredQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Company> page = new PageImpl<>(List.of(existingCompany()), pageable, 1);
        when(companyRepository.findAllByActive(true, pageable)).thenReturn(page);

        PageResponse<CompanyResponseDto> response = companyService.list(true, pageable);

        assertThat(response.content()).hasSize(1);
        verify(companyRepository, never()).findAll(any(Pageable.class));
    }

    private static DataIntegrityViolationException integrityViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "constraint violated",
                new ConstraintViolationException("constraint violated", new SQLException(), constraintName));
    }

    private Company existingCompany() {
        Company company = new Company("Acme", AtsType.GREENHOUSE, "acme", EARLIER);
        ReflectionTestUtils.setField(company, "id", COMPANY_ID);
        return company;
    }
}

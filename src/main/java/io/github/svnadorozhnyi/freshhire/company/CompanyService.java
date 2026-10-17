package io.github.svnadorozhnyi.freshhire.company;

import io.github.svnadorozhnyi.freshhire.common.PageResponse;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    static final String UNIQUE_ATS_TYPE_BOARD_TOKEN = "uq_companies_ats_type_board_token";

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final Clock clock;

    @Transactional
    public CompanyResponseDto create(CreateCompanyRequestDto requestDto) {
        boolean alreadyExists =
                companyRepository.existsByAtsTypeAndBoardToken(requestDto.atsType(), requestDto.boardToken());
        if (alreadyExists) {
            throw new DuplicateCompanyException(requestDto.atsType(), requestDto.boardToken());
        }
        Instant now = Instant.now(clock);
        Company company = new Company(requestDto.name(), requestDto.atsType(), requestDto.boardToken(), now);
        try {
            Company savedCompany = companyRepository.saveAndFlush(company);
            return companyMapper.toResponse(savedCompany);
        } catch (DataIntegrityViolationException ex) {
            throw translateUniqueViolation(ex, requestDto.atsType(), requestDto.boardToken());
        }
    }

    @Transactional(readOnly = true)
    public CompanyResponseDto getById(Long id) {
        return companyMapper.toResponse(findCompanyOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponseDto> list(Boolean active, Pageable pageable) {
        Page<Company> companies = (active == null)
                ? companyRepository.findAll(pageable)
                : companyRepository.findAllByActive(active, pageable);
        return PageResponse.from(companies.map(companyMapper::toResponse));
    }

    @Transactional
    public CompanyResponseDto update(Long id, UpdateCompanyRequestDto requestDto) {
        Company company = findCompanyOrThrow(id);
        boolean pairChanged = company.getAtsType() != requestDto.atsType()
                || !company.getBoardToken().equals(requestDto.boardToken());
        if (pairChanged
                && companyRepository.existsByAtsTypeAndBoardToken(requestDto.atsType(), requestDto.boardToken())) {
            throw new DuplicateCompanyException(requestDto.atsType(), requestDto.boardToken());
        }
        company.updateDetails(requestDto.name(), requestDto.atsType(), requestDto.boardToken(), Instant.now(clock));
        try {
            companyRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translateUniqueViolation(ex, requestDto.atsType(), requestDto.boardToken());
        }
        return companyMapper.toResponse(company);
    }

    @Transactional
    public CompanyResponseDto activate(Long id) {
        Company company = findCompanyOrThrow(id);
        company.activate(Instant.now(clock));
        return companyMapper.toResponse(company);
    }

    @Transactional
    public CompanyResponseDto deactivate(Long id) {
        Company company = findCompanyOrThrow(id);
        company.deactivate(Instant.now(clock));
        return companyMapper.toResponse(company);
    }

    private Company findCompanyOrThrow(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
    }

    private static RuntimeException translateUniqueViolation(
            DataIntegrityViolationException ex, AtsType atsType, String boardToken) {
        if (ex.getCause() instanceof ConstraintViolationException violation
                && UNIQUE_ATS_TYPE_BOARD_TOKEN.equalsIgnoreCase(violation.getConstraintName())) {
            return new DuplicateCompanyException(atsType, boardToken);
        }
        return ex;
    }
}

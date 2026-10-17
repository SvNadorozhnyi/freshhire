package io.github.svnadorozhnyi.freshhire.company;

import io.github.svnadorozhnyi.freshhire.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Companies", description = "Registry of companies whose ATS job boards FreshHire collects")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Register a company")
    @ApiResponse(responseCode = "201", description = "Company created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "Company with this ATS type and board token already exists")
    @PostMapping
    public ResponseEntity<CompanyResponseDto> create(@Valid @RequestBody CreateCompanyRequestDto request) {
        CompanyResponseDto created = companyService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List companies", description = "Returns a page of companies sorted by id by default.")
    @ApiResponse(responseCode = "200", description = "Page of companies")
    @GetMapping
    public PageResponse<CompanyResponseDto> list(
            @Parameter(description = "Filter by active flag; omit to return all companies")
                    @RequestParam(required = false)
                    Boolean active,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return companyService.list(active, pageable);
    }

    @Operation(summary = "Get a company by id")
    @ApiResponse(responseCode = "200", description = "Company found")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @GetMapping("/{id}")
    public CompanyResponseDto getById(@PathVariable Long id) {
        return companyService.getById(id);
    }

    @Operation(summary = "Replace company details")
    @ApiResponse(responseCode = "200", description = "Company updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @ApiResponse(responseCode = "409", description = "Another company already uses this ATS type and board token")
    @PutMapping("/{id}")
    public CompanyResponseDto update(@PathVariable Long id, @Valid @RequestBody UpdateCompanyRequestDto requestDto) {
        return companyService.update(id, requestDto);
    }

    @Operation(summary = "Deactivate a company", description = "Inactive companies are skipped by collectors.")
    @ApiResponse(responseCode = "200", description = "Company deactivated")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @PostMapping("/{id}/deactivate")
    public CompanyResponseDto deactivate(@PathVariable Long id) {
        return companyService.deactivate(id);
    }

    @Operation(summary = "Activate a company")
    @ApiResponse(responseCode = "200", description = "Company activated")
    @ApiResponse(responseCode = "404", description = "Company not found")
    @PostMapping("/{id}/activate")
    public CompanyResponseDto activate(@PathVariable Long id) {
        return companyService.activate(id);
    }
}

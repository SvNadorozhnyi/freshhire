package io.github.svnadorozhnyi.freshhire.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequestDto(
        @NotBlank @Size(max = 255) String name,
        @NotNull AtsType atsType,

        @NotBlank @Size(max = 255) @Pattern(regexp = BoardTokens.ALLOWED_PATTERN, message = BoardTokens.ALLOWED_MESSAGE)
        String boardToken) {

    public UpdateCompanyRequestDto {
        boardToken = BoardTokens.strip(boardToken);
    }
}

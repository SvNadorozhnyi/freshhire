package io.github.svnadorozhnyi.freshhire.company;

import lombok.Getter;

@Getter
public class DuplicateCompanyException extends RuntimeException {
    private final AtsType atsType;
    private final String boardToken;

    public DuplicateCompanyException(AtsType atsType, String boardToken) {
        super("Company with ATS type %s and board token %s already exists".formatted(atsType, boardToken));
        this.atsType = atsType;
        this.boardToken = boardToken;
    }
}

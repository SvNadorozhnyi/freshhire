package io.github.svnadorozhnyi.freshhire.company;

import lombok.Getter;

@Getter
public class CompanyNotFoundException extends RuntimeException {
    private final Long id;

    public CompanyNotFoundException(Long id) {
        super("Company not found with id: " + id);
        this.id = id;
    }
}

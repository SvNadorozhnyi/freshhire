package io.github.svnadorozhnyi.freshhire.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByAtsTypeAndBoardToken(AtsType atsType, String boardToken);

    Page<Company> findAllByActive(boolean active, Pageable pageable);
}

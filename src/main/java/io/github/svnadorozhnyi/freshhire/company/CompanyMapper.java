package io.github.svnadorozhnyi.freshhire.company;

import io.github.svnadorozhnyi.freshhire.common.SharedMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = SharedMapperConfig.class)
public interface CompanyMapper {

    CompanyResponseDto toResponse(Company company);
}

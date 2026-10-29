package io.github.svnadorozhnyi.freshhire.collector;

import io.github.svnadorozhnyi.freshhire.company.AtsType;
import java.util.List;

public interface AtsCollector {

    AtsType atsType();

    List<CollectedPosting> fetchPostings(String boardToken);
}

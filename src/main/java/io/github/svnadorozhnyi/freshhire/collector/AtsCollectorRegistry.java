package io.github.svnadorozhnyi.freshhire.collector;

import io.github.svnadorozhnyi.freshhire.company.AtsType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AtsCollectorRegistry {

    private final Map<AtsType, AtsCollector> collectorsByType = new EnumMap<>(AtsType.class);

    public AtsCollectorRegistry(List<AtsCollector> collectors) {
        for (AtsCollector collector : collectors) {
            AtsCollector previous = collectorsByType.put(collector.atsType(), collector);
            if (previous != null) {
                throw new IllegalStateException("Duplicate collectors for ATS type %s: %s and %s"
                        .formatted(
                                collector.atsType(),
                                previous.getClass().getSimpleName(),
                                collector.getClass().getSimpleName()));
            }
        }
    }

    public AtsCollector getCollector(AtsType atsType) {
        AtsCollector collector = collectorsByType.get(atsType);
        if (collector == null) {
            throw new IllegalArgumentException("No collector registered for ATS type " + atsType);
        }
        return collector;
    }
}

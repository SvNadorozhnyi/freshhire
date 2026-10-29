package io.github.svnadorozhnyi.freshhire.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.svnadorozhnyi.freshhire.company.AtsType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AtsCollectorRegistryTest {

    private final AtsCollector greenhouseCollector = new FakeCollector(AtsType.GREENHOUSE);
    private final AtsCollector leverCollector = new FakeCollector(AtsType.LEVER);

    @ParameterizedTest
    @EnumSource(AtsType.class)
    void returnsCollectorRegisteredForEachAtsType(AtsType atsType) {
        AtsCollectorRegistry registry = new AtsCollectorRegistry(List.of(greenhouseCollector, leverCollector));

        AtsCollector collector = registry.getCollector(atsType);

        assertThat(collector.atsType()).isEqualTo(atsType);
    }

    @Test
    void returnsTheSameInstanceThatWasRegistered() {
        AtsCollectorRegistry registry = new AtsCollectorRegistry(List.of(greenhouseCollector, leverCollector));

        assertThat(registry.getCollector(AtsType.GREENHOUSE)).isSameAs(greenhouseCollector);
        assertThat(registry.getCollector(AtsType.LEVER)).isSameAs(leverCollector);
    }

    @Test
    void doesNotDependOnRegistrationOrder() {
        AtsCollectorRegistry registry = new AtsCollectorRegistry(List.of(leverCollector, greenhouseCollector));

        assertThat(registry.getCollector(AtsType.GREENHOUSE)).isSameAs(greenhouseCollector);
        assertThat(registry.getCollector(AtsType.LEVER)).isSameAs(leverCollector);
    }

    @Test
    void throwsWhenNoCollectorIsRegisteredForAtsType() {
        AtsCollectorRegistry registry = new AtsCollectorRegistry(List.of(greenhouseCollector));

        assertThatThrownBy(() -> registry.getCollector(AtsType.LEVER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LEVER");
    }

    @Test
    void startsEmptyWhenNoCollectorsExist() {
        AtsCollectorRegistry registry = new AtsCollectorRegistry(List.of());

        assertThatThrownBy(() -> registry.getCollector(AtsType.GREENHOUSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GREENHOUSE");
    }

    @Test
    void failsFastWhenTwoCollectorsClaimTheSameAtsType() {
        AtsCollector duplicate = new AnotherFakeCollector(AtsType.GREENHOUSE);

        assertThatThrownBy(() -> new AtsCollectorRegistry(List.of(greenhouseCollector, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GREENHOUSE")
                .hasMessageContaining("FakeCollector")
                .hasMessageContaining("AnotherFakeCollector");
    }

    private record FakeCollector(AtsType atsType) implements AtsCollector {

        @Override
        public List<CollectedPosting> fetchPostings(String boardToken) {
            return List.of();
        }
    }

    private record AnotherFakeCollector(AtsType atsType) implements AtsCollector {

        @Override
        public List<CollectedPosting> fetchPostings(String boardToken) {
            return List.of();
        }
    }
}

package io.quarkiverse.langchain4j.bam.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface ChatModelBuildConfig {

    /**
     * Whether the model should be enabled
     */
    @ConfigDocDefault("true")
    Optional<Boolean> enabled();

    /**
     * Embedding model related settings
     */
    EmbeddingModelBuildConfig embeddingModel();
}

package io.quarkiverse.langchain4j.huggingface.deployment;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = BUILD_TIME)
@ConfigMapping(prefix = "quarkus.langchain4j.huggingface")
public interface Langchain4jHuggingFaceBuildConfig {

    /**
     * Chat model related settings
     */
    ChatModelBuildConfig chatModel();

    /**
     * Embedding model related settings
     */
    EmbeddingModelBuildConfig embeddingModel();

    /**
     * Moderation model related settings
     */
    ModerationModelBuildConfig moderationModel();
}

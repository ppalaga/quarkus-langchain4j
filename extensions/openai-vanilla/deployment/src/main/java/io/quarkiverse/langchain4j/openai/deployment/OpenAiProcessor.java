package io.quarkiverse.langchain4j.openai.deployment;

import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.CHAT_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.EMBEDDING_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.IMAGE_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.MODERATION_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.STREAMING_CHAT_MODEL;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.deployment.EmbeddingModelBuildItem;
import io.quarkiverse.langchain4j.deployment.items.ChatModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.EmbeddingModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.ImageModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.ModerationModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedChatModelProviderBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedEmbeddingModelCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedImageModelProviderBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedModerationModelProviderBuildItem;
import io.quarkiverse.langchain4j.openai.runtime.OpenAiRecorder;
import io.quarkiverse.langchain4j.openai.runtime.config.Langchain4jOpenAiConfig;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

public class OpenAiProcessor {

    private static final String FEATURE = "langchain4j-openai";
    private static final String PROVIDER = "openai";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public void providerCandidates(BuildProducer<EmbeddingModelBuildItem> embeddingModelProducer,
            BuildProducer<ChatModelProviderCandidateBuildItem> chatProducer,
            BuildProducer<EmbeddingModelProviderCandidateBuildItem> embeddingProducer,
            BuildProducer<ModerationModelProviderCandidateBuildItem> moderationProducer,
            BuildProducer<ImageModelProviderCandidateBuildItem> imageProducer,
            Langchain4jOpenAiBuildConfig config) {
        if (config.chatModel().enabled().isEmpty() || config.chatModel().enabled().get()) {
            chatProducer.produce(new ChatModelProviderCandidateBuildItem(PROVIDER));
        }
        if (config.embeddingModel().enabled().isEmpty() || config.embeddingModel().enabled().get()) {
            embeddingProducer.produce(new EmbeddingModelProviderCandidateBuildItem(PROVIDER));
            embeddingModelProducer.produce(new EmbeddingModelBuildItem());
        }
        if (config.moderationModel().enabled().isEmpty() || config.moderationModel().enabled().get()) {
            moderationProducer.produce(new ModerationModelProviderCandidateBuildItem(PROVIDER));
        }
        if (config.imageModel().enabled().isEmpty() || config.imageModel().enabled().get()) {
            imageProducer.produce(new ImageModelProviderCandidateBuildItem(PROVIDER));
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void generateBeans(OpenAiRecorder recorder,
            Optional<SelectedChatModelProviderBuildItem> selectedChatItem,
            Optional<SelectedEmbeddingModelCandidateBuildItem> selectedEmbedding,
            Optional<SelectedModerationModelProviderBuildItem> selectedModeration,
            Optional<SelectedImageModelProviderBuildItem> selectedImage,
            Langchain4jOpenAiConfig config,
            BuildProducer<SyntheticBeanBuildItem> beanProducer) {
        if (selectedChatItem.isPresent() && PROVIDER.equals(selectedChatItem.get().getProvider())) {
            beanProducer.produce(SyntheticBeanBuildItem
                    .configure(CHAT_MODEL)
                    .setRuntimeInit()
                    .defaultBean()
                    .scope(ApplicationScoped.class)
                    .supplier(recorder.chatModel(config))
                    .done());

            beanProducer.produce(SyntheticBeanBuildItem
                    .configure(STREAMING_CHAT_MODEL)
                    .setRuntimeInit()
                    .defaultBean()
                    .scope(ApplicationScoped.class)
                    .supplier(recorder.streamingChatModel(config))
                    .done());
        }

        if (selectedEmbedding.isPresent() && PROVIDER.equals(selectedEmbedding.get().getProvider())) {
            beanProducer.produce(SyntheticBeanBuildItem
                    .configure(EMBEDDING_MODEL)
                    .setRuntimeInit()
                    .defaultBean()
                    .scope(ApplicationScoped.class)
                    .supplier(recorder.embeddingModel(config))
                    .done());
        }

        if (selectedModeration.isPresent() && PROVIDER.equals(selectedModeration.get().getProvider())) {
            beanProducer.produce(SyntheticBeanBuildItem
                    .configure(MODERATION_MODEL)
                    .setRuntimeInit()
                    .defaultBean()
                    .scope(ApplicationScoped.class)
                    .supplier(recorder.moderationModel(config))
                    .done());
        }

        if (selectedImage.isPresent() && PROVIDER.equals(selectedImage.get().getProvider())) {
            beanProducer.produce(SyntheticBeanBuildItem
                    .configure(IMAGE_MODEL)
                    .setRuntimeInit()
                    .defaultBean()
                    .scope(ApplicationScoped.class)
                    .supplier(recorder.imageModel(config))
                    .done());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void cleanUp(OpenAiRecorder recorder, ShutdownContextBuildItem shutdown) {
        recorder.cleanUp(shutdown);
    }
}

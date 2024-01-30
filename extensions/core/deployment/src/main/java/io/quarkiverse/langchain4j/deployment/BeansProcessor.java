package io.quarkiverse.langchain4j.deployment;

import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.CHAT_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.EMBEDDING_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.IMAGE_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.MODERATION_MODEL;
import static io.quarkiverse.langchain4j.deployment.Langchain4jDotNames.STREAMING_CHAT_MODEL;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.formula.functions.T;
import org.jboss.jandex.DotName;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.langchain4j.deployment.config.LangChain4jBuildConfig;
import io.quarkiverse.langchain4j.deployment.items.*;
import io.quarkiverse.langchain4j.runtime.Langchain4jRecorder;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

public class BeansProcessor {

    private static final String FEATURE = "langchain4j";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> producer) {
        producer.produce(new IndexDependencyBuildItem("dev.langchain4j", "langchain4j-core"));
    }

    @BuildStep
    public void handleProviders(BeanDiscoveryFinishedBuildItem beanDiscoveryFinished,
            List<ChatModelProviderCandidateBuildItem> chatCandidateItems,
            List<EmbeddingModelProviderCandidateBuildItem> embeddingCandidateItems,
            List<ModerationModelProviderCandidateBuildItem> moderationCandidateItems,
            List<ImageModelProviderCandidateBuildItem> imageCandidateItems,
            List<RequestChatModelBeanBuildItem> requestChatModelBeanItems,
            List<RequestModerationModelBeanBuildItem> requestModerationModelBeanBuildItems,
            LangChain4jBuildConfig buildConfig,
            BuildProducer<SelectedChatModelProviderBuildItem> selectedChatProducer,
            BuildProducer<SelectedEmbeddingModelCandidateBuildItem> selectedEmbeddingProducer,
            BuildProducer<SelectedModerationModelProviderBuildItem> selectedModerationProducer,
            BuildProducer<SelectedImageModelProviderBuildItem> selectedImageProducer,
            List<InProcessEmbeddingBuildItem> inProcessEmbeddingBuildItems) {

        boolean chatModelBeanRequested = false;
        boolean streamingChatModelBeanRequested = false;
        boolean embeddingModelBeanRequested = false;
        boolean moderationModelBeanRequested = false;
        boolean imageModelBeanRequested = false;
        for (InjectionPointInfo ip : beanDiscoveryFinished.getInjectionPoints()) {
            DotName requiredName = ip.getRequiredType().name();
            if (CHAT_MODEL.equals(requiredName)) {
                chatModelBeanRequested = true;
            } else if (STREAMING_CHAT_MODEL.equals(requiredName)) {
                streamingChatModelBeanRequested = true;
            } else if (EMBEDDING_MODEL.equals(requiredName)) {
                embeddingModelBeanRequested = true;
            } else if (MODERATION_MODEL.equals(requiredName)) {
                moderationModelBeanRequested = true;
            } else if (IMAGE_MODEL.equals(requiredName)) {
                imageModelBeanRequested = true;
            }
        }
        if (!requestChatModelBeanItems.isEmpty()) {
            chatModelBeanRequested = true;
        }
        if (!requestModerationModelBeanBuildItems.isEmpty()) {
            moderationModelBeanRequested = true;
        }

        if (chatModelBeanRequested || streamingChatModelBeanRequested) {
            selectedChatProducer.produce(
                    new SelectedChatModelProviderBuildItem(
                            selectProvider(
                                    chatCandidateItems,
                                    buildConfig.chatModel().provider(),
                                    "ChatLanguageModel or StreamingChatLanguageModel",
                                    "chat-model")));
        }
        if (embeddingModelBeanRequested) {
            selectedEmbeddingProducer.produce(
                    new SelectedEmbeddingModelCandidateBuildItem(
                            selectEmbeddingModelProvider(
                                    inProcessEmbeddingBuildItems,
                                    embeddingCandidateItems,
                                    buildConfig.embeddingModel().provider(),
                                    "EmbeddingModel",
                                    "embedding-model")));
        }
        if (moderationModelBeanRequested) {
            selectedModerationProducer.produce(
                    new SelectedModerationModelProviderBuildItem(
                            selectProvider(
                                    moderationCandidateItems,
                                    buildConfig.moderationModel().provider(),
                                    "ModerationModel",
                                    "moderation-model")));
        }
        if (imageModelBeanRequested) {
            selectedImageProducer.produce(
                    new SelectedImageModelProviderBuildItem(
                            selectProvider(
                                    imageCandidateItems,
                                    buildConfig.moderationModel().provider(),
                                    "ImageModel",
                                    "image-model")));
        }

    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T extends ProviderHolder> String selectProvider(
            List<T> chatCandidateItems,
            Optional<String> userSelectedProvider,
            String requestedBeanName,
            String configNamespace) {
        List<String> availableProviders = chatCandidateItems.stream().map(ProviderHolder::getProvider)
                .collect(Collectors.toList());
        if (availableProviders.isEmpty()) {
            throw new ConfigurationException(String.format(
                    "A %s bean was requested, but no langchain4j providers were configured. Consider adding an extension like 'quarkus-langchain4j-openai'",
                    requestedBeanName));
        }
        if (availableProviders.size() == 1) {
            // user has selected a provider, but it's not the one that is available
            if (userSelectedProvider.isPresent() && !availableProviders.get(0).equals(userSelectedProvider.get())) {
                throw new ConfigurationException(String.format(
                        "A %s bean with provider=%s was requested was requested via configuration, but the only provider found on the classpath is %s.",
                        requestedBeanName, userSelectedProvider.get(), availableProviders.get(0)));
            }
            return availableProviders.get(0);
        }
        // multiple providers exist, so we now need the configuration to select the proper one
        if (userSelectedProvider.isEmpty()) {
            throw new ConfigurationException(String.format(
                    "A %s bean was requested, but since there are multiple available providers, the 'quarkus.langchain4j.%s.provider' needs to be set to one of the available options (%s).",
                    requestedBeanName, configNamespace, String.join(",", availableProviders)));
        }
        boolean matches = availableProviders.stream().anyMatch(ap -> ap.equals(userSelectedProvider.get()));
        if (matches) {
            return userSelectedProvider.get();
        }
        throw new ConfigurationException(String.format(
                "A %s bean was requested, but the value of 'quarkus.langchain4j.%s.provider' does not match any of the available options (%s).",
                requestedBeanName, configNamespace, String.join(",", availableProviders)));
    }

    private <T extends ProviderHolder> String selectEmbeddingModelProvider(
            List<InProcessEmbeddingBuildItem> inProcessEmbeddingBuildItems,
            List<T> chatCandidateItems,
            Optional<String> userSelectedProvider,
            String requestedBeanName,
            String configNamespace) {
        List<String> availableProviders = chatCandidateItems.stream().map(ProviderHolder::getProvider)
                .collect(Collectors.toList());
        availableProviders.addAll(inProcessEmbeddingBuildItems.stream().map(InProcessEmbeddingBuildItem::getProvider)
                .toList());
        if (availableProviders.isEmpty()) {
            throw new ConfigurationException(String.format(
                    "A %s bean was requested, but no langchain4j providers were configured and no in-process embedding model were found on the classpath. "
                            +
                            "Consider adding an extension like 'quarkus-langchain4j-openai' or one of the in-process embedding models.",
                    requestedBeanName));
        }
        if (availableProviders.size() == 1) {
            // user has selected a provider, but it's not the one that is available
            if (userSelectedProvider.isPresent() && !availableProviders.get(0).equals(userSelectedProvider.get())) {
                throw new ConfigurationException(String.format(
                        "Embedding model provider %s was requested via configuration, but the only provider found on the classpath is %s.",
                        userSelectedProvider.get(), availableProviders.get(0)));
            }
            return availableProviders.get(0);
        }
        // multiple providers exist, so we now need the configuration to select the proper one
        if (userSelectedProvider.isEmpty()) {
            throw new ConfigurationException(String.format(
                    "A %s bean was requested, but since there are multiple available providers, the 'quarkus.langchain4j.%s.provider' needs to be set to one of the available options (%s).",
                    requestedBeanName, configNamespace, String.join(",", availableProviders)));
        }
        boolean matches = availableProviders.stream().anyMatch(ap -> ap.equals(userSelectedProvider.get()));
        if (matches) {
            return userSelectedProvider.get();
        }
        throw new ConfigurationException(String.format(
                "A %s bean was requested, but the value of 'quarkus.langchain4j.%s.provider' does not match any of the available options (%s).",
                requestedBeanName, configNamespace, String.join(",", availableProviders)));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void cleanUp(Langchain4jRecorder recorder, ShutdownContextBuildItem shutdown) {
        recorder.cleanUp(shutdown);
    }

    @BuildStep
    public void unremoveableBeans(BuildProducer<UnremovableBeanBuildItem> unremoveableProducer) {
        unremoveableProducer.produce(UnremovableBeanBuildItem.beanTypes(ObjectMapper.class));
    }

}

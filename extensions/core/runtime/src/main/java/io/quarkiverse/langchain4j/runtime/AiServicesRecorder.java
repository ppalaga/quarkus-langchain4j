package io.quarkiverse.langchain4j.runtime;

import static io.quarkiverse.langchain4j.QuarkusAiServicesFactory.InstanceHolder.INSTANCE;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.retriever.Retriever;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.audit.AuditService;
import io.quarkiverse.langchain4j.runtime.aiservice.AiServiceClassCreateInfo;
import io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodCreateInfo;
import io.quarkiverse.langchain4j.runtime.aiservice.DeclarativeAiServiceCreateInfo;
import io.quarkiverse.langchain4j.runtime.aiservice.QuarkusAiServiceContext;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AiServicesRecorder {
    private static final TypeLiteral<Instance<Retriever<TextSegment>>> RETRIEVER_INSTANCE_TYPE_LITERAL = new TypeLiteral<>() {

    };
    private static final TypeLiteral<Instance<AuditService>> AUDIT_SERVICE_TYPE_LITERAL = new TypeLiteral<>() {
    };

    // the key is the interface's class name
    private static final Map<String, AiServiceClassCreateInfo> metadata = new HashMap<>();

    public void setMetadata(Map<String, AiServiceClassCreateInfo> metadata) {
        AiServicesRecorder.metadata.putAll(metadata);
    }

    public static Map<String, AiServiceClassCreateInfo> getMetadata() {
        return metadata;
    }

    public static void clearMetadata() {
        metadata.clear();
    }

    @SuppressWarnings("unused") // used in generated code
    public static AiServiceMethodCreateInfo getAiServiceMethodCreateInfo(String className, String methodId) {
        AiServiceClassCreateInfo classCreateInfo = metadata.get(className);
        if (classCreateInfo == null) {
            throw new RuntimeException("Quarkus was not able to determine class '" + className
                    + "' as an AiService at build time. Consider annotating the clas with @CreatedAware");
        }
        AiServiceMethodCreateInfo methodCreateInfo = classCreateInfo.getMethodMap().get(methodId);
        if (methodCreateInfo == null) {
            throw new IllegalStateException("Unable to locate method metadata for descriptor '" + methodId
                    + "'. Please report this issue to the maintainers");
        }
        return methodCreateInfo;
    }

    public <T> Function<SyntheticCreationalContext<T>, T> createDeclarativeAiService(DeclarativeAiServiceCreateInfo info) {
        return new Function<SyntheticCreationalContext<T>, T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T apply(SyntheticCreationalContext<T> creationalContext) {
                try {
                    Class<?> serviceClass = Thread.currentThread().getContextClassLoader()
                            .loadClass(info.getServiceClassName());

                    QuarkusAiServiceContext aiServiceContext = new QuarkusAiServiceContext(serviceClass);
                    // we don't really care about QuarkusAiServices here, all we care about is that it
                    // properly populates QuarkusAiServiceContext which is what we are trying to construct
                    var quarkusAiServices = INSTANCE.create(aiServiceContext);

                    if (info.getLanguageModelSupplierClassName() != null) {
                        Supplier<? extends ChatLanguageModel> supplier = (Supplier<? extends ChatLanguageModel>) Thread
                                .currentThread().getContextClassLoader().loadClass(info.getLanguageModelSupplierClassName())
                                .getConstructor().newInstance();
                        quarkusAiServices.chatLanguageModel(supplier.get());
                    } else {
                        quarkusAiServices.chatLanguageModel(creationalContext.getInjectedReference(ChatLanguageModel.class));
                    }

                    List<String> toolsClasses = info.getToolsClassNames();
                    if ((toolsClasses != null) && !toolsClasses.isEmpty()) {
                        List<Object> tools = new ArrayList<>(toolsClasses.size());
                        for (String toolClass : toolsClasses) {
                            Object tool = creationalContext.getInjectedReference(
                                    Thread.currentThread().getContextClassLoader().loadClass(toolClass));
                            tools.add(tool);
                        }
                        quarkusAiServices.tools(tools);
                    }

                    if (info.getChatMemoryProviderSupplierClassName() != null) {
                        if (RegisterAiService.BeanChatMemoryProviderSupplier.class.getName()
                                .equals(info.getChatMemoryProviderSupplierClassName())) {
                            quarkusAiServices.chatMemoryProvider(creationalContext.getInjectedReference(
                                    ChatMemoryProvider.class));
                        } else {
                            Supplier<? extends ChatMemoryProvider> supplier = (Supplier<? extends ChatMemoryProvider>) Thread
                                    .currentThread().getContextClassLoader()
                                    .loadClass(info.getChatMemoryProviderSupplierClassName())
                                    .getConstructor().newInstance();
                            quarkusAiServices.chatMemoryProvider(supplier.get());
                        }
                    }

                    if (info.getRetrieverClassName() != null) {
                        quarkusAiServices.retriever((Retriever<TextSegment>) creationalContext.getInjectedReference(
                                Thread.currentThread().getContextClassLoader().loadClass(info.getRetrieverClassName())));
                    }

                    if (info.getAuditServiceClassSupplierName() != null) {
                        if (RegisterAiService.BeanIfExistsAuditServiceSupplier.class.getName()
                                .equals(info.getAuditServiceClassSupplierName())) {
                            Instance<AuditService> instance = creationalContext
                                    .getInjectedReference(AUDIT_SERVICE_TYPE_LITERAL);
                            if (instance.isResolvable()) {
                                quarkusAiServices.auditService(instance.get());
                            }
                        } else {
                            @SuppressWarnings("rawtypes")
                            Supplier<? extends AuditService> supplier = (Supplier<? extends AuditService>) Thread
                                    .currentThread().getContextClassLoader().loadClass(info.getAuditServiceClassSupplierName())
                                    .getConstructor().newInstance();
                            quarkusAiServices.auditService(supplier.get());
                        }
                    }

                    if (info.getModerationModelSupplierClassName() != null) {
                        if (RegisterAiService.BeanModerationModelSupplier.class.getName()
                                .equals(info.getModerationModelSupplierClassName())) {
                            ModerationModel moderationModel = creationalContext.getInjectedReference(ModerationModel.class);
                            quarkusAiServices.moderationModel(moderationModel);
                        } else {
                            @SuppressWarnings("rawtypes")
                            Supplier<? extends AuditService> supplier = (Supplier<? extends AuditService>) Thread
                                    .currentThread().getContextClassLoader()
                                    .loadClass(info.getModerationModelSupplierClassName())
                                    .getConstructor().newInstance();
                            quarkusAiServices.auditService(supplier.get());
                        }
                    }

                    return (T) aiServiceContext;
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException
                        | InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}

package io.quarkiverse.langchain4j.deployment;

import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents the metadata collected from the usages of {@link io.quarkiverse.langchain4j.RegisterAiService}
 */
public final class DeclarativeAiServiceBuildItem extends MultiBuildItem {

    private final ClassInfo serviceClassInfo;
    private final DotName languageModelSupplierClassDotName;
    private final List<DotName> toolDotNames;

    private final DotName chatMemoryProviderSupplierClassDotName;
    private final DotName retrieverClassDotName;
    private final DotName auditServiceClassSupplierDotName;
    private final DotName moderationModelSupplierDotName;
    private final ScopeInfo cdiScope;

    public DeclarativeAiServiceBuildItem(ClassInfo serviceClassInfo, DotName languageModelSupplierClassDotName,
            List<DotName> toolDotNames,
            DotName chatMemoryProviderSupplierClassDotName,
            DotName retrieverClassDotName,
            DotName auditServiceClassSupplierDotName,
            DotName moderationModelSupplierDotName,
            ScopeInfo cdiScope) {
        this.serviceClassInfo = serviceClassInfo;
        this.languageModelSupplierClassDotName = languageModelSupplierClassDotName;
        this.toolDotNames = toolDotNames;
        this.chatMemoryProviderSupplierClassDotName = chatMemoryProviderSupplierClassDotName;
        this.retrieverClassDotName = retrieverClassDotName;
        this.auditServiceClassSupplierDotName = auditServiceClassSupplierDotName;
        this.moderationModelSupplierDotName = moderationModelSupplierDotName;
        this.cdiScope = cdiScope;
    }

    public ClassInfo getServiceClassInfo() {
        return serviceClassInfo;
    }

    public DotName getLanguageModelSupplierClassDotName() {
        return languageModelSupplierClassDotName;
    }

    public List<DotName> getToolDotNames() {
        return toolDotNames;
    }

    public DotName getChatMemoryProviderSupplierClassDotName() {
        return chatMemoryProviderSupplierClassDotName;
    }

    public DotName getRetrieverClassDotName() {
        return retrieverClassDotName;
    }

    public DotName getAuditServiceClassSupplierDotName() {
        return auditServiceClassSupplierDotName;
    }

    public DotName getModerationModelSupplierDotName() {
        return moderationModelSupplierDotName;
    }

    public ScopeInfo getCdiScope() {
        return cdiScope;
    }
}

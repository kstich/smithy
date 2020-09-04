/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.aws.cloudformation.schema.model;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.aws.cloudformation.schema.CloudFormationException;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ResourceSchema implements ToNode, ToSmithyBuilder<ResourceSchema> {
    private final String typeName;
    private final String description;
    private final String sourceUrl;
    private final String documentationUrl;
    private final Map<String, Schema> definitions;
    private final Map<String, Property> properties;
    private final List<String> readOnlyProperties;
    private final List<String> writeOnlyProperties;
    private final List<String> primaryIdentifier;
    private final List<String> createOnlyProperties;
    private final List<String> deprecatedProperties;
    private final List<List<String>> additionalIdentifiers;
    private final Map<String, Handler> handlers;
    // Other reserved property names:
    // * remote

    private ResourceSchema(Builder builder) {
        typeName = SmithyBuilder.requiredState("typeName", builder.typeName);
        description = SmithyBuilder.requiredState("description", builder.description);
        if (builder.properties.isEmpty()) {
            throw new CloudFormationException(format("Expected CloudFormation resource %s to have properties, "
                    + "found none", typeName));
        }
        properties = MapUtils.copyOf(builder.properties);

        sourceUrl = builder.sourceUrl;
        documentationUrl = builder.documentationUrl;
        definitions = MapUtils.copyOf(builder.definitions);
        readOnlyProperties = ListUtils.copyOf(builder.readOnlyProperties);
        writeOnlyProperties = ListUtils.copyOf(builder.writeOnlyProperties);
        primaryIdentifier = ListUtils.copyOf(builder.primaryIdentifier);
        createOnlyProperties = ListUtils.copyOf(builder.createOnlyProperties);
        deprecatedProperties = ListUtils.copyOf(builder.deprecatedProperties);
        additionalIdentifiers = ListUtils.copyOf(builder.additionalIdentifiers);
        handlers = MapUtils.copyOf(builder.handlers);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(ResourceSchema.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .typeName(typeName)
                .description(description)
                .sourceUrl(sourceUrl)
                .documentationUrl(documentationUrl)
                .definitions(definitions)
                .properties(properties)
                .readOnlyProperties(readOnlyProperties)
                .writeOnlyProperties(writeOnlyProperties)
                .primaryIdentifier(primaryIdentifier)
                .createOnlyProperties(createOnlyProperties)
                .deprecatedProperties(deprecatedProperties)
                .additionalIdentifiers(additionalIdentifiers)
                .handlers(handlers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public Map<String, Schema> getDefinitions() {
        return definitions;
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public List<String> getReadOnlyProperties() {
        return readOnlyProperties;
    }

    public List<String> getWriteOnlyProperties() {
        return writeOnlyProperties;
    }

    public List<String> getPrimaryIdentifier() {
        return primaryIdentifier;
    }

    public List<String> getCreateOnlyProperties() {
        return createOnlyProperties;
    }

    public List<String> getDeprecatedProperties() {
        return deprecatedProperties;
    }

    public List<List<String>> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    public Map<String, Handler> getHandlers() {
        return handlers;
    }

    public static final class Builder implements SmithyBuilder<ResourceSchema> {
        private String typeName;
        private String description;
        private String sourceUrl;
        private String documentationUrl;
        private final Map<String, Schema> definitions = new TreeMap<>();
        private final Map<String, Property> properties = new TreeMap<>();
        private final List<String> readOnlyProperties = new ArrayList<>();
        private final List<String> writeOnlyProperties = new ArrayList<>();
        private final List<String> primaryIdentifier = new ArrayList<>();
        private final List<String> createOnlyProperties = new ArrayList<>();
        private final List<String> deprecatedProperties = new ArrayList<>();
        private final List<List<String>> additionalIdentifiers = new ArrayList<>();
        private final Map<String, Handler> handlers = new TreeMap<>();

        private Builder() {}

        @Override
        public ResourceSchema build() {
            return new ResourceSchema(this);
        }

        public Builder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        public Builder definitions(Map<String, Schema> definitions) {
            this.definitions.clear();
            this.definitions.putAll(definitions);
            return this;
        }

        public Builder addDefinition(String name, Schema definition) {
            this.definitions.put(name, definition);
            return this;
        }

        public Builder removeDefinition(String name) {
            this.definitions.remove(name);
            return this;
        }

        public Builder clearDefinitions() {
            this.definitions.clear();
            return this;
        }

        public Builder properties(Map<String, Property> properties) {
            this.properties.clear();
            this.properties.putAll(properties);
            return this;
        }

        public Builder addProperty(String name, Property property) {
            this.properties.put(name, property);
            return this;
        }

        public Builder removeProperty(String name) {
            this.properties.remove(name);
            return this;
        }

        public Builder clearProperties() {
            this.properties.clear();
            return this;
        }

        public Builder addReadOnlyProperty(String propertyRef) {
            this.readOnlyProperties.add(propertyRef);
            return this;
        }

        public Builder readOnlyProperties(List<String> readOnlyProperties) {
            this.readOnlyProperties.clear();
            this.readOnlyProperties.addAll(readOnlyProperties);
            return this;
        }

        public Builder clearReadOnlyProperties() {
            this.readOnlyProperties.clear();
            return this;
        }

        public Builder addWriteOnlyProperty(String propertyRef) {
            this.writeOnlyProperties.add(propertyRef);
            return this;
        }

        public Builder writeOnlyProperties(List<String> writeOnlyProperties) {
            this.writeOnlyProperties.clear();
            this.writeOnlyProperties.addAll(writeOnlyProperties);
            return this;
        }

        public Builder clearWriteOnlyProperties() {
            this.writeOnlyProperties.clear();
            return this;
        }

        public Builder primaryIdentifier(List<String> primaryIdentifier) {
            this.primaryIdentifier.clear();
            this.primaryIdentifier.addAll(primaryIdentifier);
            return this;
        }

        public Builder clearPrimaryIdentifier() {
            this.primaryIdentifier.clear();
            return this;
        }

        public Builder addCreateOnlyProperty(String propertyRef) {
            this.createOnlyProperties.add(propertyRef);
            return this;
        }

        public Builder createOnlyProperties(List<String> createOnlyProperties) {
            this.createOnlyProperties.clear();
            this.createOnlyProperties.addAll(createOnlyProperties);
            return this;
        }

        public Builder clearCreateOnlyProperties() {
            this.createOnlyProperties.clear();
            return this;
        }

        public Builder addDeprecatedProperty(String propertyRef) {
            this.deprecatedProperties.add(propertyRef);
            return this;
        }

        public Builder deprecatedProperties(List<String> deprecatedProperties) {
            this.deprecatedProperties.clear();
            this.deprecatedProperties.addAll(deprecatedProperties);
            return this;
        }

        public Builder clearDeprecatedProperties() {
            this.deprecatedProperties.clear();
            return this;
        }

        public Builder addAdditionalIdentifier(List<String> additionalIdentifier) {
            this.additionalIdentifiers.add(additionalIdentifier);
            return this;
        }

        public Builder additionalIdentifiers(List<List<String>> additionalIdentifiers) {
            this.additionalIdentifiers.clear();
            this.additionalIdentifiers.addAll(additionalIdentifiers);
            return this;
        }

        public Builder clearAdditionalIdentifiers() {
            this.additionalIdentifiers.clear();
            return this;
        }

        public Builder handlers(Map<String, Handler> handlers) {
            this.handlers.clear();
            this.handlers.putAll(handlers);
            return this;
        }

        public Builder addHandler(String name, Handler handler) {
            this.handlers.put(name, handler);
            return this;
        }

        public Builder removeHandler(String name) {
            this.handlers.remove(name);
            return this;
        }

        public Builder clearHandlers() {
            this.handlers.clear();
            return this;
        }
    }
}

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

package software.amazon.smithy.aws.cloudformation.schema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class CloudFormationConfig extends JsonSchemaConfig {

    /** The JSON pointer to where CloudFormation schema shared resource properties should be written. */
    public static final String SCHEMA_COMPONENTS_POINTER = "#/definitions";

    private String defaultBlobFormat = "byte";
    private boolean disableDeprecatedPropertyGeneration = false;
    private boolean disableCapitalizedProperties = false;
    private List<String> externalDocKeys = ListUtils.of(
            "Documentation Url", "DocumentationUrl", "API Reference", "User Guide",
            "Developer Guide", "Reference", "Guide");
    private Map<String, Node> jsonAdd = Collections.emptyMap();
    private String organizationName;
    private String serviceName;
    private ShapeId service;
    private List<String> sourceDocKeys = ListUtils.of(
            "Source Url", "SourceUrl", "Source", "Source Code");

    public CloudFormationConfig() {
        super();
        setDefinitionPointer(SCHEMA_COMPONENTS_POINTER);
    }

    public String getDefaultBlobFormat() {
        return defaultBlobFormat;
    }

    public void setDefaultBlobFormat(String defaultBlobFormat) {
        this.defaultBlobFormat = defaultBlobFormat;
    }

    public boolean getDisableDeprecatedPropertyGeneration() {
        return disableDeprecatedPropertyGeneration;
    }

    public void setDisableDeprecatedPropertyGeneration(boolean value) {
        this.disableDeprecatedPropertyGeneration = value;
    }

    public boolean getDisableCapitalizedProperties() {
        return disableCapitalizedProperties;
    }

    public void setDisableCapitalizedProperties(boolean disableCapitalizedProperties) {
        this.disableCapitalizedProperties = disableCapitalizedProperties;
    }

    public List<String> getExternalDocKeys() {
        return externalDocKeys;
    }

    public void setExternalDocKeys(List<String> externalDocKeys) {
        this.externalDocKeys = externalDocKeys;
    }

    public Map<String, Node> getJsonAdd() {
        return jsonAdd;
    }

    /**
     * Adds or replaces the JSON value in the generated resource schema
     * document at the given JSON pointer locations with a different JSON
     * value.
     *
     * <p>The value must be a map where each key is a valid JSON pointer
     * string as defined in RFC 6901. Each value in the map is the JSON
     * value to add or replace at the given target.
     *
     * <p>Values are added using similar semantics of the "add" operation
     * of JSON Patch, as specified in RFC 6902, with the exception that
     * adding properties to an undefined object will create nested
     * objects in the result as needed.
     *
     * @param jsonAdd Map of JSON path to values to patch in.
     */
    public void setJsonAdd(Map<String, Node> jsonAdd) {
        this.jsonAdd = Objects.requireNonNull(jsonAdd);
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ShapeId getService() {
        return service;
    }

    public void setService(ShapeId service) {
        this.service = service;
    }

    public List<String> getSourceDocKeys() {
        return sourceDocKeys;
    }

    public void setSourceDocKeys(List<String> sourceDocKeys) {
        this.sourceDocKeys = sourceDocKeys;
    }

    public static CloudFormationConfig fromNode(Node settings) {
        NodeMapper mapper = new NodeMapper();

        mapper.setWhenMissingSetter(NodeMapper.WhenMissing.INGORE);

        ObjectNode node = settings.expectObjectNode();
        CloudFormationConfig config = new CloudFormationConfig();
        mapper.deserializeInto(node, config);

        // Add all properties to "extensions" to make them accessible
        // in plugins.
        for (Map.Entry<String, Node> entry : node.getStringMap().entrySet()) {
            config.putExtension(entry.getKey(), entry.getValue());
        }

        return config;
    }
}

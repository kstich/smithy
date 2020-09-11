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

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy;

import software.amazon.smithy.aws.cloudformation.schema.CloudFormationConfig;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.StringUtils;

public final class Context {

    private final Model model;
    private final ServiceShape service;
    private final ResourceShape resource;
    private final StructureShape resourceStructure;
    private final JsonSchemaConverter jsonSchemaConverter;
    private final CloudFormationConfig config;

    Context(
            Model model,
            ServiceShape service,
            ResourceShape resource,
            StructureShape resourceStructure,
            CloudFormationConfig config,
            JsonSchemaConverter jsonSchemaConverter
    ) {
        this.model = model;
        this.service = service;
        this.resource = resource;
        this.resourceStructure = resourceStructure;
        this.config = config;
        this.jsonSchemaConverter = jsonSchemaConverter;
    }


    /**
     * Gets the Smithy model being converted.
     *
     * @return Returns the Smithy model.
     */
    public Model getModel() {
        return model;
    }

    /**
     * Gets the service shape containing the resource being converted.
     *
     * @return Returns the service shape.
     */
    public ServiceShape getService() {
        return service;
    }

    /**
     * Gets the resource shape being converted.
     *
     * @return Returns the resource shape.
     */
    public ResourceShape getResource() {
        return resource;
    }

    /**
     * Gets the structure shape that represents the consolidated properties of the resource.
     *
     * @return Returns the structure shape.
     */
    public StructureShape getResourceStructure() {
        return resourceStructure;
    }

    /**
     * Gets the configuration object used for the conversion.
     *
     * <p>Plugins can query this object for configuration values.
     *
     * @return Returns the configuration object.
     */
    public CloudFormationConfig getConfig() {
        return config;
    }

    /**
     * Gets the JSON schema converter.
     *
     * @return Returns the JSON Schema converter.
     */
    public JsonSchemaConverter getJsonSchemaConverter() {
        return jsonSchemaConverter;
    }

    /**
     * Gets the JSON pointer string to a specific property.
     *
     * @param propertyName Property name to build a JSON pointer to.
     * @return Returns the JSON pointer to the property.
     */
    public String getPropertyPointer(String propertyName) {
        return "/properties/" + getResolvedPropertyName(propertyName);
    }

    /**
     * Gets the resolved property name based on config settings.
     *
     * @param propertyName The property name to resolve.
     * @return The resolved property name.
     */
    public String getResolvedPropertyName(String propertyName) {
        return config.getDisableCapitalizedProperties()
                ? propertyName
                : StringUtils.capitalize(propertyName);
    }
}

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import software.amazon.smithy.aws.cloudformation.schema.CloudFormationConfig;
import software.amazon.smithy.aws.cloudformation.schema.CloudFormationException;
import software.amazon.smithy.aws.cloudformation.schema.model.Property;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.aws.cloudformation.traits.ResourceIndex;
import software.amazon.smithy.aws.cloudformation.traits.ResourceTrait;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.utils.ListUtils;

public final class CloudFormationConverter {
//    private static final Logger LOGGER = Logger.getLogger(CloudFormationConverter.class.getName());

    private ClassLoader classLoader = CloudFormationConverter.class.getClassLoader();
    private CloudFormationConfig config = new CloudFormationConfig();
    private final List<Smithy2CloudFormationExtension> extensions = new ArrayList<>();

    private CloudFormationConverter() {}

    public static CloudFormationConverter create() {
        return new CloudFormationConverter();
    }

    /**
     * Get the CloudFormation configuration settings.
     *
     * @return Returns the config object.
     */
    public CloudFormationConfig getConfig() {
        return config;
    }

    /**
     * Set the CloudFormation configuration settings.
     *
     * @param config Config object to set.
     * @return Returns the converter.
     */
    public CloudFormationConverter config(CloudFormationConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Sets a {@link ClassLoader} to use to discover {@link Smithy2CloudFormationExtension}
     * service providers through SPI.
     *
     * <p>The {@code CloudFormationConverter} will use its own ClassLoader by default.
     *
     * @param classLoader ClassLoader to use.
     * @return Returns the converter.
     */
    public CloudFormationConverter classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public Map<String, ObjectNode> convertToNodes(Model model) {
        List<ConversionEnvironment> environments = createConversionEnvironments(model);
        Map<ShapeId, ResourceSchema> resources = convertWithEnvironments(environments);

        Map<String, ObjectNode> convertedNodes = new HashMap<>();
        for (ConversionEnvironment environment : environments) {
            ResourceSchema resourceSchema = resources.get(environment.context.getResource().getId());
            ObjectNode node = resourceSchema.toNode().expectObjectNode();

            // Apply all the mappers' updateNode methods.
            for (CloudFormationMapper mapper : environment.mappers) {
                node = mapper.updateNode(environment.context, resourceSchema, node);
            }
            convertedNodes.put(resourceSchema.getTypeName(), node);
        }
        return convertedNodes;
    }

    /**
     * Converts the annotated resources in the Smithy model to CloudFormation
     * Resource Schemas.
     *
     * @param model Smithy model containing resources to convert.
     * @return Returns the converted resources.
     */
    public List<ResourceSchema> convert(Model model) {
        return ListUtils.copyOf(convertWithEnvironments(createConversionEnvironments(model)).values());
    }

    private Map<ShapeId, ResourceSchema> convertWithEnvironments(List<ConversionEnvironment> environments) {
        Map<ShapeId, ResourceSchema> resourceSchemas = new HashMap<>();
        for (ConversionEnvironment environment : environments) {
            ResourceShape resourceShape = environment.context.getResource();
            ResourceSchema resourceSchema = convertResource(environment, resourceShape);
            resourceSchemas.put(resourceShape.getId(), resourceSchema);
        }
        return resourceSchemas;
    }

    private List<ConversionEnvironment> createConversionEnvironments(Model model) {
        ShapeId serviceShapeId = config.getService();

        if (serviceShapeId == null) {
            throw new CloudFormationException("cloudformation is missing required property, `service`");
        }

        // Load the Smithy2CloudFormation extensions.
        ServiceLoader.load(Smithy2CloudFormationExtension.class, classLoader).forEach(extensions::add);

        // Find the service shape.
        ServiceShape serviceShape = model.getShape(serviceShapeId)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Shape `%s` not found in model", serviceShapeId)))
                .asServiceShape()
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Shape `%s` is not a service shape", serviceShapeId)));

        TopDownIndex topDownIndex = TopDownIndex.of(model);
        Set<ResourceShape> resourceShapes = topDownIndex.getContainedResources(serviceShape);

        List<ConversionEnvironment> environments = new ArrayList<>();
        for (ResourceShape resourceShape : resourceShapes) {
            Optional<ResourceTrait> resourceTrait = resourceShape.getTrait(ResourceTrait.class);
            if (!resourceTrait.isPresent()) {
                continue;
            }

            ConversionEnvironment environment = createConversionEnvironment(model, serviceShape, resourceShape);
            environments.add(environment);
        }

        return environments;
    }

    private ConversionEnvironment createConversionEnvironment(
            Model model,
            ServiceShape serviceShape,
            ResourceShape resourceShape
    ) {
        JsonSchemaConverter.Builder jsonSchemaConverterBuilder = JsonSchemaConverter.builder();

        List<CloudFormationMapper> mappers = new ArrayList<>();
        for (Smithy2CloudFormationExtension extension : extensions) {
            mappers.addAll(extension.getCloudFormationMappers());
            // Add JSON schema mappers from found extensions.
            for (JsonSchemaMapper mapper : extension.getJsonSchemaMappers()) {
                jsonSchemaConverterBuilder.addMapper(mapper);
            }
        }
        mappers.sort(Comparator.comparingInt(CloudFormationMapper::getOrder));

        // Prepare a structure representing the CFN resource to be created.
        StructureShape pseudoResource = getPseudoResource(model, resourceShape);
        Model updatedModel = model.toBuilder().addShape(pseudoResource).build();

        jsonSchemaConverterBuilder.model(updatedModel);

        Context context = new Context(updatedModel, serviceShape, resourceShape,
                pseudoResource, config, jsonSchemaConverterBuilder.build());

        return new ConversionEnvironment(context, mappers);
    }

    private static final class ConversionEnvironment {
        private final Context context;
        private final List<CloudFormationMapper> mappers;

        private ConversionEnvironment(
                Context context,
                List<CloudFormationMapper> mappers
        ) {
            this.context = context;
            this.mappers = mappers;
        }
    }

    private ResourceSchema convertResource(ConversionEnvironment environment, ResourceShape resourceShape) {
        Context context = environment.context;
        JsonSchemaConverter jsonSchemaConverter = context.getJsonSchemaConverter().toBuilder()
                .rootShape(context.getResourceStructure())
                .build();
        SchemaDocument document = jsonSchemaConverter.convert();

        // Prepare the initial contents
        ResourceTrait resourceTrait = resourceShape.expectTrait(ResourceTrait.class);
        ResourceSchema.Builder builder = ResourceSchema.builder();
        builder.typeName(resolveResourceTypeName(environment, resourceTrait));

        // Apply the resource's documentation.
        builder.description(resourceShape.expectTrait(DocumentationTrait.class).getValue());

        // Apply all the mappers' before methods.
        for (CloudFormationMapper mapper : environment.mappers) {
            mapper.before(context, builder);
        }

        // Add the properties from the converted shape.
        document.getRootSchema().getProperties().forEach((name, schema) -> {
            Property property = Property.builder()
                    .schema(schema)
                    .build();
            builder.addProperty(context.getResolvedPropertyName(name), property);
        });

        // Supply all the definitions that were created.
        for (Map.Entry<String, Schema> definition : document.getDefinitions().entrySet()) {
            String definitionName = definition.getKey()
                    .replace(CloudFormationConfig.SCHEMA_COMPONENTS_POINTER, "")
                    .substring(1);
            builder.addDefinition(definitionName, definition.getValue());
        }

        // Apply all the mappers' after methods.
        ResourceSchema resourceSchema = builder.build();
        for (CloudFormationMapper mapper : environment.mappers) {
            resourceSchema = mapper.after(context, resourceSchema);
        }

        return resourceSchema;
    }

    private String resolveResourceTypeName(ConversionEnvironment environment, ResourceTrait resourceTrait) {
        CloudFormationConfig config = environment.context.getConfig();

        String organizationName = config.getOrganizationName();
        if (organizationName == null) {
            throw new CloudFormationException("cloudformation is missing required property, `organizationName`");
        }

        String serviceName = Optional.ofNullable(config.getServiceName())
                .orElseGet(() -> config.getService().getName());

        return String.format("%s::%s::%s", organizationName, serviceName, resourceTrait.getName());
    }

    private StructureShape getPseudoResource(Model model, ResourceShape resource) {
        ResourceIndex resourceIndex = ResourceIndex.of(model);

        StructureShape.Builder builder = StructureShape.builder();
        ShapeId resourceId = resource.getId();
        builder.id(ShapeId.fromParts(resourceId.getNamespace(), resourceId.getName() + "__SYNTHETIC__"));

        resourceIndex.getProperties(resource).forEach((name, definition) -> {
            Shape definitionShape = model.expectShape(definition.getShapeId());
            // We got a member that's pulled in, so reparent it.
            if (definitionShape.isMemberShape()) {
                MemberShape member = definitionShape.asMemberShape().get();
                // Adjust the ID of the member.
                member = member.toBuilder().id(builder.getId().withMember(name)).build();
                builder.addMember(member);
            } else {
                // This is an identifier, create a new member.
                builder.addMember(name, definition.getShapeId());
            }
        });
        return builder.build();
    }
}

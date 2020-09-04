/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Index of resources to their CloudFormation identifiers
 * and properties.
 *
 * <p>This index performs no validation that the identifiers
 * and reference valid shapes.
 */
public final class ResourceIndex implements KnowledgeIndex {
    private final Model model;
    private final Map<ShapeId, Map<String, ResourcePropertyDefinition>> resourcePropertyMutabilities = new HashMap<>();
    private final Map<ShapeId, Set<ShapeId>> resourceExcludedProperties = new HashMap<>();
    private final Map<ShapeId, Set<String>> resourcePrimaryIdentifiers = new HashMap<>();
    private final Map<ShapeId, List<Set<String>>> resourceAdditionalIdentifiers = new HashMap<>();

    public enum ConstraintType {
        CREATE_ONLY,
        READ_ONLY,
        WRITE_ONLY
    }

    public ResourceIndex(Model model) {
        this.model = model;

        OperationIndex operationIndex = OperationIndex.of(model);
        model.shapes(ResourceShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, ResourceTrait.class))
                .forEach(pair -> {
                    ResourceShape resource = pair.getLeft();
                    ShapeId resourceId = resource.getId();

                    // Start with the explicit resource identifiers.
                    resourcePrimaryIdentifiers.put(resourceId, SetUtils.copyOf(resource.getIdentifiers().keySet()));
                    setIdentifierMutabilities(resource);

                    // Use the read lifecycle's input to collect the additional identifiers
                    // and its output to collect readable properties.
                    resource.getRead().ifPresent(operationId -> {
                        operationIndex.getInput(operationId).ifPresent(input -> {
                            addAdditionalIdentifiers(resource, computeResourceAdditionalIdentifiers(input));
                        });
                        operationIndex.getOutput(operationId).ifPresent(output -> {
                            updatePropertyMutabilities(resourceId, operationId, output,
                                    SetUtils.of(ConstraintType.READ_ONLY), this::addReadOnlyMutability);
                        });
                    });

                    // Use the put lifecycle's input to collect puttable properties.
                    resource.getPut().ifPresent(operationId -> {
                        operationIndex.getInput(operationId).ifPresent(input -> {
                            updatePropertyMutabilities(resourceId, operationId, input,
                                    SetUtils.of(ConstraintType.WRITE_ONLY), this::addWriteOnlyMutability);
                        });
                    });

                    // Use the create lifecycle's input to collect creatable properties.
                    resource.getCreate().ifPresent(operationId -> {
                        operationIndex.getInput(operationId).ifPresent(input -> {
                            updatePropertyMutabilities(resourceId, operationId, input,
                                    SetUtils.of(ConstraintType.CREATE_ONLY), this::addCreateOnlyMutability);
                        });
                    });

                    // Use the update lifecycle's input to collect writeable properties.
                    resource.getUpdate().ifPresent(operationId -> {
                        operationIndex.getInput(operationId).ifPresent(input -> {
                            updatePropertyMutabilities(resourceId, operationId, input,
                                    SetUtils.of(ConstraintType.WRITE_ONLY), this::addWriteOnlyMutability);
                        });
                    });

                    // Apply any members found through the trait's additionalSchemas property.
                    for (ShapeId additionalSchema : pair.getRight().getAdditionalSchemas()) {
                        StructureShape shape = model.expectShape(additionalSchema, StructureShape.class);
                        updatePropertyMutabilities(resourceId, null, shape,
                                SetUtils.of(), Function.identity());
                    }
                });
    }

    public static ResourceIndex of(Model model) {
        return model.getKnowledge(ResourceIndex.class, ResourceIndex::new);
    }

    /**
     * Get all members of the CloudFormation resource.
     *
     * @param resource ShapeID of a resource.
     * @return Returns all members that map to CloudFormation resource
     *   properties.
     */
    public Map<String, ResourcePropertyDefinition> getProperties(ToShapeId resource) {
        return resourcePropertyMutabilities.getOrDefault(resource.toShapeId(), MapUtils.of())
                .entrySet().stream()
                .filter(entry -> !getExcludedProperties(resource).contains(entry.getValue().getShapeId()))
                .collect(MapUtils.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets the specified member of the CloudFormation resource.
     *
     * @param resource ShapeID of a resource
     * @param propertyName Name of the property to retrieve
     * @return The property definition.
     */
    public Optional<ResourcePropertyDefinition> getProperty(ToShapeId resource, String propertyName) {
        return Optional.ofNullable(getProperties(resource).get(propertyName));
    }

    /**
     * Get create-specifiable-only members of the CloudFormation resource.
     * These are properties that can only be specified during creation of a
     * resource via CloudFormation. They may also be read via CloudFormation.
     *
     * @param resource ShapeID of a resource.
     * @return Returns create-only member names that map to CloudFormation resource
     *   properties.
     */
    public List<String> getCreateOnlyProperties(ToShapeId resource) {
        return getConstrainedProperties(resource, ConstraintType.CREATE_ONLY);
    }

    /**
     * Get read-only members of the CloudFormation resource. These are
     * properties that cannot be specified but may be read from via
     * CloudFormation.
     *
     * @param resource ShapeID of a resource.
     * @return Returns read-only member names that map to CloudFormation resource
     *   properties.
     */
    public List<String> getReadOnlyProperties(ToShapeId resource) {
        return getConstrainedProperties(resource, ConstraintType.READ_ONLY);
    }

    /**
     * Get write-only members of the CloudFormation resource. These are
     * properties that can be specified but not read from via CloudFormation.
     *
     * @param resource ShapeID of a resource.
     * @return Returns write-only member names that map to CloudFormation resource
     *   properties.
     */
    public List<String> getWriteOnlyProperties(ToShapeId resource) {
        return getConstrainedProperties(resource, ConstraintType.WRITE_ONLY);
    }

    private List<String> getConstrainedProperties(ToShapeId resource, ConstraintType constraint) {
        return resourcePropertyMutabilities.getOrDefault(resource.toShapeId(), MapUtils.of())
                .entrySet()
                .stream()
                .filter(property -> property.getValue().getConstraints().contains(constraint))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get members that have been explicitly excluded from the CloudFormation
     * resource.
     *
     * @param resource ShapeID of a resource.
     * @return Returns members that have been excluded from a CloudFormation
     *   resource.
     */
    public Set<ShapeId> getExcludedProperties(ToShapeId resource) {
        return resourceExcludedProperties.getOrDefault(resource.toShapeId(), SetUtils.of());
    }

    /**
     * Gets a set of member shape ids that represent the primary way
     * to identify a CloudFormation resource.
     *
     * @param resource ShapeID of a resource.
     * @return Returns the identifier set primarily used to access a
     *   CloudFormation resource.
     */
    public Set<String> getPrimaryIdentifiers(ToShapeId resource) {
        return resourcePrimaryIdentifiers.get(resource.toShapeId());
    }

    /**
     * Get a list of sets of member shape ids, each set can be used to identify
     * the CloudFormation resource in addition to its primary identifier(s).
     *
     * @param resource ShapeID of a resource.
     * @return Returns identifier sets used to access a CloudFormation resource.
     */
    public List<Set<String>> getAdditionalIdentifiers(ToShapeId resource) {
        return resourceAdditionalIdentifiers.getOrDefault(resource.toShapeId(), ListUtils.of());
    }

    private void setIdentifierMutabilities(ResourceShape resource) {
        Set<ConstraintType> mutability = getDefaultIdentifierMutabilities(resource);

        ShapeId resourceId = resource.getId();

        resource.getIdentifiers().forEach((name, shape) -> {
            setResourceProperty(resourceId, name, ResourcePropertyDefinition.builder()
                    .hasExplicitConstraints(true)
                    .constraints(mutability)
                    .shapeId(shape)
                    .build());
        });
    }

    private void setResourceProperty(ShapeId resourceId, String name, ResourcePropertyDefinition property) {
        Map<String, ResourcePropertyDefinition> resourceProperties =
                resourcePropertyMutabilities.getOrDefault(resourceId, new HashMap<>());
        resourceProperties.put(name, property);
        resourcePropertyMutabilities.put(resourceId, resourceProperties);
    }

    private Set<ConstraintType> getDefaultIdentifierMutabilities(ResourceShape resource) {
        // If we have a put operation, the identifier mutability is
        // determined by the @noReplace trait.
        // Otherwise, it's read only.
        if (resource.getPut().isPresent()) {
            return SetUtils.of(ConstraintType.CREATE_ONLY);
        }

        return SetUtils.of(ConstraintType.READ_ONLY);
    }

    private List<Map<String, ShapeId>> computeResourceAdditionalIdentifiers(StructureShape readInput) {
        List<Map<String, ShapeId>> identifiers = new ArrayList<>();
        for (MemberShape member : readInput.members()) {
            if (!member.hasTrait(AdditionalIdentifierTrait.class)) {
                continue;
            }

            identifiers.add(MapUtils.of(member.getMemberName(), member.getId()));
        }
        return identifiers;
    }

    private void addAdditionalIdentifiers(ResourceShape resource, List<Map<String, ShapeId>> addedIdentifiers) {
        if (addedIdentifiers.isEmpty()) {
            return;
        }
        ShapeId resourceId = resource.getId();

        List<Set<String>> newIdentifierNames = new ArrayList<>();
        // Make sure we have properties entries for the additional identifiers.
        for (Map<String, ShapeId> addedIdentifier : addedIdentifiers) {
            for (Map.Entry<String, ShapeId> idEntry : addedIdentifier.entrySet()) {
                setResourceProperty(resourceId, idEntry.getKey(), ResourcePropertyDefinition.builder()
                        .constraints(SetUtils.of(ConstraintType.READ_ONLY))
                        .shapeId(idEntry.getValue())
                        .build());
            }
            newIdentifierNames.add(addedIdentifier.keySet());
        }

        List<Set<String>> currentIdentifiers =
                resourceAdditionalIdentifiers.getOrDefault(resourceId, new ArrayList<>());
        currentIdentifiers.addAll(newIdentifierNames);
        resourceAdditionalIdentifiers.put(resourceId, currentIdentifiers);
    }

    private void updatePropertyMutabilities(
            ShapeId resourceId,
            ShapeId operationId,
            StructureShape propertyContainer,
            Set<ConstraintType> defaultMutabilities,
            Function<Set<ConstraintType>, Set<ConstraintType>> updater
    ) {
        addExcludedProperties(resourceId, propertyContainer);

        for (MemberShape member : propertyContainer.members()) {
            // We've explicitly set identifier mutability based on how the
            // resource instance comes about, so only handle non-identifiers.
            if (!operationMemberIsIdentifier(resourceId, operationId, member)) {
                // TODO Does this need a trait because people suck at naming?
                //   Or do all members need to be up-cased in the documents?
                String memberName = member.getMemberName();
                Set<ConstraintType> explicitMutability = getExplicitMutability(member);

                ResourcePropertyDefinition memberProperty = getProperties(resourceId).get(memberName);
                if (memberProperty == null || !explicitMutability.isEmpty()) {
                    memberProperty = ResourcePropertyDefinition.builder()
                            .shapeId(member.getId())
                            .constraints(explicitMutability.isEmpty() ? defaultMutabilities : explicitMutability)
                            .hasExplicitConstraints(!explicitMutability.isEmpty())
                            .build();
                } else if (!memberProperty.hasExplicitConstraints()) {
                    memberProperty = memberProperty.toBuilder()
                            .constraints(updater.apply(memberProperty.getConstraints()))
                            .build();
                }
                setResourceProperty(resourceId, memberName, memberProperty);
            }
        }
    }

    private void addExcludedProperties(ShapeId resourceId, StructureShape propertyContainer) {
        Set<ShapeId> currentExcludedProperties =
                resourceExcludedProperties.getOrDefault(resourceId, new HashSet<>());
        currentExcludedProperties.addAll(propertyContainer.accept(new ExcludedPropertiesVisitor()));
        resourceExcludedProperties.put(resourceId, currentExcludedProperties);
    }

    private boolean operationMemberIsIdentifier(ShapeId resourceId, ShapeId operationId, MemberShape member) {
        // The operationId will be null in the case of additionalSchemas, so
        // we shouldn't worry if these are bound to operation identifiers.
        if (operationId == null) {
            return false;
        }

        IdentifierBindingIndex index = IdentifierBindingIndex.of(model);
        Map<String, String> bindings = index.getOperationBindings(resourceId, operationId);
        String memberName = member.getMemberName();
        // Check for literal identifier bindings.
        for (String bindingMemberName : bindings.values()) {
            if (memberName.equals(bindingMemberName)) {
                return true;
            }
        }

        return false;
    }

    private Set<ConstraintType> getExplicitMutability(MemberShape member) {
        if (member.getMemberTrait(model, ReadOnlyPropertyTrait.class).isPresent()) {
            return SetUtils.of(ConstraintType.READ_ONLY);
        }
        if (member.getMemberTrait(model, CreateOnlyPropertyTrait.class).isPresent()) {
            return SetUtils.of(ConstraintType.CREATE_ONLY);
        }
        if (member.getMemberTrait(model, WriteOnlyPropertyTrait.class).isPresent()) {
            return SetUtils.of(ConstraintType.WRITE_ONLY);
        }
        return SetUtils.of();
    }

    private Set<ConstraintType> addReadOnlyMutability(Set<ConstraintType> constraintTypes) {
        Set<ConstraintType> constraints = new HashSet<>(constraintTypes);
        constraints.add(ConstraintType.READ_ONLY);
        return constraints;
    }

    private Set<ConstraintType> addCreateOnlyMutability(Set<ConstraintType> constraintTypes) {
        Set<ConstraintType> constraints = new HashSet<>(constraintTypes);
        constraints.remove(ConstraintType.READ_ONLY);
        constraints.add(ConstraintType.CREATE_ONLY);
        return constraints;
    }

    private Set<ConstraintType> addWriteOnlyMutability(Set<ConstraintType> constraintTypes) {
        Set<ConstraintType> constraints = new HashSet<>(constraintTypes);
        if (constraintTypes.contains(ConstraintType.READ_ONLY)
                || constraintTypes.contains(ConstraintType.CREATE_ONLY)) {
            constraints.remove(ConstraintType.READ_ONLY);
            constraints.remove(ConstraintType.CREATE_ONLY);
        } else {
            constraints.add(ConstraintType.WRITE_ONLY);
        }
        return constraints;
    }

    private final class ExcludedPropertiesVisitor extends ShapeVisitor.Default<Set<ShapeId>> {
        @Override
        protected Set<ShapeId> getDefault(Shape shape) {
            return SetUtils.of();
        }

        @Override
        public Set<ShapeId> structureShape(StructureShape shape) {
            Set<ShapeId> excludedShapes = new HashSet<>();
            for (MemberShape member : shape.members()) {
                if (member.hasTrait(ExcludePropertyTrait.ID)) {
                    excludedShapes.add(member.getId());
                } else {
                    excludedShapes.addAll(model.expectShape(member.getTarget()).accept(this));
                }
            }
            return excludedShapes;
        }
    }
}

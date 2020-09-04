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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.aws.cloudformation.traits.ResourceIndex.ConstraintType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;

public class ResourceIndexTest {
    private static final ShapeId FOO = ShapeId.from("smithy.example#FooResource");
    private static final ShapeId BAR = ShapeId.from("smithy.example#BarResource");
    private static final ShapeId BAZ = ShapeId.from("smithy.example#BazResource");
    private static final ShapeId MOO = ShapeId.from("smithy.example#MooResource");

    private static Model model;
    private static ResourceIndex resourceIndex;

    @BeforeAll
    public static void loadTestModel() {
        model = Model.assembler()
                .discoverModels(ResourceIndexTest.class.getClassLoader())
                .addImport(ResourceIndexTest.class.getResource("test-service.smithy"))
                .assemble()
                .unwrap();
        resourceIndex = ResourceIndex.of(model);
    }

    private static class ResourceData {
        ShapeId resourceId;
        Collection<String> identifiers;
        List<Set<String>> additionalIdentifiers;
        Map<String, Collection<ConstraintType>> constraints;
    }

    public static Collection<ResourceData> data() {
        ResourceData fooResource = new ResourceData();
        fooResource.resourceId = FOO;
        fooResource.identifiers = SetUtils.of("fooId");
        fooResource.additionalIdentifiers = ListUtils.of();
        fooResource.constraints = MapUtils.of(
                "fooId", SetUtils.of(ConstraintType.READ_ONLY),
                "fooValidFullyMutableProperty", SetUtils.of(),
                "fooValidCreateProperty", SetUtils.of(ConstraintType.CREATE_ONLY),
                "fooValidReadProperty", SetUtils.of(ConstraintType.READ_ONLY),
                "fooValidWriteProperty", SetUtils.of(ConstraintType.WRITE_ONLY));

        ResourceData barResource = new ResourceData();
        barResource.resourceId = BAR;
        barResource.identifiers = SetUtils.of("barId");
        barResource.additionalIdentifiers = ListUtils.of(SetUtils.of("arn"));
        barResource.constraints = MapUtils.of(
                "barId", SetUtils.of(ConstraintType.CREATE_ONLY),
                "arn", SetUtils.of(ConstraintType.READ_ONLY),
                "barValidAdditionalProperty", SetUtils.of(),
                "barImplicitReadProperty", SetUtils.of(ConstraintType.READ_ONLY),
                "barImplicitWriteProperty", SetUtils.of(ConstraintType.WRITE_ONLY));

        ResourceData bazResource = new ResourceData();
        bazResource.resourceId = BAZ;
        bazResource.identifiers = SetUtils.of("barId", "bazId");
        bazResource.additionalIdentifiers = ListUtils.of();
        bazResource.constraints = MapUtils.of(
                "barId", SetUtils.of(ConstraintType.READ_ONLY),
                "bazId", SetUtils.of(ConstraintType.READ_ONLY),
                "bazImplicitFullyMutableProperty", SetUtils.of(),
                "bazImplicitCreateProperty", SetUtils.of(ConstraintType.CREATE_ONLY),
                "bazImplicitReadProperty", SetUtils.of(ConstraintType.READ_ONLY),
                "bazImplicitWriteProperty", SetUtils.of(ConstraintType.WRITE_ONLY));

        return ListUtils.of(fooResource, barResource, bazResource);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void detectsPrimaryIdentifiers(ResourceData data) {
        assertThat(resourceIndex.getPrimaryIdentifiers(data.resourceId),
                containsInAnyOrder(data.identifiers.toArray()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void detectsAdditionalIdentifiers(ResourceData data) {
        assertThat(resourceIndex.getAdditionalIdentifiers(data.resourceId),
                containsInAnyOrder(data.additionalIdentifiers.toArray()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void findsAllProperties(ResourceData data) {
        Map<String, ResourcePropertyDefinition> properties = resourceIndex.getProperties(data.resourceId);

        assertThat(properties, aMapWithSize(data.constraints.size()));
        assertThat(properties.keySet(), containsInAnyOrder(data.constraints.keySet().toArray()));
        properties.forEach((name, definition) -> {
            assertThat(String.format("Mismatch on property %s for %s.", name, data.resourceId),
                    definition.getConstraints(), containsInAnyOrder(data.constraints.get(name).toArray()));
        });
    }

    @Test
    public void setsProperIdentifierMutability() {
        Map<String, ResourcePropertyDefinition> fooProperties = resourceIndex.getProperties(FOO);
        Map<String, ResourcePropertyDefinition> barProperties = resourceIndex.getProperties(BAR);

        assertThat(fooProperties.get("fooId").getConstraints(), contains(ConstraintType.READ_ONLY));
        assertThat(barProperties.get("barId").getConstraints(), contains(ConstraintType.CREATE_ONLY));
    }

    @Test
    public void handlesAdditionalSchemaProperty() {
        Map<String, ResourcePropertyDefinition> barProperties = resourceIndex.getProperties(BAR);

        assertTrue(barProperties.containsKey("barValidAdditionalProperty"));
        assertTrue(barProperties.get("barValidAdditionalProperty").getConstraints().isEmpty());
        assertFalse(barProperties.containsKey("barValidExcludedProperty"));
    }

    @Test
    public void findsCreateOnlyProperties() {
        List<String> fooProperties = resourceIndex.getCreateOnlyProperties(FOO);

        assertThat(fooProperties, hasSize(1));
        String property = fooProperties.get(0);
        assertThat(property, equalTo("fooValidCreateProperty"));
    }

    @Test
    public void findsReadOnlyProperties() {
        List<String> fooProperties = resourceIndex.getReadOnlyProperties(FOO);

        assertThat(fooProperties, hasSize(2));
        String property = fooProperties.get(0);
        assertThat(property, equalTo("fooId"));
        property = fooProperties.get(1);
        assertThat(property, equalTo("fooValidReadProperty"));
    }

    @Test
    public void findsWriteOnlyProperties() {
        List<String> fooProperties = resourceIndex.getWriteOnlyProperties(FOO);

        assertThat(fooProperties, hasSize(1));
        String property = fooProperties.get(0);
        assertThat(property, equalTo("fooValidWriteProperty"));
    }
}

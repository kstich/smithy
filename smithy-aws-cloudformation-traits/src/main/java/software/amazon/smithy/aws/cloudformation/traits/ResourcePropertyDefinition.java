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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.aws.cloudformation.traits.ResourceIndex.ConstraintType;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ResourcePropertyDefinition implements ToSmithyBuilder<ResourcePropertyDefinition> {
    private final ShapeId shapeId;
    private final Set<ConstraintType> constraints;
    private final boolean hasExplicitConstraints;

    private ResourcePropertyDefinition(Builder builder) {
        shapeId = Objects.requireNonNull(builder.shapeId);
        constraints = SetUtils.copyOf(builder.constraints);
        hasExplicitConstraints = builder.hasExplicitConstraints;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ShapeId getShapeId() {
        return shapeId;
    }

    public boolean hasExplicitConstraints() {
        return hasExplicitConstraints;
    }

    public Set<ConstraintType> getConstraints() {
        return constraints;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .shapeId(shapeId)
                .constraints(constraints);
    }

    public static final class Builder implements SmithyBuilder<ResourcePropertyDefinition> {
        private ShapeId shapeId;
        private Set<ConstraintType> constraints = new HashSet<>();
        private boolean hasExplicitConstraints = false;

        @Override
        public ResourcePropertyDefinition build() {
            return new ResourcePropertyDefinition(this);
        }

        public Builder shapeId(ShapeId shapeId) {
            this.shapeId = shapeId;
            return this;
        }

        public Builder constraints(Set<ConstraintType> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder hasExplicitConstraints(boolean hasExplicitConstraints) {
            this.hasExplicitConstraints = hasExplicitConstraints;
            return this;
        }
    }
}

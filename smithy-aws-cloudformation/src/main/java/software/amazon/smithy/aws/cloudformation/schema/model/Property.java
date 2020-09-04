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

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class Property implements ToNode, ToSmithyBuilder<Property> {
    private final boolean insertionOrder;
    private final List<String> dependencies;
    private final Schema schema;
    // Other reserved property names:
    // * readOnly
    // * writeOnly

    private Property(Builder builder) {
        this.insertionOrder = builder.insertionOrder;
        this.dependencies = ListUtils.copyOf(builder.dependencies);
        this.schema = builder.schema;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember("schema", schema.toNode());

        // Only serialize these properties if set to non-defaults.
        if (insertionOrder) {
            builder.withMember("insertionOrder", Node.from(insertionOrder));
        }
        if (!dependencies.isEmpty()) {
            builder.withMember("dependencies", Node.fromStrings(dependencies));
        }

        return builder.build();
    }

    @Override
    public SmithyBuilder<Property> toBuilder() {
        return builder()
                .insertionOrder(insertionOrder)
                .dependencies(dependencies)
                .schema(schema);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isInsertionOrder() {
        return insertionOrder;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public Schema getSchema() {
        return schema;
    }

    public static final class Builder implements SmithyBuilder<Property> {
        private boolean insertionOrder = false;
        private final List<String> dependencies = new ArrayList<>();
        private Schema schema;

        private Builder() {}

        @Override
        public Property build() {
            return new Property(this);
        }

        public Builder insertionOrder(boolean insertionOrder) {
            this.insertionOrder = insertionOrder;
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies.clear();
            this.dependencies.addAll(dependencies);
            return this;
        }

        public Builder addDependency(String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public Builder clearDependencies() {
            this.dependencies.clear();
            return this;
        }

        public Builder schema(Schema schema) {
            this.schema = schema;
            return this;
        }
    }
}

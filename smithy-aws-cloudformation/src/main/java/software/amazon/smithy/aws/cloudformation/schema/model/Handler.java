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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class Handler implements ToNode, ToSmithyBuilder<Handler> {
    public static final String CREATE = "create";
    public static final String READ = "read";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String LIST = "list";

    private final List<String> permissions;

    private Handler(Builder builder) {
        this.permissions = ListUtils.copyOf(builder.permissions);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(Handler.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public SmithyBuilder<Handler> toBuilder() {
        return builder()
                .permissions(permissions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public static final class Builder implements SmithyBuilder<Handler> {
        private final List<String> permissions = new ArrayList<>();

        private Builder() {}

        @Override
        public Handler build() {
            return new Handler(this);
        }

        public Builder permissions(List<String> permissions) {
            this.permissions.clear();
            this.permissions.addAll(permissions);
            return this;
        }

        public Builder addPermission(String permission) {
            this.permissions.add(permission);
            return this;
        }

        public Builder clearPermissions() {
            this.permissions.clear();
            return this;
        }
    }
}

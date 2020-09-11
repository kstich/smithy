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

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * Indicates that the CloudFormation property generated from this member can
 * only be specified when creating a resource and able to be returned in a
 * read request.
 */
public final class CreateOnlyPropertyTrait extends AnnotationTrait {
    public static final ShapeId ID = ShapeId.from("aws.cloudformation#createOnlyProperty");

    public CreateOnlyPropertyTrait(ObjectNode node) {
        super(ID, node);
    }

    public CreateOnlyPropertyTrait() {
        this(Node.objectNode());
    }

    public static final class Provider extends AnnotationTrait.Provider<CreateOnlyPropertyTrait> {
        public Provider() {
            super(ID, CreateOnlyPropertyTrait::new);
        }
    }
}

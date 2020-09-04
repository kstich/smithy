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

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CloudFormationMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.aws.cloudformation.traits.ResourceIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies property mutability restrictions to their proper location
 * in the resulting resource schema.
 */
@SmithyInternalApi
public final class MutabilityMapper implements CloudFormationMapper {

    @Override
    public void before(Context context, ResourceSchema.Builder builder) {
        ResourceIndex resourceIndex = ResourceIndex.of(context.getModel());
        ResourceShape resource = context.getResource();

        resourceIndex.getCreateOnlyProperties(resource).stream()
                .map(context::getPropertyPointer)
                .forEach(builder::addCreateOnlyProperty);

        resourceIndex.getReadOnlyProperties(resource).stream()
                .map(context::getPropertyPointer)
                .forEach(builder::addReadOnlyProperty);

        resourceIndex.getWriteOnlyProperties(resource).stream()
                .map(context::getPropertyPointer)
                .forEach(builder::addWriteOnlyProperty);
    }
}

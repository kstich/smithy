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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CloudFormationMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema.Builder;
import software.amazon.smithy.aws.cloudformation.traits.ResourceIndex;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Applies the resource's identifier and annotated additional identifiers
 * to the resulting resource schema.
 */
@SmithyInternalApi
public final class IdentifierMapper implements CloudFormationMapper {

    @Override
    public void before(Context context, Builder builder) {
        ResourceIndex resourceIndex = ResourceIndex.of(context.getModel());
        ResourceShape resource = context.getResource();

        // Add the primary identifier.
        Set<String> primaryIdentifier = resourceIndex.getPrimaryIdentifiers(resource);
        builder.primaryIdentifier(primaryIdentifier.stream()
                .map(context::getPropertyPointer)
                .collect(Collectors.toList()));

        // Add any additional identifiers.
        List<Set<String>> additionalIdentifiers = resourceIndex.getAdditionalIdentifiers(resource);
        additionalIdentifiers.stream()
                .map(additionalIdentifier -> additionalIdentifier.stream()
                        .map(context::getPropertyPointer)
                        .collect(Collectors.toList()))
                .forEach(builder::addAdditionalIdentifier);
    }
}

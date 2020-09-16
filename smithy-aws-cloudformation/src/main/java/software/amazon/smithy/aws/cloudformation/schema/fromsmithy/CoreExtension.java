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

import java.util.List;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers.DeprecatedMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers.DocumentationMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers.IdentifierMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers.JsonAddMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers.MutabilityMapper;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.utils.ListUtils;

public final class CoreExtension implements Smithy2CloudFormationExtension {
    @Override
    public List<CloudFormationMapper> getCloudFormationMappers() {
        return ListUtils.of(
                new DeprecatedMapper(),
                new DocumentationMapper(),
                new IdentifierMapper(),
                new JsonAddMapper(),
                new MutabilityMapper());
    }
}

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

import software.amazon.smithy.aws.cloudformation.schema.CloudFormationConfig;
import software.amazon.smithy.jsonschema.JsonSchemaConfig;
import software.amazon.smithy.jsonschema.JsonSchemaMapper;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public final class CloudFormationJsonSchemaMapper implements JsonSchemaMapper {
    @Override
    public Schema.Builder updateSchema(Shape shape, Schema.Builder schemaBuilder, JsonSchemaConfig config) {
        if (shape.hasTrait(BoxTrait.class)) {
            schemaBuilder.putExtension("nullable", Node.from(true));
        }

        // Don't overwrite an existing format setting.
        if (!schemaBuilder.getFormat().isPresent()) {
            if (shape.isIntegerShape()) {
                schemaBuilder.format("int32");
            } else if (shape.isLongShape()) {
                schemaBuilder.format("int64");
            } else if (shape.isFloatShape()) {
                schemaBuilder.format("float");
            } else if (shape.isDoubleShape()) {
                schemaBuilder.format("double");
            } else if (shape.isBlobShape()) {
                if (config instanceof CloudFormationConfig) {
                    String blobFormat = ((CloudFormationConfig) config).getDefaultBlobFormat();
                    return schemaBuilder.format(blobFormat);
                }
            } else if (shape.hasTrait(SensitiveTrait.class)) {
                schemaBuilder.format("password");
            }
        }

        return schemaBuilder;
    }
}

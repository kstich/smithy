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

import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.aws.cloudformation.schema.CloudFormationException;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CloudFormationMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodePointer;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds JSON values into the generated CloudFormation resource schemas using
 * a JSON Patch like "add" operation that also generated intermediate objects
 * as needed. Any existing property is overwritten.
 */
@SmithyInternalApi
public final class JsonAddMapper implements CloudFormationMapper {

    private static final Logger LOGGER = Logger.getLogger(JsonAddMapper.class.getName());

    @Override
    public byte getOrder() {
        return 96;
    }

    @Override
    public ObjectNode updateNode(Context context, ResourceSchema resourceSchema, ObjectNode node) {
        Map<String, Node> add = context.getConfig().getJsonAdd();

        if (add.isEmpty()) {
            return node;
        }

        // TODO Update this to take per-resource-shapeId-adjustments
        ObjectNode result = node;
        for (Map.Entry<String, Node> entry : add.entrySet()) {
            try {
                LOGGER.info(() -> "CloudFormation `jsonAdd`: adding `" + entry.getKey() + "`");
                result = NodePointer.parse(entry.getKey())
                        .addWithIntermediateValues(result, entry.getValue().toNode())
                        .expectObjectNode();
            } catch (IllegalArgumentException e) {
                throw new CloudFormationException(e.getMessage(), e);
            }
        }

        return result;
    }
}

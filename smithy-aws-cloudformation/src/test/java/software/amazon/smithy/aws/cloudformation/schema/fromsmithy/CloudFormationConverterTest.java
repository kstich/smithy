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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CloudFormationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

public class CloudFormationConverterTest {

    private static Model testService;

    @BeforeAll
    private static void setup() {
        testService = Model.assembler()
                .addImport(CloudFormationConverterTest.class.getResource("test-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @Test
    public void convertsResourcesToCloudFormation() {
        CloudFormationConfig config = new CloudFormationConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        Map<String, ObjectNode> result = CloudFormationConverter.create().config(config)
                .convertToNodes(testService);

        assertEquals(result.keySet().size(), 3);
        assertThat(result.keySet(), containsInAnyOrder(ListUtils.of(
                "Smithy::TestService::Bar",
                "Smithy::TestService::Basil",
                "Smithy::TestService::FooResource").toArray()));
        for (String resourceTypeName : result.keySet()) {
            String filename = Smithy2CloudFormation.getFileNameFromResourceType(resourceTypeName);
            Node expectedNode = Node.parse(IoUtils.toUtf8String(
                    getClass().getResourceAsStream(filename)));

            Node.assertEquals(result.get(resourceTypeName), expectedNode);
        }
    }

    @Test
    public void usesConfiguredServiceName() {
        CloudFormationConfig config = new CloudFormationConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setServiceName("ExampleService");
        Map<String, ObjectNode> result = CloudFormationConverter.create().config(config)
                .convertToNodes(testService);

        assertEquals(result.keySet().size(), 3);
        assertThat(result.keySet(), containsInAnyOrder(ListUtils.of(
                "Smithy::ExampleService::Bar",
                "Smithy::ExampleService::Basil",
                "Smithy::ExampleService::FooResource").toArray()));
    }

    @Test
    public void handlesDisabledPropertyCaps() {
        CloudFormationConfig config = new CloudFormationConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setDisableCapitalizedProperties(true);
        Map<String, ObjectNode> result = CloudFormationConverter.create().config(config)
                    .convertToNodes(testService);

        assertEquals(result.keySet().size(), 3);
        assertThat(result.keySet(), containsInAnyOrder(ListUtils.of(
                "Smithy::TestService::Bar",
                "Smithy::TestService::Basil",
                "Smithy::TestService::FooResource").toArray()));
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("disable-property-caps.json")));

        Node.assertEquals(result.get("Smithy::TestService::FooResource"), expectedNode);
    }

    @Test
    public void createsAwsSqsQueueResource() {
        Model model = Model.assembler()
                .addImport(CloudFormationConverterTest.class.getResource("sqs.2012-11-05.smithy"))
                // TODO Get this model fixed up with the other traits
                .addImport("/Users/stickevi/Documents/Repositories/smithy/smithy-aws-traits/src/main/resources/META-INF/smithy")
                .discoverModels()
                .assemble()
                .unwrap();

        CloudFormationConfig config = new CloudFormationConfig();
        config.setOrganizationName("AWS");
        config.setService(ShapeId.from("com.amazonaws.sqs#AmazonSQS"));
        config.setServiceName("SQS");
        config.setDisableCapitalizedProperties(true);
        Map<String, ObjectNode> result = CloudFormationConverter.create().config(config)
                .convertToNodes(model);

        assertEquals(result.keySet().size(), 1);
        assertThat(result.keySet(), containsInAnyOrder(ListUtils.of("AWS::SQS::Queue").toArray()));
    }
}

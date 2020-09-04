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

package software.amazon.smithy.aws.cloudformation.schema.fromsmithy.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.cloudformation.schema.CloudFormationConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CloudFormationConverter;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class DocumentationMapperTest {

    private static Model model;

    @BeforeAll
    public static void loadModel() {
        model = Model.assembler()
                .addImport(CloudFormationJsonAddTest.class.getResource("simple.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @Test
    public void supportsExternalDocumentationUrls() {
        CloudFormationConfig config = new CloudFormationConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));

        List<ResourceSchema> schemas = CloudFormationConverter.create()
                .config(config)
                .convert(model);

        assertEquals(1, schemas.size());
        ResourceSchema schema = schemas.get(0);
        assertEquals("https://docs.example.com", schema.getDocumentationUrl());
        assertEquals("https://source.example.com", schema.getSourceUrl());
    }

    @Test
    public void supportsCustomExternalDocNames() {
        CloudFormationConfig config = new CloudFormationConfig();
        config.setOrganizationName("Smithy");
        config.setService(ShapeId.from("smithy.example#TestService"));
        config.setExternalDocKeys(ListUtils.of("main"));
        config.setSourceDocKeys(ListUtils.of("code"));

        List<ResourceSchema> schemas = CloudFormationConverter.create()
                .config(config)
                .convert(model);

        assertEquals(1, schemas.size());
        ResourceSchema schema = schemas.get(0);
        assertEquals("https://docs2.example.com", schema.getDocumentationUrl());
        assertEquals("https://source2.example.com", schema.getSourceUrl());
    }
}

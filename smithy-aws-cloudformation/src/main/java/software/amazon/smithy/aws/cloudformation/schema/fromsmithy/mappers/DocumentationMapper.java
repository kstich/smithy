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

import static java.util.function.Function.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.aws.cloudformation.schema.CloudFormationConfig;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CloudFormationMapper;
import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Context;
import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates the schema doc urls based on the resource's {@code @externalDocumentation}
 * trait. This is configurable based on the {@code "sourceDocKeys"} and
 * {@code "externalDocKeys"} plugin properties.
 */
@SmithyInternalApi
public final class DocumentationMapper implements CloudFormationMapper {

    @Override
    public void before(Context context, ResourceSchema.Builder builder) {
        ResourceShape resource = context.getResource();
        Optional<ExternalDocumentationTrait> traitOptional = resource.getTrait(ExternalDocumentationTrait.class);

        if (!traitOptional.isPresent()) {
            return;
        }

        CloudFormationConfig config = context.getConfig();

        getResolvedExternalDocs(traitOptional.get(), config.getSourceDocKeys()).ifPresent(builder::sourceUrl);
        getResolvedExternalDocs(traitOptional.get(), config.getExternalDocKeys()).ifPresent(builder::documentationUrl);
    }

    private Optional<String> getResolvedExternalDocs(ExternalDocumentationTrait trait, List<String> enabledKeys) {
        // Get the valid list of lower case names to look for when converting.
        List<String> externalDocKeys = new ArrayList<>(enabledKeys.size());
        for (String key : enabledKeys) {
            externalDocKeys.add(key.toLowerCase(Locale.ENGLISH));
        }

        // Get lower case keys to check for when converting.
        Map<String, String> traitUrls = trait.getUrls();
        Map<String, String> lowercaseKeyMap = traitUrls.keySet().stream()
                .collect(MapUtils.toUnmodifiableMap(i -> i.toLowerCase(Locale.US), identity()));

        for (String externalDocKey : externalDocKeys) {
            // Compare the lower case name, but use the specified name.
            if (lowercaseKeyMap.containsKey(externalDocKey)) {
                String traitKey = lowercaseKeyMap.get(externalDocKey);
                // Return the url from the trait.
                return Optional.of(traitUrls.get(traitKey));
            }
        }

        // We didn't find any external docs with the a name in the specified set.
        return Optional.empty();
    }
}

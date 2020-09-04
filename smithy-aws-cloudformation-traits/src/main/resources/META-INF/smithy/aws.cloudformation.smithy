$version: "1.0"

namespace aws.cloudformation

/// Indicates that the CloudFormation property generated from this member is an
/// additional identifier for the resource.
@trait(
    selector: "structure > :test(member > string)",
    conflicts: ["aws.cloudformation#writeOnly"]
)
@tags(["diff.error.remove"])
structure additionalIdentifier {}

/// Indicates that the CloudFormation property generated from this member can
/// only be specified when creating a resource and able to be returned in a
/// read request. Properties not marked createOnly can be specified in an
/// update request.
@trait(
    selector: "structure > member",
    conflicts: ["aws.cloudformation#readOnly", "aws.cloudformation#excludeProperty"]
)
@tags(["diff.error.add"])
structure createOnlyProperty {}

/// Indicates that structure member should not be included in generated
/// CloudFormation resource definitions.
@trait(
    selector: "member :test(< structure)",
    conflicts: [
        "aws.cloudformation#writeOnly",
        "aws.cloudformation#createOnly",
        "aws.cloudformation#readOnly",
        "aws.cloudformation#additionalIdentifier"
    ]
)
@tags(["diff.error.add"])
structure excludeProperty {}

/// Indicates that the CloudFormation property generated from this member
/// cannot be specified but is able to be found in a read request.
@trait(
    selector: "structure > member",
    conflicts: [
        "aws.cloudformation#writeOnly",
        "aws.cloudformation#createOnly",
        "aws.cloudformation#excludeProperty"
    ]
)
@tags(["diff.error.add"])
structure readOnlyProperty {}

/// Indicates that a Smithy resource is a CloudFormation resource.
@trait(selector: "resource")
@tags(["diff.error.const"])
structure resource {
    /// Provides a custom CloudFormation resource name.
    name: String,

    /// A list of additional Shape Ids of structures that will have their
    /// properties added to the CloudFormation resource.
    additionalSchemas: StructureIdList,
}

/// Indicates that the CloudFormation property generated from this member can
/// be specified but is unable to be returned in a read request.
@trait(
    selector: "structure > member",
    conflicts: [
        "aws.cloudformation#readOnly",
        "aws.cloudformation#additionalIdentifier",
        "aws.cloudformation#excludeProperty"
    ]
)
@tags(["diff.error.add"])
structure writeOnlyProperty {}

@private
list StructureIdList {
    @idRef(failWhenMissing: true, selector: "structure")
    member: String
}

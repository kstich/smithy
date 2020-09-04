$version: "1.0"

namespace aws.cloudformation

/// Indicates that the CloudFormation property generated from this member is an
/// additional identifier for the resource.
@trait(
    selector: "structure > :test(member > string)",
    conflicts: ["aws.cloudformation#writeOnlyProperty"]
)
@tags(["diff.error.remove"])
structure additionalIdentifier {}

/// The propertyName trait allows a CloudFormation resource property name to
/// differ from a structure member name used in the model.
@trait(selector: "structure > member")
@tags(["diff.error.const"])
string propertyName

/// Indicates that the CloudFormation property generated from this member can
/// be specified only during resource creation and can be returned in a `read`
/// or `list` request.
@trait(
    selector: "structure > member",
    conflicts: [
        "aws.cloudformation#additionalIdentifier",
        "aws.cloudformation#excludeProperty",
        "aws.cloudformation#mutableProperty",
        "aws.cloudformation#readOnlyProperty",
        "aws.cloudformation#writeOnlyProperty",
    ]
)
structure createOnlyProperty {}

/// Indicates that structure member should not be included in generated
/// CloudFormation resource definitions.
@trait(
    selector: "structure > member",
    conflicts: [
        "aws.cloudformation#additionalIdentifier",
        "aws.cloudformation#createOnlyProperty",
        "aws.cloudformation#mutableProperty",
        "aws.cloudformation#readOnlyProperty",
        "aws.cloudformation#writeOnlyProperty",
    ]
)
@tags(["diff.error.add"])
structure excludeProperty {}

/// Indicates that the CloudFormation property generated from this member
/// does not have any mutability restrictions.
@trait(
    selector: "structure > member",
    conflicts: [
        "aws.cloudformation#createOnlyProperty",
        "aws.cloudformation#excludeProperty",
        "aws.cloudformation#readOnlyProperty",
        "aws.cloudformation#writeOnlyProperty",
    ]
)
@tags(["diff.error.remove"])
structure mutableProperty {}

/// Indicates that the CloudFormation property generated from this member can
/// be returned by a `read` or `list` request, but cannot be set by the user.
@trait(
    selector: "structure > member",
    conflicts: [
        "aws.cloudformation#createOnlyProperty",
        "aws.cloudformation#excludeProperty",
        "aws.cloudformation#mutableProperty",
        "aws.cloudformation#writeOnlyProperty",
    ]
)
@tags(["diff.error.add"])
structure readOnlyProperty {}

/// Indicates that a Smithy resource is a CloudFormation resource.
@trait(selector: "resource")
@tags(["diff.error.add", "diff.error.remove"])
structure resource {
    /// Provides a custom CloudFormation resource name.
    name: String,

    /// A list of additional shape IDs of structures that will have their
    /// properties added to the CloudFormation resource.
    additionalSchemas: StructureIdList,
}

/// Indicates that the CloudFormation property generated from this member can
/// be specified by the user, but cannot be returned by a `read` or `list`
/// request.
@trait(
    selector: "structure > member",
    conflicts: [
        "aws.cloudformation#additionalIdentifier",
        "aws.cloudformation#createOnlyProperty",
        "aws.cloudformation#excludeProperty",
        "aws.cloudformation#mutableProperty",
        "aws.cloudformation#readOnlyProperty",
    ]
)
@tags(["diff.error.add"])
structure writeOnlyProperty {}

@private
list StructureIdList {
    @idRef(failWhenMissing: true, selector: "structure")
    member: String
}

=========================
AWS CloudFormation traits
=========================

CloudFormation traits are used to describe Smithy resources and their
components as they would be converted to `CloudFormation Resource Schemas`_.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.cloudformation#resource-trait:

-------------------------------------
``aws.cloudformation#resource`` trait
-------------------------------------

Summary
    Indicates that a Smithy resource is a CloudFormation resource.
Trait selector
    ``resource``
Value type
    ``structure``

The ``aws.cloudformation#resource`` trait is a structure that supports the
following members:

.. list-table::
    :header-rows: 1
    :widths:  10 20 70

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - Provides a custom CloudFormation resource name.
    * - additionalSchemas
      - ``list<shapeId>``
      - A list of additional :ref:`shape IDs <shape-id>` of structures that
        will have their properties added to the CloudFormation resource.

The following example defines a simple resource that is also a CloudFormation
resource:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#resource

        service TestService {
            version: "2020-07-02",
            resources: [
                Foo,
            ],
        }

        @resource
        resource Foo {
            identifiers: {
                fooId: String,
            },
        }


The following example provides a value for each of the optional properties:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#resource

        service TestService {
            version: "2020-07-02",
            resources: [
                FooResource,
            ],
        }

        @resource(
            name: "Foo",
            additionalSchemas: [AdditionalFooProperties])
        resource FooResource {
            identifiers: {
                fooId: String,
            },
        }

        structure AdditionalFooProperties {
            barProperty: String,
        }

.. _aws.cloudformation#additionalIdentifier-trait:

-------------------------------------------------
``aws.cloudformation#additionalIdentifier`` trait
-------------------------------------------------

Summary
    Indicates that the CloudFormation property generated from this member is an
    additional identifier for the resource.
Trait selector
    ``structure > :test(member > string)``

    *Any structure member that targets a string*
Value type
    Annotation trait
Conflicts with
    :ref:`aws.cloudformation#writeOnlyProperty-trait`

The following example defines a CloudFormation resource that has the
``fooAlias`` property as an additional identifier:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#additionalIdentifier
        use aws.cloudformation#resource

        @resource
        resource Foo {
            identifiers: {
                fooId: String,
            },
            get: GetFoo,
        }

        operation GetFoo {
            input: GetFooRequest,
        }

        structure GetFooRequest {
            @required
            fooId: String,

            @additionalIdentifier
            fooAlias: String,
        }


.. _aws.cloudformation#propertyName-trait:

-----------------------------------------
``aws.cloudformation#propertyName`` trait
-----------------------------------------

Summary
    The propertyName trait allows a CloudFormation resource property name to
    differ from a structure member name used in the model.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    ``string``

Given the following structure definition that is derived in to a CloudFormation
resource:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#propertyName

        structure AdditionalFooProperties {
            bar: String,

            @propertyName("Tags")
            tagList: TagList,
        }

the CloudFormation resource would have the following property names derived
from it:

::

    "bar"
    "Tags"


.. _aws.cloudformation#excludeProperty-trait:

--------------------------------------------
``aws.cloudformation#excludeProperty`` trait
--------------------------------------------

Summary
    Indicates that structure member should not be included in generated
    CloudFormation resource definitions.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    Annotation trait
Conflicts with
    :ref:`aws.cloudformation#additionalIdentifier-trait`,
    :ref:`aws.cloudformation#createOnlyProperty-trait`,
    :ref:`aws.cloudformation#mutableProperty-trait`,
    :ref:`aws.cloudformation#readOnlyProperty-trait`,
    :ref:`aws.cloudformation#writeOnlyProperty-trait`

The following example defines a CloudFormation resource that has the derivable
``responseCode`` property excluded:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#excludeProperty
        use aws.cloudformation#resource

        @resource
        resource Foo {
            identifiers: {
                fooId: String,
            },
            get: GetFoo,
        }

        @http(method: "GET", uri: "/foos/{fooId}", code: 200)
        operation GetFoo {
            input: GetFooRequest,
            output: GetFooResponse,
        }

        structure GetFooRequest {
            @httpLabel
            @required
            fooId: String,
        }

        structure GetFooResponse {
            fooId: String,

            @httpResponseCode
            @excludeProperty
            responseCode: Integer,
        }


.. _aws-cloudformation-mutability-traits:

--------------------------
Property Mutability Traits
--------------------------

The following set of traits define how resource properties can be altered, if
at all. When resolving the properties of a resource, these traits have a
cascading effect on the shape targeted by the member. Shapes that set one of
these traits explicitly will override any inherited value, specified or
derived.

.. _aws.cloudformation#mutableProperty-trait:

``aws.cloudformation#mutableProperty`` trait
--------------------------------------------

Summary
    Indicates that the CloudFormation property generated from this member
    does not have any mutability restrictions.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    Annotation trait
Conflicts with
    :ref:`aws.cloudformation#createOnlyProperty-trait`,
    :ref:`aws.cloudformation#excludeProperty-trait`,
    :ref:`aws.cloudformation#readOnlyProperty-trait`,
    :ref:`aws.cloudformation#writeOnlyProperty-trait`

The following example defines a CloudFormation resource that has the derivable
``tags`` and ``barProperty`` properties marked as fully mutable:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#mutableProperty
        use aws.cloudformation#resource

        @resource(additionalSchemas: FooProperties)
        resource Foo {
            identifiers: {
                fooId: String,
            },
            create: CreateFoo,
        }

        operation CreateFoo {
            input: CreateFooRequest,
            output: CreateFooResponse,
        }

        structure CreateFooRequest {
            @mutableProperty
            tags: TagList,
        }

        structure CreateFooResponse {
            fooId: String,
        }

        structure FooProperties {
            @mutableProperty
            barProperty: String,
        }


.. _aws.cloudformation#createOnlyProperty-trait:

``aws.cloudformation#createOnlyProperty`` trait
-----------------------------------------------

Summary
    Indicates that the CloudFormation property generated from this member can
    be specified only during resource creation and can be returned in a `read`
    or `list` request.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    Annotation trait
Conflicts with
    :ref:`aws.cloudformation#additionalIdentifier-trait`,
    :ref:`aws.cloudformation#excludeProperty-trait`,
    :ref:`aws.cloudformation#mutableProperty-trait`,
    :ref:`aws.cloudformation#readOnlyProperty-trait`,
    :ref:`aws.cloudformation#writeOnlyProperty-trait`

The following example defines a CloudFormation resource that has the derivable
``immutableSetting`` property marked as create only:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#createOnlyProperty
        use aws.cloudformation#resource

        @resource(additionalSchemas: FooProperties)
        resource Foo {
            identifiers: {
                fooId: String,
            },
        }

        structure FooProperties {
            @createOnlyProperty
            immutableSetting: Boolean,
        }


.. _aws.cloudformation#readOnlyProperty-trait:

``aws.cloudformation#readOnlyProperty`` trait
---------------------------------------------

Summary
    Indicates that the CloudFormation property generated from this member can
    be returned by a `read` or `list` request, but cannot be set by the user.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    Annotation trait
Conflicts with
    :ref:`aws.cloudformation#createOnlyProperty-trait`,
    :ref:`aws.cloudformation#excludeProperty-trait`,
    :ref:`aws.cloudformation#mutableProperty-trait`,
    :ref:`aws.cloudformation#writeOnlyProperty-trait`

The following example defines a CloudFormation resource that has the derivable
``updatedAt`` and ``createdAt`` properties marked as read only:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#readOnlyProperty
        use aws.cloudformation#resource

        @resource(additionalSchemas: FooProperties)
        resource Foo {
            identifiers: {
                fooId: String,
            },
            get: GetFoo,
        }

        operation GetFoo {
            input: GetFooRequest,
            output: GetFooResponse,
        }

        structure GetFooRequest {
            fooId: String
        }

        structure GetFooResponse {
            @readOnlyProperty
            updatedAt: Timestamp,
        }

        structure FooProperties {
            @readOnlyProperty
            createdAt: Timestamp,
        }


.. _aws.cloudformation#writeOnlyProperty-trait:

``aws.cloudformation#writeOnlyProperty`` trait
----------------------------------------------

Summary
    Indicates that the CloudFormation property generated from this member can
    be specified by the user, but cannot be returned by a `read` or `list`
    request.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    Annotation trait
Conflicts with
    :ref:`aws.cloudformation#additionalIdentifier-trait`,
    :ref:`aws.cloudformation#createOnlyProperty-trait`,
    :ref:`aws.cloudformation#excludeProperty-trait`,
    :ref:`aws.cloudformation#mutableProperty-trait`,
    :ref:`aws.cloudformation#readOnlyProperty-trait`

The following example defines a CloudFormation resource that has the derivable
``secret`` and ``password`` properties marked as write only:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#writeOnlyProperty
        use aws.cloudformation#resource

        @resource(additionalSchemas: FooProperties)
        resource Foo {
            identifiers: {
                fooId: String,
            },
            create: CreateFoo,
        }

        operation CreateFoo {
            input: CreateFooRequest,
            output: CreateFooResponse,
        }

        structure CreateFooRequest {
            @writeOnlyProperty
            secret: String,
        }

        structure CreateFooResponse {
            fooId: String,
        }

        structure FooProperties {
            @writeOnlyProperty
            password: String,
        }


.. _aws-cloudformation-mutability-derivation:

----------------------------
Deriving Property Mutability
----------------------------

Smithy will automatically derive property mutability information for resources
with the ``@aws.cloudformation#resource`` trait applied.

A resources properties include those that are inferred from the resource's
identifiers as well as ``read`` lifecycle output, ``put`` lifecycle input,
``create`` lifecycle input, ``update`` lifecycle input, and any structures
listed in the ``@aws.cloudformation#resource`` trait's ``additionalSchemas``
property.

A resource's property mutabilities are only derived for the top level of any
of the above structures.

Given the following model,

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#additionalIdentifier
        use aws.cloudformation#createOnlyProperty
        use aws.cloudformation#excludeProperty
        use aws.cloudformation#mutableProperty
        use aws.cloudformation#readOnlyProperty
        use aws.cloudformation#writeOnlyProperty
        use aws.cloudformation#resource

        @resource(additionalSchemas: [FooProperties])
        resource Foo {
            identifiers: {
                fooId: String,
            },
            create: CreateFoo,
            read: GetFoo,
            update: UpdateFoo,
        }

        @http(method: "POST", uri: "/foos", code: 200)
        operation CreateFoo {
            input: CreateFooRequest,
            output: CreateFooResponse,
        }

        structure CreateFooRequest {
            @mutableProperty
            tags: TagList,

            @writeOnlyProperty
            secret: String,

            fooAlias: String,

            mutableProperty: ComplexProperty,
            createProperty: ComplexProperty,
            writeProperty: ComplexProperty,
        }

        structure CreateFooResponse {
            fooId: String,
        }

        @readonly
        @http(method: "GET", uri: "/foos/{fooId}", code: 200)
        operation GetFoo {
            input: GetFooRequest,
            output: GetFooResponse,
        }

        structure GetFooRequest {
            @httpLabel
            @required
            fooId: String,

            @httpQuery("fooAlias")
            @additionalIdentifier
            fooAlias: String,
        }

        structure GetFooResponse {
            fooId: String,

            @httpResponseCode
            @excludeProperty
            responseCode: Integer,

            @readOnlyProperty
            updatedAt: Timestamp,

            mutableProperty: ComplexProperty,
            createProperty: ComplexProperty,
            readProperty: ComplexProperty,
        }

        @idempotent
        @http(method: "PUT", uri: "/foos/{fooId}", code: 200)
        operation UpdateFoo {
            input: UpdateFooRequest,
        }

        structure UpdateFooRequest {
            @httpLabel
            @required
            fooId: String,

            fooAlias: String,
            writeProperty: ComplexProperty,
            mutableProperty: ComplexProperty,
        }

        structure FooProperties {
            addedProperty: String,

            @mutableProperty
            barProperty: String,

            @createOnlyProperty
            immutableSetting: Boolean,

            @readOnlyProperty
            createdAt: Timestamp,

            @writeOnlyProperty
            password: String,
        }

        structure ComplexProperty {
            anotherProperty: String,
        }

The computed resource property mutabilities are:

.. list-table::
    :header-rows: 1
    :widths: 50 50

    * - Name
      - Mutability
    * - ``addedProperty``
      - Mutable
    * - ``barProperty``
      - Mutable
    * - ``createProperty``
      - Create only
    * - ``createdAt``
      - Read only
    * - ``fooAlias``
      - Mutable
    * - ``fooId``
      - Read only
    * - ``immutableSetting``
      - Create only
    * - ``mutableProperty``
      - Mutable
    * - ``password``
      - Write only
    * - ``readProperty``
      - Read only
    * - ``secret``
      - Write only
    * - ``tags``
      - Mutable
    * - ``updatedAt``
      - Read only
    * - ``writeProperty``
      - Write only

.. _CloudFormation Resource Schemas: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html

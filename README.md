# Enterprise Architect RDF Conversion Tool

This project was created as part of the OSLO² ([Github](http://informatievlaanderen.github.io/OSLO/),
 [Vlaanderen.be](https://overheid.vlaanderen.be/producten-diensten/OSLO2)) initiative by the Flemish Government.
 The OSLO² project aims to achieve a solid standard for data exchange between (local) governments.
 These data exchange formats are created as RDF vocabularies (or ontologies), following the principles of the Linked
 Data movement.

This tool provides an automatic way to create ontologies from an Enterprise Architect (a commonly 
used UML modelling tool) project that has the proper format.

## Building & Running

Building requires Maven and Java (JDK) to be installed.

    mvn clean package
    cd target
    java -jar <jarfile> --help

Typical usage (for more options/commands, use `--help`):

    # Converts a diagram in an EA project to a RDF ontology (in the Turtle format).
    # Any ERROR or WARNING log statements should be adressed before using the generated ontology.
    java -jar <jarfile> convert --diagram <diagramName> --config <configFile> --input <EA project file> --output <turtle output file>
    
    # Converts a diagram in a tab separated value file listing the hierarchy, datatypes and more.
    java -jar <jarfile> tsv --diagram <diagramName> --config <configFile> --input <EA project file> --output <turtle output file>

Example config file:

    # Newly defined terms will have a "nl"-typed label and comment. Existing terms will only get a "nl" translations.
    # The "mandatory" key determines whether the tool warns for missing values.
    {
      prefixes: {
        rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        rdfs: "http://www.w3.org/2000/01/rdf-schema#",
    	owl: "http://www.w3.org/2002/07/owl#"
      },
      internalMappings: [
        {
    	  tag: "label-nl",
    	  property: "http://www.w3.org/2000/01/rdf-schema#label",
    	  mandatory: true,
    	  lang: "nl"
    	},
    	{
    	  tag: "definition-nl",
    	  property: "http://www.w3.org/2000/01/rdf-schema#comment",
    	  mandatory: true,
    	  lang: "nl"
    	}
      ],
      externalMappings: [
        {
    	  tag: "label-nl",
    	  property: "http://www.w3.org/2000/01/rdf-schema#label",
    	  lang: "nl"
    	}
      ]
    }

## Conversion Conventions

Due to the mismatch between UML and RDF, there are some constraints that must be followed
and some metadata that should be specified. This metadata is presented in the form of tags
assigned to the elements in the EA project. The configuration specified when running the tool
determines how these tags are converted into RDF.

### Package

A package is mapped to an `owl:Ontology`, each element specified in the ontology is assumed to be specified in the ontology.

Tags:

- `baseURI`: The base URI for each element defined in this package (eg: `http://example.org/ns#`).
- (optional) `baseURIabbrev`: The preferred abbreviated form of the `baseURI`,
 used to generate the [preferred namespace prefix](http://vocab.org/vann/#preferredNamespacePrefix)
 as well as turtle prefixes. (Eg: `ex`)
- (optional) `ignore`: A boolean flag that will make the tool ignore this package and anything defined inside it. (Eg: `true`)
- (optional) `ontologyURI`: The URI of the corresponding `owl:Ontology`. Defaults to `baseURI` minus the last character
 (eg: `http://example.org/ns`).

### Class, DataType & Enumeration

A class, datatype or enumeration is mapped to an `owl:Class`.

Enumerations are restricted to one of the specified values using `owl:oneOf`.

Tags:

- (optional) `ignore`: A boolean flag that will make the tool ignore this element and its attributes (eg: `true`).
- (optional) `name`: The string used to complete the URI for this element.
 If not specified, the name of the class/datatype will be used.
 (Eg: specifying `Canine` as `name` on a class called `Dog` will result in the URI `http://example.org/ns#Canine`.)
- (optional) `package`: The name of the package (representing an ontology) that should define the translations
 for the externally defined class. Only useful in combination with the `uri` tag. Defaults to the
 name of the package in which this element is defined. [More details below.](#specifying-packages)
- (optional) `uri`: The URI of the externally defined class, this will take preference over the `baseURI`/`name` combo.
 Eg: `http://example.org/ns/special#Canine`.

### Attribute

Attributes are mapped to `rdf:Property`, `owl:DatatypeProperty` or `owl:ObjectProperty`.

Tags:

- (optional) `ignore`: A boolean flag that will make the tool ignore this property (eg: `true`).
- (optional) `name`: The string used to complete the URI for this element.
If not specified, the name of the attribute will be used. (Eg: `canine-name`.)
- (optional) `package`: The name of the package (representing an ontology) that should define this
property. Defaults to the package of the class/datatype/enumeration in which this attribute is
specified. [More details below.](#specifying-packages)
- (optional) `parentURI`: the full URI of any property this property should be a subProperty of.
(Eg: `https://www.w3.org/2000/01/rdf-schema#label`)
- (optional) `uri`: The complete URI to use, this will take preference over the `baseURI`/`name` combo.
Eg: `http://example.org/ns/special#canine-name`.

The type of the attribute will be mapped to a class in the project (as an `owl:ObjectProperty`), or to one the supported primitive types:

- Boolean
- Date
- DateTime
- Double
- Int
- String
- Time 

### Connector

A generalization connector will be converted into a `rdfs:subClassOf` triple.
An association or aggregation connector will be converted into a `owl:ObjectProperty`.
Properties follow the direction of the label associated with the connector, _not the direction of
connector_.

Connectors can be defined between elements from different packages, so it is not always clear to
which package (= ontology) they belong.

Tags:

- `package`: the name of the package (representing an ontology)that should define this
property. Defaults to guessing this based on the connected elements.
[More details below.](#specifying-packages)
- (optional) `ignore`: A boolean flag that will make the tool ignore this property (eg: `true`).
- (optional) `name`: The string used to complete the URI for this element.
If not specified, the name of the attribute will be used. (Eg: `petPicture`.)
- (optional) `parentURI`: the full URI of any property this property should be a subProperty of.
(Eg: `http://xmlns.com/foaf/spec/#term_depiction`)
- (optional) `uri`: The complete URI to use, this will take preference over the `baseURI`/`name` combo.
Eg: `http://example.org/ns/special#petPicture`.


## Specifying Packages

Transforming simple models to RDF is straightforward. This becomes more complicated once
multiple vocabularies interact. For example, we could define a new attribute on an externally
defined class. In this case we do not want to include the definition of the class itself in our
ontology. On the other hand, we could want to include additional translations of that class.

Each section below describes what gets exported to the RDF file
when running this tool. There are 3 options:
- An internal, active term: the term is defined by us, in the diagram being exported.
 This means all information should be outputted. This uses the `internalMapping` from the configuration.
- An external term: the term is externally defined and reused by us, we may want to add some
additional information to it, such as a translation.
 This uses the `externalMapping` from the configuration.
- An internal, nonactive term: the term is defined by us, but in a different diagram.
 In this case nothing will be outputted.

### Class, Datatype & Enumeration

The term is seen as an internal, active term when:
- there is no `uri` tag present
- the element is defined in the package being exported

It is considered an external term when:
- there is a `uri` tag present
- the `package` tag (or its default value) refers to the package being exported

### Attributes & Connectors

The term is seen as an internal, active term when:
- there is no `uri` tag present
- the `package` tag (or its default value) refers to the package being exported

It is considered an external term when:
- there is a `uri` tag present
- the `package` tag (or its default value) refers to the package being exported

## Association Classes

In UML, connectors can have a association class to identify information being tracked on the association between
2 classes. In RDF this is represented by simply having the association class being put in the middle of the
connecting property, effectively forming 2 separate properties instead of one.
Since a connector with an association class will result in 2 different RDF properties,
there is a need to differentiate between the tags supplied to the connector.

Any connector that has an association class should prefix all tags with `source-` or `target-`.
Tags of the first form (eg: `source-label-en`) will relate to the property connecting the starting element
with the association class. Likewise, the second form will relate to the property connecting the association class
with the ending element. Starting and ending element are defined by the direction of the label of the connector.
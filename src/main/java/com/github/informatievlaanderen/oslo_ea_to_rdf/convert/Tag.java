package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

/**
 * Definitions of all tags used in the conversion code.
 *
 * @author Dieter De Paepe
 */
public enum Tag {
    // Documented in README.md
    LOCALNAME("name"),
    EXTERNAL_URI("uri"),
    IGNORE("ignore"),
    DEFINING_PACKAGE("package"),
    SUBPROPERTY_OF("parentURI"),
    DOMAIN("domain"),
    RANGE("range"),
    IS_LITERAL("literal"),
    PACKAGE_BASE_URI("baseURI"),
    PACKAGE_BASE_URI_ABBREVIATION("baseURIabbrev"),
    PACKAGE_ONTOLOGY_URI("ontologyURI");

    public static final String ASSOCIATION_SOURCE_PREFIX = "source-";
    public static final String ASSOCIATION_SOURCE_REV_PREFIX = "source-rev-";
    public static final String ASSOCIATION_DEST_PREFIX = "target-";
    public static final String ASSOCIATION_DEST_REV_PREFIX = "target-rev-";

    private String defaultTagName;

    Tag(String defaultTagName) {
        this.defaultTagName = defaultTagName;
    }

    public String getDefaultTagName() {
        return defaultTagName;
    }
}

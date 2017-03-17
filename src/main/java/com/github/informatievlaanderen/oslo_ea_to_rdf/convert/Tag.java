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
    ASSOCIATION("association"),
    PACKAGE_BASE_URI("baseURI"),
    PACKAGE_BASE_URI_ABBREVIATION("baseURIabbrev"),
    PACKAGE_ONTOLOGY_URI("ontologyURI");

    public static final String ASSOCIATION_SOURCE_PREFIX = "source-";
    public static final String ASSOCIATION_DEST_PREFIX = "target-";

    private String defaultTagName;

    Tag(String defaultTagName) {
        this.defaultTagName = defaultTagName;
    }

    public String getDefaultTagName() {
        return defaultTagName;
    }
}

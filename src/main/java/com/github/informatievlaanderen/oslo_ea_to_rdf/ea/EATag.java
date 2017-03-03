package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

/**
 * Represents a tag that is associated to a package, element, connector or attribute.
 */
public interface EATag {
    /**
     * Gets the notes associated with this tag.
     * @return the notes or {@code null}
     */
    String getNotes();

    /**
     * Gets the value of this tag.
     * @return the value
     */
    String getValue();

    /**
     * Gets the key of this tag. Note that multiple tags may have the same key.
     * @return the key value
     */
    String getKey();
}

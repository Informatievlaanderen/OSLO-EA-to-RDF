package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import com.google.common.collect.ListMultimap;

/**
 * An attribute specified for a {@link EAElement}.
 *
 * @author Dieter De Paepe
 */
public interface EAAttribute {
    /**
     * Returns the element in which this attribute is declared.
     * @return the element containing this attribute
     */
    EAElement getElement();

    /**
     * Returns the GUID for this attribute.
     * @return the GUID
     */
    String getGuid();

    /**
     * Returns the name for this attribute.
     * @return the name, or {@code null}
     */
    String getName();

    /**
     * Returns the notes for this attribute.
     * @return the notes, or {@code null}
     */
    String getNotes();

    /**
     * Returns the data type for this attribute.
     * @return the type, or {@code null}
     */
    String getType();

    /**
     * Get the tags for this attribute.
     * @return an immutable multimap
     */
    ListMultimap<String, String> getTags();
}

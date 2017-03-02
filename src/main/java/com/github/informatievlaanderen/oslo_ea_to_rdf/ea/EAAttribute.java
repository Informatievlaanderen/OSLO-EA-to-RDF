package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import com.google.common.collect.ListMultimap;

/**
 * An attribute specified for a {@link EAElement}.
 *
 * @author Dieter De Paepe
 */
public interface EAAttribute extends EAObject {
    /**
     * Returns the element in which this attribute is declared.
     * @return the element containing this attribute
     */
    EAElement getElement();

    /**
     * Returns the data type for this attribute.
     * @return the type, or {@code null}
     */
    String getType();
}

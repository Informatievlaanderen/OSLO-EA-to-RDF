package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

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

    /**
     * Returns the lower cardinality of this attribute.
     * @return a string, not guaranteed to be a parsable number, or {@code null}
     */
    String getLowerBound();

    /**
     * Returns the upper cardinality of this attribute.
     * @return a string, not guaranteed to be a parsable number, or {@code null}
     */
    String getUpperBound();
}

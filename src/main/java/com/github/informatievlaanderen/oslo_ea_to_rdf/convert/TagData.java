package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;

/**
 * Minimal data container describing a RDF (predicate, object) pair that was derived from relevant tags.
 *
 * @author Dieter De Paepe
 */
public class TagData {
    private String originTag;
    private Property property;
    private Literal literal;

    public TagData(String originTag, Property property, Literal literal) {
        this.originTag = originTag;
        this.property = property;
        this.literal = literal;
    }

    public String getOriginTag() {
        return originTag;
    }

    public Property getProperty() {
        return property;
    }

    public Literal getValue() {
        return literal;
    }
}

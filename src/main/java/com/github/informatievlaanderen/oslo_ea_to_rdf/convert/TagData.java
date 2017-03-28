package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Minimal data container describing a RDF (predicate, object) pair that was derived from relevant tags.
 *
 * @author Dieter De Paepe
 */
public class TagData {
    private String originTag;
    private Property property;
    private RDFNode value;

    public TagData(String originTag, Property property, RDFNode value) {
        this.originTag = originTag;
        this.property = property;
        this.value = value;
    }

    public String getOriginTag() {
        return originTag;
    }

    public Property getProperty() {
        return property;
    }

    public RDFNode getValue() {
        return value;
    }
}

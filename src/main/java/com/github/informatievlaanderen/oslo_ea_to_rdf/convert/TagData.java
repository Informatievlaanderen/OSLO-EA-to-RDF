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
    private String originValue;

    public TagData(String originTag, Property property, RDFNode value, String originValue) {
        this.originTag = originTag;
        this.property = property;
        this.value = value;
        this.originValue = originValue;
    }

    public String getOriginTag() {
        return originTag;
    }

    public String getOriginValue() {
        return originValue;
    }

    public Property getProperty() {
        return property;
    }

    public RDFNode getValue() {
        return value;
    }
}

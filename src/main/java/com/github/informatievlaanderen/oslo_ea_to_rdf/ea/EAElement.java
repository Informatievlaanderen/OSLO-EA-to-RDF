package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import com.google.common.collect.ListMultimap;

import java.util.List;

/**
 * A definition of an independent data model. Elements form the basis of Enterprise Architect projects.
 *
 * Three types of elements are discerned: classes, enumerations and datatypes.
 *
 * @author Dieter De Paepe
 */
public interface EAElement extends EAObject {
    /**
     * Gets the primary stereotype for this element.
     * @return the stereotype, or {@code null}
     */
    String getStereoType();

    /**
     * Gets the type of this element.
     * @return the type
     */
    Type getType();

    /**
     * Gets the package in which this element is defined.
     * @return the package
     */
    EAPackage getPackage();

    /**
     * Gets the attributes defined for this element.
     * @return an immutable list
     */
    List<? extends EAAttribute> getAttributes();

    /**
     * Gets all connectors starting from or arriving at this element.
     * @return an immutable list
     */
    List<? extends EAConnector> getConnectors();

    enum Type {
        CLASS,
        DATATYPE,
        ENUMERATION;

        public static Type parse(String eaString) {
            switch (eaString){
                case "Class": return CLASS;
                case "DataType": return DATATYPE;
                case "Enumeration": return ENUMERATION;
                default: throw new IllegalArgumentException("Invalid element type: " + eaString);
            }
        }
    }
}

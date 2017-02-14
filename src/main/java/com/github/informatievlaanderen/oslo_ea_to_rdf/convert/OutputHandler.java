package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import java.util.List;

/**
 * A class that receives the results of a conversion and does something appropriate with it.
 *
 * @author Dieter De Paepe
 */
public interface OutputHandler {
    /**
     * Handles the definition of an ontology.
     * @param sourcePackage the EA element from which the ontology was deduced
     * @param ontology the resource representing the ontology
     * @param prefix the preferred prefix, nullable
     */
    void handleOntology(EAPackage sourcePackage, Resource ontology, String prefix);

    /**
     * Handles the definition of a class.
     * @param sourceElement the EA element from which the class was deduced.
     * @param clazz the resource representing the class
     * @param ontology the ontology in which this class is defined
     * @param parentClasses parent classes of this class
     * @param labels labels for the class
     * @param definitions definitions for the class
     * @param allowedValues nullable, an optional list of valid values that restricts the class
     */
    void handleClass(DiagramElement sourceElement, Resource clazz, Resource ontology, List<Resource> parentClasses,
                     List<Literal> labels, List<Literal> definitions, List<Resource> allowedValues);

    /**
     * Handles the definition of a property.
     * @param source the source EA element
     * @param property the resource representing the property
     * @param ontology the ontology in which the property is defined
     * @param propertyType the type of the property (owl:Property, owl:ObjectProperty, owl:DataProperty)
     * @param domain nullable, domain of the property
     * @param range nullable, range of the property
     * @param labels labels of the property
     * @param definitions definitions of the property
     * @param superProperties all super properties of this property
     */
    void handleProperty(PropertySource source, Resource property, Resource ontology, Resource propertyType, Resource domain,
                              Resource range, List<Literal> labels, List<Literal> definitions,
                              List<Resource> superProperties);

    /**
     * Handles the definition of an instance of a class.
     * @param source the source EA element
     * @param instance the resource representing the instance
     * @param ontology the ontology in which the instance is defined
     * @param clazz the class of which this instance is an instance
     * @param labels the labels of the instance
     * @param definitions the definitions of the instance
     */
    void handleInstance(EAAttribute source, Resource instance, Resource ontology, Resource clazz, List<Literal> labels,
                        List<Literal> definitions);

    class PropertySource {
        public final DiagramConnector connector;
        public final EAAttribute attribute;

        private PropertySource(DiagramConnector connector, EAAttribute attribute) {
            this.connector = connector;
            this.attribute = attribute;
        }

        public static PropertySource from(DiagramConnector conn) {
            return new PropertySource(conn, null);
        }

        public static PropertySource from(EAAttribute att) {
            return new PropertySource(null, att);
        }
    }

}

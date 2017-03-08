package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAAttribute;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
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
     * @param baseURI the namespace of any terms defined in this ontology
     */
    void handleOntology(EAPackage sourcePackage, Resource ontology, String prefix, String baseURI);

    /**
     * Handles the definition of a class.
     * @param sourceElement the EA element from which the class was deduced.
     * @param clazz the resource representing the class
     * @param scope hint on how to handle this element in output
     * @param ontology the ontology in which this class is defined
     * @param parentClasses parent classes of this class
     * @param allowedValues nullable, an optional list of valid values that restricts the class
     */
    void handleClass(EAElement sourceElement, Resource clazz, Scope scope, Resource ontology,
                     List<Resource> parentClasses, List<Resource> allowedValues);

    /**
     * Handles the definition of a property.
     * @param source the source EA element
     * @param property the resource representing the property
     * @param scope hint on how to handle this property in the output
     * @param ontology the ontology in which the property is defined
     * @param propertyType the type of the property (owl:Property, owl:ObjectProperty, owl:DataProperty)
     * @param domain nullable, domain of the property
     * @param range nullable, range of the property
     * @param superProperties all super properties of this property
     * @param lowerbound lower cardinality
     * @param upperbound higher cardinality
     */
    void handleProperty(PropertySource source, Resource property, Scope scope, Resource ontology,
                        Resource propertyType, Resource domain, Resource range, String lowerbound, String upperbound,
                        List<Resource> superProperties);

    /**
     * Handles the definition of an instance of a class.
     * @param source the source EA element
     * @param instance the resource representing the instance
     * @param scope hint on how to handle this property in the output
     * @param ontology the ontology in which the instance is defined
     * @param clazz the class of which this instance is an instance
     */
    void handleInstance(EAAttribute source, Resource instance, Scope scope, Resource ontology,
                        Resource clazz);

    class PropertySource {
        public final EAConnector connector;
        public final EAAttribute attribute;

        private PropertySource(EAConnector connector, EAAttribute attribute) {
            this.connector = connector;
            this.attribute = attribute;
        }

        public static PropertySource from(EAConnector conn) {
            return new PropertySource(conn, null);
        }

        public static PropertySource from(EAAttribute att) {
            return new PropertySource(null, att);
        }
    }

}

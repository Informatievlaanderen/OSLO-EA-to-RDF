package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TagNames.DEFINITON;
import static com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TagNames.LABEL;

/**
 * Conversion functionality.
 *
 * @author Dieter De Paepe
 */
public class Converter {
    // https://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#section-Graph-Literal
    // https://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#built-in-datatypes
    private static final Map<String, Resource> DATATYPES = ImmutableMap.<String, Resource>builder()
            .put("String", XSD.xstring)
            .put("Date", XSD.date)
            .put("Time", XSD.time)
            .put("DateTime", XSD.dateTime)
            .put("Int", XSD.xint) //Note: this differs from "...XMLSchema#integer"!
            .put("Double", XSD.xdouble)
            .put("Boolean", XSD.xboolean)
            .build();

    private final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

    private EARepository repo;
    private List<String> languages;
    private Multimap<String, EAPackage> nameToPackages;
    private Multimap<String, EAElement> nameToElements;
    private OutputHandler outputHandler;

    public Converter(EARepository repo, List<String> mandatoryLanguages, OutputHandler outputHandler) {
        this.repo = repo;
        this.languages = mandatoryLanguages;
        this.outputHandler = outputHandler;

        ImmutableListMultimap.Builder<String, EAPackage> pBuilder = ImmutableListMultimap.builder();
        ImmutableListMultimap.Builder<String, EAElement> eBuilder = ImmutableListMultimap.builder();

        for (EAPackage eaPackage : repo.getPackages()) {
            pBuilder.put(eaPackage.getName(), eaPackage);
            for (EAElement element : eaPackage.getElements())
                eBuilder.put(element.getName(), element);
        }
        nameToPackages = pBuilder.build();
        nameToElements = eBuilder.build();
    }

    public void convertDiagram(EADiagram diagram) {
        UriAssigner.Result uris = new UriAssigner().assignURIs(repo.getPackages(), nameToPackages);

        // Prefixes
        //for (EAPackage eaPackage : uris.packageURIs.keySet()) {
        //    String prefix = Util.getOptionalTag(eaPackage, TagNames.PACKAGE_BASE_URI_ABBREVIATION, null);
        //    if (prefix != null)
        //        model.setNsPrefix(prefix, uris.packageURIs.get(eaPackage));
        //}

        // Convert package
        Resource ontology = convertPackage(diagram.getPackage(), uris.ontologyURIs);

        // Convert elements.
        for (DiagramElement diagramElement : diagram.getElements()) {
            if (Boolean.valueOf(Util.getOptionalTag(diagramElement.getReferencedElement(), TagNames.IGNORE, "false"))) {
                LOGGER.info("Skipping class \"{}\" since it is marked as ignored.", Util.getFullName(diagramElement.getReferencedElement()));
                continue;
            }

            // Do not convert elements from other packages (ontologies)
            if (!diagramElement.getReferencedElement().getPackage().equals(diagram.getPackage())) {
                LOGGER.info("Skipping class \"{}\" since it is defined in a different package.", Util.getFullName(diagramElement.getReferencedElement()));
                continue;
            }

            // Do not convert if the element has an explicit URI defined, it is already defined somewhere else
            if (Util.getOptionalTag(diagramElement.getReferencedElement(), TagNames.EXPLICIT_URI, null) != null) {
                LOGGER.info("Skipping class \"{}\" since it has an explicit URI defined.", Util.getFullName(diagramElement.getReferencedElement()));
                continue;
            }

            convertElement(diagramElement, uris.elementURIs, uris.instanceURIs, ontology);
        }

        // Convert connectors.
        Set<DiagramConnector> connectors = new HashSet<>();
        for (DiagramElement diagramElement : diagram.getElements())
            for (DiagramConnector dConnector : diagramElement.getConnectors())
                connectors.add(dConnector);

        for (DiagramConnector dConnector : connectors) {
            // Skip if the connector is hidden in the diagram.
            if (dConnector.isHidden())
                continue;

            EAConnector connector = dConnector.getReferencedConnector();

            // Inheritance was handled during element processing
            if (EAConnector.TYPE_GENERALIZATION.equals(connector.getType()))
                continue;

            // Skip if marked as ignore.
            if (Boolean.valueOf(Util.getOptionalTag(connector, TagNames.IGNORE, "false"))) {
                LOGGER.info("Skipping connector \"{}\" since it is marked as ignored.", Util.getFullName(connector));
                continue;
            }

            EAPackage definingPackage = uris.definingPackages.get(connector);
            if (!diagram.getPackage().equals(definingPackage)) {
                LOGGER.info("Skipping connector \"{}\" since it is defined in another package.", Util.getFullName(connector));
                continue;
            }

            // Do not convert if the connector has an explicit URI defined, it is already defined somewhere else
            if (Util.getOptionalTag(connector, TagNames.EXPLICIT_URI, null) != null) {
                LOGGER.info("Skipping connector \"{}\" since it has an explicit URI defined.", Util.getFullName(connector));
                continue;
            }

            convertConnector(dConnector, uris.elementURIs, uris.connectorURIs, ontology);
        }

        // Convert non-enum attributes.
        for (DiagramElement diagramElement : diagram.getElements()) {
            // Skip enum attributes
            EAElement element = diagramElement.getReferencedElement();
            if (element.getType() == EAElement.Type.ENUMERATION)
                continue;

            // Skip if the element is set to ignore
            if (Boolean.valueOf(Util.getOptionalTag(element, TagNames.IGNORE, "false"))) {
                // No need for logging, this was already mentioned when the element was skipped
                continue;
            }

            // Skip if the element is defined in another package
            if (!element.getPackage().equals(diagram.getPackage())) {
                // No need for logging, this was already mentioned when the element processed
                // LOGGER.info("Skipping attributes of \"{}\" since that class is defined in another package.", Util.getFullName(diagramElement.getReferencedElement()));
                continue;
            }

            // Do not skip if the element has an external URI - it should be possible to define extra properties on existing elements

            for (EAAttribute attribute : element.getAttributes()) {
                // Do not convert if the connector has an explicit URI defined, it is already defined somewhere else
                if (Util.getOptionalTag(attribute, TagNames.EXPLICIT_URI, null) != null) {
                    LOGGER.info("Skipping attribute \"{}\" since it has an explicit URI defined.", Util.getFullName(attribute));
                    continue;
                }

                // Skip if marked as ignore.
                if (!uris.attributeURIs.containsKey(attribute)) {
                    LOGGER.info("Skipping attribute \"{}\" since it is marked as ignored.", Util.getFullName(attribute));
                    continue;
                }

                convertAttribute(attribute, nameToElements, uris.elementURIs, uris.attributeURIs, ontology);
            }
        }

        // Convert enum values
        for (DiagramElement diagramElement : diagram.getElements()) {
            // Skip enum attributes
            EAElement element = diagramElement.getReferencedElement();
            if (element.getType() != EAElement.Type.ENUMERATION)
                continue;

            // Skip if the element is set to ignore
            if (Boolean.valueOf(Util.getOptionalTag(element, TagNames.IGNORE, "false"))) {
                // No need for logging, this was already mentioned when the element was skipped
                continue;
            }

            // Skip if the element is defined in another package
            if (!element.getPackage().equals(diagram.getPackage())) {
                // No need for logging, this was already mentioned when the element processed
                // LOGGER.info("Skipping enum values of \"{}\" since that class is defined in another package.", Util.getFullName(diagramElement.getReferencedElement()));
                continue;
            }

            // Skip if the element has an external URI - enums shouldn't be adjusted
            if (Util.getOptionalTag(element, TagNames.EXPLICIT_URI, null) != null) {
                LOGGER.info("Skipping enum values of \"{}\" since it has an explicit URI defined.", Util.getFullName(element));
                continue;
            }

            convertEnumerationValues(element, uris.elementURIs, uris.instanceURIs, ontology);
        }
    }

    private Resource convertPackage(EAPackage aPackage, Map<EAPackage, String> ontologyURIs) {
        Resource ontology = ResourceFactory.createResource(ontologyURIs.get(aPackage));
        String prefix = Util.getOptionalTag(aPackage, TagNames.PACKAGE_BASE_URI_ABBREVIATION, null);

        outputHandler.handleOntology(
                aPackage,
                ontology,
                prefix
        );

        return ontology;
    }

    private void convertEnumerationValues(EAElement element, Map<EAElement, String> elementURIs,
                                          Map<EAAttribute, String> instanceURIs, Resource ontology) {
        Resource elementRes = ResourceFactory.createResource(elementURIs.get(element));
        List<? extends EAAttribute> attributes = element.getAttributes();

        for (EAAttribute attribute : attributes) {
            Resource attResource = ResourceFactory.createResource(instanceURIs.get(attribute));

            // Label
            List<Literal> labels = languages.stream()
                    .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(attribute, addLang(LABEL, lang), attribute.getName()), lang))
                    .collect(Collectors.toList());

            // Definition
            List<Literal> definitions = languages.stream()
                    .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(attribute, addLang(DEFINITON, lang), attribute.getName()), lang))
                    .collect(Collectors.toList());

            outputHandler.handleInstance(attribute, attResource, ontology, elementRes, labels, definitions);
        }
    }

    private void convertAttribute(EAAttribute attribute, Multimap<String, EAElement> elementIndex,
                                  Map<EAElement, String> elementURIs, Map<EAAttribute, String> attributeURIs,
                                  Resource ontology) {
        Property attResource = ResourceFactory.createProperty(attributeURIs.get(attribute));

        Resource domain = ResourceFactory.createResource(elementURIs.get(attribute.getElement()));
        Resource range = null;
        Resource propertyType;

        // Type and range
        if (DATATYPES.containsKey(attribute.getType())){
            propertyType = OWL.DatatypeProperty;
            range = DATATYPES.get(attribute.getType());
        } else if (elementIndex.containsKey(attribute.getType())) {
            if (elementIndex.get(attribute.getType()).size() > 1)
                LOGGER.warn("Ambiguous data type \"{}\" for attribute \"{}\".", attribute.getType(), Util.getFullName(attribute));
            propertyType = OWL.ObjectProperty;
            range = ResourceFactory.createResource(elementURIs.get(elementIndex.get(attribute.getType()).iterator().next()));
        } else {
            propertyType = RDF.Property;
            LOGGER.warn("Missing data type for attribute \"{}\".", Util.getFullName(attribute));
        }

        // Label
        List<Literal> labels = languages.stream()
                .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(attribute, addLang(LABEL, lang), attribute.getName()), lang))
                .collect(Collectors.toList());

        // Definition
        List<Literal> definitions = languages.stream()
                .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(attribute, addLang(DEFINITON, lang), attribute.getName()), lang))
                .collect(Collectors.toList());

        // Subproperty
        List<Resource> superProperties = attribute.getTags().get(TagNames.SUBPROPERTY_OF).stream()
                .map(ResourceFactory::createResource)
                .collect(Collectors.toList());

        outputHandler.handleProperty(
                OutputHandler.PropertySource.from(attribute),
                attResource,
                ontology,
                propertyType,
                domain,
                range,
                labels,
                definitions,
                superProperties
        );
    }

    private String addLang(String tagName, String lang) {
        if (lang.isEmpty())
            return tagName;
        else
            return tagName + "-" + lang;
    }

    private void convertConnector(DiagramConnector dConnector,
                                  Map<EAElement, String> elementURIs, Map<EAConnector, String> connectorURIs,
                                  Resource ontology) {
        EAConnector connector = dConnector.getReferencedConnector();
        Resource connResource = ResourceFactory.createResource(connectorURIs.get(connector));

        EAElement source = dConnector.getSource().getReferencedElement();
        EAElement target = dConnector.getDestination().getReferencedElement();
        Resource sourceRes = ResourceFactory.createResource(elementURIs.get(source));
        Resource targetRes = ResourceFactory.createResource(elementURIs.get(target));

        if (connector.getAssociationClass() != null)
            LOGGER.warn("Ignoring association class for connector \"{}\" - association classes are not supported.", Util.getFullName(connector));

        if (Arrays.asList(EAConnector.TYPE_ASSOCIATION, EAConnector.TYPE_AGGREGATION).contains(connector.getType())) {
            // Label
            List<Literal> labels = languages.stream()
                    .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(connector, addLang(LABEL, lang), connector.getName()), lang))
                    .collect(Collectors.toList());

            // Definition
            List<Literal> definitions = languages.stream()
                    .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(connector, addLang(DEFINITON, lang), connector.getName()), lang))
                    .collect(Collectors.toList());

            // Subproperty
            List<Resource> superProperties = connector.getTags().get(TagNames.SUBPROPERTY_OF).stream()
                    .map(ResourceFactory::createResource)
                    .collect(Collectors.toList());

            Resource domain = null;
            Resource range = null;
            // Range and domain
            if (dConnector.getLabelDirection() == EAConnector.Direction.SOURCE_TO_DEST) {
                domain = sourceRes;
                range = targetRes;
            } else if (dConnector.getLabelDirection() == EAConnector.Direction.DEST_TO_SOURCE) {
                domain = targetRes;
                range = sourceRes;
            } else {
                LOGGER.error("Connector \"{}\" has no specified direction - domain/range unspecified.", Util.getFullName(connector));
            }

            outputHandler.handleProperty(
                    OutputHandler.PropertySource.from(dConnector),
                    connResource,
                    ontology,
                    OWL.ObjectProperty,
                    domain,
                    range,
                    labels,
                    definitions,
                    superProperties);
        } else {
            LOGGER.error("Unsupported connector type for \"{}\" - skipping.", Util.getFullName(connector));
        }
    }

    private void convertElement(DiagramElement diagramElement, Map<EAElement, String> elementURIs,
                                Map<EAAttribute, String> instanceURIs, Resource ontology) {
        EAElement element = diagramElement.getReferencedElement();
        Resource classEntity = ResourceFactory.createResource(elementURIs.get(element));

        // Label
        List<Literal> labels = languages.stream()
                .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(element, addLang(LABEL, lang), element.getName()), lang))
                .collect(Collectors.toList());

        // Definition
        List<Literal> definitions = languages.stream()
                .map(lang -> ResourceFactory.createLangLiteral(Util.getMandatoryTag(element, addLang(DEFINITON, lang), element.getName()), lang))
                .collect(Collectors.toList());

        List<Resource> allowedValues = null;
        if (element.getType().equals(EAElement.Type.ENUMERATION)) {
            List<? extends EAAttribute> attributes = element.getAttributes();
            allowedValues = Lists.transform(attributes, a -> ResourceFactory.createResource(instanceURIs.get(a)));
            if (allowedValues.isEmpty())
                LOGGER.warn("No possible values defined for enumeration \"{}\".", Util.getFullName(element));
        }

        List<Resource> parentClasses = new ArrayList<>();

        for (DiagramConnector diagramConnector : diagramElement.getConnectors()) {
            EAConnector connector = diagramConnector.getReferencedConnector();
            if (!EAConnector.TYPE_GENERALIZATION.equals(connector.getType()))
                continue;

            if (connector.getDirection() == EAConnector.Direction.SOURCE_TO_DEST) {
                if (connector.getSource().equals(element))
                    parentClasses.add(ResourceFactory.createResource(elementURIs.get(connector.getDestination())));
            } else if (connector.getDirection() == EAConnector.Direction.DEST_TO_SOURCE) {
                if (connector.getDestination().equals(element))
                    parentClasses.add(ResourceFactory.createResource(elementURIs.get(connector.getSource())));
            } else {
                LOGGER.error("Generalization connector \"{}\" does not specify a direction - skipping.", Util.getFullName(connector));
            }
        }

        outputHandler.handleClass(diagramElement, classEntity, ontology, parentClasses, labels, definitions, allowedValues);
    }
}

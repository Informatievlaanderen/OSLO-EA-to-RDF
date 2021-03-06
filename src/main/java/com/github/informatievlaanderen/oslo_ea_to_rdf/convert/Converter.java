package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
            .put("Integer", XSD.integer)
            .put("Double", XSD.xdouble)
            .put("Boolean", XSD.xboolean)
            .put("LangString", RDF.langString)
            .put("Literal", RDFS.Literal)
            .put("Year", XSD.gYear)
            .put("YearMonth", XSD.gYearMonth)
            .put("Month", XSD.gMonth)
            .put("MonthDay", XSD.gMonthDay)
            .put("Duration", XSD.duration)
            .put("HTML", RDF.HTML)
            .put("URI", XSD.anyURI)
            .build();

    private final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

    private EARepository repo;
    private TagHelper tagHelper;
    private Multimap<String, EAPackage> nameToPackages;
    private Multimap<String, EAElement> nameToElements;
    private OutputHandler outputHandler;

    public Converter(EARepository repo, TagHelper tagHelper, OutputHandler outputHandler) {
        this.repo = repo;
        this.tagHelper = tagHelper;
        this.outputHandler = outputHandler;

        ImmutableListMultimap.Builder<String, EAPackage> pBuilder = ImmutableListMultimap.builder();
        ImmutableListMultimap.Builder<String, EAElement> eBuilder = ImmutableListMultimap.builder();

        for (EAPackage eaPackage : repo.getPackages()) {
            if (Boolean.valueOf(tagHelper.getOptionalTag(eaPackage, Tag.IGNORE, "false")))
                continue;
            pBuilder.put(eaPackage.getName(), eaPackage);
            for (EAElement element : eaPackage.getElements()) {
                if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false")))
                    continue;
                eBuilder.put(element.getName(), element);
            }
        }
        nameToPackages = pBuilder.build();
        nameToElements = eBuilder.build();
    }

    public void convertDiagram(EADiagram diagram) {
        Map<EAConnector, EAConnector.Direction> connectorDirections = indexDirections(diagram);
        UriAssigner.Result uris = new UriAssigner(tagHelper).assignURIs(repo.getPackages(), nameToPackages, connectorDirections);

        // Prefixes
        //for (EAPackage eaPackage : uris.packageURIs.keySet()) {
        //    String prefix = tagHelper.getOptionalTag(eaPackage, Tag.PACKAGE_BASE_URI_ABBREVIATION, null);
        //    if (prefix != null)
        //        model.setNsPrefix(prefix, uris.packageURIs.get(eaPackage));
        //}

        // Convert package
        Resource ontology = convertPackage(diagram.getPackage(), uris.ontologyURIs, uris.packageURIs);

        // Convert elements.
        for (DiagramElement diagramElement : diagram.getElements()) {
            EAElement element = diagramElement.getReferencedElement();
            if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false"))) {
                LOGGER.info("Skipping class \"{}\" since it is marked as ignored.", element.getPath());
                continue;
            }

            boolean currentPackageTerm = element.getPackage().equals(diagram.getPackage());
            boolean customURI = tagHelper.getOptionalTag(element, Tag.EXTERNAL_URI, null) != null;
            boolean refersToThisPackage = diagram.getPackage().getName().equals(tagHelper.getOptionalTag(element, Tag.DEFINING_PACKAGE, element.getPackage().getName()));
            Scope scope = Scope.NOTHING;
            if (currentPackageTerm && !customURI)
                scope = Scope.FULL_DEFINITON;
            else if (customURI && refersToThisPackage)
                scope = Scope.TRANSLATIONS_ONLY;

            convertElement(diagramElement, uris.elementURIs, uris.instanceURIs, ontology, scope);
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
            if (Boolean.valueOf(tagHelper.getOptionalTag(connector, Tag.IGNORE, "false"))) {
                LOGGER.info("Skipping connector \"{}\" since it is marked as ignored.", connector.getPath());
                continue;
            }

            convertConnector(dConnector.getReferencedConnector(), connectorDirections, uris.elementURIs,
                    uris.connectorURIs, uris.definingPackages, ontology, diagram.getPackage());
        }

        // Convert non-enum attributes.
        for (DiagramElement diagramElement : diagram.getElements()) {
            // Skip enum attributes
            EAElement element = diagramElement.getReferencedElement();
            if (element.getType() == EAElement.Type.ENUMERATION)
                continue;

            // Skip if the element is set to ignore
            if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false"))) {
                // No need for logging, this was already mentioned when the element was skipped
                continue;
            }

            for (EAAttribute attribute : element.getAttributes()) {
                // Skip if marked as ignore.
                if (!uris.attributeURIs.containsKey(attribute)) {
                    LOGGER.info("Skipping attribute \"{}\" since it is marked as ignored.", attribute.getPath());
                    continue;
                }

                String definingPackageName = tagHelper.getOptionalTag(attribute, Tag.DEFINING_PACKAGE, attribute.getElement().getPackage().getName());
                boolean currentPackageTerm = diagram.getPackage().getName().equals(definingPackageName);
                boolean externalTerm = tagHelper.getOptionalTag(attribute, Tag.EXTERNAL_URI, null) != null;
                Scope scope = Scope.NOTHING;
                if (!externalTerm && currentPackageTerm)
                    scope = Scope.FULL_DEFINITON;
                else if (externalTerm && currentPackageTerm)
                    scope = Scope.TRANSLATIONS_ONLY;
                convertAttribute(attribute, nameToElements, uris.elementURIs, uris.attributeURIs, ontology, scope);
            }
        }

        // Convert enum values
        for (DiagramElement diagramElement : diagram.getElements()) {
            // Skip enum attributes
            EAElement element = diagramElement.getReferencedElement();
            if (element.getType() != EAElement.Type.ENUMERATION)
                continue;

            // Skip if the element is set to ignore
            if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false"))) {
                // No need for logging, this was already mentioned when the element was skipped
                continue;
            }

            convertEnumerationValues(diagram.getPackage(), element, uris.elementURIs, uris.instanceURIs, ontology);
        }
    }

    /**
     * Creates a mapping of all connectors (including all normalized connectors when an association class is present)
     * in the given diagram to their label direction, or if that is not present,
     * the direction of the connection.
     */
    private Map<EAConnector, EAConnector.Direction> indexDirections(EADiagram diagram) {
        Set<DiagramConnector> connectors = diagram.getElements().stream()
                .flatMap(o -> o.getConnectors().stream())
                .collect(Collectors.toSet());

        ImmutableMap.Builder<EAConnector, EAConnector.Direction> builder = ImmutableMap.builder();
        for (DiagramConnector connector : connectors) {
            EAConnector.Direction direction = connector.getLabelDirection();
            if (direction == EAConnector.Direction.UNSPECIFIED)
                direction = connector.getReferencedConnector().getDirection();
            builder.put(connector.getReferencedConnector(), direction);

            if (connector.getReferencedConnector().getAssociationClass() != null) {
                for (EAConnector innerConnector : Util.extractAssociationElement(connector.getReferencedConnector(), direction)) {
                    builder.put(innerConnector, innerConnector.getDirection());
                }
            }
        }
        return builder.build();
    }

    private Resource convertPackage(EAPackage aPackage, Map<EAPackage, String> ontologyURIs, Map<EAPackage, String> baseURIs) {
        Resource ontology = ResourceFactory.createResource(ontologyURIs.get(aPackage));
        String prefix = tagHelper.getOptionalTag(aPackage, Tag.PACKAGE_BASE_URI_ABBREVIATION, null);
        String baseUri = baseURIs.get(aPackage);

        outputHandler.handleOntology(
                aPackage,
                ontology,
                prefix,
                baseUri
        );

        return ontology;
    }

    private void convertEnumerationValues(EAPackage activePackage, EAElement element, Map<EAElement, String> elementURIs,
                                          Map<EAAttribute, String> instanceURIs, Resource ontology) {
        Resource elementRes = ResourceFactory.createResource(elementURIs.get(element));
        List<? extends EAAttribute> attributes = element.getAttributes();

        for (EAAttribute attribute : attributes) {
            Resource attResource = ResourceFactory.createResource(instanceURIs.get(attribute));

            if (Boolean.valueOf(tagHelper.getOptionalTag(attribute, Tag.IGNORE, "false"))) {
                continue;
            }

            String definingPackageName = tagHelper.getOptionalTag(attribute, Tag.DEFINING_PACKAGE, attribute.getElement().getPackage().getName());
            boolean currentPackageTerm = activePackage.getName().equals(definingPackageName);
            boolean customURI = tagHelper.getOptionalTag(attribute, Tag.EXTERNAL_URI, null) != null;
            Scope scope = Scope.NOTHING;
            if (!customURI && currentPackageTerm)
                scope = Scope.FULL_DEFINITON;
            else if (customURI && currentPackageTerm)
                scope = Scope.TRANSLATIONS_ONLY;

            outputHandler.handleInstance(attribute, attResource, scope, ontology, elementRes);
        }
    }

    private void convertAttribute(EAAttribute attribute, Multimap<String, EAElement> elementIndex,
                                  Map<EAElement, String> elementURIs, Map<EAAttribute, String> attributeURIs,
                                  Resource ontology, Scope scope) {
        if (!attributeURIs.containsKey(attribute))
            return;

        Property attResource = ResourceFactory.createProperty(attributeURIs.get(attribute));

        Resource domain;
        Resource range = null;
        Resource propertyType;

        String customDomain = tagHelper.getOptionalTag(attribute, Tag.DOMAIN, null);
        if (customDomain == null) {
            domain = ResourceFactory.createResource(elementURIs.get(attribute.getElement()));
        } else {
            domain = ResourceFactory.createResource(customDomain);
        }

        String customRange = tagHelper.getOptionalTag(attribute, Tag.RANGE, null);

        // Type and range
        if (customRange != null) {
            boolean rangeIsLiteral = Boolean.parseBoolean(tagHelper.getOptionalTag(attribute, Tag.IS_LITERAL, "false"));
            propertyType = rangeIsLiteral ? OWL.DatatypeProperty : OWL.ObjectProperty;
            range = ResourceFactory.createProperty(customRange);
        } else if (DATATYPES.containsKey(attribute.getType())){
            propertyType = OWL.DatatypeProperty;
            range = DATATYPES.get(attribute.getType());
        } else if (elementIndex.containsKey(attribute.getType())) {
            Collection<EAElement> refElements = elementIndex.get(attribute.getType());
            if (refElements.size() > 1) {
                Iterable<String> names = Iterables.transform(refElements, EAObject::getPath);
                LOGGER.warn("Ambiguous data type \"{}\" for attribute \"{}\": {}.", attribute.getType(), attribute.getPath(), Joiner.on(", ").join(names));
            }
            EAElement selectedElement = refElements.iterator().next();
            boolean isLiteral = Boolean.parseBoolean(tagHelper.getOptionalTag(selectedElement, Tag.IS_LITERAL, "false"));
            propertyType = isLiteral ? OWL.DatatypeProperty : OWL.ObjectProperty;
            range = ResourceFactory.createResource(elementURIs.get(selectedElement));
        } else {
            propertyType = RDF.Property;
            LOGGER.warn("Missing data type for attribute \"{}\".", attribute.getPath());
        }

        // Subproperty
        List<Resource> superProperties = attribute.getTags()
                .stream()
                .filter(t -> tagHelper.getTagKey(Tag.SUBPROPERTY_OF).equals(t.getKey()))
                .map(tag -> TagHelper.USE_NOTE_VALUE.equals(tag.getValue()) ? tag.getNotes() : tag.getValue())
                .map(ResourceFactory::createResource)
                .collect(Collectors.toList());

        outputHandler.handleProperty(
                OutputHandler.PropertySource.from(attribute),
                attResource,
                scope,
                scope == Scope.FULL_DEFINITON ? PackageExported.ACTIVE_PACKAGE : PackageExported.OTHER_PACKAGE,
                ontology,
                propertyType,
                domain,
                range,
                attribute.getLowerBound(),
                attribute.getUpperBound(),
                superProperties
        );
    }

    private void convertConnector(EAConnector bareConnector, Map<EAConnector, EAConnector.Direction> directions,
                                  Map<EAElement, String> elementURIs, Map<EAConnector, String> connectorURIs,
                                  Map<EAConnector, EAPackage> definingPackages, Resource ontology, EAPackage convertedPackage) {
        EAConnector.Direction rawDirection = directions.getOrDefault(bareConnector, EAConnector.Direction.UNSPECIFIED);
        for (EAConnector connector : Util.extractAssociationElement(bareConnector, rawDirection)) {
            if (!connectorURIs.containsKey(connector))
                continue;

            Resource connResource = ResourceFactory.createResource(connectorURIs.get(connector));

            EAElement source = connector.getSource();
            EAElement target = connector.getDestination();
            Resource sourceRes = ResourceFactory.createResource(elementURIs.get(source));
            Resource targetRes = ResourceFactory.createResource(elementURIs.get(target));

            if (connector.getAssociationClass() != null)
                throw new AssertionError("Association class should not be present.");

            if (Arrays.asList(EAConnector.TYPE_ASSOCIATION, EAConnector.TYPE_AGGREGATION).contains(connector.getType())) {
                // Subproperty
                List<Resource> superProperties = connector.getTags()
                        .stream()
                        .filter(t -> tagHelper.getTagKey(Tag.SUBPROPERTY_OF).equals(t.getKey()))
                        .map(tag -> TagHelper.USE_NOTE_VALUE.equals(tag.getValue()) ? tag.getNotes() : tag.getValue())
                        .map(ResourceFactory::createResource)
                        .collect(Collectors.toList());

                Resource domain = null;
                Resource range = null;

                String customDomain = tagHelper.getOptionalTag(connector, Tag.DOMAIN, null);
                String customRange = tagHelper.getOptionalTag(connector, Tag.RANGE, null);

                String cardinality = null;
                boolean rangeIsLiteral = false;

                // Range, domain & cardinality
                EAConnector.Direction connectorDirection = directions.getOrDefault(connector, EAConnector.Direction.UNSPECIFIED);
                if (connectorDirection == EAConnector.Direction.SOURCE_TO_DEST) {
                    domain = sourceRes;
                    range = targetRes;
                    cardinality = connector.getDestinationCardinality();
                    rangeIsLiteral = Boolean.parseBoolean(tagHelper.getOptionalTag(target, Tag.IS_LITERAL, "false"));
                } else if (connectorDirection == EAConnector.Direction.DEST_TO_SOURCE) {
                    domain = targetRes;
                    range = sourceRes;
                    cardinality = connector.getSourceCardinality();
                    rangeIsLiteral = Boolean.parseBoolean(tagHelper.getOptionalTag(target, Tag.IS_LITERAL, "false"));
                } else {
                    LOGGER.error("Connector \"{}\" has no specified direction - domain/range unspecified.", connector.getPath());
                }

                if (customDomain != null)
                    domain = ResourceFactory.createResource(customDomain);
                if (customRange != null)
                    range = ResourceFactory.createResource(customRange);

                String lowerCardinality = null;
                String higherCardinality = null;

                if (cardinality != null && cardinality.contains("..")) {
                    String[] parts = cardinality.split("\\.\\.");
                    lowerCardinality = parts[0];
                    higherCardinality = parts[1];
                } else if (cardinality != null) {
                    lowerCardinality = cardinality;
                    higherCardinality = cardinality;
                }

                EAPackage definingPackage = definingPackages.get(connector);
                PackageExported packageExported;
                if (definingPackage == null)
                    packageExported = PackageExported.UNKNOWN;
                else if (convertedPackage.equals(definingPackage))
                    packageExported = PackageExported.ACTIVE_PACKAGE;
                else
                    packageExported = PackageExported.OTHER_PACKAGE;

                boolean externalTerm = tagHelper.getOptionalTag(connector, Tag.EXTERNAL_URI, null) != null;
                Scope scope = Scope.NOTHING;
                if (!externalTerm && packageExported == PackageExported.ACTIVE_PACKAGE)
                    scope = Scope.FULL_DEFINITON;
                else if (externalTerm && packageExported == PackageExported.ACTIVE_PACKAGE)
                    scope = Scope.TRANSLATIONS_ONLY;

                outputHandler.handleProperty(
                        OutputHandler.PropertySource.from(connector),
                        connResource,
                        scope,
                        packageExported,
                        ontology,
                        rangeIsLiteral ? OWL.DatatypeProperty : OWL.ObjectProperty,
                        domain,
                        range,
                        lowerCardinality,
                        higherCardinality,
                        superProperties);
            } else {
                LOGGER.error("Unsupported connector type for \"{}\" - skipping.", connector.getPath());
            }
        }
    }

    private void convertElement(DiagramElement diagramElement, Map<EAElement, String> elementURIs,
                                Map<EAAttribute, String> instanceURIs, Resource ontology, Scope scope) {
        EAElement element = diagramElement.getReferencedElement();
        Resource classEntity = ResourceFactory.createResource(elementURIs.get(element));

        List<Resource> allowedValues = null;
        if (element.getType().equals(EAElement.Type.ENUMERATION)) {
            List<? extends EAAttribute> attributes = element.getAttributes();
            allowedValues = Lists.transform(attributes, a -> ResourceFactory.createResource(instanceURIs.get(a)));
            if (allowedValues.isEmpty())
                LOGGER.warn("No possible values defined for enumeration \"{}\".", element.getPath());
        }

        List<Resource> parentClasses = new ArrayList<>();

        for (EAConnector connector : element.getConnectors()) {
            if (!EAConnector.TYPE_GENERALIZATION.equals(connector.getType()))
                continue;

            if (connector.getDirection() == EAConnector.Direction.SOURCE_TO_DEST) {
                if (connector.getSource().equals(element))
                    parentClasses.add(ResourceFactory.createResource(elementURIs.get(connector.getDestination())));
            } else if (connector.getDirection() == EAConnector.Direction.DEST_TO_SOURCE) {
                if (connector.getDestination().equals(element))
                    parentClasses.add(ResourceFactory.createResource(elementURIs.get(connector.getSource())));
            } else {
                LOGGER.error("Generalization connector \"{}\" does not specify a direction - skipping.", connector.getPath());
            }
        }

        outputHandler.handleClass(element, classEntity, scope, ontology, parentClasses, allowedValues);
    }
}

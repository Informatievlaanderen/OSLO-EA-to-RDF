package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.SortedOutputModel;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.Charsets;
import com.google.common.collect.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
    private Multimap<String, EADiagram> nameToDiagrams;

    public Converter(EARepository repo, List<String> mandatoryLanguages) {
        this.repo = repo;
        this.languages = mandatoryLanguages;

        ImmutableListMultimap.Builder<String, EAPackage> pBuilder = ImmutableListMultimap.builder();
        ImmutableListMultimap.Builder<String, EAElement> eBuilder = ImmutableListMultimap.builder();
        ImmutableListMultimap.Builder<String, EADiagram> dBuilder = ImmutableListMultimap.builder();

        for (EAPackage eaPackage : repo.getPackages()) {
            pBuilder.put(eaPackage.getName(), eaPackage);
            for (EADiagram eaDiagram : eaPackage.getDiagrams())
                dBuilder.put(eaDiagram.getName(), eaDiagram);
            for (EAElement element : eaPackage.getElements())
                eBuilder.put(element.getName(), element);
        }
        nameToPackages = pBuilder.build();
        nameToDiagrams = dBuilder.build();
        nameToElements = eBuilder.build();
    }

    public void convertDiagramToFile(Path baseModelPath, String diagramName, Path outputFile) throws ConversionException {
        Collection<EADiagram> diagrams = nameToDiagrams.get(diagramName);
        if (diagrams.size() > 1)
            throw new ConversionException("Multiple diagrams share the name \"" + diagramName + "\" - cannot continue.");
        else if (diagrams.isEmpty())
            throw new ConversionException("Diagram not found: " + diagramName + ".");

        EADiagram diagram = diagrams.iterator().next();


        Model model = convertDiagram(diagram);

        // Read the starting model
        if (baseModelPath != null) {
            try (Reader reader = Files.newBufferedReader(baseModelPath)) {
                model.read(reader, null, "TTL");
            } catch (IOException e) {
                throw new ConversionException(e);
            }
        }

        try (Writer w = Files.newBufferedWriter(outputFile, Charsets.UTF_8)) {
            model.write(w, "TTL");
        } catch (IOException e) {
            throw new ConversionException(e);
        }
    }

    public Model convertDiagram(EADiagram diagram) {
        UriAssigner.Result uris = new UriAssigner().assignURIs(repo.getPackages(), nameToPackages);

        Model model = new SortedOutputModel();

        // Prefixes
        model.setNsPrefix("rdf", RDF.uri);
        model.setNsPrefix("rdfs", RDFS.uri);
        model.setNsPrefix("owl", OWL.NS);
        for (EAPackage eaPackage : uris.packageURIs.keySet()) {
            String prefix = Util.getOptionalTag(eaPackage, TagNames.PACKAGE_BASE_URI_ABBREVIATION, null);
            if (prefix != null)
                model.setNsPrefix(prefix, uris.packageURIs.get(eaPackage));
        }

        // Convert package
        Resource packageResource = convertPackage(model, diagram.getPackage(), uris.packageURIs);

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

            convertElement(model, diagramElement, uris.elementURIs, packageResource);
        }

        // Convert connectors.
        for (DiagramElement diagramElement : diagram.getElements()) {
            for (DiagramConnector dConnector : diagramElement.getConnectors()) {
                // Skip if the connector is hidden in the diagram.
                if (dConnector.isHidden())
                    continue;

                EAConnector connector = dConnector.getReferencedConnector();

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

                if (!EAConnector.TYPE_GENERALIZATION.equals(connector.getType())) {
                    // Do not convert if the connector has an explicit URI defined, it is already defined somewhere else
                    if (Util.getOptionalTag(connector, TagNames.EXPLICIT_URI, null) != null) {
                        LOGGER.info("Skipping connector \"{}\" since it has an explicit URI defined.", Util.getFullName(connector));
                        continue;
                    }
                }

                convertConnector(model, dConnector, uris.elementURIs, uris.connectorURIs, packageResource);
            }
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

                convertAttribute(model, attribute, nameToElements, uris.elementURIs, uris.attributeURIs, packageResource);
            }
        }

        // Convert enums
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

            convertEnumeration(model, element, uris.elementURIs, uris.attributeURIs, packageResource);
        }

        return model;
    }

    private Resource convertPackage(Model model, EAPackage aPackage, Map<EAPackage, String> packageURIs) {
        Resource packResource = model.createResource(packageURIs.get(aPackage), OWL.Ontology);
        model.add(packResource, model.createProperty("http://purl.org/vocab/vann/preferredNamespaceUri"), packageURIs.get(aPackage));

        String prefix = Util.getOptionalTag(aPackage, TagNames.PACKAGE_BASE_URI_ABBREVIATION, null);
        if (prefix != null)
            model.add(packResource, model.createProperty("http://purl.org/vocab/vann/preferredNamespacePrefix"), prefix);

        return packResource;
    }

    private void convertEnumeration(Model model, EAElement element, Map<EAElement, String> elementURIs, Map<EAAttribute, String> attributeURIs, Resource packageResource) {
        Resource elementRes = model.createResource(elementURIs.get(element));

        List<? extends EAAttribute> attributes = element.getAttributes();
        List<RDFNode> attributeResources = Lists.transform(attributes, a -> model.createResource(attributeURIs.get(a)));

        if (attributeResources.isEmpty())
            LOGGER.warn("No possible values defined for enumeration \"{}\".", Util.getFullName(element));
        model.add(elementRes, OWL.oneOf, model.createList(attributeResources.iterator()));

        for (EAAttribute attribute : attributes) {
            Resource attResource = model.createResource(attributeURIs.get(attribute));

            model.add(attResource, RDF.type, elementRes);
            model.add(attResource, RDFS.isDefinedBy, packageResource);

            // Label
            for (String lang : languages)
                model.add(attResource, RDFS.label, Util.getMandatoryTag(attribute, addLang(LABEL, lang), attribute.getName()), lang);

            // Definition
            for (String lang : languages)
                model.add(attResource, RDFS.comment, Util.getMandatoryTag(attribute, addLang(DEFINITON, lang), attribute.getName()), lang);
        }
    }

    private void convertAttribute(Model model, EAAttribute attribute, Multimap<String, EAElement> elementIndex,
                                  Map<EAElement, String> elementURIs, Map<EAAttribute, String> attributeURIs,
                                  Resource packageResource) {
        Resource attResource = model.createResource(attributeURIs.get(attribute));

        // Type and range
        if (DATATYPES.containsKey(attribute.getType())){
            model.add(attResource, RDF.type, OWL.DatatypeProperty);
            model.add(attResource, RDFS.range, DATATYPES.get(attribute.getType()));
        } else if (elementIndex.containsKey(attribute.getType())) {
            if (elementIndex.get(attribute.getType()).size() > 1)
                LOGGER.warn("Ambiguous data type \"{}\" for attribute \"{}\".", attribute.getType(), Util.getFullName(attribute));
            EAElement range = elementIndex.get(attribute.getType()).iterator().next();
            model.add(attResource, RDF.type, OWL.ObjectProperty);
            model.add(attResource, RDFS.range, model.createResource(elementURIs.get(range)));
        } else {
            model.add(attResource, RDF.type, RDF.Property);
            LOGGER.warn("Missing data type for attribute \"{}\".", Util.getFullName(attribute));
        }

        // Domain
        model.add(attResource, RDFS.domain, elementURIs.get(attribute.getElement()));

        model.add(attResource, RDFS.isDefinedBy, packageResource);

        // Label
        for (String lang : languages)
            model.add(attResource, RDFS.label, Util.getMandatoryTag(attribute, addLang(LABEL, lang), attribute.getName()), lang);

        // Definition
        for (String lang : languages)
            model.add(attResource, RDFS.comment, Util.getMandatoryTag(attribute, addLang(DEFINITON, lang), attribute.getName()), lang);

        // Subproperty
        for (String superProperty : attribute.getTags().get(TagNames.SUBPROPERTY_OF))
            model.add(attResource, RDFS.subPropertyOf, superProperty);
    }

    private String addLang(String tagName, String lang) {
        if (lang.isEmpty())
            return tagName;
        else
            return tagName + "-" + lang;
    }

    private void convertConnector(Model model, DiagramConnector dConnector,
                                  Map<EAElement, String> elementURIs, Map<EAConnector, String> connectorURIs,
                                  Resource packageResource) {
        EAConnector connector = dConnector.getReferencedConnector();
        Resource connResource = model.createResource(connectorURIs.get(connector), OWL.ObjectProperty);

        EAElement source = dConnector.getSource().getReferencedElement();
        EAElement target = dConnector.getDestination().getReferencedElement();
        Resource sourceRes = model.createResource(elementURIs.get(source));
        Resource targetRes = model.createResource(elementURIs.get(target));

        if (connector.getAssociationClass() != null)
            LOGGER.warn("Ignoring association class for connector \"{}\" - association classes are not supported.", Util.getFullName(connector));

        if (EAConnector.TYPE_GENERALIZATION.equals(connector.getType())) {
            if (connector.getDirection() == EAConnector.Direction.SOURCE_TO_DEST) {
                model.add(sourceRes, RDFS.subClassOf, targetRes);
            } else if (connector.getDirection() == EAConnector.Direction.DEST_TO_SOURCE) {
                model.add(targetRes, RDFS.subClassOf, sourceRes);
            } else {
                LOGGER.error("Generalization connector \"{}\" does not specify a direction - skipping.", Util.getFullName(connector));
            }
        } else if (Arrays.asList(EAConnector.TYPE_ASSOCIATION, EAConnector.TYPE_AGGREGATION).contains(connector.getType())) {
            model.add(connResource, RDFS.isDefinedBy, packageResource);

            // Label
            for (String lang : languages)
                model.add(connResource, RDFS.label, Util.getMandatoryTag(connector, addLang(LABEL, lang), connector.getName()), lang);

            // Definition
            for (String lang : languages)
                model.add(connResource, RDFS.comment, Util.getMandatoryTag(connector, addLang(DEFINITON, lang), connector.getName()), lang);

            // Subproperty
            for (String superProperty : connector.getTags().get(TagNames.SUBPROPERTY_OF))
                model.add(connResource, RDFS.subPropertyOf, superProperty);

            // Range and domain
            if (dConnector.getLabelDirection() == EAConnector.Direction.SOURCE_TO_DEST) {
                model.add(connResource, RDFS.domain, sourceRes);
                model.add(connResource, RDFS.range, targetRes);
            } else if (dConnector.getLabelDirection() == EAConnector.Direction.DEST_TO_SOURCE) {
                model.add(connResource, RDFS.domain, targetRes);
                model.add(connResource, RDFS.range, sourceRes);
            } else {
                LOGGER.error("Connector \"{}\" has no specified direction - skipping domain/range.", Util.getFullName(connector));
            }
        } else {
            LOGGER.error("Unsupported connector type for \"{}\" - skipping.", Util.getFullName(connector));
        }
    }

    private void convertElement(Model model, DiagramElement diagramElement, Map<EAElement, String> elementURIs, RDFNode packageUri) {
        EAElement element = diagramElement.getReferencedElement();
        Resource classEntity = model.createResource(elementURIs.get(element), OWL.Class);

        model.add(classEntity, RDFS.isDefinedBy, packageUri);

        // Label
        for (String lang : languages)
            model.add(classEntity, RDFS.label, Util.getMandatoryTag(element, addLang(LABEL, lang), element.getName()), lang);

        // Definition
        for (String lang : languages)
            model.add(classEntity, RDFS.comment, Util.getMandatoryTag(element, addLang(DEFINITON, lang), element.getName()), lang);
    }
}

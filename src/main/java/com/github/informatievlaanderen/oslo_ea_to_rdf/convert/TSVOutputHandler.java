package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.RangeData;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that aggregates the conversion results in tab separated value file.
 *
 * @author Dieter De Paepe
 */
public class TSVOutputHandler implements OutputHandler {
    private final static Joiner JOINER = Joiner.on(", ");
    private final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

    private BufferedWriter writer;
    private TagHelper tagHelper;
    private EADiagram diagram;
    private List<String> tagNames;

    public TSVOutputHandler(BufferedWriter writer, TagHelper tagHelper, EADiagram diagram) throws IOException {
        this.writer = writer;
        this.tagHelper = tagHelper;
        this.diagram = diagram;
        this.tagNames = tagHelper.getTagNames(tagHelper.getContentMappings(Scope.FULL_DEFINITON));

        write("EA-Type");
        write("EA-Package");
        write("EA-Name");
        write("EA-GUID");
        write("EA-Parent");
        write("EA-Domain");
        write("EA-Domain-GUID");
        write("EA-Range");

        for (String tagName : tagNames) {
            write(tagName);
        }

        write("external term");
        write("namespace");
        write("localname");
        write("type");
        write("domain");
        write("range");
        write("parent");
        write("min card");
        writeNl("max card");
    }

    private DiagramElement findInDiagram(EAElement element) {
        for (DiagramElement diagramElement : diagram.getElements()) {
            if (diagramElement.getReferencedElement().equals(element))
                return diagramElement;
        }
        return null;
    }

    private DiagramConnector findInDiagram(EAConnector connector) {
        for (DiagramElement element : diagram.getElements()) {
            for (DiagramConnector rawConnector : element.getConnectors()) {
                for (EAConnector conn : Util.extractAssociationElement(rawConnector.getReferencedConnector(), rawConnector.getLabelDirection())) {
                    if (connector.equals(conn))
                        return rawConnector;
                }
            }
        }
        return null;
    }

    private List<String> extactTagValues(List<TagData> tagData) {
        List<String> result = new ArrayList<>();

        for (String tagName : tagNames) {
            String s = tagData.stream()
                    .filter(t -> tagName.equals(t.getOriginTag()))
                    .findFirst()
                    .map(t -> t.getValue().isLiteral() ? t.getValue().asLiteral().getString() : t.getValue().asResource().getURI())
                    .orElse("");
            result.add(s);
        }
        return result;
    }

    @Override
    public void handleOntology(EAPackage sourcePackage, Resource ontology, String prefix, String baseURI) {
        write("Package");
        write(""); // Package
        write(sourcePackage.getName());
        write(sourcePackage.getGuid());
        write(""); // Parent
        write(""); // Domain
        write(""); // Domain GUID
        write(""); // Range

        for (String s : tagNames) {
            write("");
        }

        write(""); // external term
        write(ontology.getNameSpace()); //namespace
        write(ontology.getLocalName()); //localname
        write(OWL.Ontology.getURI()); //type
        write(""); // domain
        write(""); // range
        write(""); // parent
        write(""); // min card
        writeNl(""); // max card
    }

    @Override
    public void handleClass(EAElement sourceElement, Resource clazz, Scope scope,
                            Resource ontology, List<Resource> parentClasses, List<EAElement> parentElements, 
                            Map<EAElement, String> elementURIs,
		            List<Resource> allowedValues) {
        write(sourceElement.getType().toString());
        write(sourceElement.getPackage().getName());
        write(sourceElement.getName());
        write(sourceElement.getGuid());

        List<EAElement> parents = findParents(findInDiagram(sourceElement));
        write(JOINER.join(Lists.transform(parents, EAElement::getName)));
        write(""); // Domain
        write(""); // Domain GUID
        write(""); // Range

        for (String tag : extactTagValues(tagHelper.getTagDataFor(sourceElement, tagHelper.getContentMappings(Scope.FULL_DEFINITON)))) {
            write(tag);
        }

        write(String.valueOf(scope != Scope.FULL_DEFINITON));
        write(clazz.getNameSpace());
        write(clazz.getLocalName());
        write(RDFS.Class.getURI());
        write("");
        write("");
        write(JOINER.join(parentClasses));
        write("");
        writeNl("");
    }

    @Override
    public void handleProperty(PropertySource source, Resource property, Scope scope,
                               PackageExported packageExported, Resource ontology,
                               Resource propertyType, Resource domain, Resource range,
			       RangeData rangedata,
                               String lowerbound, String upperbound, List<Resource> superProperties) {
        if (source.attribute != null) {
            write("attribute"); // Type
            write(source.attribute.getElement().getPackage().getName()); // Package
            write(source.attribute.getName()); // Name
            write(source.attribute.getGuid()); // GUID
            write(""); // Parent
            write(source.attribute.getElement().getName()); // Domain
            write(source.attribute.getElement().getGuid()); // Domain GUID
            write(source.attribute.getType()); // Range
        } else {
            write("connector"); // Type
            write(""); // Package
            write(source.connector.getName()); // Name
            write(source.connector.getGuid()); // GUID
            write(""); // Parent
            DiagramConnector dConnector = findInDiagram(source.connector);
            EAConnector.Direction direction = dConnector.getLabelDirection();
            if (direction == EAConnector.Direction.UNSPECIFIED)
                direction = dConnector.getReferencedConnector().getDirection();
            if (EAConnector.Direction.SOURCE_TO_DEST.equals(direction)) {
                write(source.connector.getSource().getName()); // Domain
                write(source.connector.getSource().getGuid()); // Domain GUID
                write(source.connector.getDestination().getName()); // Range
            } else if (EAConnector.Direction.DEST_TO_SOURCE.equals(direction)) {
                write(source.connector.getDestination().getName()); // Domain
                write(source.connector.getDestination().getGuid()); // Domain GUID
                write(source.connector.getSource().getName()); // Range
            } else {
                write(""); // Domain
                write(""); // Domain GUID
                write(""); // Range
            }
        }

        for (String tag : extactTagValues(tagHelper.getTagDataFor(MoreObjects.firstNonNull(source.attribute, source.connector), tagHelper.getContentMappings(Scope.FULL_DEFINITON)))) {
            write(tag);
        }

        write(Boolean.toString(scope != Scope.FULL_DEFINITON));
        write(property.getNameSpace());
        write(property.getLocalName());
        write(propertyType.getURI());
        write(domain != null ? domain.getURI() : "");
        write(range != null ? range.getURI() : "");
        write(JOINER.join(Iterables.transform(superProperties, Resource::getURI)));
        write(lowerbound);
        writeNl(upperbound);
    }

    @Override
    public void handleInstance(EAAttribute source, Resource instance, Scope scope,
                               Resource ontology, Resource clazz) {
        write("attribute"); // Type
        write(source.getElement().getPackage().getName()); // Package
        write(source.getName()); // Name
        write(source.getGuid()); // GUID
        write(""); // Parent
        write(source.getElement().getName()); // Domain
        write(source.getElement().getGuid()); // Domain GUID
        write(""); // Range

        for (String tag : extactTagValues(tagHelper.getTagDataFor(source, tagHelper.getContentMappings(Scope.FULL_DEFINITON)))) {
            write(tag);
        }

        write(Boolean.toString(scope != Scope.FULL_DEFINITON));
        write(instance.getNameSpace());
        write(instance.getURI());
        write(clazz.getURI());
        write("");
        write("");
        write("");
        write("");
        writeNl("");
    }

  @Override
  // expected input is a directed connector
  public void handlePropertyConnector(
      Boolean derived,
      EAConnector source,
      Resource property,
      Scope scope,
      PackageExported packageExported,
      Resource ontology,
      Resource propertyType,
      Resource domain,
      Resource range,
      RangeData rangedata,
      String lowerbound,
      String upperbound,
      List<Resource> superProperties
      ) {
    LOGGER.warn("HANDLEPROPERTYCONNECTOR NOT HANDLED: {} - {}", source.getName(), source.getGuid());
    }


    private List<EAElement> findParents(DiagramElement child) {
        return child.getConnectors().stream()
                .map(dConn -> {
                    EAConnector conn = dConn.getReferencedConnector();
                    if (!EAConnector.TYPE_GENERALIZATION.equals(conn.getType())
                            || Boolean.valueOf(tagHelper.getOptionalTag(conn, Tag.IGNORE, "false"))
                            || dConn.isHidden()
                            || EAConnector.Direction.BIDIRECTIONAL.equals(conn.getDirection())
                            || EAConnector.Direction.UNSPECIFIED.equals(conn.getDirection()))
                        return null;
                    if (EAConnector.Direction.SOURCE_TO_DEST.equals(conn.getDirection())) {
                        if (child.getReferencedElement().equals(conn.getSource()))
                            return conn.getDestination();
                        else
                            return null;
                    } else {
                        if (child.getReferencedElement().equals(conn.getDestination()))
                            return conn.getSource();
                        else
                            return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void write(String s) {
        try {
            writer.write("\"");
            writer.write(Strings.nullToEmpty(s).replaceAll("\"", "\"\""));
            writer.write("\"");
            writer.write("\t");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

     private void writeNl(String s) {
         try {
             writer.write("\"");
             writer.write(Strings.nullToEmpty(s).replaceAll("\"", "\"\""));
             writer.write("\"");
             writer.write("\n");
         } catch (IOException e) {
             throw new RuntimeException(e);
         }
     }
}

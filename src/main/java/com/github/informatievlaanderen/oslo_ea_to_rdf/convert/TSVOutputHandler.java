package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class that aggregates the conversion results in tab separated value file.
 *
 * @author Dieter De Paepe
 */
public class TSVOutputHandler implements OutputHandler {
    private List<String> languages;
    private BufferedWriter writer;

    public TSVOutputHandler(BufferedWriter writer, List<String> languages) throws IOException {
        this.writer = writer;
        this.languages = languages;

        write("EA-Type");
        write("EA-Package");
        write("EA-Name");
        write("EA-Parent");
        write("EA-Domain");
        write("EA-Range");

        for (String language : languages)
            write("Label" + (language == null ? "" : " (" + language + ")"));
        for (String language : languages)
            write("Definition" + (language == null ? "" : " (" + language + ")"));

        write("namespace");
        write("localname");
        write("type");
        write("domain");
        write("range");
        writeNl("parent");
    }

    @Override
    public void handleOntology(EAPackage sourcePackage, Resource ontology, String prefix) {
        write("Package");
        write("");
        write(sourcePackage.getName());
        write("");
        write("");
        write("");

        for (String language : languages)
            write("");
        for (String language : languages)
            write("");

        write(ontology.getNameSpace());
        write(ontology.getLocalName());
        write(OWL.Ontology.getURI());
        write("");
        write("");
        writeNl("");
    }

    @Override
    public void handleClass(DiagramElement sourceElement, Resource clazz, Resource ontology, List<Resource> parentClasses, List<Literal> labels, List<Literal> definitions, List<Resource> allowedValues) {
        write(sourceElement.getReferencedElement().getType().toString());
        write(sourceElement.getReferencedElement().getPackage().getName());
        write(sourceElement.getReferencedElement().getName());
        write(Joiner.on(", ").join(Iterables.transform(findParents(sourceElement), EAElement::getName)));
        write("");
        write("");

        for (String language : languages)
            write(findLiteral(labels, language));

        for (String language : languages)
            write(findLiteral(definitions, language));

        write(clazz.getNameSpace());
        write(clazz.getLocalName());
        write(RDFS.Class.getURI());
        write("");
        write("");
        writeNl(Joiner.on(", ").join(parentClasses));
    }

    @Override
    public void handleProperty(PropertySource source, Resource property, Resource ontology, Resource propertyType,
                               Resource domain, Resource range, List<Literal> labels, List<Literal> definitions,
                               List<Resource> superProperties) {
        if (source.attribute != null) {
            write("attribute");
            write(source.attribute.getElement().getPackage().getName());
            write(source.attribute.getName());
            write("");
            write(source.attribute.getElement().getName());
            write(source.attribute.getType());
        } else {
            write("connector");
            write("");
            write(source.connector.getReferencedConnector().getName());
            write("");
            if (EAConnector.Direction.SOURCE_TO_DEST.equals(source.connector.getLabelDirection())) {
                write(source.connector.getReferencedConnector().getSource().getName());
                write(source.connector.getReferencedConnector().getDestination().getName());
            } else if (EAConnector.Direction.DEST_TO_SOURCE.equals(source.connector.getLabelDirection())) {
                write(source.connector.getReferencedConnector().getDestination().getName());
                write(source.connector.getReferencedConnector().getSource().getName());
            } else {
                write("");
                write("");
            }
        }

        for (String language : languages)
            write(findLiteral(labels, language));
        for (String language : languages)
            write(findLiteral(definitions, language));

        write(property.getNameSpace());
        write(property.getLocalName());
        write(propertyType.getURI());
        write(domain != null ? domain.getURI() : "");
        write(range != null ? range.getURI() : "");
        writeNl(Joiner.on(", ").join(Iterables.transform(superProperties, Resource::getURI)));
    }

    @Override
    public void handleInstance(EAAttribute source, Resource instance, Resource ontology, Resource clazz,
                               List<Literal> labels, List<Literal> definitions) {
        write("attribute");
        write(source.getElement().getPackage().getName());
        write(source.getName());
        write("");
        write(source.getElement().getName());
        write("");

        for (String language : languages)
            write(findLiteral(labels, language));
        for (String language : languages)
            write(findLiteral(definitions, language));

        write(instance.getNameSpace());
        write(instance.getURI());
        write(clazz.getURI());
        write("");
        write("");
        writeNl("");
    }

    private List<EAElement> findParents(DiagramElement child) {
        return child.getConnectors().stream()
                .map(dConn -> {
                    EAConnector conn = dConn.getReferencedConnector();
                    if (!EAConnector.TYPE_GENERALIZATION.equals(conn.getType())
                            || Boolean.valueOf(Util.getOptionalTag(conn, TagNames.IGNORE, "false"))
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

    private String findLiteral(List<Literal> list, String lang) {
        for (Literal literal : list) {
            if (lang.equals(literal.getLanguage()))
                return literal.getString();
        }
        return "";
    }

    private void write(String s) {
        try {
            writer.write(Strings.nullToEmpty(s));
            writer.write("\t");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

     private void writeNl(String s) {
         try {
             writer.write(Strings.nullToEmpty(s));
             writer.write("\n");
         } catch (IOException e) {
             throw new RuntimeException(e);
         }
     }
}

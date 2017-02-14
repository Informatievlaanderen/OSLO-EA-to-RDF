package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.SortedOutputModel;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.DiagramElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAAttribute;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import com.google.common.base.Charsets;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Class that aggregates the conversion results in a RDF model.
 *
 * @author Dieter De Paepe
 */
public class RDFOutputHandler implements OutputHandler {
    private Model model;

    public RDFOutputHandler() {
        model = new SortedOutputModel();
        model.setNsPrefix("rdf", RDF.uri);
        model.setNsPrefix("rdfs", RDFS.uri);
        model.setNsPrefix("owl", OWL.NS);
    }

    /**
     * Adds the content of the specified turtle file to the model
     * @param inputFile a turtle file
     * @throws IOException if an exception occurred while reading the file
     */
    public void addToModel(Path inputFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(inputFile)) {
            model.read(reader, null, "TTL");
        }
    }

    /**
     * Writes the internal model to the specified turtle file.
     * @param outputFile the desired output turtle file
     * @throws IOException if an exception occurred while writing the file
     */
    public void writeToFile(Path outputFile) throws IOException {
        try (Writer w = Files.newBufferedWriter(outputFile, Charsets.UTF_8)) {
            model.write(w, "TTL");
        }
    }

    @Override
    public void handleOntology(EAPackage sourcePackage, Resource ontology, String prefix) {
        model.add(ontology, RDF.type, OWL.Ontology);
        model.add(ontology, model.createProperty("http://purl.org/vocab/vann/preferredNamespaceUri"), ontology.getURI());
        if (prefix != null) {
            model.setNsPrefix(prefix, ontology.getURI());
            model.add(ontology, model.createProperty("http://purl.org/vocab/vann/preferredNamespacePrefix"), prefix);
        }
    }

    @Override
    public void handleClass(DiagramElement sourceElement, Resource clazz, Resource ontology,
                            List<Resource> parentClasses, List<Literal> labels,
                            List<Literal> definitions, List<Resource> allowedValues) {
        model.add(clazz, RDF.type, OWL.Class);
        model.add(clazz, RDFS.isDefinedBy, ontology);
        for (Resource parent : parentClasses)
            model.add(clazz, RDFS.subClassOf, parent);
        for (Literal label : labels)
            model.add(clazz, RDFS.label, label);
        for (Literal definition : definitions)
            model.add(clazz, RDFS.comment, definition);
        if (allowedValues != null)
            model.add(clazz, OWL.oneOf, model.createList(allowedValues.iterator()));
    }

    @Override
    public void handleProperty(PropertySource source, Resource property, Resource ontology, Resource propertyType, Resource domain,
                                     Resource range, List<Literal> labels, List<Literal> definitions, List<Resource> superProperties) {
        model.add(property, RDF.type, propertyType);
        model.add(property, RDFS.isDefinedBy, ontology);
        if (domain != null)
            model.add(property, RDFS.domain, domain);
        if (range != null)
            model.add(property, RDFS.range, range);
        for (Literal label : labels)
            model.add(property, RDFS.label, label);
        for (Literal definition : definitions)
            model.add(property, RDFS.comment, definition);
        for (Resource superProperty : superProperties)
            model.add(property, RDFS.subPropertyOf, superProperty);
    }

    @Override
    public void handleInstance(EAAttribute source, Resource instance, Resource ontology, Resource clazz, List<Literal> labels, List<Literal> definitions) {
        model.add(instance, RDF.type, clazz);
        model.add(instance, RDFS.isDefinedBy, ontology);
        for (Literal label : labels)
            model.add(instance, RDFS.label, label);
        for (Literal definition : definitions)
            model.add(instance, RDFS.comment, definition);
    }
}

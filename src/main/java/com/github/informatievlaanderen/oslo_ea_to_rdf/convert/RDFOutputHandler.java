package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.SortedOutputModel;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAAttribute;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that aggregates the conversion results in a RDF model.
 *
 * @author Dieter De Paepe
 */
public class RDFOutputHandler implements OutputHandler {
  private final Logger LOGGER = LoggerFactory.getLogger(RDFOutputHandler.class);

  private Model model;
  private TagHelper tagHelper;
  private boolean forceFullOutput;

  public RDFOutputHandler(Map<String, Resource> prefixes, TagHelper tagHelper, boolean fullOutput) {
    this.tagHelper = tagHelper;
    this.forceFullOutput = fullOutput;

    model = new SortedOutputModel();
    for (Map.Entry<String, Resource> entry : prefixes.entrySet()) {
      model.setNsPrefix(entry.getKey(), entry.getValue().getURI());
    }
  }

  /**
   * Adds the content of the specified turtle file to the model
   *
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
   *
   * @param outputFile the desired output turtle file
   * @throws IOException if an exception occurred while writing the file
   */
  public void writeToFile(Path outputFile) throws IOException {
    Files.createDirectories(outputFile.toAbsolutePath().getParent());
    try (Writer w = Files.newBufferedWriter(outputFile, Charsets.UTF_8)) {
      model.write(w, "TTL");
    }
  }

  @Override
  public void handleOntology(
      EAPackage sourcePackage, Resource ontology, String prefix, String baseURI) {
    model.add(ontology, RDF.type, OWL.Ontology);
    model.add(
        ontology,
        model.createProperty("http://purl.org/vocab/vann/preferredNamespaceUri"),
        ontology.getURI());
    if (prefix != null) {
      model.setNsPrefix(prefix, baseURI);
      model.add(
          ontology,
          model.createProperty("http://purl.org/vocab/vann/preferredNamespacePrefix"),
          prefix);
    }

    for (TagData tag : tagHelper.getTagDataFor(sourcePackage, tagHelper.getOntologyMappings()))
      model.add(ontology, tag.getProperty(), tag.getValue());
  }

  @Override
  public void handleClass(
      EAElement sourceElement,
      Resource clazz,
      Scope scope,
      Resource ontology,
      List<Resource> parentClasses,
      List<EAElement> parentElements,
      Map<EAElement, String> elementURIs,
      List<Resource> allowedValues) {
    if (!forceFullOutput && scope == Scope.NOTHING) return;

    if (scope == Scope.FULL_DEFINITON) model.add(clazz, RDFS.isDefinedBy, ontology);

    if (forceFullOutput || scope == Scope.FULL_DEFINITON) {
      model.add(clazz, RDF.type, OWL.Class);
      for (Resource parent : parentClasses) model.add(clazz, RDFS.subClassOf, parent);
      if (allowedValues != null)
        model.add(clazz, OWL.oneOf, model.createList(allowedValues.iterator()));
    }

    if (scope == Scope.TRANSLATIONS_ONLY && !tagHelper.getContentMappings(scope).isEmpty())
      model.add(clazz, RDF.type, RDFS.Class);

    for (TagData tag :
        tagHelper.getTagDataFor(sourceElement, tagHelper.getContentMappings(scope))) {
      model.add(clazz, tag.getProperty(), tag.getValue());
    }
  }

  @Override
  public void handleProperty(
      PropertySource source,
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
      List<Resource> superProperties) {
    if (!forceFullOutput && scope == Scope.NOTHING) return;

    if (packageExported == PackageExported.UNKNOWN) {
      // Can only occur for connectors
      LOGGER.warn(
          "Ignoring connector \"{}\" since it lacks a defining package.",
          source.connector.getPath());
      return;
    }

    if (scope == Scope.FULL_DEFINITON) model.add(property, RDFS.isDefinedBy, ontology);

    if (forceFullOutput || scope == Scope.FULL_DEFINITON) {
      model.add(property, RDF.type, propertyType);

      if (domain != null) model.add(property, RDFS.domain, domain);
      if (range != null) model.add(property, RDFS.range, range);
      for (Resource superProperty : superProperties)
        model.add(property, RDFS.subPropertyOf, superProperty);
    }

    if (scope == Scope.TRANSLATIONS_ONLY && !tagHelper.getContentMappings(scope).isEmpty())
      model.add(property, RDF.type, RDF.Property);

    for (TagData tag :
        tagHelper.getTagDataFor(
            MoreObjects.firstNonNull(source.attribute, source.connector),
            tagHelper.getContentMappings(scope))) {
      model.add(property, tag.getProperty(), tag.getValue());
    }
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
      List<Resource> superProperties) {
    LOGGER.warn("HANDLEPROPERTYCONNECTOR NOT HANDLED: {} - {}", source.getName(), source.getGuid());
  }

  @Override
  public void handleInstance(
      EAAttribute source, Resource instance, Scope scope, Resource ontology, Resource clazz) {
    if (!forceFullOutput && scope == Scope.NOTHING) return;

    if (scope == Scope.FULL_DEFINITON) model.add(instance, RDFS.isDefinedBy, ontology);

    if (forceFullOutput || scope == Scope.FULL_DEFINITON) {
      model.add(instance, RDF.type, clazz);
    }

    for (TagData tag : tagHelper.getTagDataFor(source, tagHelper.getContentMappings(scope))) {
      model.add(instance, tag.getProperty(), tag.getValue());
    }
  }
}

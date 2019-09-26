package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.Tag;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;

/**
 * Configuration class for the converter.
 *
 * <p>The main purpose is to have a flexible way to determine which tags map to which RDF terms.
 *
 * @author Dieter De Paepe
 */
public class Configuration {
  private Map<String, Resource> prefixes;
  private Map<Tag, String> builtinTags;
  private List<Mapping> ontologyMappings;
  private List<Mapping> internalMappings;
  private List<Mapping> externalMappings;

  public Configuration(
      Map<String, Resource> prefixes,
      Map<Tag, String> builtinTags,
      List<Mapping> ontologyMappings,
      List<Mapping> internalMappings,
      List<Mapping> externalMappings) {
    this.prefixes = prefixes;
    this.builtinTags = builtinTags;
    this.ontologyMappings = ontologyMappings;
    this.internalMappings = internalMappings;
    this.externalMappings = externalMappings;
  }

  /**
   * Gets the customised tag names for the tags used to construct the RDF graph.
   *
   * @return never {@code null}
   */
  public Map<Tag, String> getBuiltinTags() {
    if (builtinTags == null) return Collections.emptyMap();
    return Collections.unmodifiableMap(builtinTags);
  }

  /**
   * Gets the mapping of prefixes to RDF namespaces that was defined in the configuration.
   *
   * @return never {@code null}
   */
  public Map<String, Resource> getPrefixes() {
    if (prefixes == null) return Collections.emptyMap();
    return Collections.unmodifiableMap(prefixes);
  }

  /**
   * Gets the mappings intended for the ontologies.
   *
   * @return never {@code null}
   */
  public List<Mapping> getOntologyMappings() {
    if (ontologyMappings == null) return Collections.emptyList();
    return Collections.unmodifiableList(ontologyMappings);
  }

  /**
   * Gets the mappings intended for terms that were defined by the user.
   *
   * @return never {@code null}
   */
  public List<Mapping> getInternalMappings() {
    if (internalMappings == null) return Collections.emptyList();
    return Collections.unmodifiableList(internalMappings);
  }

  /**
   * Gets the mappings intended for existing, external terms.
   *
   * @return never {@code null}
   */
  public List<Mapping> getExternalMappings() {
    if (externalMappings == null) return Collections.emptyList();
    return Collections.unmodifiableList(externalMappings);
  }
}

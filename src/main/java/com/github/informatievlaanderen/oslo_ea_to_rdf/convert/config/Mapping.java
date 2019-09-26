package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config;

import java.util.List;
import org.apache.jena.rdf.model.Property;

/**
 * Description of how a single Enterprise Architect tag should be mapped to a RDF term.
 *
 * @author Dieter De Paepe
 */
public class Mapping {
  private String tag;
  private List<String> fallbackTags;
  private Property property;
  private boolean mandatory;
  private String type;
  private String lang;

  public Mapping(
      String tag,
      List<String> fallbackTags,
      Property property,
      boolean mandatory,
      String type,
      String lang) {
    this.tag = tag;
    this.fallbackTags = fallbackTags;
    this.property = property;
    this.mandatory = mandatory;
    this.type = type;
    this.lang = lang;
  }

  /** Gets the name of the tag this mapping refers to. */
  public String getTag() {
    return tag;
  }

  /** Gets the name of the tags to try in case the main tag is not present. */
  public List<String> getFallbackTags() {
    return fallbackTags;
  }

  /** Gets the RDF property which should hold the value for this mapping. */
  public Property getProperty() {
    return property;
  }

  /**
   * Returns whether the absence of the specified tag should cause a warning or error.
   *
   * @return {@code false} if the tag should not cause a warning or error
   */
  public boolean isMandatory() {
    return mandatory;
  }

  /**
   * Returns the RDF datatype associated with this term.
   *
   * @return may be {@code null}
   */
  public String getType() {
    return type;
  }

  /**
   * The language of the value associated with the tag.
   *
   * @return may be {@code null}
   */
  public String getLang() {
    return lang;
  }
}

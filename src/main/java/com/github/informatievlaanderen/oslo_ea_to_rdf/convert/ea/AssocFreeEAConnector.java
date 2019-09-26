package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A association connector created for the AssociationClass connected with the assocation <inner>
 */
public class AssocFreeEAConnector implements EAConnector {
  private EAConnector inner;
  private String derivedURI;

  private final Logger LOGGER = LoggerFactory.getLogger(RoleEAConnector.class);

  /**
   * Creates a new connector to represent one of four parts of a connector with an association
   * class.
   *
   * @param inner the base connector
   * @param part the part of the base connector to be represented by this connector
   */
  public AssocFreeEAConnector(EAConnector inner) {

    this.inner = inner;
  }

  @Override
  public String getName() {
    return inner.getName();
  }

  @Override
  public Direction getDirection() {
    return inner.getDirection();
  }

  @Override
  public String getNotes() {
    return inner.getNotes();
  }

  @Override
  public String getType() {
    return inner.getType();
  }

  @Override
  public String getSourceRole() {
    return inner.getSourceRole();
  }

  @Override
  public List<EATag> getSourceRoleTags() {
    return inner.getSourceRoleTags();
  }

  @Override
  public String getDestRole() {
    return inner.getDestRole();
  }

  @Override
  public List<EATag> getDestRoleTags() {
    return inner.getSourceRoleTags();
  }

  @Override
  public EAElement getSource() {
    return inner.getSource();
  }

  @Override
  public EAElement getDestination() {
    return inner.getDestination();
  }

  @Override
  public EAElement getAssociationClass() {
    // Always null
    return null;
  }

  @Override
  public List<EATag> getTags() {
    return inner.getTags();
  }

  @Override
  public String getPath() {
    return inner.getPath();
  }

  public String getGuid() {
    return inner.getGuid();
  }

  @Override
  public String getSourceCardinality() {
    return inner.getSourceCardinality();
  }

  @Override
  public String getDestinationCardinality() {
    return inner.getDestinationCardinality();
  }

  /**
   * Gets the derived URI of the element.
   *
   * @return a string, or {@code null} if not set
   */
  public String getDerivedURI() {
    return derivedURI;
  }

  /** Sets the derived URI of the element. */
  void setDerivedURI(String uri) {
    derivedURI = uri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssocFreeEAConnector that = (AssocFreeEAConnector) o;
    return Objects.equals(inner, that.inner);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inner);
  }
}

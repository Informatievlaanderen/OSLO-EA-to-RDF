package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import java.util.List;

/**
 * A link between two {@link EAElement}s, containing additional information.
 *
 * @author Dieter De Paepe
 */
public interface EAConnector extends EAObject {
  String TYPE_AGGREGATION = "Aggregation";
  String TYPE_ASSOCIATION = "Association";
  String TYPE_GENERALIZATION = "Generalization";

  /**
   * Gets the direction defined for this connector.
   *
   * @return the direction
   */
  Direction getDirection();

  /**
   * Gets the typing of this connector.
   *
   * <p>See also the static fields in {@link EAConnector} for some typical values.
   *
   * @return the type
   */
  String getType();

  /**
   * Gets the role on the source of this connector.
   *
   * @return the role, or {@code null}
   */
  String getSourceRole();

  /** Gets the tags linked to this object. */
  List<EATag> getSourceRoleTags();

  /**
   * Gets the role on the destination of this connector.
   *
   * @return the role, or {@code null}
   */
  String getDestRole();

  /** Gets the tags linked to this object. */
  List<EATag> getDestRoleTags();

  /**
   * Gets the source of this connector.
   *
   * @return the source
   */
  EAElement getSource();

  /**
   * Gets the destination (end) of this connector.
   *
   * @return the destination
   */
  EAElement getDestination();

  /**
   * Gets the element associated with this connector.
   *
   * @return the element, or {@code null}
   */
  EAElement getAssociationClass();

  /**
   * Gets the cardinality specified associated with the source.
   *
   * @return a string, or {@code null}
   */
  String getSourceCardinality();

  /**
   * Gets the cardinality specified associated with the target.
   *
   * @return a string, or {@code null}
   */
  String getDestinationCardinality();

  enum Direction {
    UNSPECIFIED,
    SOURCE_TO_DEST,
    BIDIRECTIONAL,
    DEST_TO_SOURCE;

    public static Direction parse(String eaString) {
      switch (eaString) {
        case "Source -> Destination":
          return SOURCE_TO_DEST;
        case "Destination -> Source":
          return DEST_TO_SOURCE;
        case "Bi-Directional":
          return BIDIRECTIONAL;
        case "Unspecified":
          return UNSPECIFIED;
        default:
          throw new IllegalArgumentException(
              "Could not parse \"" + eaString + "\" as a connector direction");
      }
    }
  }
}

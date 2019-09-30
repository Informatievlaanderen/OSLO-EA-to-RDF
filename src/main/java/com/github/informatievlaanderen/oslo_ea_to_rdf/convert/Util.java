package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.AssocFreeEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.NormalizedEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.RoleEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility method container class.
 *
 * @author Dieter De Paepe
 */
public class Util {
  private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

  private Util() {}

  /**
   * Converts a connector either without directions, either connected to an association class class.
   *
   * @param conn connector that may have an association class
   * @param direction direction of the connector, determines which end is source and destination for
   *     extracting the tags from a connector with an association class
   * @return a collections of connectors
   */

  /**
   * Algorithm: 1. detect if the connector is specified according to the deprecated method for
   * association classes. if so, apply the deprecated interpretation 2. handle the connector: a. add
   * the connector as such b. if a role is non-empty, add the corresponding role as association c.
   * if the connector has an undefined direction, add roles for both sides d. add the connectors for
   * to the eventual existing association class
   */
  public static Collection<EAConnector> extractAssociationElement2(
      EAConnector conn, EAConnector.Direction direction, TagHelper tagHelper) {
    Collection<EAConnector> result = new ArrayList<>();

    //        if ( conn.getAssociationClass() != null) {
    if (connectorHasOldAssociationClassTags(conn)) {
      // handling association classes has priority
      LOGGER.debug("0) add connectors based on deprecated tags for {}", conn.getPath());
      result = extractAssociationElement(conn, direction);
    } else {
      if ((conn.getDestRole() == null || conn.getDestRole() == "")
          && (conn.getSourceRole() == null || conn.getSourceRole() == "")
          && (direction != EAConnector.Direction.UNSPECIFIED)) {

        LOGGER.debug("0) add AssocFree connector {}", conn.getPath());
        result.add(new AssocFreeEAConnector(conn));
      }
      ;
      if (conn.getSourceRole() != null && conn.getSourceRole() != "") {
        result.add(
            new RoleEAConnector(conn, RoleEAConnector.ConnectionPart.DEST_TO_SOURCE, tagHelper));
        LOGGER.debug("1) add Role connector {}", conn.getPath());
      }
      ;
      if (conn.getDestRole() != null && conn.getDestRole() != "") {
        result.add(
            new RoleEAConnector(conn, RoleEAConnector.ConnectionPart.SOURCE_TO_DEST, tagHelper));
        LOGGER.debug("2) add Role connector {}", conn.getPath());
      }
      ;
      if ((conn.getDestRole() == null || conn.getDestRole() == "")
          && (conn.getSourceRole() == null || conn.getSourceRole() == "")
          && (direction == EAConnector.Direction.UNSPECIFIED)) {
        LOGGER.debug("3) add Role connector {}", conn.getPath());
        result.add(
            new RoleEAConnector(
                conn, RoleEAConnector.ConnectionPart.UNSPEC_SOURCE_TO_DEST, tagHelper));
        result.add(
            new RoleEAConnector(
                conn, RoleEAConnector.ConnectionPart.UNSPEC_DEST_TO_SOURCE, tagHelper));
      }
      ;
      // outcommented because not anymore used
      //      result = handleAssociationElement(conn, result);
    }
    ;
    return result;
  }

  public static Collection<EAConnector> extractAssociationElement3(
      EAConnector conn, EAConnector.Direction direction, TagHelper tagHelper) {
    LOGGER.debug("add connector {}", conn.getPath());
    Collection<EAConnector> result = new ArrayList<>(3);
    result.add(new RoleEAConnector(conn, RoleEAConnector.ConnectionPart.SOURCE_TO_DEST, tagHelper));
    result.add(conn);
    return result;
  };

  /**
   * Converts a connector in one or four connectors so that the resulting connectors do not have any
   * association class.
   *
   * @param conn connector that may have an association class
   * @param direction direction of the connector, determines which end is source and destination for
   *     extracting the tags from a connector with an association class
   * @return four connectors that do not have an association class, or the original connector
   */
  public static Collection<EAConnector> extractAssociationElement(
      EAConnector conn, EAConnector.Direction direction) {
    if (conn.getAssociationClass() == null) return Collections.singleton(conn);

    List<String> prefixes =
        Arrays.asList(
            Tag.ASSOCIATION_SOURCE_PREFIX, Tag.ASSOCIATION_SOURCE_REV_PREFIX,
            Tag.ASSOCIATION_DEST_PREFIX, Tag.ASSOCIATION_DEST_REV_PREFIX);
    List<NormalizedEAConnector.ConnectionPart> parts =
        Arrays.asList(
            NormalizedEAConnector.ConnectionPart.SOURCE_TO_ASSOCIATION,
            NormalizedEAConnector.ConnectionPart.ASSOCIATION_TO_SOURCE,
            NormalizedEAConnector.ConnectionPart.ASSOCIATION_TO_DESTINATION,
            NormalizedEAConnector.ConnectionPart.DESTINATION_TO_ASSOCIATION);

    if (direction == EAConnector.Direction.DEST_TO_SOURCE) {
      Collections.reverse(parts);
    }

    Collection<EAConnector> result = new ArrayList<>(4);
    for (int i = 0; i < 4; i++) {
      result.add(new NormalizedEAConnector(conn, parts.get(i), prefixes.get(i)));
    }

    return result;
  }

  public static Boolean connectorHasOldAssociationClassTags(EAConnector conn) {
    Boolean bool = false;
    if (conn.getAssociationClass() != null) {

      for (EATag tag : conn.getTags()) {
        if (tag.getKey().startsWith(Tag.ASSOCIATION_SOURCE_PREFIX)) bool = true;
        if (tag.getKey().startsWith(Tag.ASSOCIATION_SOURCE_REV_PREFIX)) bool = true;
        if (tag.getKey().startsWith(Tag.ASSOCIATION_DEST_PREFIX)) bool = true;
        if (tag.getKey().startsWith(Tag.ASSOCIATION_DEST_REV_PREFIX)) bool = true;
      }
    }
    ;
    return bool;
  }

  /*
   * correct interpretation with roles
   */
  /* outcommented because not anymore used
    public static Collection<EAConnector> handleAssociationElement(
        EAConnector conn, Collection<EAConnector> result) {
      if (conn.getAssociationClass() == null) return result;

      EAElement assocClass = conn.getAssociationClass();
      LOGGER.debug("5) add AssocationClass connectors {}", assocClass.getName());

      // Hier is de issue voor de naam: dat is de localName van de klasse
      result.add(
          new AssociationEAConnector(
              conn,
              assocClass,
              conn.getDestination(),
              conn.getDestination().getName(),
              conn.getDestinationCardinality(),
              "1"));
      result.add(
          new AssociationEAConnector(
              conn,
              assocClass,
              conn.getSource(),
              conn.getSource().getName(),
              conn.getSourceCardinality(),
              "1"));

      return result;
    }
  */
}

package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.NormalizedEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.MoreObjects;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Utility method container class.
 *
 * @author Dieter De Paepe
 */
public class Util {
    private Util() {}

    public static String getFullName(EAObject object) {
        if (object instanceof EAPackage)
            return getFullName((EAPackage) object);
        else if (object instanceof EAElement)
            return getFullName((EAElement) object);
        else if (object instanceof EAConnector)
            return getFullName((EAConnector) object);
        else if (object instanceof EAAttribute)
            return getFullName((EAAttribute) object);
        else
            throw new IllegalArgumentException("Unsupported class passed: " + object.getClass().getName());
    }

    public static String getFullName(EAPackage pack) {
        String name = pack.getName();
        while (pack.getParent() != null) {
            pack = pack.getParent();
            name = pack.getName() + "." + name;
        }
        return name;
    }

    public static String getFullName(EAElement element) {
        return getFullName(element.getPackage()) + ":" + element.getName();
    }

    public static String getFullName(EAAttribute attribute) {
        return getFullName(attribute.getElement()) + ":" + attribute.getName();
    }

    public static String getFullName(EAConnector connector) {
        return getFullName(connector.getSource())
                + ":" + MoreObjects.firstNonNull(
                        connector.getName(),
                        "(" + connector.getSource().getName() + " -> " + connector.getDestination().getName() + ")");
    }

    /**
     * Converts a connector in one or two connectors so that the resulting connectors do not have any association
     * class.
     * @param conn connector that may have an association class
     * @param direction direction of the connector, determines which end is source and destination for extracting the
     *                  tags from a connector with an association class
     * @param helper tag helper instance
     * @return two connectors that do not have an association class, or the original connector
     */
    public static Collection<EAConnector> extractAssociationElement(EAConnector conn, EAConnector.Direction direction, TagHelper helper) {
        if (conn.getAssociationClass() == null)
            return Collections.singleton(conn);

        String associationDirection = helper.getOptionalTag(conn, Tag.ASSOCIATION, "follow");
        if (!Arrays.asList("follow", "in", "out").contains(associationDirection)) {
            associationDirection = "follow";
        }

        String tagPrefix1 = Tag.ASSOCIATION_SOURCE_PREFIX;
        String tagPrefix2 = Tag.ASSOCIATION_DEST_PREFIX;

        if (direction == EAConnector.Direction.DEST_TO_SOURCE) {
            tagPrefix1 = Tag.ASSOCIATION_DEST_PREFIX;
            tagPrefix2 = Tag.ASSOCIATION_SOURCE_PREFIX;
        }

        if ("follow".equals(associationDirection))
            if (direction == EAConnector.Direction.SOURCE_TO_DEST) {
                return Arrays.asList(
                        new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.SOURCE_TO_ASSOCIATION, tagPrefix1),
                        new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.ASSOCIATION_TO_DESTINATION, tagPrefix2)
                );
            }
            if (direction == EAConnector.Direction.DEST_TO_SOURCE) {
                return Arrays.asList(
                        new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.ASSOCIATION_TO_SOURCE, tagPrefix1),
                        new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.DESTINATION_TO_ASSOCIATION, tagPrefix2)
                );
            }
        if ("in".equals(associationDirection))
            return Arrays.asList(
                new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.SOURCE_TO_ASSOCIATION, tagPrefix1),
                new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.DESTINATION_TO_ASSOCIATION, tagPrefix2)
            );
        if ("out".equals(associationDirection))
            return Arrays.asList(
                new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.ASSOCIATION_TO_SOURCE, tagPrefix1),
                new NormalizedEAConnector(conn, NormalizedEAConnector.ConnectionPart.ASSOCIATION_TO_DESTINATION, tagPrefix2)
            );

        return Collections.emptyList();
    }
}

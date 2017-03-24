package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.NormalizedEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.MoreObjects;

import java.util.*;

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
     * Converts a connector in one or four connectors so that the resulting connectors do not have any association
     * class.
     * @param conn connector that may have an association class
     * @param direction direction of the connector, determines which end is source and destination for extracting the
     *                  tags from a connector with an association class
     * @return four connectors that do not have an association class, or the original connector
     */
    public static Collection<EAConnector> extractAssociationElement(EAConnector conn, EAConnector.Direction direction) {
        if (conn.getAssociationClass() == null)
            return Collections.singleton(conn);

        List<String> prefixes = Arrays.asList(
                Tag.ASSOCIATION_SOURCE_PREFIX, Tag.ASSOCIATION_SOURCE_REV_PREFIX,
                Tag.ASSOCIATION_DEST_PREFIX, Tag.ASSOCIATION_DEST_REV_PREFIX);
        List<NormalizedEAConnector.ConnectionPart> parts = Arrays.asList(
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
}

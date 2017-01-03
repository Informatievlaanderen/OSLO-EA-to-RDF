package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import com.google.common.collect.ListMultimap;

/**
 * A link between two {@link EAElement}s, containing additional information.
 *
 * @author Dieter De Paepe
 */
public interface EAConnector {
    public static final String TYPE_AGGREGATION = "Aggregation";
    public static final String TYPE_ASSOCIATION = "Association";
    public static final String TYPE_GENERALIZATION = "Generalization";

    /**
     * Gets the name for this connector.
     * @return the name, or {@code null}
     */
    String getName();

    /**
     * Gets the direction defined for this connector.
     * @return the direction
     */
    Direction getDirection();

    /**
     * Gets the notes for this connector.
     * @return the notes, or {@code null}
     */
    String getNotes();

    /**
     * Gets the typing of this connector.
     *
     * See also the static fields in {@link EAConnector} for some typical values.
     * @return the type
     */
    String getType();

    /**
     * Gets the role on the source of this connector.
     * @return the role, or {@code null}
     */
    String getSourceRole();

    /**
     * Gets the role on the destination of this connector.
     * @return the role, or {@code null}
     */
    String getDestRole();

    /**
     * Gets the source of this connector.
     * @return the source
     */
    EAElement getSource();

    /**
     * Gets the destination (end) of this connector.
     * @return the destination
     */
    EAElement getDestination();

    /**
     * Gets the element associated with this connector.
     * @return the element, or {@code null}
     */
    EAElement getAssociationClass();

    /**
     * Gets the GUID for this connector.
     * @return the GUID
     */
    String getGuid();

    /**
     * Gets the tags for this connector.
     * @return an unmodifiable multimap
     */
    ListMultimap<String, String> getTags();


    enum Direction {
        UNSPECIFIED,
        SOURCE_TO_DEST,
        BIDIRECTIONAL,
        DEST_TO_SOURCE;

        public static Direction parse(String eaString) {
            switch (eaString) {
                case "Source -> Destination": return SOURCE_TO_DEST;
                case "Destination -> Source": return DEST_TO_SOURCE;
                case "Bi-Directional": return BIDIRECTIONAL;
                case "Unspecified": return UNSPECIFIED;
                default: throw new IllegalArgumentException("Could not parse \"" + eaString + "\" as a connector direction");
            }
        }
    }
}

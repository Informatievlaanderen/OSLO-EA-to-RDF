package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

/**
 * A {@link EAConnector} as visualised in a {@link EADiagram}.
 *
 * Different diagrams can visualise the same connector in different ways, hence the need for specific data containers.
 *
 * @author Dieter De Paepe
 */
public interface DiagramConnector {
    /**
     * Gets the definition of the connector displayed.
     * @return the connector
     */
    EAConnector getReferencedConnector();

    /**
     * Gets the diagram specific source element of this connector.
     * @return the diagram specific source
     */
    DiagramElement getSource();

    /**
     * Gets the diagram specific destination of this connector.
     * @return the diagram specific destination
     */
    DiagramElement getDestination();

    /**
     * Gets the diagram specific element associated with this connector.
     * @return the element, or {@code null}
     */
    DiagramElement getAssociationElement();

    /**
     * Gets the direction of the arrow of the label for this connector.
     * @return a direction
     */
    EAConnector.Direction getLabelDirection();

    /**
     * Returns whether this connector is hidden.
     * @return a boolean
     */
    boolean isHidden();
}

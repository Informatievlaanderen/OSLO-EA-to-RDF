package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import javafx.geometry.Rectangle2D;

import java.util.List;

/**
 * An {@link EAElement} as visualised on a {@link EADiagram}.
 *
 * Diagrams can visualise the same elements in different ways, hence the need for containers of this data.
 *
 * @author Dieter De Paepe
 */
public interface DiagramElement {
    /**
     * Gets the definition of the element displayed on the diagram.
     * @return an element
     */
    EAElement getReferencedElement();

    /**
     * Gets the diagram in which this object is stored.
     * @return the diagram
     */
    EADiagram getDiagram();

    /**
     * Gets the boundaries in which the element is displayed in the diagram.
     * @return the boundaries
     */
    Rectangle2D getBoundaries();

    /**
     * Gets the diagram specific links for this element.
     * @return an immutable list
     */
    List<DiagramConnector> getConnectors();
}

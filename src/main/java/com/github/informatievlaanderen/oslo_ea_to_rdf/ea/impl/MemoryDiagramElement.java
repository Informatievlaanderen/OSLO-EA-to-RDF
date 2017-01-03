package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.DiagramConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.DiagramElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EADiagram;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import javafx.geometry.Rectangle2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dieter De Paepe
 */
class MemoryDiagramElement implements DiagramElement {
    private EADiagram diagram;
    private EAElement innerElement;
    private List<DiagramConnector> connectors;
    private Rectangle2D rect;

    MemoryDiagramElement(EADiagram diagram, EAElement element, Rectangle2D rect) {
        this.diagram = diagram;
        this.innerElement = element;
        this.connectors = new ArrayList<>();
        this.rect = rect;
    }

    @Override
    public EAElement getReferencedElement() {
        return innerElement;
    }

    @Override
    public EADiagram getDiagram() {
        return diagram;
    }

    @Override
    public Rectangle2D getBoundaries() {
        return rect;
    }

    @Override
    public List<DiagramConnector> getConnectors() {
        return Collections.unmodifiableList(connectors);
    }

    List<DiagramConnector> getConnectorsOrig() {
        return connectors;
    }
}

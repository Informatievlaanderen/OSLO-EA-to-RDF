package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.DiagramConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.DiagramElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;

/** @author Dieter De Paepe */
class MemoryDiagramConnector implements DiagramConnector {
  private EAConnector.Direction labelDirection;
  private boolean hidden;
  private EAConnector innerConnector;
  private DiagramElement source;
  private DiagramElement target;
  private DiagramElement associationClass;

  MemoryDiagramConnector(
      EAConnector.Direction labelDirection,
      boolean hidden,
      EAConnector innerConnector,
      DiagramElement source,
      DiagramElement target,
      DiagramElement associationClass) {
    this.labelDirection = labelDirection;
    this.hidden = hidden;
    this.innerConnector = innerConnector;
    this.source = source;
    this.target = target;
    this.associationClass = associationClass;
  }

  @Override
  public EAConnector getReferencedConnector() {
    return innerConnector;
  }

  @Override
  public DiagramElement getSource() {
    return source;
  }

  @Override
  public DiagramElement getDestination() {
    return target;
  }

  @Override
  public DiagramElement getAssociationElement() {
    return associationClass;
  }

  @Override
  public EAConnector.Direction getLabelDirection() {
    return labelDirection;
  }

  @Override
  public boolean isHidden() {
    return hidden;
  }
}

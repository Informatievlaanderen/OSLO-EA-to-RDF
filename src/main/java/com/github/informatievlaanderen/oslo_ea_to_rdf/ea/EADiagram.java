package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import java.util.List;

/**
 * A class diagram in an Enterprise Architect project. A diagram contains a subset of the elements
 * declared in the project and has specific layout information.
 *
 * @author Dieter De Paepe
 */
public interface EADiagram {
  /**
   * Returns the name of this diagram.
   *
   * @return the name
   */
  String getName();

  /**
   * Returns the GUID of this diagram (even though it may not be visible in EA).
   *
   * @return the guid
   */
  String getGuid();

  /**
   * Returns the notes of this diagram.
   *
   * @return the notes, or {@code null}
   */
  String getNotes();

  /**
   * Returns the package in which this diagram is contained.
   *
   * @return the package
   */
  EAPackage getPackage();

  /**
   * Returns the elements that are visualised in this diagram.
   *
   * @return an immutable list of the elements
   */
  List<? extends DiagramElement> getElements();
}

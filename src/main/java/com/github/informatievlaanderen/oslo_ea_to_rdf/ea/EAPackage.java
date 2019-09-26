package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import java.util.List;

/**
 * A package in an Enterprise Architect project. Packages are used to structure the project in a
 * tree structure. A package bundles {@link EAElement}s, {@link EADiagram}s and other {@code
 * EAPackage}s.
 *
 * @author Dieter De Paepe
 */
public interface EAPackage extends EAObject {
  /**
   * Gets the primary stereotype of this package
   *
   * @return the stereotype, or {@code null}
   */
  String getStereoType();

  /**
   * Gets the packages contained in this package.
   *
   * @return an immutable list
   */
  List<? extends EAPackage> getPackages();

  /**
   * Gets the package in which this package is contained.
   *
   * @return {@code null} if this is the root package
   */
  EAPackage getParent();

  /**
   * Gets the elements contained in this package.
   *
   * @return an immutable list
   */
  List<? extends EAElement> getElements();

  /**
   * Gets the diagrams contained in this package.
   *
   * @return an immutable list
   */
  List<? extends EADiagram> getDiagrams();
}

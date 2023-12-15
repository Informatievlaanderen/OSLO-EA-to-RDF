package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import com.google.gson.annotations.Expose;
import java.util.List;

/**
 * A container class representing the content of an Enterprise Architect project.
 *
 * @author Dieter De Paepe
 */
public class EARepository {
  @Expose private EAPackage rootPackage;
  @Expose private List<EAPackage> packages;
  @Expose private List<EAElement> elements;
  @Expose private List<EADiagram> diagrams;

  public EARepository(
      EAPackage rootPackage,
      List<EAPackage> packages,
      List<EAElement> elements,
      List<EADiagram> diagrams) {
    this.rootPackage = rootPackage;
    this.packages = packages;
    this.elements = elements;
    this.diagrams = diagrams;
  }

  public EAPackage getRootPackage() {
    return rootPackage;
  }

  public List<EAPackage> getPackages() {
    return packages;
  }

  public List<EAElement> getElements() {
    return elements;
  }

  public List<EADiagram> getDiagrams() {
    return diagrams;
  }
}

package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.PackageExported;
import java.util.ArrayList;
import java.util.List;

/** Created by langens-jonathan on 11/5/18. */
public class ClassDescription implements Comparable<ClassDescription> {
  private String uri;
  private String type;
  private String scopetags;
  private String extra;
  private PackageExported inpackage;
  private List<LanguageStringDescription> name;
  private List<LanguageStringDescription> description;
  private List<LanguageStringDescription> usage;
  private List<String> parents;

  public ClassDescription() {
    this.name = new ArrayList<>();
    this.description = new ArrayList<>();
    this.usage = new ArrayList<>();
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public PackageExported getInPackage() {
    return inpackage;
  }

  public void setInPackage(PackageExported inpackage) {
    this.inpackage = inpackage;
  }

  public List<LanguageStringDescription> getName() {
    return name;
  }

  public void setName(List<LanguageStringDescription> name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<LanguageStringDescription> getDescription() {
    return description;
  }

  public void setDescription(List<LanguageStringDescription> description) {
    this.description = description;
  }

  public List<LanguageStringDescription> getUsage() {
    return usage;
  }

  public void setUsage(List<LanguageStringDescription> usage) {
    this.usage = usage;
  }

  public String getExtra() {
    return extra;
  }

  public void setExtra(String extra) {
    this.extra = extra;
  }

  public String getScopetags() {
    return scopetags;
  }

  public void setScopetags(String scopetags) {
    this.scopetags = scopetags;
  }

  public List<String> getParents() {
    return parents;
  }

  public void setParents(List<String> parents) {
    this.parents = parents;
  }

  // Used for sorting in ascending order of
  @Override
  public int compareTo(ClassDescription b) {
    String avalue = this.uri;
    String bvalue = b.getUri();
    return avalue.compareTo(bvalue);
  }
}

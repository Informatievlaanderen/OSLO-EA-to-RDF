package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.PackageExported;
import java.util.ArrayList;
import java.util.List;

/** Created by langens-jonathan on 11/5/18. */
public class PropertyDescription implements Comparable<PropertyDescription> {
  private String uri;
  private String type;
  private String scopetags;
  private String extra;
  private PackageExported inpackage;
  private List<LanguageStringDescription> name;
  private List<LanguageStringDescription> description;
  private List<LanguageStringDescription> usage;
  private List<String> domain;
  private List<String> range;
  private List<String> generalization;
  private List<String> codelist;
  private String minCount;
  private String maxCount;

  public PropertyDescription() {
    this.name = new ArrayList<>();
    this.description = new ArrayList<>();
    this.usage = new ArrayList<>();
    this.domain = new ArrayList<>();
    this.range = new ArrayList<>();
    this.generalization = new ArrayList<>();
    this.codelist = new ArrayList<>();
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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

  public List<String> getDomain() {
    return domain;
  }

  public void setDomain(List<String> domain) {
    this.domain = domain;
  }

  public List<String> getCodelist() {
    return codelist;
  }

  public void setCodelist(List<String> codelist) {
    this.codelist = codelist;
  }

  public List<String> getGeneralization() {
    return generalization;
  }

  public void setGeneralization(List<String> generalization) {
    this.generalization = generalization;
  }

  public String getMinCount() {
    return minCount;
  }

  public void setMinCount(String minCount) {
    this.minCount = minCount;
  }

  public String getMaxCount() {
    return maxCount;
  }

  public void setMaxCount(String maxCount) {
    this.maxCount = maxCount;
  }

  public List<String> getRange() {
    return range;
  }

  public void setRange(List<String> range) {
    this.range = range;
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

  // Used for sorting in ascending order of
  @Override
  public int compareTo(PropertyDescription b)
      {
          String avalue = this.uri;
          String bvalue = b.getUri();
          return avalue.compareTo(bvalue);
      }
}

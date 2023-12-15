package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAAttribute;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** @author Dieter De Paepe */
class MemoryEAAttribute implements EAAttribute {
  private EAElement element;
  private String guid;
  private String name;
  private String notes;
  private String type;
  private String lowerBound;
  private String upperBound;
  private int attributeID;
  private List<EATag> tags;

  MemoryEAAttribute(
      EAElement element,
      String guid,
      String name,
      String notes,
      String type,
      int attributeID,
      String lowerBound,
      String upperBound) {
    this.element = element;
    this.guid = guid;
    this.name = name;
    this.notes = notes;
    this.type = type;
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.attributeID = attributeID;
    this.tags = new ArrayList<>();
  }

  @Override
  public EAElement getElement() {
    return element;
  }

  @Override
  public String getGuid() {
    return guid;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getNotes() {
    return notes;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getLowerBound() {
    return lowerBound;
  }

  @Override
  public String getUpperBound() {
    return upperBound;
  }

  @Override
  public List<EATag> getTags() {
    return Collections.unmodifiableList(tags);
  }

  @Override
  public String getPath() {
    return getElement().getPath() + ":" + getName();
  }

  List<EATag> getTagsOrig() {
    return tags;
  }

  int getAttributeID() {
    return attributeID;
  }
}

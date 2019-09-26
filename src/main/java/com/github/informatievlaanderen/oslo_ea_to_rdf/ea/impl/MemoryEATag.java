package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;

/** @author Dieter De Paepe */
public class MemoryEATag implements EATag {
  private String key;
  private String value;
  private String notes;

  public MemoryEATag(String key, String value, String notes) {
    this.key = key;
    this.value = value;
    this.notes = notes;
  }

  @Override
  public String getNotes() {
    return notes;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String getKey() {
    return key;
  }
}

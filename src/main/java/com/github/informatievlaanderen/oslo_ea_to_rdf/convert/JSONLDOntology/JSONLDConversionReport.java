package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

import java.util.ArrayList;
import java.util.List;

/** Created by langens-jonathan on 11/12/18. */
public class JSONLDConversionReport {
  private List<String> remarks;

  public JSONLDConversionReport() {
    this.remarks = new ArrayList<>();
  }

  public List<String> getRemarks() {
    return remarks;
  }

  public void setRemarks(List<String> remarks) {
    this.remarks = remarks;
  }

  public void addRemark(String remark) {
    this.remarks.add(remark);
  }
}

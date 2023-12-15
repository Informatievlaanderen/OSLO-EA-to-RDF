package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

import com.google.gson.annotations.SerializedName;

/** Created by langens-jonathan on 11/16/18. */
public class ThemaConfiguration {
  @SerializedName("name")
  private String name;

  @SerializedName("type")
  private String type;

  @SerializedName("eap")
  private String eap;

  @SerializedName("diagram")
  private String diagram;

  @SerializedName("contributors-column")
  private String contributorsColumn;

  @SerializedName("template")
  private String template;

  @SerializedName("title")
  private String title;

  @SerializedName("github")
  private String github;

  @SerializedName("branch")
  private String branch;

  @SerializedName("config")
  private String config;

  @SerializedName("publication-state")
  private String publicationState;

  @SerializedName("publication-date")
  private String publicationDate;

  @SerializedName("previous-state")
  private String previousState;

  @SerializedName("next-version")
  private String nextVersion;

  @SerializedName("contributors-file")
  private String contributorsFile;

  public String getContributorsFile() {
    return contributorsFile;
  }

  public void setContributorsFile(String contributorsFile) {
    this.contributorsFile = contributorsFile;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getEap() {
    return eap;
  }

  public void setEap(String eap) {
    this.eap = eap;
  }

  public String getDiagram() {
    return diagram;
  }

  public void setDiagram(String diagram) {
    this.diagram = diagram;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String getContributorsColumn() {
    return contributorsColumn;
  }

  public void setContributorsColumn(String contributorsColumn) {
    this.contributorsColumn = contributorsColumn;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getGithub() {
    return github;
  }

  public void setGithub(String github) {
    this.github = github;
  }

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  public String getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(String publicationDate) {
    this.publicationDate = publicationDate;
  }

  public String getPublicationState() {
    return publicationState;
  }

  public void setPublicationState(String publicationState) {
    this.publicationState = publicationState;
  }

  public String getPreviousState() {
    return previousState;
  }

  public void setPreviousState(String previousState) {
    this.previousState = previousState;
  }

  public String getNextVersion() {
    return nextVersion;
  }

  public void setNextVersion(String nextVersion) {
    this.nextVersion = nextVersion;
  }
}

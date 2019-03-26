package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

/** Created by langens-jonathan on 11/11/18. */
public class ContributorDescription {
  private String firstName;
  private String lastName;
  private String affiliation;
  private String email;
  private String website;

  public ContributorDescription() {}

  public ContributorDescription(
      String firstName, String lastName, String affiliation, String email, String website) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.affiliation = affiliation;
    this.website = website;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getAffiliation() {
    return affiliation;
  }

  public void setAffiliation(String affiliation) {
    this.affiliation = affiliation;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}

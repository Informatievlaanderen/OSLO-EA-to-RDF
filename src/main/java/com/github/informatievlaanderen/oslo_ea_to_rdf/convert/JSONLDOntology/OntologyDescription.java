package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by langens-jonathan on 11/5/18.
 */
public class OntologyDescription {
    private String uri;
    private String type;
    private String label;
    private List<ClassDescription> classes;
    private List<PropertyDescription> properties;
    private List<DatatypeDescription> datatypes;
    private List<ContributorDescription> editors;
    private List<ContributorDescription> authors;
    private List<ContributorDescription> contributors;

    public OntologyDescription() {
        this.classes = new ArrayList<>();
        this.properties = new ArrayList<>();
        this.datatypes = new ArrayList<>();
        this.editors = new ArrayList<>();
        this.authors = new ArrayList<>();
        this.contributors = new ArrayList<>();
    }

    public List<ContributorDescription> getEditors() {
        return editors;
    }

    public void setEditors(List<ContributorDescription> editors) {
        this.editors = editors;
    }

    public List<ContributorDescription> getAuthors() {
        return authors;
    }

    public void setAuthors(List<ContributorDescription> authors) {
        this.authors = authors;
    }

    public List<ContributorDescription> getContributors() {
        return contributors;
    }

    public void setContributors(List<ContributorDescription> contributors) {
        this.contributors = contributors;
    }

    public String getUri() {
        return uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public List<ClassDescription> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassDescription> classes) {
        this.classes = classes;
    }

    public List<PropertyDescription> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyDescription> properties) {
        this.properties = properties;
    }

    public List<DatatypeDescription> getDatatypes() {
        return datatypes;
    }

    public void setDatatypes(List<DatatypeDescription> datatypes) {
        this.datatypes = datatypes;
    }
}

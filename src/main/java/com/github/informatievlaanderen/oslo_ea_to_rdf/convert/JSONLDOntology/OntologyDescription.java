package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by langens-jonathan on 11/5/18.
 */
public class OntologyDescription {
    private String uri;
    private String type;
    private String label;
    private String scopetags;
    private String extra;
    private List<ClassDescription> classes;
    private List<PropertyDescription> properties;
    private List<DatatypeDescription> datatypes;
    private List<ContributorDescription> editors;
    private List<ContributorDescription> authors;
    private List<ContributorDescription> contributors;
    private Set<String> externals;
    private List<ClassDescription> externalClasses;
    private List<PropertyDescription> externalProperties;

    public OntologyDescription() {
        this.classes = new ArrayList<>();
        this.properties = new ArrayList<>();
        this.datatypes = new ArrayList<>();
        this.editors = new ArrayList<>();
        this.authors = new ArrayList<>();
        this.contributors = new ArrayList<>();
        this.externals = new HashSet<>();
        this.externalClasses = new ArrayList<>();
        this.externalProperties = new ArrayList<>();
    }

    public Set<String> getExternals() {
        return externals;
    }

    public void setExternals(Set<String> externals) {
        this.externals = externals;
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

    public List<ClassDescription> getExternalClasses() {
        return externalClasses;
    }

    public void setExternalClasses(List<ClassDescription> classes) {
        this.externalClasses = classes;
    }

    public List<PropertyDescription> getExternalProperties() {
        return externalProperties;
    }

    public void setExternalProperties(List<PropertyDescription> properties) {
        this.externalProperties = properties;
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

}

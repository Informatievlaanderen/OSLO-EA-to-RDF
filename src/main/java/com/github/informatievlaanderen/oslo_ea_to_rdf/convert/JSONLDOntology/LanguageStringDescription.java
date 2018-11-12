package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology;

/**
 * Created by langens-jonathan on 11/5/18.
 */
public class LanguageStringDescription {
    private String language;
    private String value;

    public LanguageStringDescription(){

    }

    public LanguageStringDescription(String language, String value) {
        this.language = language;
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

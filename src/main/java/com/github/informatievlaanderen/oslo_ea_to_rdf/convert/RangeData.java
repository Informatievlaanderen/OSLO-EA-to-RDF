package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import org.apache.jena.rdf.model.Resource;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;

/**
 * Minimal data container describing a Range 
 *
 * @author Bert Van Nuffelen 
 */
public class RangeData {
    private String eaname;
    private String eapackage;
    private Resource uri;
    private EAElement origin;

    public RangeData(){
	this.eaname = "";
	this.eapackage ="";
    }

    public RangeData(String eaname, String eapackage, Resource uri) {
        this.eaname = eaname;
        this.eapackage = eapackage;
	this.uri = uri;
    }

    public RangeData(String eaname, String eapackage, Resource uri, EAElement origin) {
        this.eaname = eaname;
        this.eapackage = eapackage;
	this.uri = uri;
	this.origin = origin;
    }

    public String getEaname() {
        return eaname;
    }

    public String getEapackage() {
        return eapackage;
    }

    public Resource getUri() {
        return uri;
    }

    public EAElement getOrigin() {
        return origin;
    }

   public String toJson() {
	String json = "";
        if (origin != null) {
	  json = "{" +
             "\"EA-Name\" : \"" +  eaname +
             "\", \"EA-GUID\" : \"" +  origin.getGuid() +
             "\", \"EA-Package\" : \"" + eapackage +
             "\", \"uri\" : \"" + uri +
	     "\" }";
	} else {
	  json = "{" +
             "\"EA-Name\" : \"" +  eaname +
             "\", \"EA-Package\" : \"" + eapackage +
             "\", \"uri\" : \"" + uri +
	     "\" }";
	}
	
	return json ;
	}
}

package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology.*;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that aggregates the conversion results in JSONLD file that follows the
 * context as it was defined for consumption by the OSLO-Specification-JS service.
 *
 * TODO go throught the methods of this class and:
 *   1. make sure their naming is consistent
 *   2. make sure they are all needed
 *
 * @author Jonathan Langens
 */
public class JSONLDOutputHandler implements OutputHandler {
    private final static Joiner JOINER = Joiner.on(", ");
    private String contributorsList;
    private String ontologyName;
    private BufferedWriter writer;
    private TagHelper tagHelper;
    private EADiagram diagram;
    private List<String> tagNames;
    private OntologyDescription ontologyDescription = new OntologyDescription();
    private JSONLDConversionReport conversionReport = new JSONLDConversionReport();

    private final Logger LOGGER = LoggerFactory.getLogger(JSONLDOutputHandler.class);

    public JSONLDOutputHandler(String ontologyName, String contributorsList, BufferedWriter writer, TagHelper tagHelper, EADiagram diagram) throws IOException {
        this.contributorsList = contributorsList;
        this.ontologyName = ontologyName;
        this.writer = writer;
        this.tagHelper = tagHelper;
        this.diagram = diagram;
        this.tagNames = tagHelper.getTagNames(tagHelper.getContentMappings(Scope.FULL_DEFINITON));
    }

    public void handleContributors(URL url) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            this.handleContributors(in);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void handleContributors(File file) {
        try (Reader r = Files.newBufferedReader(file.toPath())) {
            handleContributors(new BufferedReader(r));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleContributors(BufferedReader reader) {
        try {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.newFormat(';'));

            // This list contains all records in the contributors CSV
            // the first line contains just the headers.
            List<CSVRecord> csvRecords = csvParser.getRecords();

            // now we will get the column that corresponds to this ontology
            CSVRecord header = csvRecords.get(0);
            int ontologyField = 0;
            for(int i = 0; i < header.size(); ++i) {
                if(header.get(i).toLowerCase().equals(this.contributorsList.toLowerCase())) {
                    ontologyField = i;
                }
            }
            if(ontologyField == 0) {
                // the ontology name was not found in the header
                
//                this.conversionReport.addRemark("[!] Error: The header with name: " + this.contributorsList + " was not found in the contributors.csv file");
                LOGGER.error("The header with name: {} was not found in the contributors.csv file", this.contributorsList);
                return;
            }
            for(int i = 1; i < csvRecords.size(); ++i) {
                CSVRecord contributor = csvRecords.get(i);
                if(contributor.get(ontologyField) != "") {
                    // ok they contributed to our ontology
                    ContributorDescription cd = new ContributorDescription(
                            contributor.get(0),
                            contributor.get(1),
                            contributor.get(2),
                            contributor.get(3),
                            contributor.get(4));
                    if(contributor.get((ontologyField)).equals("A")) {
                        this.ontologyDescription.getAuthors().add(cd);
                    } else if (contributor.get((ontologyField)).equals("E")) {
                        this.ontologyDescription.getEditors().add(cd);
                    } else if (contributor.get((ontologyField)).equals("C")) {
                        this.ontologyDescription.getContributors().add(cd);
                    } else if(!contributor.get(ontologyField).trim().isEmpty()){
                        LOGGER.warn("The contributor with name: {}  {}'s code was not recognized. Code found was: {} ", contributor.get(0), contributor.get(1), contributor.get(ontologyField));
//                        this.conversionReport.addRemark("[ ] Warning: The contributor with name: " + contributor.get(0) + " " + contributor.get(1) + "'s code was not recognized. Code found was: " + contributor.get(ontologyField));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Writes the internal model to the specified turtle file.
     * @param outputFile the desired output turtle file
     * @throws IOException if an exception occurred while writing the file
     */
    public void writeToFile(Path outputFile) throws IOException {
        this.writeOntology();
    }

    public void writeReportToFile(String outputFile) {
        try {
            FileWriter writer = new FileWriter(outputFile, true);
            for(String remark : this.conversionReport.getRemarks()) {
                writer.write(remark);
                writer.write("\n");
            }
            writer.flush();
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void addToReport(String remark) {
        this.conversionReport.addRemark(remark);
    }

    private DiagramElement findInDiagram(EAElement element) {
        for (DiagramElement diagramElement : diagram.getElements()) {
            if (diagramElement.getReferencedElement().equals(element))
                return diagramElement;
        }
        return null;
    }

    private DiagramConnector findInDiagram(EAConnector connector) {
        for (DiagramElement element : diagram.getElements()) {
            for (DiagramConnector rawConnector : element.getConnectors()) {
                for (EAConnector conn : Util.extractAssociationElement(rawConnector.getReferencedConnector(), rawConnector.getLabelDirection())) {
                    if (connector.equals(conn))
                        return rawConnector;
                }
            }
        }
        return null;
    }

    private List<String> extractTagValues(List<TagData> tagData) {
        List<String> result = new ArrayList<>();

        for (String tagName : tagNames) {
            String s = tagData.stream()
                    .filter(t -> tagName.equals(t.getOriginTag()))
                    .findFirst()
                    .map(t -> t.getValue().isLiteral() ? t.getValue().asLiteral().getString() : t.getValue().asResource().getURI())
                    .orElse("");
            result.add(s);
        }
        return result;
    }

    private List<String> extractTagsJson(List<TagData> tagData) {
        List<String> result = new ArrayList<>();

        for (String tagName : tagNames) {
            String s = tagData.stream()
                    .filter(t -> tagName.equals(t.getOriginTag()))
                    .findFirst()
                    .map(t -> t.getValue().isLiteral() ? t.getValue().asLiteral().getString() : t.getValue().asResource().getURI())
                    .orElse("");
            result.add("\"" + tagName + "\" : \"" + StringEscapeUtils.escapeJson(s) + "\"");
        }
        return result;
    }

    private String extractVocabulary(String URI) {
        if(URI.lastIndexOf("#") > -1) {
            return URI.substring(0, URI.lastIndexOf("#"));
        } else {
            return URI.substring(0, URI.lastIndexOf("/"));
        }
    }

    public void handleExternalVocabularies() {
        // we want to ignore the protocols http and https when comparing
        // URI's to decide whether or not a URI falls within this ontology
        String ontologyURINoHttps = this.ontologyDescription.getUri().replace("https", "").replace("http", "");
        Set<String> vocabularies = new HashSet<>();
        for(PropertyDescription propertyDescription : this.ontologyDescription.getProperties()) {
            if(!propertyDescription.getType().contains(ontologyURINoHttps)) {
                vocabularies.add(extractVocabulary(propertyDescription.getType()));
            }
            for(String domain : propertyDescription.getDomain()) {
                if(!domain.contains(ontologyURINoHttps)) {
                    vocabularies.add(extractVocabulary(domain));
                }
            }
            for(String range : propertyDescription.getRange()) {
                if(!range.contains(ontologyURINoHttps)) {
                    vocabularies.add(extractVocabulary(range));
                }
            }
        }
        for(ClassDescription classDescription : this.ontologyDescription.getClasses()) {
            if(!classDescription.getType().contains(ontologyURINoHttps)) {
                vocabularies.add(extractVocabulary(classDescription.getType()));
            }
        }
        this.ontologyDescription.getExternals().addAll(vocabularies);
    }

    @Override
    public void handleOntology(EAPackage sourcePackage, Resource ontology, String prefix, String baseURI) {
        ontologyDescription.setUri(ontology.getURI());
        ontologyDescription.setType(OWL.Ontology.getURI());
        ontologyDescription.setLabel(this.ontologyName);


        List<String> tagJsons = extractTagsJson(tagHelper.getTagDataFor(sourcePackage, tagHelper.getOntologyMappings()));
        String tags = "";
	if (!tagJsons.isEmpty()) {
	    tags = ", ";
	}
        tags = tags + JOINER.join(tagJsons);

        String extra = "{\"EA-Name\" : \"" + sourcePackage.getName() + "\", \"EA-Guid\" : \"," + sourcePackage.getGuid() + "\" " + tags + "}"; 
        ontologyDescription.setExtra(extra);

        /*  
           EA Details:
             sourcePackage.getName();
             sourcePackage.getGuid();
           OntologyParsed Details:
             ontology.getNameSpace();
             ontology.getLocalName();
        */
        /*
        TODO STILL ADD:
              "label": {
                "nl": "Gebouw",
                "en": "Building"
              },
              "modified": "22-10-2018",
              "issued": "TBD",
              "description": {
                "nl": " Dit is een applicatieprofiel op het OSLO-Gebouw vocabularium. De applicatie waarop dit profiel betrekking heeft is een Gebouwenregister. Gebouwregistratie houdt in dat de beheerders van dit gegeven gebouwen en gebouweenheden officieel vaststellen en vastleggen in een register. Het Gebouwenregister vormt zo de enige en unieke bron voor gebouwinformatie..",
                "en": "This is the description of this ontology..."
              }
         */
    }

    @Override
    public void handleClass(EAElement sourceElement, Resource clazz, Scope scope,
                            Resource ontology, List<Resource> parentClasses, List<Resource> allowedValues) {
        /* EA details:
           write(sourceElement.getType().toString());
           write(sourceElement.getPackage().getName());
           write(sourceElement.getName());
           write(sourceElement.getGuid());
           
           write(String.valueOf(scope != Scope.FULL_DEFINITON)); # external term
           Ontology details:
             clazz.getNamespace
             class.getLocalName();
           write(JOINER.join(parentClasses));
        */
  
        
        LOGGER.info("handle class \"{}\" .", sourceElement.getName());
             
        ClassDescription classDescription = new ClassDescription();
        classDescription.setUri(clazz.getURI());
        classDescription.setType("http://www.w3.org/2002/07/owl#Class");
        // write(RDFS.Class.getURI());

        List<EAElement> parents = findParents(findInDiagram(sourceElement));
        String eaparents = JOINER.join(Lists.transform(parents, EAElement::getName));

        String tags = "";

        List<String> tagJsons = extractTagsJson(tagHelper.getTagDataFor(sourceElement, tagHelper.getContentMappings(Scope.FULL_DEFINITON)));
        tags = JOINER.join(tagJsons);

        String extra = "{\"EA-Name\" : \"" + sourceElement.getName() + 
                         "\", \"EA-Guid\" : \"" + sourceElement.getGuid() + 
                         "\", \"EA-Package\" : \"" + sourceElement.getPackage().getName()  +
                         "\", \"EA-Type\" : \"" + sourceElement.getType() +
                         "\", \"EA-Parents\" : \"" + eaparents +
                         "\", \"parentclasses\" : \"" + JOINER.join(parentClasses) +
                         "\", " + tags +
                       "}"; 

        classDescription.setExtra(extra);

        // support via the mapping rules calculated tags - these tags are not to be explicitely encoded, but used to extract 
        // data for later processing
        String tv = "";
        for (TagData t : tagHelper.getTagDataFor(sourceElement, tagHelper.getContentMappings(Scope.FULL_DEFINITON))) {
                LOGGER.debug("process class-tag \"{}\" having value {}.", t.getOriginTag(), t.getValue().toString());
		tv = t.getOriginValue();
           switch (t.getOriginTag()) {
		case "label":
                        classDescription.getName().add(new LanguageStringDescription("nl", tv));
			break;
		case "definition":
                        classDescription.getDescription().add(new LanguageStringDescription("nl", tv));
			break;
		case "usage":
                        classDescription.getUsage().add(new LanguageStringDescription("nl", tv));
			break;
			
		};
        };

        // Quality Control
        if(classDescription.getName().size() < 1 && (classDescription.getUri() == null || classDescription.getUri().length() < 1)) {
//            this.addToReport("[!] Class without name or URI found, this class will be ignored");
            LOGGER.error(" Class {} without name or URI found, further processing this class will be incoherent", sourceElement.getName());
        } else if(classDescription.getName().size() < 1) {
//            this.addToReport("[!] Class with URI " + classDescription.getUri() + " has no proper name in dutch (nl).");
            LOGGER.error(" Class {} without name in dutch, further processing this class will be incoherent", sourceElement.getName());
        } else {
            for (LanguageStringDescription name : classDescription.getName()) {
                if (name.getLanguage() == "nl") {
                    if (name.getValue() == null || name.getValue().length() < 1 || name.getValue().toLowerCase().trim().equals("todo")) {
//                        this.addToReport("[!] Class with URI " + classDescription.getUri() + " has no proper name in dutch (nl).");
                        LOGGER.error(" Class {} with empty or dummy name in dutch, further processing this class will be incoherent", sourceElement.getName());
                    }
                }
            }
        }
        // always add the class
        // determin to which categorie the class belongs:
        if (scope != Scope.FULL_DEFINITON) {
          // external
           this.ontologyDescription.getExternalClasses().add(classDescription);
        } else {
           this.ontologyDescription.getClasses().add(classDescription);
         }
    }

    private String extractURI(EAElement element) {
        for(EAAttribute attribute : element.getAttributes()) {
            for(EATag tag : attribute.getTags()) {
                if(tag.getKey().toLowerCase().equals("uri")) {
                    return tag.getValue();
                }
            }
        }
//        this.conversionReport.addRemark("[!] Severe warning: The EA element with name " + element.getName() + " does not have an associated URI within it's tags.");
        LOGGER.warn("The EA element with name {} does not have an associated URI within it's tags.", element.getName());
        return element.getName();
    }

    @Override
    public void handleProperty(PropertySource source, Resource property, Scope scope,
                               PackageExported packageExported, Resource ontology,
                               Resource propertyType, Resource domain, Resource range,
                               String lowerbound, String upperbound, List<Resource> superProperties) {
        PropertyDescription propertyDescription = new PropertyDescription();
        propertyDescription.setUri(property.getURI());
        propertyDescription.setType(propertyType.getURI());

        String tags = "";

        List<String> tagJsons = extractTagsJson(tagHelper.getTagDataFor(MoreObjects.firstNonNull(source.attribute, source.connector), tagHelper.getContentMappings(Scope.FULL_DEFINITON)));
        tags = JOINER.join(tagJsons);
        String extra ="";

        String pdomain = "";
        String pdomainguid = "";
        String prange = "";
        if (source.attribute != null) {
           pdomain = source.attribute.getElement().getName();
           prange = source.attribute.getType() ;
           extra = "{\"EA-Name\" : \"" + source.attribute.getName() + 
                         "\", \"EA-Guid\" : \"" + source.attribute.getGuid() + 
                         "\", \"EA-Package\" : \"" + source.attribute.getElement().getPackage().getName()  +
                         "\", \"EA-Type\" : \"" + "attribute" +
                         "\", \"EA-Domain\" : \"" + pdomain + 
                         "\", \"EA-Domain-Guid\" : \"" + source.attribute.getElement().getGuid() +
                         "\", \"EA-Range\" : \"" + prange +
                         "\", " + tags +
                       "}"; 
        } else {
            DiagramConnector dConnector = findInDiagram(source.connector);
            EAConnector.Direction direction = dConnector.getLabelDirection();
            if (direction == EAConnector.Direction.UNSPECIFIED)
                direction = dConnector.getReferencedConnector().getDirection();
            if (EAConnector.Direction.SOURCE_TO_DEST.equals(direction)) {
                pdomain = source.connector.getSource().getName(); // Domain
                pdomainguid = source.connector.getSource().getGuid(); // Domain GUID
                prange = source.connector.getDestination().getName(); // Range
            } else if (EAConnector.Direction.DEST_TO_SOURCE.equals(direction)) {
                pdomain = source.connector.getDestination().getName(); // Domain
                pdomainguid = source.connector.getDestination().getGuid(); // Domain GUID
                prange = source.connector.getSource().getName(); // Range
            } ; 
		
           extra = "{\"EA-Name\" : \"" + source.connector.getName() + 
                         "\", \"EA-Guid\" : \"" + source.connector.getGuid() + 
                         "\", \"EA-Package\" : \"" + "" +
                         "\", \"EA-Type\" : \"" + "connector" +
                         "\", \"EA-Domain\" : \"" + pdomain +
                         "\", \"EA-Domain-Guid\" : \"" + pdomainguid +
                         "\", \"EA-Range\" : \"" + prange +
                         "\", " + tags +
                       "}"; 
        }

        propertyDescription.setExtra(extra);

        // support via the mapping rules calculated tags - these tags are not to be explicitely encoded, but used to extract 
        // data for later processing
        String tv = "";
        for (TagData t : tagHelper.getTagDataFor(MoreObjects.firstNonNull(source.attribute, source.connector), tagHelper.getContentMappings(Scope.FULL_DEFINITON)) ) {
                LOGGER.debug("process property-tag \"{}\" having value {}.", t.getOriginTag(), t.getValue().toString());
		tv = t.getOriginValue();
           switch (t.getOriginTag()) {
		case "label":
                        propertyDescription.getName().add(new LanguageStringDescription("nl", tv));
			break;
		case "definition":
                        propertyDescription.getDescription().add(new LanguageStringDescription("nl", tv));
			break;
		case "usage":
                        propertyDescription.getUsage().add(new LanguageStringDescription("nl", tv));
			break;
			
		};
        };


        // TODO: this is a quickfix, but does not resolve the case for the following scenario
        // if there are 2 classes mapped on the same URI and one of the classes is used as domain/range the label maight get wrong
        // Therefore the label should be part of the domain/range resolvement. => Impact on other processing parts
        if(domain != null) {
	    String propdomain = "{ \"uri\": \"" + domain.getURI() + "\", \"EA-Name\" : \"" + pdomain + "\" }";
            propertyDescription.getDomain().add(propdomain);
        }
        if(range != null) {
	    String proprange= "{ \"uri\": \"" + range.getURI() + "\", \"EA-Name\" : \"" + prange + "\" }";
            propertyDescription.getRange().add(proprange);
        }

        String parent = JOINER.join(Iterables.transform(superProperties, Resource::getURI));
        if(parent.length() > 0) {
            propertyDescription.getGeneralization().add(parent);
        }

        if(lowerbound != null && lowerbound.length() > 0) {
            propertyDescription.setMinCount(lowerbound);
        }

        if(upperbound != null && upperbound.length() > 0) {
            propertyDescription.setMaxCount(upperbound);
        }

        // Quality control
        String pname = MoreObjects.firstNonNull(source.attribute, source.connector).getName();
        if(propertyDescription.getName().size() < 1 && (propertyDescription.getUri() == null || propertyDescription.getUri().length() < 1)) {
//            this.addToReport("[!] Property without name or URI found, this class will be ignored");
            LOGGER.error(" Property {} without name or URI found, further processing this property will be incoherent", pname);
        } else if(propertyDescription.getName().size() < 1) {
//            this.addToReport("[!] Property with URI " + propertyDescription.getUri() + " has no proper name in dutch (nl).");
            LOGGER.error(" Property {} without name in dutch, further processing this property will be incoherent", pname);
        } else {
            for (LanguageStringDescription name : propertyDescription.getName()) {
                if (name.getLanguage() == "nl") {
                    if (name.getValue() == null || name.getValue().length() < 1 || name.getValue().toLowerCase().trim().equals("todo")) {
//                        this.addToReport("[!] Property with URI " + propertyDescription.getUri() + " has no proper name in dutch (nl).");
                        LOGGER.error(" Property {} without with empty or dummy name in dutch, further processing this property will be incoherent", pname);
                    }
                }
            }
        }

        // always add property
        // determin to which categorie the class belongs:
        if (scope != Scope.FULL_DEFINITON) {
          // external for the vocabulary definition
           this.ontologyDescription.getExternalProperties().add(propertyDescription);
        } else {
           this.ontologyDescription.getProperties().add(propertyDescription);
         }
    }

    private String getExternalName(String external) {
        if(external.lastIndexOf("/") > external.lastIndexOf("#")) {
            return external.substring(external.lastIndexOf("/") + 1, external.length());
        } else {
            return external.substring(external.lastIndexOf("#") + 1, external.length());
        }
    }

    private void writeOntology() {
        // TODO instead of writing this through write statements we need the inclusion of a
        //      json manipulation library such as jackson
        try {
            writer.write("{\n");
            writer.write(generateContext());

            /*
            The things that still need to be added:
              "label": {
                "nl": "Gebouw",
                "en": "Building"
              },
              "modified": "22-10-2018",
              "issued": "TBD",
              "description": {
                "nl": " Dit is een applicatieprofiel op het OSLO-Gebouw vocabularium. De applicatie waarop dit profiel betrekking heeft is een Gebouwenregister. Gebouwregistratie houdt in dat de beheerders van dit gegeven gebouwen en gebouweenheden officieel vaststellen en vastleggen in een register. Het Gebouwenregister vormt zo de enige en unieke bron voor gebouwinformatie..",
                "en": "This is the description of this ontology..."
              }
             */
            writer.write("\"@id\": \"" + ontologyDescription.getUri() + "\",\n");
            writer.write("\"@type\": \"" + ontologyDescription.getType() + "\",\n");
            writer.write("\"label\": {\n  \"nl\": \"" +
                    ontologyDescription.getLabel() +
                    "\",\n  \"en\": \"" +
                    ontologyDescription.getLabel() +
                    "\"\n},\n");
            writer.write("\"extra\": " + ontologyDescription.getExtra() + ",\n");

            String authorsJSON = ""; // notice the authors (plural)
            writer.write("\"authors\": [\n");
            for(ContributorDescription author : ontologyDescription.getAuthors()) {
                String authorJSON = "{\n";
                if(!author.getFirstName().isEmpty()) {
                    authorJSON += "\"foaf:first_name\": \"" + author.getFirstName() + "\",\n";
                }
                if(!author.getLastName().isEmpty()) {
                    authorJSON += "\"foaf:last_name\": \"" + author.getLastName() + "\",\n";
                }
                if(!author.getAffiliation().isEmpty() || !author.getWebsite().isEmpty()) {
                    authorJSON += "\"affiliation\": {";
                    if(!author.getAffiliation().isEmpty()) {
                        authorJSON += "\"foaf:name\": \"" + author.getAffiliation() + "\",\n";
                    }
                    if(!author.getWebsite().isEmpty()) {
                        authorJSON += "\"foaf:homepage\": \"" + author.getWebsite() + "\",\n";
                    }
                    authorJSON = authorJSON.substring(0, authorJSON.length()-2);
                    authorJSON += "},\n";
                }
                if(!(author.getEmail().isEmpty())) {
                    authorJSON += "\"foaf:mbox\": \"" + author.getEmail() + "\",\n";
                }
                if(!authorJSON.replace("{","").replace("}","").replace("\"","").replace("\n","").replace(" ","").replace(",", "").isEmpty()) {
                    // author has some fields
                    authorJSON += "\"@type\": \"person:Person\"";
                    authorJSON += "\n},\n";
                    authorsJSON += authorJSON;
                }
            }
            if(this.ontologyDescription.getAuthors().size() > 0) {
                authorsJSON = authorsJSON.substring(0, authorsJSON.length() - 2);
                writer.write(authorsJSON);
            }
            writer.write("],\n");

            String editorsJSON = ""; // notice the editors (plural)
            writer.write("\"editors\": [\n");
            for(ContributorDescription editor : ontologyDescription.getEditors()) {
                String editorJSON = "{\n";
                if(!editor.getFirstName().isEmpty()) {
                    editorJSON += "\"foaf:first_name\": \"" + editor.getFirstName() + "\",\n";
                }
                if(!editor.getLastName().isEmpty()) {
                    editorJSON += "\"foaf:last_name\": \"" + editor.getLastName() + "\",\n";
                }
                if(!editor.getAffiliation().isEmpty() || !editor.getWebsite().isEmpty()) {
                    editorJSON += "\"affiliation\": {";
                    if(!editor.getAffiliation().isEmpty()) {
                        editorJSON += "\"foaf:name\": \"" + editor.getAffiliation() + "\",\n";
                    }
                    if(!editor.getWebsite().isEmpty()) {
                        editorJSON += "\"foaf:homepage\": \"" + editor.getWebsite() + "\",\n";
                    }
                    editorJSON = editorJSON.substring(0, editorJSON.length()-2);
                    editorJSON += "},\n";
                }
                if(!(editor.getEmail().isEmpty())) {
                    editorJSON += "\"foaf:mbox\": \"" + editor.getEmail() + "\",\n";
                }
                if(!editorJSON.replace("{","").replace("}","").replace("\"","").replace("\n","").replace(" ","").replace(",", "").isEmpty()) {
                    // editor has some fields
                    editorJSON += "\"@type\": \"person:Person\"";
                    editorJSON += "\n},\n";
                    editorsJSON += editorJSON;
                }
            }
            if(this.ontologyDescription.getEditors().size() > 0) {
                editorsJSON = editorsJSON.substring(0, editorsJSON.length() - 2);
                writer.write(editorsJSON);
            }
            writer.write("],\n");

            String contributorsJSON = ""; // notice the contributors (plural)
            writer.write("\"contributors\": [\n");
            for(ContributorDescription contributor : ontologyDescription.getContributors()) {
                String contributorJSON = "{\n";
                if(!contributor.getFirstName().isEmpty()) {
                    contributorJSON += "\"foaf:first_name\": \"" + contributor.getFirstName() + "\",\n";
                }
                if(!contributor.getLastName().isEmpty()) {
                    contributorJSON += "\"foaf:last_name\": \"" + contributor.getLastName() + "\",\n";
                }
                if(!contributor.getAffiliation().isEmpty() || !contributor.getWebsite().isEmpty()) {
                    contributorJSON += "\"affiliation\": {";
                    if(!contributor.getAffiliation().isEmpty()) {
                        contributorJSON += "\"foaf:name\": \"" + contributor.getAffiliation() + "\",\n";
                    }
                    if(!contributor.getWebsite().isEmpty()) {
                        contributorJSON += "\"foaf:homepage\": \"" + contributor.getWebsite() + "\",\n";
                    }
                    contributorJSON = contributorJSON.substring(0, contributorJSON.length()-2);
                    contributorJSON += "},\n";
                }
                if(!(contributor.getEmail().isEmpty())) {
                    contributorJSON += "\"foaf:mbox\": \"" + contributor.getEmail() + "\",\n";
                }
                if(!contributorJSON.replace("{","").replace("}","").replace("\"","").replace("\n","").replace(" ","").replace(",", "").isEmpty()) {
                    // contributor has some field
                    contributorJSON += "\"@type\": \"person:Person\"";
                    contributorJSON += "\n},\n";
                    contributorsJSON += contributorJSON;
                }
            }
            if(this.ontologyDescription.getContributors().size() > 0) {
                contributorsJSON = contributorsJSON.substring(0, contributorsJSON.length() - 2);
                writer.write(contributorsJSON);
            }
            writer.write("],\n");

            writer.write("\"classes\": [\n");
            String outputString = "";
            for (ClassDescription classDescription : ontologyDescription.getClasses()) {
                outputString += "{\n";
                outputString += "\"@id\": \"" + classDescription.getUri() + "\",\n";
                outputString += "\"@type\": \"" + classDescription.getType() + "\",\n";
                outputString += "\"extra\": " + classDescription.getExtra() + ",\n";
                outputString += "\"name\": {\n";
                outputString += print_languagetagged(classDescription.getName());
                outputString += "},\n";
                outputString += "\"description\": {\n";
                outputString += print_languagetagged(classDescription.getDescription());
                outputString += "},\n";
                outputString += "\"usage\": {\n";
                outputString += print_languagetagged(classDescription.getUsage());
                outputString += "}\n";
                outputString += "},\n";
            }
            if(ontologyDescription.getClasses().size() > 0) {
                outputString = outputString.substring(0, outputString.length() - 2);
            }
            outputString += "\n],\n";
            writer.write(outputString);

            writer.write("\"properties\": [\n");
            outputString = "";
            outputString += print_propertydescription(ontologyDescription.getProperties());
            outputString += "],\n";

            outputString += "\"externals\": [\n";
            
            List<String> excls = new ArrayList<>();
            String externalS = "";
            for(ClassDescription external : ontologyDescription.getExternalClasses()) {
                externalS += "{\n";
                externalS += "\"@id\": \"" + external.getUri() + "\",\n";
                externalS += "\"@type\": \"" + external.getType() + "\",\n";
                externalS += "\"extra\": " + external.getExtra() + ",\n";
                externalS += "\"name\": {\n";
                externalS += print_languagetagged(external.getName());
 		externalS += "}\n";
                externalS += "}\n";
 		excls.add(externalS);
                externalS = "";
            }
            outputString += JOINER.join(excls);
            outputString += "],\n";
            outputString += "\"externalproperties\": [\n";
            outputString += print_propertydescription(ontologyDescription.getExternalProperties());
            outputString += "]\n";

            writer.write(outputString);

            writer.write("}");
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    private String print_propertydescription(List<PropertyDescription> properties) {
            List<String> props = new ArrayList<>();
            String outputString = "";
            for (PropertyDescription propertyDescription : properties) {
                outputString = "";
                outputString += "{\n";
                outputString += "\"@id\": \"" + propertyDescription.getUri() + "\",\n";
                outputString += "\"@type\": \"" + propertyDescription.getType() + "\",\n";
                outputString += "\"name\": {\n";
                outputString += print_languagetagged(propertyDescription.getName());
                outputString += "},\n";
                outputString += "\"extra\": " + propertyDescription.getExtra() + ",\n";
                outputString += "\"description\": {\n";
                outputString += print_languagetagged(propertyDescription.getDescription());
                outputString += "},\n";
                outputString += "\"usage\": {\n";
                outputString += print_languagetagged(propertyDescription.getUsage());
                outputString += "},\n";
                outputString += "\"domain\": [\n";
                for(String domain : propertyDescription.getDomain()) {
                    outputString += "" + domain + ",\n";
                }
                if(propertyDescription.getDomain().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2);
                }
                outputString += "\n],\n";
                outputString += "\"range\": [\n";
                for(String range : propertyDescription.getRange()) {
                    outputString += "" + range + ",\n";
                }
                if(propertyDescription.getRange().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2);
                }
                outputString += "\n],\n";
                outputString += "\"generalization\": [\n";
                for(String generalization : propertyDescription.getGeneralization()) {
                    outputString += "\"" + generalization + "\",\n";
                }
                if(propertyDescription.getGeneralization().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2);
                }
                outputString += "\n],\n";
                if(propertyDescription.getMinCount() != null && propertyDescription.getMinCount().length() > 0) {
                    outputString += "\"minCardinality\": \"" + propertyDescription.getMinCount() + "\",\n";
                }
                if(propertyDescription.getMaxCount() != null && propertyDescription.getMaxCount().length() > 0) {
                    outputString += "\"maxCardinality\": \"" + propertyDescription.getMaxCount() + "\",\n";
                }
                outputString = outputString.substring(0, outputString.length() - 2);
                outputString += "}\n";
 		props.add(outputString);
            }
            return JOINER.join(props);

    }

    private String print_languagetagged(List<LanguageStringDescription> lvalues) {

       List<String> results = new ArrayList<>();
       for (LanguageStringDescription lsd : lvalues ) {
                    results.add("\"" + lsd.getLanguage() + "\": \"" + StringEscapeUtils.escapeJson(lsd.getValue()) + "\"");
                }
       String result = JOINER.join(results);
       return result;
    }

    @Override
    public void handleInstance(EAAttribute source, Resource instance, Scope scope,
                               Resource ontology, Resource clazz) {
       LOGGER.warn("INSTANCE NOT HANDLED: {} - {}", source.getName(), source.getGuid());
    }

    private List<EAElement> findParents(DiagramElement child) {
        return child.getConnectors().stream()
                .map(dConn -> {
                    EAConnector conn = dConn.getReferencedConnector();
                    if (!EAConnector.TYPE_GENERALIZATION.equals(conn.getType())
                            || Boolean.valueOf(tagHelper.getOptionalTag(conn, Tag.IGNORE, "false"))
                            || dConn.isHidden()
                            || EAConnector.Direction.BIDIRECTIONAL.equals(conn.getDirection())
                            || EAConnector.Direction.UNSPECIFIED.equals(conn.getDirection()))
                        return null;
                    if (EAConnector.Direction.SOURCE_TO_DEST.equals(conn.getDirection())) {
                        if (child.getReferencedElement().equals(conn.getSource()))
                            return conn.getDestination();
                        else
                            return null;
                    } else {
                        if (child.getReferencedElement().equals(conn.getDestination()))
                            return conn.getSource();
                        else
                            return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

  // TODO: read this from a file in the configuration
    private String generateContext() {
        return "  \"@context\": {\n" +
                "    \"vlaanderen\": \"http://data.vlaanderen.be/ns/\",\n" +
                "    \"owl\": \"http://www.w3.org/2002/07/owl#\",\n" +
                "    \"void\": \"http://rdfs.org/ns/void#\",\n" +
                "    \"dcterms\": \"http://purl.org/dc/terms/\",\n" +
                "    \"rdf\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\n" +
                "    \"dcat\": \"http://www.w3.org/ns/dcat#\",\n" +
                "    \"sdmx-dimension\": \"http://purl.org/linked-data/sdmx/2009/dimension#\",\n" +
                "    \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n" +
                "    \"sdmx-attribute\": \"http://purl.org/linked-data/sdmx/2009/attribute#\",\n" +
                "    \"qb\": \"http://purl.org/linked-data/cube#\",\n" +
                "    \"skos\": \"http://www.w3.org/2004/02/skos/core#\",\n" +
                "    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\",\n" +
                "    \"sdmx-concept\": \"http://purl.org/linked-data/sdmx/2009/concept#\",\n" +
                "    \"foaf\": \"http://xmlns.com/foaf/0.1/\",\n" +
                "    \"person\": \"http://www.w3.org/ns/person#\",\n" +
                "    \"rec\": \"http://www.w3.org/2001/02pd/rec54#\",\n" +
                "    \"vann\": \"http://purl.org/vocab/vann/\",\n" +
                "    \"sh\": \"http://w3.org/ns/shacl#\",\n" +
                "\n" +
                "    \"label\": {\n" +
                "      \"@id\": \"rdfs:label\",\n" +
                "      \"@container\": \"@language\"\n" +
                "    },\n" +
                "    \"modified\": {\n" +
                "      \"@id\": \"dcterms:modified\",\n" +
                "      \"@type\": \"xsd:date\"\n" +
                "    },\n" +
                "    \"issued\": {\n" +
                "      \"@id\": \"dcterms:issued\"\n" +
                "    },\n" +
                "    \"authors\": {\n" +
                "      \"@type\": \"person:Person\",\n" +
                "      \"@id\": \"foaf:maker\"\n" +
                "    },\n" +
                "    \"editors\": {\n" +
                "      \"@type\": \"person:Person\",\n" +
                "      \"@id\": \"rec:editor\"\n" +
                "    },\n" +
                "    \"contributors\": {\n" +
                "      \"@type\": \"person:Person\",\n" +
                "      \"@id\": \"dcterms:contributor\"\n" +
                "    },\n" +
                "    \"affiliation\": {\n" +
                "      \"@id\": \"http://schema.org/affiliation\"\n" +
                "    },\n" +
                "    \"classes\": {\n" +
                "      \"@reverse\": \"rdfs:isDefinedBy\"\n" +
                "      },\n" +
                "    \"datatypes\": {\n" +
                "      \"@reverse\": \"rdfs:isDefinedBy\"\n" +
                "      },\n" +
                "    \"name\": {\n" +
                "      \"@id\": \"rdfs:label\",\n" +
                "      \"@container\": \"@language\"\n" +
                "    },\n" +
                "    \"description\": {\n" +
                "      \"@id\": \"rdfs:comment\",\n" +
                "      \"@container\": \"@language\"\n" +
                "    },\n" +
                "    \"properties\": {\n" +
                "      \"@reverse\": \"rdfs:isDefinedBy\"\n" +
                "    },\n" +
                "    \"domain\": {\n" +
                "      \"@id\": \"rdfs:domain\"\n" +
                "    },\n" +
                "    \"range\": {\n" +
                "      \"@id\": \"rdfs:range\"\n" +
                "    },\n" +
                "    \"minCardinality\": {\n" +
                "    \"@id\": \"sh:minCount\"\n" +
                "    },\n" +
                "    \"maxCardinality\": {\n" +
                "    \"@id\": \"sh:maxCount\"\n" +
                "    },\n" +
                "    \"generalization\": {\n" +
                "      \"@id\": \"rdfs:subPropertyOf\"\n" +
                "    },\n" +
                "    \"externals\": {\n" +
                "      \"@type\": \"http://www.w3.org/2000/01/rdf-schema#Class\",\n" +
                "      \"@id\": \"rdfs:seeAlso\"\n" +
                "      },\n" +
                "    \"label\": {\n" +
                "      \"@id\": \"rdfs:label\",\n" +
                "      \"@container\": \"@language\"\n" +
                "    },\n" +
                "    \"usage\": {\n" +
                "      \"@id\": \"vann:usageNote\",\n" +
                "      \"@container\": \"@language\"\n" +
                "    }\n" +
                "  },\n";
    }
}

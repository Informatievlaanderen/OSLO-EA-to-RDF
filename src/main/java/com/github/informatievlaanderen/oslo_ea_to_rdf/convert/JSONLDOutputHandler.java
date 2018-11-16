package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology.*;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class that aggregates the conversion results in JSONLD file that follows the
 * context as it was defined for consumption by the OSLO-Specification-JS service.
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

    public JSONLDOutputHandler(String ontologyName, String contributorsList, BufferedWriter writer, TagHelper tagHelper, EADiagram diagram) throws IOException {
        this.contributorsList = contributorsList;
        this.ontologyName = ontologyName;
        this.writer = writer;
        this.tagHelper = tagHelper;
        this.diagram = diagram;
        this.tagNames = tagHelper.getTagNames(tagHelper.getContentMappings(Scope.FULL_DEFINITON));
    }

    public void handleContributers(URL url) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            this.handleContributors(in);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void handleContributors(BufferedReader reader) {
        try {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

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
                this.conversionReport.addRemark("[!] Error: The header with name: " + this.contributorsList + " was not found in the contributors.csv file");
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
                        this.conversionReport.addRemark("[ ] Warning: The contributor with name: " + contributor.get(0) + " " + contributor.get(1) + "'s code was not recognized. Code found was: " + contributor.get(ontologyField));
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

    public void writeRapportToFile(String outputFile) {
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

    private List<String> extactTagValues(List<TagData> tagData) {
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

    @Override
    public void handleOntology(EAPackage sourcePackage, Resource ontology, String prefix, String baseURI) {
        ontologyDescription.setUri("TODO");
        ontologyDescription.setType(OWL.Ontology.getURI());
        ontologyDescription.setLabel(this.ontologyName);
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
        ClassDescription classDescription = new ClassDescription();
        classDescription.setUri(clazz.getURI());
        classDescription.setType("http://www.w3.org/2002/07/owl#Class");

        List<String> tagValues = extactTagValues(tagHelper.getTagDataFor(sourceElement, tagHelper.getContentMappings(Scope.FULL_DEFINITON)));
        for(String value : tagValues) {
            if(value != null && !value.equals("")) {
                String tag = this.tagNames.get(tagValues.indexOf(value));
                int cutOff = ((tag.indexOf('-') > -1)?tag.indexOf('-'):0);
                String puretag = tag.substring(0, cutOff);
                switch(puretag) {
                    case "definition":
                        classDescription.getDescription().add(new LanguageStringDescription(tag.substring(11,tag.length()), value));
                        break;
                    case "label":
                        classDescription.getName().add(new LanguageStringDescription(tag.substring(6, tag.length()), value));
                        break;
                    case "usage":
                        classDescription.getUsage().add(new LanguageStringDescription(tag.substring(6, tag.length()), value));
                        break;
                }
            }
        }
        this.ontologyDescription.getClasses().add(classDescription);
    }

    private String extractURI(EAElement element) {
        for(EAAttribute attribute : element.getAttributes()) {
            for(EATag tag : attribute.getTags()) {
                if(tag.getKey().toLowerCase().equals("uri")) {
                    return tag.getValue();
                }
            }
        }
        this.conversionReport.addRemark("[!] Severe warning: The EA element with name " + element.getName() + " does not have an associated URI within it's tags.");
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


        List<String> tagValues = extactTagValues(tagHelper.getTagDataFor(MoreObjects.firstNonNull(source.attribute, source.connector), tagHelper.getContentMappings(Scope.FULL_DEFINITON)));
        for(String value : tagValues) {
            if(value != null && !value.equals("")) {
                String tag = this.tagNames.get(tagValues.indexOf(value));
                int cutOff = ((tag.indexOf('-') > -1)?tag.indexOf('-'):0);
                String puretag = tag.substring(0, cutOff);
                switch(puretag) {
                    case "definition":
                        propertyDescription.getDescription().add(new LanguageStringDescription(tag.substring(11,tag.length()), value));
                        break;
                    case "label":
                        propertyDescription.getName().add(new LanguageStringDescription(tag.substring(6, tag.length()), value));
                        break;
                    case "usage":
                        propertyDescription.getUsage().add(new LanguageStringDescription(tag.substring(6, tag.length()), value));
                        break;
                }
            }
        }

//        if(source.connector != null) {
//            DiagramConnector dConnector = findInDiagram(source.connector);
//            EAConnector.Direction direction = dConnector.getLabelDirection();
//            if (direction == EAConnector.Direction.UNSPECIFIED)
//                direction = dConnector.getReferencedConnector().getDirection();
//            if (EAConnector.Direction.SOURCE_TO_DEST.equals(direction)) {
//                propertyDescription.getDomain().add(this.extractURI(source.connector.getSource()));
//                propertyDescription.getRange().add(this.extractURI(source.connector.getDestination()));
//            } else if (EAConnector.Direction.DEST_TO_SOURCE.equals(direction)) {
//                propertyDescription.getDomain().add(this.extractURI(source.connector.getDestination()));
//                propertyDescription.getRange().add(this.extractURI(source.connector.getSource()));
//            } else {
//                this.conversionReport.addRemark("[ ] relation direction for source " + source.toString() + " was not found.");
//            }
//        }
        if(domain != null) {
            propertyDescription.getDomain().add(domain.getURI());
        }
        if(range != null) {
            propertyDescription.getRange().add(range.getURI());
        }

        String parent = JOINER.join(Iterables.transform(superProperties, Resource::getURI));
        if(parent.length() > 0) {
            propertyDescription.getGeneralization().add(parent);
        }

        // this needs some work..
        if(lowerbound != null && lowerbound.length() > 0) {
            String cardinality = "";
            cardinality = lowerbound;
            if(upperbound != null && upperbound.length() > 0) {
                cardinality += ".." + upperbound;
            }
            propertyDescription.setCardinality(cardinality);
        } else {
            if(upperbound != null && upperbound.length() > 0) {
                propertyDescription.setCardinality(upperbound);
            }
        }
        this.ontologyDescription.getProperties().add(propertyDescription);
    }

    private void writeOntology() {
        // todo instead of writing this through write statements we need the inclusion of a
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
                outputString += "\"name\": {\n";

                for (LanguageStringDescription name : classDescription.getName()) {
                    outputString += "\"" + name.getLanguage() + "\": \"" + name.getValue() + "\",\n";
                }
                if (classDescription.getName().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2) + "\n";
                }
                outputString += "},\n";
                outputString += "\"description\": {\n";
                for (LanguageStringDescription description : classDescription.getDescription()) {
                    outputString += "\"" + description.getLanguage() + "\": \"" + description.getValue().replace("\"", "\\\"") + "\",\n";
                }
                if (classDescription.getDescription().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2) + "\n";
                }
                outputString += "},\n";
                outputString += "\"usage\": {\n";
                for (LanguageStringDescription usage : classDescription.getUsage()) {
                    outputString += "\"" + usage.getLanguage() + "\": \"" + usage.getValue() + "\",\n";
                }
                if (classDescription.getUsage().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2) + "\n";
                }
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
            for (PropertyDescription propertyDescription : ontologyDescription.getProperties()) {
                /*
                        "domain": [
          "http://data.vlaanderen.be/ns/gebouw#Gebouw",
          "http://data.vlaanderen.be/ns/gebouw#Gebouweenheid"
        ],
        "range": [
          "http://www.w3.org/2001/XMLSchema#integer"
        ],
        "cardinality": "0..1",
        "description": {
          "nl": "Totaal van het aantal boven- en ondergrondse gebouwlagen, bekeken over alle gebouwdelen heen.",
          "en": "to be translated"
        },
        "generalization": [
          "http://www.w3.org/ns/regorg#orgType"
        ],

                 */
                outputString += "{\n";
                outputString += "\"@id\": \"" + propertyDescription.getUri() + "\",\n";
                outputString += "\"@type\": \"" + propertyDescription.getType() + "\",\n";
                outputString += "\"name\": {\n";

                for (LanguageStringDescription name : propertyDescription.getName()) {
                    outputString += "\"" + name.getLanguage() + "\": \"" + name.getValue() + "\",\n";
                }
                if (propertyDescription.getName().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2) + "\n";
                }
                outputString += "},\n";
                outputString += "\"description\": {\n";
                for (LanguageStringDescription description : propertyDescription.getDescription()) {
                    outputString += "\"" + description.getLanguage() + "\": \"" + description.getValue() + "\",\n";
                }
                if (propertyDescription.getDescription().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2) + "\n";
                }
                outputString += "},\n";
                outputString += "\"usage\": {\n";
                for (LanguageStringDescription usage : propertyDescription.getUsage()) {
                    outputString += "\"" + usage.getLanguage() + "\": \"" + usage.getValue() + "\",\n";
                }
                if (propertyDescription.getUsage().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2) + "\n";
                }
                outputString += "},\n";
                outputString += "\"domain\": [\n";
                for(String domain : propertyDescription.getDomain()) {
                    outputString += "\"" + domain + "\",\n";
                }
                if(propertyDescription.getDomain().size() > 0) {
                    outputString = outputString.substring(0, outputString.length() - 2);
                }
                outputString += "\n],\n";
                outputString += "\"range\": [\n";
                for(String range : propertyDescription.getRange()) {
                    outputString += "\"" + range + "\",\n";
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
                if(propertyDescription.getCardinality() != null && propertyDescription.getCardinality().length() > 0) {
                    outputString += "\"cardinality\": \"" + propertyDescription.getCardinality() + "\",\n";
                }
                outputString = outputString.substring(0, outputString.length() - 2);
                outputString += "},\n";
            }
            if(ontologyDescription.getProperties().size() > 0) {
                outputString = outputString.substring(0, outputString.length() - 2);
            }
            outputString += "\n]\n";
            writer.write(outputString);

            writer.write("}");
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void handleInstance(EAAttribute source, Resource instance, Scope scope,
                               Resource ontology, Resource clazz) {
//        write("attribute"); // Type
//        write(source.getElement().getPackage().getName()); // Package
//        write(source.getName()); // Name
//        write(source.getGuid()); // GUID
//        write(""); // Parent
//        write(source.getElement().getName()); // Domain
//        write(source.getElement().getGuid()); // Domain GUID
//        write(""); // Range
//
//        for (String tag : extactTagValues(tagHelper.getTagDataFor(source, tagHelper.getContentMappings(Scope.FULL_DEFINITON)))) {
//            write(tag);
//        }
//
//        write(Boolean.toString(scope != Scope.FULL_DEFINITON));
//        write(instance.getNameSpace());
//        write(instance.getURI());
//        write(clazz.getURI());
//        write("");
//        write("");
//        write("");
//        write("");
//        writeNl("");
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
                "    \"cardinality\": {\n" +
                "    \"@id\": \"owl:cardinality\"\n" +
                "    },\n" +
                "    \"generalization\": {\n" +
                "      \"@id\": \"rdfs:subPropertyOf\"\n" +
                "    },\n" +
                "    \"externals\": {\n" +
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

//    private void write(String s) {
//        try {
//            writer.write("\"");
//            writer.write(Strings.nullToEmpty(s).replaceAll("\"", "\"\""));
//            writer.write("\"");
//            writer.write("\t");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//     private void writeNl(String s) {
//         try {
//             writer.write("\"");
//             writer.write(Strings.nullToEmpty(s).replaceAll("\"", "\"\""));
//             writer.write("\"");
//             writer.write("\n");
//         } catch (IOException e) {
//             throw new RuntimeException(e);
//         }
//     }
}

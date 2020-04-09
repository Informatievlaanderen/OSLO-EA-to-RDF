package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.RangeData.*;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.AssocFreeEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.AssociationEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea.RoleEAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl.MemoryEATag;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Conversion functionality.
 *
 * @author Dieter De Paepe
 */
public class Converter {
  // https://www.w3.org/TR/2004/REC-rdf-concepts-20040210/#section-Graph-Literal
  // https://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#built-in-datatypes
  private static final Map<String, Resource> DATATYPES =
      ImmutableMap.<String, Resource>builder()
          .put("String", XSD.xstring)
          .put("Date", XSD.date)
          .put("Time", XSD.time)
          .put("DateTime", XSD.dateTime)
          .put("Int", XSD.xint) // Note: this differs from "...XMLSchema#integer"!
          .put("Integer", XSD.integer)
          .put("Double", XSD.xdouble)
          .put("Boolean", XSD.xboolean)
          .put("LangString", RDF.langString)
          .put("Literal", RDFS.Literal)
          .put("Year", XSD.gYear)
          .put("YearMonth", XSD.gYearMonth)
          .put("Month", XSD.gMonth)
          .put("MonthDay", XSD.gMonthDay)
          .put("Duration", XSD.duration)
          .put("HTML", RDF.HTML)
          .put("URI", XSD.anyURI)
          .build();

  private final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

  private EARepository repo;
  private TagHelper tagHelper;
  private Multimap<String, EAPackage> nameToPackages;
  private Multimap<String, EAElement> nameToElements;
  private OutputHandler outputHandler;

  public Converter(EARepository repo, TagHelper tagHelper, OutputHandler outputHandler) {
    this.repo = repo;
    this.tagHelper = tagHelper;
    this.outputHandler = outputHandler;

    ImmutableListMultimap.Builder<String, EAPackage> pBuilder = ImmutableListMultimap.builder();
    ImmutableListMultimap.Builder<String, EAElement> eBuilder = ImmutableListMultimap.builder();

    for (EAPackage eaPackage : repo.getPackages()) {
      if (Boolean.valueOf(tagHelper.getOptionalTag(eaPackage, Tag.IGNORE, "false"))) continue;
      pBuilder.put(eaPackage.getName(), eaPackage);
      for (EAElement element : eaPackage.getElements()) {
        if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false"))) continue;
        eBuilder.put(element.getName(), element);
      }
    }
    nameToPackages = pBuilder.build();
    nameToElements = eBuilder.build();
  }

  public void convertDiagram(EADiagram diagram) {
    Map<EAConnector, EAConnector.Direction> connectorDirections = indexDirections(diagram);
    UriAssigner.Result uris =
        new UriAssigner(tagHelper)
            .assignURIs(repo.getPackages(), nameToPackages, connectorDirections);

    // Prefixes
    // for (EAPackage eaPackage : uris.packageURIs.keySet()) {
    //    String prefix = tagHelper.getOptionalTag(eaPackage, Tag.PACKAGE_BASE_URI_ABBREVIATION,
    // null);
    //    if (prefix != null)
    //        model.setNsPrefix(prefix, uris.packageURIs.get(eaPackage));
    // }

    // Convert package
    Resource ontology = convertPackage(diagram.getPackage(), uris.ontologyURIs, uris.packageURIs);
    LOGGER.debug("handle ontology");

    // Convert elements.
    for (DiagramElement diagramElement : diagram.getElements()) {
      EAElement element = diagramElement.getReferencedElement();
      if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false"))) {
        LOGGER.info("Skipping class \"{}\" since it is marked as ignored.", element.getPath());
        continue;
      }

      boolean currentPackageTerm = element.getPackage().equals(diagram.getPackage());
      boolean customURI = tagHelper.getOptionalTag(element, Tag.EXTERNAL_URI, null) != null;
      boolean refersToThisPackage =
          diagram
              .getPackage()
              .getName()
              .equals(
                  tagHelper.getOptionalTag(
                      element, Tag.DEFINING_PACKAGE, element.getPackage().getName()));
      Scope scope = Scope.NOTHING;
      if (currentPackageTerm && !customURI) scope = Scope.FULL_DEFINITON;
      else if (customURI) {
        boolean customURIsamePrefix =
            StringUtils.startsWith(
                tagHelper.getOptionalTag(element, Tag.EXTERNAL_URI, null), ontology.toString());
        if (refersToThisPackage) {
          scope = Scope.TRANSLATIONS_ONLY;
        }
        ;
        if (customURIsamePrefix) {
          scope = Scope.FULL_DEFINITON;
          LOGGER.warn(
              "Element {} has same prefix as package: simplify by removing the extra tag uri",
              element.getName());
        }
        ;
      }

      LOGGER.debug("Scope of covertion for diagram elements is \"{}\"", scope);
      convertElement(diagramElement, uris.elementURIs, uris.instanceURIs, ontology, scope);
    }

    // Convert connectors.
    Set<DiagramConnector> connectors = new HashSet<>();
    for (DiagramElement diagramElement : diagram.getElements())
      for (DiagramConnector dConnector : diagramElement.getConnectors()) connectors.add(dConnector);

    for (DiagramConnector dConnector : connectors) {
      // Skip if the connector is hidden in the diagram.
      if (dConnector.isHidden()) continue;

      EAConnector connector = dConnector.getReferencedConnector();

      // Inheritance was handled during element processing
      if (EAConnector.TYPE_GENERALIZATION.equals(connector.getType())) continue;

      // Skip if marked as ignore.
      if (Boolean.valueOf(tagHelper.getOptionalTag(connector, Tag.IGNORE, "false"))) {
        LOGGER.info(
            "Skipping connector \"{}\" since it is marked as ignored.", connector.getPath());
        continue;
      }

      //            convertConnector(dConnector, dConnector.getReferencedConnector(),
      // connectorDirections, uris.elementURIs,
      convertConnector2(
          dConnector,
          connectorDirections,
          uris.elementURIs,
          uris.connectorURIs,
          uris.definingPackages,
          nameToPackages,
          uris.packageURIs,
          ontology,
          diagram.getPackage());
    }

    // Convert non-enum attributes.
    for (DiagramElement diagramElement : diagram.getElements()) {
      // Skip enum attributes
      EAElement element = diagramElement.getReferencedElement();
      if (element.getType() == EAElement.Type.ENUMERATION) continue;

      // Skip if the element is set to ignore
      if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false"))) {
        // No need for logging, this was already mentioned when the element was skipped
        continue;
      }

      for (EAAttribute attribute : element.getAttributes()) {
        // Skip if marked as ignore.
        if (!uris.attributeURIs.containsKey(attribute)) {
          LOGGER.info(
              "Skipping attribute \"{}\" since it is marked as ignored.", attribute.getPath());
          continue;
        }

        String definingPackageName =
            tagHelper.getOptionalTag(
                attribute, Tag.DEFINING_PACKAGE, attribute.getElement().getPackage().getName());
        boolean currentPackageTerm = diagram.getPackage().getName().equals(definingPackageName);
        boolean customURI = tagHelper.getOptionalTag(attribute, Tag.EXTERNAL_URI, null) != null;
        Scope scope = Scope.NOTHING;
        if (!customURI && currentPackageTerm) scope = Scope.FULL_DEFINITON;
        else if (customURI) {
          boolean customURIsamePrefix =
              StringUtils.startsWith(
                  tagHelper.getOptionalTag(attribute, Tag.EXTERNAL_URI, null), ontology.toString());
          if (currentPackageTerm) {
            scope = Scope.TRANSLATIONS_ONLY;
          }
          ;
          if (customURIsamePrefix) {
            scope = Scope.FULL_DEFINITON;
            LOGGER.warn(
                "Element {} has same prefix as package: simplify by removing the extra tag uri",
                attribute.getName());
          }
          ;
        }
        LOGGER.debug("Scope of covertion for attributes is \"{}\"", scope);

        convertAttribute(
            attribute, nameToElements, uris.elementURIs, uris.attributeURIs, ontology, scope);
      }
    }

    // Convert enum values
    for (DiagramElement diagramElement : diagram.getElements()) {
      // Skip enum attributes
      EAElement element = diagramElement.getReferencedElement();
      if (element.getType() != EAElement.Type.ENUMERATION) continue;

      // Skip if the element is set to ignore
      if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false"))) {
        // No need for logging, this was already mentioned when the element was skipped
        continue;
      }

      convertEnumerationValues(
          diagram.getPackage(), element, uris.elementURIs, uris.instanceURIs, ontology);
    }
  }

  /**
   * Creates a mapping of all connectors (including all normalized connectors when an association
   * class is present) in the given diagram to their label direction, or if that is not present, the
   * direction of the connection.
   */
  private Map<EAConnector, EAConnector.Direction> indexDirections(EADiagram diagram) {
    Set<DiagramConnector> connectors =
        diagram.getElements().stream()
            .flatMap(o -> o.getConnectors().stream())
            .collect(Collectors.toSet());

    ImmutableMap.Builder<EAConnector, EAConnector.Direction> builder2 = ImmutableMap.builder();
    Map<EAConnector, EAConnector.Direction> builder = new LinkedHashMap<>();
    for (DiagramConnector connector : connectors) {
      EAConnector.Direction direction = connector.getLabelDirection();
      if (direction == EAConnector.Direction.UNSPECIFIED)
        direction = connector.getReferencedConnector().getDirection();
      builder.put(connector.getReferencedConnector(), direction);

      if (connector.getReferencedConnector().getAssociationClass() != null) {
        for (EAConnector innerConnector :
            Util.extractAssociationElement2(
                connector.getReferencedConnector(), direction, tagHelper)) {
          builder.put(innerConnector, innerConnector.getDirection());
        }
      } else {
        if (direction == EAConnector.Direction.UNSPECIFIED
            || direction == EAConnector.Direction.BIDIRECTIONAL) {
          for (EAConnector innerConnector :
              Util.extractAssociationElement2(
                  connector.getReferencedConnector(), direction, tagHelper)) {
            if (builder.containsKey(innerConnector)) {
              LOGGER.warn(
                  "Connector {} without explicit direction already added to the set of directions",
                  innerConnector.getName());
            } else {
              builder.put(innerConnector, innerConnector.getDirection());
            }
            ;
          }
        }
      }
    }
    builder2.putAll(builder);
    return builder2.build();
  }

  private Resource convertPackage(
      EAPackage aPackage, Map<EAPackage, String> ontologyURIs, Map<EAPackage, String> baseURIs) {
    Resource ontology = ResourceFactory.createResource(ontologyURIs.get(aPackage));
    String prefix = tagHelper.getOptionalTag(aPackage, Tag.PACKAGE_BASE_URI_ABBREVIATION, null);
    String baseUri = baseURIs.get(aPackage);

    outputHandler.handleOntology(aPackage, ontology, prefix, baseUri);

    LOGGER.debug("Ontology {}", ontology);

    return ontology;
  }

  private void convertEnumerationValues(
      EAPackage activePackage,
      EAElement element,
      Map<EAElement, String> elementURIs,
      Map<EAAttribute, String> instanceURIs,
      Resource ontology) {
    Resource elementRes = ResourceFactory.createResource(elementURIs.get(element));
    List<? extends EAAttribute> attributes = element.getAttributes();

    for (EAAttribute attribute : attributes) {
      Resource attResource = ResourceFactory.createResource(instanceURIs.get(attribute));

      if (Boolean.valueOf(tagHelper.getOptionalTag(attribute, Tag.IGNORE, "false"))) {
        continue;
      }

      String definingPackageName =
          tagHelper.getOptionalTag(
              attribute, Tag.DEFINING_PACKAGE, attribute.getElement().getPackage().getName());
      boolean currentPackageTerm = activePackage.getName().equals(definingPackageName);
      boolean customURI = tagHelper.getOptionalTag(attribute, Tag.EXTERNAL_URI, null) != null;
      Scope scope = Scope.NOTHING;
      if (!customURI && currentPackageTerm) scope = Scope.FULL_DEFINITON;
      else if (customURI) {
        boolean customURIsamePrefix =
            StringUtils.startsWith(
                tagHelper.getOptionalTag(attribute, Tag.EXTERNAL_URI, null), ontology.toString());
        if (currentPackageTerm) {
          scope = Scope.TRANSLATIONS_ONLY;
        }
        ;
        if (customURIsamePrefix) {
          scope = Scope.FULL_DEFINITON;
          LOGGER.warn(
              "Element {} has same prefix as package: simplify by removing the extra tag uri",
              attribute.getName());
        }
        ;
      }

      outputHandler.handleInstance(attribute, attResource, scope, ontology, elementRes);
    }
  }

  private void convertAttribute(
      EAAttribute attribute,
      Multimap<String, EAElement> elementIndex,
      Map<EAElement, String> elementURIs,
      Map<EAAttribute, String> attributeURIs,
      Resource ontology,
      Scope scope) {
    LOGGER.debug("converting Attribute \"{}\".", attribute.getPath());
    if (!attributeURIs.containsKey(attribute)) return;

    Property attResource = ResourceFactory.createProperty(attributeURIs.get(attribute));

    Resource domain;
    Resource range = null;
    Resource propertyType;

    String customDomain = tagHelper.getOptionalTag(attribute, Tag.DOMAIN, null);
    if (customDomain == null) {
      domain = ResourceFactory.createResource(elementURIs.get(attribute.getElement()));
    } else {
      domain = ResourceFactory.createResource(customDomain);
    }

    String customRange = tagHelper.getOptionalTag(attribute, Tag.RANGE, null);
    RangeData rangedata = new RangeData();

    // Type and range
    if (customRange != null) {
      boolean rangeIsLiteral =
          Boolean.parseBoolean(tagHelper.getOptionalTag(attribute, Tag.IS_LITERAL, "false"));
      propertyType = rangeIsLiteral ? OWL.DatatypeProperty : OWL.ObjectProperty;
      range = ResourceFactory.createProperty(customRange);
      rangedata = new RangeData("", "", range);

    } else if (DATATYPES.containsKey(attribute.getType())) {
      propertyType = OWL.DatatypeProperty;
      range = DATATYPES.get(attribute.getType());
      rangedata = new RangeData(attribute.getType().toString(), "", range);
    } else if (elementIndex.containsKey(attribute.getType())) {
      Collection<EAElement> refElements = elementIndex.get(attribute.getType());
      if (refElements.size() > 1) {
        Iterable<String> names = Iterables.transform(refElements, EAObject::getPath);
        LOGGER.warn(
            "Ambiguous data type \"{}\" for attribute \"{}\": {}.",
            attribute.getType(),
            attribute.getPath(),
            Joiner.on(", ").join(names));
      }
      EAElement selectedElement = refElements.iterator().next();
      LOGGER.debug(
          "Attribute range is EA Element {} from type {} ",
          selectedElement.getName(),
          attribute.getType());
      LOGGER.debug("Attribute range package {} ", selectedElement.getPackage().getName());
      boolean isLiteral =
          Boolean.parseBoolean(tagHelper.getOptionalTag(selectedElement, Tag.IS_LITERAL, "false"));
      propertyType = isLiteral ? OWL.DatatypeProperty : OWL.ObjectProperty;
      range = ResourceFactory.createResource(elementURIs.get(selectedElement));
      rangedata =
          new RangeData(
              selectedElement.getName(),
              selectedElement.getPackage().getName(),
              range,
              selectedElement);
    } else {
      propertyType = RDF.Property;
      LOGGER.warn("Missing data type for attribute \"{}\".", attribute.getPath());
    }

    // Subproperty
    List<Resource> superProperties =
        attribute.getTags().stream()
            .filter(t -> tagHelper.getTagKey(Tag.SUBPROPERTY_OF).equals(t.getKey()))
            .map(
                tag ->
                    TagHelper.USE_NOTE_VALUE.equals(tag.getValue())
                        ? tag.getNotes()
                        : tag.getValue())
            .map(ResourceFactory::createResource)
            .collect(Collectors.toList());

    LOGGER.debug(
        "Attribute cardinality {} - {}", attribute.getLowerBound(), attribute.getUpperBound());

    outputHandler.handleProperty(
        OutputHandler.PropertySource.from(attribute),
        attResource,
        scope,
        scope == Scope.FULL_DEFINITON
            ? PackageExported.ACTIVE_PACKAGE
            : PackageExported.OTHER_PACKAGE,
        ontology,
        propertyType,
        domain,
        range,
        rangedata,
        attribute.getLowerBound(),
        attribute.getUpperBound(),
        superProperties);
  }

  // process a single a Diagramconnector by determining its derived connectors:
  /*
   * The processing of EA connectors is a complex process as
   *    - One connector might represent several RDF properties
   *    - The uris of those derived properties depend on the newly contructed derived context
   * Therefore the process is as follows:
   *   identify the case
   *   generate the derived connectors, if necessary. (*)
   *   determin the URI of the connector and process it
   * (*) Note softwarewise the same interface for the connectors is being used, making the serializers
   *     think that it are all simple EA Connectors which map 1 to 1 to a RDF property
   *
   * (*) This complex derivation case breaks the assumption that correctors can be simple mapped upfront to a URI.
   */
  private void convertConnector2(
      DiagramConnector dconnector,
      Map<EAConnector, EAConnector.Direction> directions,
      Map<EAElement, String> elementURIs,
      Map<EAConnector, String> connectorURIs, // obsolete
      Map<EAConnector, EAPackage> definingPackages, // obsolete
      Multimap<String, EAPackage> nameToPackages,
      Map<EAPackage, String> packageURIs,
      Resource ontology,
      EAPackage convertedPackage) {

    EAConnector bareConnector = dconnector.getReferencedConnector();
    UriAssigner UA = new UriAssigner(tagHelper);
    if (bareConnector.getAssociationClass() != null) {
      // connector with AssociationClass
      convertConnector3(
          dconnector,
          directions,
          elementURIs,
          connectorURIs,
          definingPackages,
          nameToPackages,
          packageURIs,
          ontology,
          convertedPackage);
      /*
              EAConnector.Direction rawDirection = directions.getOrDefault(bareConnector, EAConnector.Direction.UNSPECIFIED);
              for (EAConnector connector : Util.extractAssociationElement2(bareConnector, rawDirection)) {
      	    UriAssigner.ConnectorURI c = UA.assignConnectorURI(false, connector, null, nameToPackages, packageURIs);
      	    if (c != null) {
                     LOGGER.debug("calculated uri for connector \"{}\" is {}", connector.getPath(), c.curi );
      	    } else {
                     LOGGER.debug("calculated uri for connector \"{}\" not found ", connector.getPath());
      	    };
                  // URI calculation works
                  // need to know the defining package which is also part from the URI calculation
                  convertConnector_base(true, dconnector, connector, c, directions, elementURIs, connectorURIs, definingPackages, ontology, convertedPackage);
      	}
      */
    } else {
      // if has direction -> export direction
      // if has role -> export Role
      // Note if has both then both will happen see case 02bTweede
      EAConnector.Direction rawDirection =
          directions.getOrDefault(bareConnector, EAConnector.Direction.UNSPECIFIED);
      if (rawDirection == EAConnector.Direction.SOURCE_TO_DEST) {
        // simple directed connector
        LOGGER.debug("directed Connector \"{}\" SOURCE_TO_DEST ", bareConnector.getPath());
        UriAssigner.ConnectorURI c =
            UA.assignConnectorURI(false, bareConnector, null, "", nameToPackages, packageURIs);
        convertConnector_base(
            false,
            dconnector,
            bareConnector,
            c,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      } else {
        if (rawDirection == EAConnector.Direction.DEST_TO_SOURCE) {
          // simple directed connector
          LOGGER.debug("directed Connector \"{}\" DEST_TO_SOURCE", bareConnector.getPath());
          UriAssigner.ConnectorURI c =
              UA.assignConnectorURI(false, bareConnector, null, "", nameToPackages, packageURIs);
          convertConnector_base(
              false,
              dconnector,
              bareConnector,
              c,
              directions,
              elementURIs,
              connectorURIs,
              definingPackages,
              ontology,
              convertedPackage);
        }
      }
      if (bareConnector.getSourceRole() != null && bareConnector.getSourceRole() != "") {
        LOGGER.debug("undirected Connector \"{}\" DEST_TO_SOURCE ", bareConnector.getPath());
        RoleEAConnector roleConnector =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.DEST_TO_SOURCE, tagHelper);
        UriAssigner.ConnectorURI c =
            UA.assignConnectorURI(false, roleConnector, null, "", nameToPackages, packageURIs);
        convertConnector_base(
            false,
            dconnector,
            roleConnector,
            c,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
      if (bareConnector.getDestRole() != null && bareConnector.getDestRole() != "") {
        // not directed connector => both directions are created
        LOGGER.debug("undirected Connector \"{}\" SOURCE_TO_DEST ", bareConnector.getPath());
        RoleEAConnector roleConnector =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.SOURCE_TO_DEST, tagHelper);
        UriAssigner.ConnectorURI c =
            UA.assignConnectorURI(false, roleConnector, null, "", nameToPackages, packageURIs);
        convertConnector_base(
            false,
            dconnector,
            roleConnector,
            c,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
      if ((bareConnector.getDestRole() == null || bareConnector.getDestRole() == "")
          && (bareConnector.getSourceRole() == null || bareConnector.getSourceRole() == "")
          && (rawDirection == EAConnector.Direction.UNSPECIFIED)) {

        String destName = bareConnector.getDestination().getName();
        String sourceName = bareConnector.getSource().getName();
        String destDis = "";
        String sourceDis = "";
        if (destName == sourceName) {
          destName = destName + ".target";
          sourceName = sourceName + ".source";
          destDis = "target";
          sourceDis = "source";
        }
        ;
        RoleEAConnector roleConnector1 =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.UNSPEC_DEST_TO_SOURCE, tagHelper);
        RoleEAConnector roleConnector2 =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.UNSPEC_SOURCE_TO_DEST, tagHelper);
        UriAssigner.ConnectorURI c1 =
            UA.assignConnectorURI(
                false,
                roleConnector1,
                roleConnector1.getSource(),
                destDis,
                nameToPackages,
                packageURIs);
        UriAssigner.ConnectorURI c2 =
            UA.assignConnectorURI(
                false,
                roleConnector2,
                roleConnector2.getSource(),
                sourceDis,
                nameToPackages,
                packageURIs);
        convertConnector_base(
            true,
            dconnector,
            roleConnector1,
            c1,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
        convertConnector_base(
            true,
            dconnector,
            roleConnector2,
            c2,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
    }
  }

  // separate processing for connectors with association classes attached
  private void convertConnector3(
      DiagramConnector dconnector,
      Map<EAConnector, EAConnector.Direction> directions,
      Map<EAElement, String> elementURIs,
      Map<EAConnector, String> connectorURIs, // obsolete
      Map<EAConnector, EAPackage> definingPackages, // obsolete
      Multimap<String, EAPackage> nameToPackages,
      Map<EAPackage, String> packageURIs,
      Resource ontology,
      EAPackage convertedPackage) {

    EAConnector bareConnector = dconnector.getReferencedConnector();
    UriAssigner UA = new UriAssigner(tagHelper);
    EAConnector.Direction rawDirection =
        directions.getOrDefault(bareConnector, EAConnector.Direction.UNSPECIFIED);

    if (Util.connectorHasOldAssociationClassTags(bareConnector)) {
      // handling association classes with old definition has priority
      LOGGER.debug("0) add connectors based on deprecated tags for {}", bareConnector.getPath());
      for (EAConnector connector : Util.extractAssociationElement(bareConnector, rawDirection)) {
        UriAssigner.ConnectorURI c =
            UA.assignConnectorURI(false, connector, null, "", nameToPackages, packageURIs);
        if (c != null) {
          LOGGER.debug("calculated uri for connector \"{}\" is {}", connector.getPath(), c.curi);
        } else {
          LOGGER.debug("calculated uri for connector \"{}\" not found ", connector.getPath());
        }
        ;
        // URI calculation works
        // need to know the defining package which is also part from the URI calculation
        convertConnector_base(
            true,
            dconnector,
            connector,
            c,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
    } else {
      if ((bareConnector.getDestRole() == null || bareConnector.getDestRole() == "")
          && (bareConnector.getSourceRole() == null || bareConnector.getSourceRole() == "")
          && (rawDirection != EAConnector.Direction.UNSPECIFIED)) {

        LOGGER.debug("0) add AssocFree connector {}", bareConnector.getPath());
        AssocFreeEAConnector aconn = new AssocFreeEAConnector(bareConnector);
        UriAssigner.ConnectorURI c =
            UA.assignConnectorURI(true, aconn, null, "", nameToPackages, packageURIs);
        convertConnector_base(
            true,
            dconnector,
            aconn,
            c,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
      ;
      if (bareConnector.getSourceRole() != null && bareConnector.getSourceRole() != "") {
        LOGGER.debug("1) add Role connector {}", bareConnector.getPath());
        RoleEAConnector roleConnector =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.DEST_TO_SOURCE, tagHelper);
        UriAssigner.ConnectorURI c =
            UA.assignConnectorURI(false, roleConnector, null, "", nameToPackages, packageURIs);
        convertConnector_base(
            true,
            dconnector,
            roleConnector,
            c,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
      ;
      if (bareConnector.getDestRole() != null && bareConnector.getDestRole() != "") {
        LOGGER.debug("2) add Role connector {}", bareConnector.getPath());
        RoleEAConnector roleConnector =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.SOURCE_TO_DEST, tagHelper);
        UriAssigner.ConnectorURI c =
            UA.assignConnectorURI(false, roleConnector, null, "", nameToPackages, packageURIs);
        convertConnector_base(
            true,
            dconnector,
            roleConnector,
            c,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
      ;
      if ((bareConnector.getDestRole() == null || bareConnector.getDestRole() == "")
          && (bareConnector.getSourceRole() == null || bareConnector.getSourceRole() == "")
          && (rawDirection == EAConnector.Direction.UNSPECIFIED)) {
        LOGGER.debug("3) add Role connector {}", bareConnector.getPath());
        String destName = bareConnector.getDestination().getName();
        String sourceName = bareConnector.getSource().getName();
        String destDis = "";
        String sourceDis = "";
        // collect the labels to extend
        String cname = tagHelper.getOptionalTag(bareConnector, "label-nl", "");
        String cnameap = tagHelper.getOptionalTag(bareConnector, "ap-label-nl", "");
        LOGGER.debug("role labels {} - {} ", cname, cnameap);
        String dcname = "";
        String dcnameap = "";
        String scname = "";
        String scnameap = "";

        if (destName == sourceName) {
          destName = destName + ".target";
          sourceName = sourceName + ".source";
          destDis = "target";
          sourceDis = "source";
          dcname = cname + " (target)";
          scname = cname + " (source)";
          if (cnameap != "") dcnameap = cnameap + " (target)";
          if (cnameap != "") scnameap = cnameap + " (source)";
        }
        ;
        LOGGER.debug("disambiguated role labels {} - {} ", dcname, dcnameap);

        EATag dcnameTag = new MemoryEATag("label-nl", dcname, "");
        EATag scnameTag = new MemoryEATag("label-nl", scname, "");
        EATag dcnameapTag = null;
        EATag scnameapTag = null;
        if (dcnameap != "") dcnameapTag = new MemoryEATag("ap-label-nl", dcnameap, "");
        if (scnameap != "") scnameapTag = new MemoryEATag("ap-label-nl", scnameap, "");
        List<EATag> dresult = new ArrayList<>();
        List<EATag> sresult = new ArrayList<>();
        if (dcnameap != "") dresult.add(dcnameapTag);
        dresult.add(dcnameTag);
        if (scnameap != "") sresult.add(scnameapTag);
        sresult.add(scnameTag);

        RoleEAConnector roleConnector1 =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.UNSPEC_SOURCE_TO_DEST, tagHelper);
        RoleEAConnector roleConnector2 =
            new RoleEAConnector(
                bareConnector, RoleEAConnector.ConnectionPart.UNSPEC_DEST_TO_SOURCE, tagHelper);
        UriAssigner.ConnectorURI c1 =
            UA.assignConnectorURI(
                false,
                roleConnector1,
                roleConnector1.getSource(),
                sourceDis,
                nameToPackages,
                packageURIs);
        convertConnector_base(
            true,
            dconnector,
            roleConnector1,
            c1,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
        UriAssigner.ConnectorURI c2 =
            UA.assignConnectorURI(
                false,
                roleConnector2,
                roleConnector2.getSource(),
                destDis,
                nameToPackages,
                packageURIs);
        convertConnector_base(
            true,
            dconnector,
            roleConnector2,
            c2,
            directions,
            elementURIs,
            connectorURIs,
            definingPackages,
            ontology,
            convertedPackage);
      }
      ;
      EAElement assocClass = bareConnector.getAssociationClass();
      LOGGER.debug("5) add AssocationClass connectors {}", assocClass.getName());

      String destName = bareConnector.getDestination().getName();
      String sourceName = bareConnector.getSource().getName();
      String destDis = "";
      String sourceDis = "";
      if (destName == sourceName) {
        destName = destName + ".target";
        sourceName = sourceName + ".source";
        destDis = "target";
        sourceDis = "source";
      }
      ;

      // TODO support tags name-source-class name-target-class for disambiguation
      AssociationEAConnector assocConnector1 =
          new AssociationEAConnector(
              bareConnector,
              assocClass,
              bareConnector.getDestination(),
              destDis,
              bareConnector.getDestinationCardinality(),
              "1",
              tagHelper);
      AssociationEAConnector assocConnector2 =
          new AssociationEAConnector(
              bareConnector,
              assocClass,
              bareConnector.getSource(),
              sourceDis,
              bareConnector.getSourceCardinality(),
              "1",
              tagHelper);
      UriAssigner.ConnectorURI c1 =
          UA.assignConnectorURI(
              false,
              assocConnector1,
              assocConnector1.getSource(),
              destDis,
              nameToPackages,
              packageURIs);
      convertConnector_base(
          true,
          dconnector,
          assocConnector1,
          c1,
          directions,
          elementURIs,
          connectorURIs,
          definingPackages,
          ontology,
          convertedPackage);
      UriAssigner.ConnectorURI c2 =
          UA.assignConnectorURI(
              false,
              assocConnector2,
              assocConnector2.getSource(),
              sourceDis,
              nameToPackages,
              packageURIs);
      convertConnector_base(
          true,
          dconnector,
          assocConnector2,
          c2,
          directions,
          elementURIs,
          connectorURIs,
          definingPackages,
          ontology,
          convertedPackage);
    }
    ;
  }

  // process a single combination of a Diagramconnector and its derived connector
  // the connector here should be a directed connector with all tags at the right place
  private void convertConnector_base(
      Boolean derived,
      DiagramConnector dconnector,
      EAConnector connector,
      UriAssigner.ConnectorURI dconnectorUri,
      Map<EAConnector, EAConnector.Direction> directions,
      Map<EAElement, String> elementURIs,
      Map<EAConnector, String> connectorURIs, // obsolete
      Map<EAConnector, EAPackage> definingPackages, // obsolete
      Resource ontology,
      EAPackage convertedPackage) {
    LOGGER.debug("initiating conversion Connector \"{}\" in a directed form.", connector.getPath());

    if (connector.getAssociationClass() != null)
      throw new AssertionError("Association class should not be present.");

    //        if (connectorURIs.containsKey(connector)) {
    if (dconnectorUri != null) {
      // must have a URI

      if (Arrays.asList(EAConnector.TYPE_ASSOCIATION, EAConnector.TYPE_AGGREGATION)
          .contains(connector.getType())) {
        // must not be a is-a or aggregation type

        LOGGER.debug("Connector \"{}\" is processed.", connector.getPath());

        //                Resource connResource =
        // ResourceFactory.createResource(connectorURIs.get(connector));
        Resource connResource = ResourceFactory.createResource(dconnectorUri.curi);

        // source and target are the two ends of the connector
        // which element is source or target is determined by the drawing order
        EAElement source = connector.getSource();
        EAElement target = connector.getDestination();
        Resource sourceRes = ResourceFactory.createResource(elementURIs.get(source));
        Resource targetRes = ResourceFactory.createResource(elementURIs.get(target));

        // Subproperties
        List<Resource> superProperties =
            connector.getTags().stream()
                .filter(t -> tagHelper.getTagKey(Tag.SUBPROPERTY_OF).equals(t.getKey()))
                .map(
                    tag ->
                        TagHelper.USE_NOTE_VALUE.equals(tag.getValue())
                            ? tag.getNotes()
                            : tag.getValue())
                .map(ResourceFactory::createResource)
                .collect(Collectors.toList());

        Resource domain = null;
        Resource range = null;

        String customDomain = tagHelper.getOptionalTag(connector, Tag.DOMAIN, null);
        String customRange = tagHelper.getOptionalTag(connector, Tag.RANGE, null);

        String cardinality = null;
        boolean rangeIsLiteral = false;
        RangeData rangedata = new RangeData();

        // Range, domain & cardinality
        domain = sourceRes;
        range = targetRes;
        cardinality = connector.getDestinationCardinality();
        rangeIsLiteral =
            Boolean.parseBoolean(tagHelper.getOptionalTag(target, Tag.IS_LITERAL, "false"));
        rangedata = new RangeData(target.getName(), target.getPackage().getName(), range, target);

        if (customDomain != null) {
          domain = ResourceFactory.createResource(customDomain);
          LOGGER.warn(
              "Connector {} overwrites domain with custom domain {} ", connector.getPath(), domain);
        }
        ;
        if (customRange != null) {
          range = ResourceFactory.createResource(customRange);
          LOGGER.warn(
              "Connector {} overwrites range with custom range {} ", connector.getPath(), range);
        }
        ;

        // split cardinality
        String lowerCardinality = null;
        String higherCardinality = null;

        if (cardinality != null && cardinality.contains("..")) {
          String[] parts = cardinality.split("\\.\\.");
          lowerCardinality = parts[0];
          higherCardinality = parts[1];
        } else if (cardinality != null) {
          lowerCardinality = cardinality;
          higherCardinality = cardinality;
        }

        //                EAPackage definingPackage = definingPackages.get(connector);
        EAPackage definingPackage = dconnectorUri.cpackage;
        PackageExported packageExported;
        if (definingPackage == null) {
          packageExported = PackageExported.UNKNOWN;
          LOGGER.warn("Package for connector {} is unknown", connector.getPath());
        } else if (convertedPackage.equals(definingPackage))
          packageExported = PackageExported.ACTIVE_PACKAGE;
        else packageExported = PackageExported.OTHER_PACKAGE;

        boolean externalTerm = tagHelper.getOptionalTag(connector, Tag.EXTERNAL_URI, null) != null;
        Scope scope = Scope.NOTHING;
        if (!externalTerm && packageExported == PackageExported.ACTIVE_PACKAGE)
          scope = Scope.FULL_DEFINITON;
        else if (externalTerm && packageExported == PackageExported.ACTIVE_PACKAGE)
          scope = Scope.TRANSLATIONS_ONLY;
        LOGGER.debug("Scope of covertion for connector {} is \"{}\"", connector.getPath(), scope);

        outputHandler.handlePropertyConnector(
            derived,
            connector,
            connResource,
            scope,
            packageExported,
            ontology,
            rangeIsLiteral ? OWL.DatatypeProperty : OWL.ObjectProperty,
            domain,
            range,
            rangedata,
            lowerCardinality,
            higherCardinality,
            superProperties);
      } else {
        LOGGER.error("Unsupported connector type for \"{}\" - skipping.", connector.getPath());
      }
    } else {
      LOGGER.debug("connector \"{}\" has no uri - skipping.", connector.getPath());
      /*
      		for ( EAConnector k : connectorURIs.keySet()) {
      			LOGGER.error("contains key {}", k.getPath());
      		};
                      LOGGER.error("connector \"{}\" has no uri - skipping.", connector.getPath());
      */
    }
  }

  /* deprecated
      private void convertConnector(DiagramConnector dconnector, EAConnector bareConnector, Map<EAConnector, EAConnector.Direction> directions,
                                    Map<EAElement, String> elementURIs, Map<EAConnector, String> connectorURIs,
                                    Map<EAConnector, EAPackage> definingPackages, Resource ontology, EAPackage convertedPackage) {
          LOGGER.debug("converting Connector \"{}\".", bareConnector.getPath());
          EAConnector.Direction rawDirection = directions.getOrDefault(bareConnector, EAConnector.Direction.UNSPECIFIED);
          for (EAConnector connector : Util.extractAssociationElement2(bareConnector, rawDirection)) {
              if (connector.getAssociationClass() != null)
                  throw new AssertionError("Association class should not be present.");

              LOGGER.debug("Connector \"{}\" trying for processing.", connector.getPath());
              if (!connectorURIs.containsKey(connector))
                  continue;
              LOGGER.debug("Connector \"{}\" is processed.", connector.getPath());

              Resource connResource = ResourceFactory.createResource(connectorURIs.get(connector));

              EAElement source = connector.getSource();
              EAElement target = connector.getDestination();
              Resource sourceRes = ResourceFactory.createResource(elementURIs.get(source));
              Resource targetRes = ResourceFactory.createResource(elementURIs.get(target));


              if (Arrays.asList(EAConnector.TYPE_ASSOCIATION, EAConnector.TYPE_AGGREGATION).contains(connector.getType())) {
                  // Subproperty
                  List<Resource> superProperties = connector.getTags()
                          .stream()
                          .filter(t -> tagHelper.getTagKey(Tag.SUBPROPERTY_OF).equals(t.getKey()))
                          .map(tag -> TagHelper.USE_NOTE_VALUE.equals(tag.getValue()) ? tag.getNotes() : tag.getValue())
                          .map(ResourceFactory::createResource)
                          .collect(Collectors.toList());

                  Resource domain = null;
                  Resource range = null;

                  String customDomain = tagHelper.getOptionalTag(connector, Tag.DOMAIN, null);
                  String customRange = tagHelper.getOptionalTag(connector, Tag.RANGE, null);

                  String cardinality = null;
                  boolean rangeIsLiteral = false;
   		RangeData rangedata = new RangeData();

                  // Range, domain & cardinality
                  EAConnector.Direction connectorDirection = directions.getOrDefault(connector, EAConnector.Direction.UNSPECIFIED);
                  LOGGER.debug("Connector direction \"{}\" is processed.", connectorDirection);

                  if (connectorDirection == EAConnector.Direction.SOURCE_TO_DEST) {
                      domain = sourceRes;
                      range = targetRes;
                      cardinality = connector.getDestinationCardinality();
                      rangeIsLiteral = Boolean.parseBoolean(tagHelper.getOptionalTag(target, Tag.IS_LITERAL, "false"));
   	    	    rangedata = new RangeData(target.getName(), target.getPackage().getName(), range, target);
                  } else if (connectorDirection == EAConnector.Direction.DEST_TO_SOURCE) {
                      domain = targetRes;
                      range = sourceRes;
                      cardinality = connector.getSourceCardinality();
                      rangeIsLiteral = Boolean.parseBoolean(tagHelper.getOptionalTag(target, Tag.IS_LITERAL, "false")); // XXX is this not a BUG? SHOULD target NOT BE source?
   	    	    rangedata = new RangeData(source.getName(), source.getPackage().getName(), range, source);
                  } else {
                      LOGGER.error("Connector \"{}\" has no specified direction - domain/range unspecified.", connector.getPath());
                  }

                  LOGGER.debug("Connector cardinality {}", cardinality);
                  if (customDomain != null)
                      domain = ResourceFactory.createResource(customDomain);
                  if (customRange != null)
                      range = ResourceFactory.createResource(customRange);

                  String lowerCardinality = null;
                  String higherCardinality = null;

                  if (cardinality != null && cardinality.contains("..")) {
                      String[] parts = cardinality.split("\\.\\.");
                      lowerCardinality = parts[0];
                      higherCardinality = parts[1];
                  } else if (cardinality != null) {
                      lowerCardinality = cardinality;
                      higherCardinality = cardinality;
                  }

                  EAPackage definingPackage = definingPackages.get(connector);
                  PackageExported packageExported;
                  if (definingPackage == null) {
                      packageExported = PackageExported.UNKNOWN;
                      LOGGER.info("Package for connector {} is unknown", connector.getPath());
                  } else if (convertedPackage.equals(definingPackage))
                      packageExported = PackageExported.ACTIVE_PACKAGE;
                  else
                      packageExported = PackageExported.OTHER_PACKAGE;

                  boolean externalTerm = tagHelper.getOptionalTag(connector, Tag.EXTERNAL_URI, null) != null;
                  Scope scope = Scope.NOTHING;
                  if (!externalTerm && packageExported == PackageExported.ACTIVE_PACKAGE)
                      scope = Scope.FULL_DEFINITON;
                  else if (externalTerm && packageExported == PackageExported.ACTIVE_PACKAGE)
                      scope = Scope.TRANSLATIONS_ONLY;
                 LOGGER.debug("Scope of covertion for connector {} is \"{}\"", connector.getPath(), scope);


                  outputHandler.handleProperty(
                  	OutputHandler.PropertySource.from(connector),
                          connResource,
                          scope,
                          packageExported,
                          ontology,
                          rangeIsLiteral ? OWL.DatatypeProperty : OWL.ObjectProperty,
                          domain,
                          range,
  			rangedata,
                          lowerCardinality,
                          higherCardinality,
                          superProperties);
              } else {
                  LOGGER.error("Unsupported connector type for \"{}\" - skipping.", connector.getPath());
              }
          }
      }
  */

  private void convertElement(
      DiagramElement diagramElement,
      Map<EAElement, String> elementURIs,
      Map<EAAttribute, String> instanceURIs,
      Resource ontology,
      Scope scope) {
    EAElement element = diagramElement.getReferencedElement();
    Resource classEntity = ResourceFactory.createResource(elementURIs.get(element));

    LOGGER.debug("converting element \"{}\".", element.getPath());

    List<Resource> allowedValues = null;
    if (element.getType().equals(EAElement.Type.ENUMERATION)) {
      List<? extends EAAttribute> attributes = element.getAttributes();
      allowedValues =
          Lists.transform(attributes, a -> ResourceFactory.createResource(instanceURIs.get(a)));
      if (allowedValues.isEmpty())
        LOGGER.warn("No possible values defined for enumeration \"{}\".", element.getPath());
    }

    List<Resource> parentClasses = new ArrayList<>();
    List<EAElement> parentElements = new ArrayList<>();

    for (EAConnector connector : element.getConnectors()) {
      if (!EAConnector.TYPE_GENERALIZATION.equals(connector.getType())) continue;

      if (connector.getDirection() == EAConnector.Direction.SOURCE_TO_DEST) {
        if (connector.getSource().equals(element)) {
          parentClasses.add(
              ResourceFactory.createResource(elementURIs.get(connector.getDestination())));
          parentElements.add(connector.getDestination());
        }
        ;
      } else if (connector.getDirection() == EAConnector.Direction.DEST_TO_SOURCE) {
        if (connector.getDestination().equals(element)) {
          parentClasses.add(ResourceFactory.createResource(elementURIs.get(connector.getSource())));
          parentElements.add(connector.getSource());
        }
        ;
      } else {
        LOGGER.error(
            "Generalization connector \"{}\" does not specify a direction - skipping.",
            connector.getPath());
      }
    }

    outputHandler.handleClass(
        element,
        classEntity,
        scope,
        ontology,
        parentClasses,
        parentElements,
        elementURIs,
        allowedValues);
  }
}

package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EADiagram;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EARepository;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import javafx.geometry.Rectangle2D;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A builder for an {@link EARepository}. This builder creates a complete in-memory version of the content of an
 * Enterprise Architect project.
 *
 * @author Dieter De Paepe
 */
public class MemoryRepositoryBuilder {
    /**
     * Creates a new memory-based repository from the given Enterprise Architect project file. After calling this
     * method, the builder should not be reused.
     *
     * @param eaFile the file
     * @return a new repository
     * @throws SQLException
     */
    public EARepository build(File eaFile) throws SQLException {
        Properties prop = new Properties();
        prop.setProperty("jackcessopener", EAPJackcessOpener.class.getName());

        String uri = "jdbc:ucanaccess://" + eaFile.getAbsolutePath();

        Map<Integer, MemoryEAElement> elements; //Key: object id
        Map<Integer, MemoryEAPackage> packages; //Key: package id
        Map<Integer, MemoryEADiagram> diagrams; //Key: diagram id
        Map<Integer, MemoryEAConnector> connectors; //Key: connector id
        Map<Integer, MemoryEAAttribute> attributes; //Key: attribute id

        try (Connection conn = DriverManager.getConnection(uri, prop)){
            packages = loadPackages(conn);
            elements = loadElements(conn, packages);
            attributes = loadAttributes(conn, elements);
            connectors = loadElementConnectors(conn, elements);
            loadObjectTags(conn, elements, packages);
            loadAttributeTags(conn, attributes);
            loadConnectorTags(conn, connectors);
            diagrams = loadDiagrams(conn, packages);
            loadDiagramObjects(conn, elements, packages, diagrams);
            loadDiagramConnectors(conn, diagrams, connectors);
        }

        Optional<MemoryEAPackage> rootPackage = Iterables.tryFind(packages.values(), p -> p.getParent() == null);
        if (!rootPackage.isPresent())
            throw new IllegalStateException("Did not find a root package.");

        return new EARepository(
                rootPackage.get(),
                Collections.unmodifiableList(new ArrayList<>(packages.values())),
                Collections.unmodifiableList(new ArrayList<>(elements.values())),
                Collections.unmodifiableList(new ArrayList<>(diagrams.values())));
    }

    private Map<Integer, MemoryEAConnector> loadElementConnectors(Connection connection, Map<Integer, MemoryEAElement> elements) throws SQLException {
        Map<Integer, MemoryEAConnector> connectors = new HashMap<>();

        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery(
                    "SELECT c.Connector_ID, c.Name, c.Direction, c.Notes, c.Connector_Type, c.SourceRole, c.DestRole, c.Start_Object_ID, c.End_Object_ID, c.PDATA1, c.ea_guid " +
                    "FROM ((t_connector AS c " +
                    "INNER JOIN t_object AS src ON c.Start_Object_ID = src.Object_ID) " +
                    "INNER JOIN t_object AS dest ON c.End_Object_ID = dest.Object_ID) " +
                    "WHERE src.Object_Type IN ('Class', 'DataType', 'Enumeration') " +
                    "AND dest.Object_Type IN ('Class', 'DataType', 'Enumeration')");

            while (rs.next()) {
                int connectorId = rs.getInt("Connector_ID");
                String name = rs.getString("Name");
                String direction = rs.getString("Direction");
                String notes = rs.getString("Notes");
                String connectorType = rs.getString("Connector_Type");
                String sourceRole = rs.getString("SourceRole");
                String destRole = rs.getString("DestRole");
                int startObjectId = rs.getInt("Start_Object_ID");
                int endObjectId = rs.getInt("End_Object_ID");
                String associationClassId = rs.getString("PDATA1");
                String guid = rs.getString("ea_guid");

                MemoryEAElement source = elements.get(startObjectId);
                MemoryEAElement destination = elements.get(endObjectId);
                MemoryEAElement associationClass = null;

                if (!Strings.isNullOrEmpty(associationClassId))
                    associationClass = elements.get(Integer.parseUnsignedInt(associationClassId));

                MemoryEAConnector memoryEAConnector = new MemoryEAConnector(
                        connectorId, name, EAConnector.Direction.parse(direction),
                        notes, connectorType, sourceRole,
                        destRole, source, destination, associationClass, guid);

                source.getConnectorsOrig().add(memoryEAConnector);
                if (source != destination)
                    destination.getConnectorsOrig().add(memoryEAConnector);

                connectors.put(connectorId, memoryEAConnector);
            }
        }
        return connectors;
    }

    /**
     * Assumes elements and packages are loaded.
     * 
     * @throws SQLException
     */
    private void loadDiagramObjects(Connection connection, Map<Integer, MemoryEAElement> elements, Map<Integer, MemoryEAPackage> packages, Map<Integer, MemoryEADiagram> diagrams) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery(
                    "SELECT Diagram_ID, Object_ID, RectTop, RectLeft, RectRight, RectBottom " +
                    "FROM t_diagramobjects INNER JOIN t_object ON t_diagramobjects.Object_ID = t_object.Object_ID " +
                    "WHERE t_object.Object_Type IN ('Class', 'DataType', 'Enumeration')");

            while (rs.next()) {
                int diagramId = rs.getInt("Diagram_ID");
                int objectId = rs.getInt("Object_ID");
                int top = rs.getInt("RectTop");
                int left = rs.getInt("RectLeft");
                int right = rs.getInt("RectRight");
                int bottom = rs.getInt("RectBottom");

                MemoryDiagramElement diagramClass = new MemoryDiagramElement(
                        diagrams.get(diagramId),
                        elements.get(objectId),
                        new Rectangle2D(left, bottom, right - left, top - bottom)
                );

                diagrams.get(diagramId).getClassesOrig().add(diagramClass);
            }
        }
    }

    /**
     * Assumes packages are fully loaded.
     *
     * @throws SQLException
     */
    private Map<Integer, MemoryEADiagram> loadDiagrams(Connection connection, Map<Integer, MemoryEAPackage> packages) throws SQLException {
        Map<Integer, MemoryEADiagram> diagrams = new LinkedHashMap<>();

        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT Diagram_ID, Package_ID, Name, Notes, ea_guid FROM t_diagram");

            while (rs.next()) {
                int diagramId = rs.getInt("Diagram_ID");
                int packageId = rs.getInt("Package_ID");
                String name = rs.getString("Name");
                String notes = rs.getString("Notes");
                String guid = rs.getString("ea_guid");

                MemoryEAPackage containingPackage = packages.get(packageId);
                MemoryEADiagram diagram = new MemoryEADiagram(diagramId, name, guid, notes, containingPackage, new ArrayList<>());
                containingPackage.getDiagramsOrig().add(diagram);

                diagrams.put(diagramId, diagram);
            }
        }
        return diagrams;
    }

    private Map<Integer, MemoryEAPackage> loadPackages(Connection connection) throws SQLException {
        Map<Integer, MemoryEAPackage> packages = new LinkedHashMap<>();

        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery(
                    "SELECT t_package.Package_ID, Name, Parent_ID, ea_guid, Object_ID, Stereotype, Note " +
                    "FROM t_package LEFT JOIN t_object ON t_package.ea_guid = t_object.ea_guid");

            while (rs.next()) {
                int packageId = rs.getInt("Package_ID");
                String name = rs.getString("Name");
                int parentId = rs.getInt("Parent_ID");
                String guid = rs.getString("ea_guid");
                int objectId = rs.getInt("Object_ID");
                String stereotype = rs.getString("Stereotype");
                String note = rs.getString("Note");

                MemoryEAPackage newPackage = new MemoryEAPackage(name, guid, stereotype, note, objectId, packageId);

                if (parentId != 0) {
                    // This is not the root package
                    packages.get(parentId).getPackagesOrig().add(newPackage);
                    newPackage.setParent(packages.get(parentId));
                }

                packages.put(packageId, newPackage);
            }
        }
        return packages;
    }

    /**
     * Assumes packages are fully loaded.
     *
     * @throws SQLException
     */
    private Map<Integer, MemoryEAElement> loadElements(Connection connection, Map<Integer, MemoryEAPackage> packages) throws SQLException {
        Map<Integer, MemoryEAElement> elements = new LinkedHashMap<>();

        try (Statement s = connection.createStatement()){
            ResultSet rs = s.executeQuery("SELECT Object_ID, Object_Type, Name, Note, Package_ID, Stereotype, ea_guid FROM t_object WHERE Object_Type IN ('Class', 'Enumeration', 'DataType')");

            while (rs.next()) {
                int id = rs.getInt("Object_ID");
                String name = rs.getString("Name");
                String type = rs.getString("Object_Type");
                String notes = rs.getString("Note");
                int packageID = rs.getInt("Package_ID");
                // Note: elements can have multiple stereotypes, see "skos" example in ShapeChange - unsure where other stereotypes are saved.
                String stereotype = rs.getString("Stereotype");
                String guid = rs.getString("ea_guid");

                MemoryEAElement newClass = new MemoryEAElement(id, name, notes, guid, stereotype, EAElement.Type.parse(type), packages.get(packageID));

                elements.put(newClass.getObjectID(), newClass);
                packages.get(packageID).getElementsOrig().add(newClass);
            }
        }

        return elements;
    }

    /**
     * Assumes elements are fully loaded.
     *
     * @throws SQLException
     */
    private Map<Integer, MemoryEAAttribute> loadAttributes(Connection connection, Map<Integer, MemoryEAElement> elements) throws SQLException {
        Map<Integer, MemoryEAAttribute> attributes = new HashMap<>();

        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT ea_guid, ID, Object_ID, Name, Type, Notes FROM t_attribute");

            while (rs.next()) {
                String guid = rs.getString("ea_guid");
                int id = rs.getInt("ID");
                int objectID = rs.getInt("Object_ID");
                String name = rs.getString("Name");
                String type = rs.getString("Type");
                String notes = rs.getString("Notes");

                MemoryEAElement element = elements.get(objectID);
                MemoryEAAttribute att = new MemoryEAAttribute(element, guid, name, notes, type, id);
                element.getAttributesOrig().add(att);
                attributes.put(id, att);
            }
        }
        return attributes;
    }

    /**
     * Loads tags of packages and elements. Assumes elements and packages are fully loaded.
     *
     * @throws SQLException
     */
    private void loadObjectTags(Connection connection, Map<Integer, MemoryEAElement> elements, Map<Integer, MemoryEAPackage> packages) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT Property, Value, Object_ID FROM t_objectproperties");

            while (rs.next()) {
                String key = rs.getString("Property");
                String value = rs.getString("Value");
                int objectId = rs.getInt("Object_ID");

                if (elements.containsKey(objectId)) {
                    MemoryEAElement element = elements.get(objectId);
                    element.getTagsOrig().put(key, value);
                } else if (packages.containsKey(objectId)) {
                    MemoryEAPackage pack = packages.get(objectId);
                    pack.getTagsOrig().put(key, value);
                }
            }
        }
    }

    /**
     * Assumes attributes are fully loaded.
     *
     * @throws SQLException
     */
    private void loadAttributeTags(Connection connection, Map<Integer, MemoryEAAttribute> attributes) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT Property, VALUE, ElementID FROM t_attributetag");

            while (rs.next()) {
                String key = rs.getString("Property");
                String value = rs.getString("VALUE");
                int attributeId = rs.getInt("ElementID");

                MemoryEAAttribute attribute = attributes.get(attributeId);
                attribute.getTagsOrig().put(key, value);
            }
        }
    }

    /**
     * Assumes connectors are fully loaded.
     *
     * @throws SQLException
     */
    private void loadConnectorTags(Connection connection, Map<Integer, MemoryEAConnector> connectors) throws SQLException {
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT Property, VALUE, ElementID FROM t_connectortag");

            while (rs.next()) {
                String key = rs.getString("Property");
                String value = rs.getString("VALUE");
                int elementId = rs.getInt("ElementID");

                MemoryEAConnector connector = connectors.get(elementId);
                connector.getTagsOrig().put(key, value);
            }
        }
    }

    private void loadDiagramConnectors(Connection connection, Map<Integer, MemoryEADiagram> diagrams, Map<Integer, MemoryEAConnector> connectors) throws SQLException {
        Pattern p1 = Pattern.compile("LMT=[^;]+"); //Pattern for matching the label info
        Pattern p2 = Pattern.compile("DIR=(-?[01])"); // DIR=-1 or DIR=0 or DIR=1

        Table<EADiagram, EAElement, MemoryDiagramElement> diagramElementIndex = HashBasedTable.create();
        for (MemoryEADiagram diagram : diagrams.values()) {
            for (MemoryDiagramElement element : diagram.getElements()) {
                diagramElementIndex.put(diagram, element.getReferencedElement(), element);
            }
        }

        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT DiagramID, ConnectorID, Geometry, Hidden, Instance_ID FROM t_diagramlinks");

            while (rs.next()) {
                int diagramId = rs.getInt("DiagramID");
                int connectorId = rs.getInt("ConnectorID");
                int instanceId = rs.getInt("Instance_ID");
                String labelStyling = rs.getString("Geometry"); //Yes, this is correct
                boolean hidden = rs.getBoolean("Hidden");

                EAConnector.Direction labelDirection = EAConnector.Direction.UNSPECIFIED;
                Matcher matcher1 = p1.matcher(labelStyling);
                if (matcher1.find()) {
                    Matcher matcher2 = p2.matcher(matcher1.group(0));
                    if (matcher2.find()) {
                        switch (matcher2.group(1)) {
                            case "-1": labelDirection = EAConnector.Direction.DEST_TO_SOURCE; break;
                            case "0" : labelDirection = EAConnector.Direction.UNSPECIFIED; break;
                            case "1" : labelDirection = EAConnector.Direction.SOURCE_TO_DEST; break;
                            default: throw new IllegalStateException("Invalid direction: " + matcher2.group(1));
                        }
                    }
                }

                MemoryEADiagram diagram = diagrams.get(diagramId);
                MemoryEAConnector connector = connectors.get(connectorId);

                // Ignore connectors between unsupported elements (eg Notes)
                if (connector == null)
                    continue;

                MemoryDiagramElement source = diagramElementIndex.get(diagram, connector.getSource());
                MemoryDiagramElement dest = diagramElementIndex.get(diagram, connector.getDestination());

                // Strangely, it can occur that the referenced elements are not present in the diagram. Skip this case.
                if (source == null || dest == null)
                    continue;

                MemoryDiagramElement assoc = diagramElementIndex.get(diagram, connector.getAssociationClass());

                MemoryDiagramConnector newConn = new MemoryDiagramConnector(
                        labelDirection, hidden, connector, source, dest, assoc
                );
                source.getConnectorsOrig().add(newConn);
                if (source != dest)
                    dest.getConnectorsOrig().add(newConn);
            }
        }
    }
}

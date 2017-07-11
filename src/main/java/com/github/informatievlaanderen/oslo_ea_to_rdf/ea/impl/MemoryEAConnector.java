package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dieter De Paepe
 */
class MemoryEAConnector implements EAConnector {
    private int connectorId;
    private String name;
    private Direction direction;
    private String notes;
    private String type;
    private String sourceRole;
    private String destinationRole;
    private String sourceCardinality;
    private String targetCardinality;
    private EAElement source;
    private EAElement destination;
    private EAElement associationClass;
    private String guid;
    private List<EATag> tags;

    MemoryEAConnector(int connectorId, String name, Direction direction, String notes, String type,
                      String sourceRole, String destinationRole, String sourceCardinality, String targetCardinality,
                      EAElement source, EAElement destination,
                      EAElement associationClass, String guid) {
        this.connectorId = connectorId;
        this.name = name;
        this.direction = direction;
        this.notes = notes;
        this.type = type;
        this.sourceRole = sourceRole;
        this.destinationRole = destinationRole;
        this.sourceCardinality = sourceCardinality;
        this.targetCardinality = targetCardinality;
        this.source = source;
        this.destination = destination;
        this.associationClass = associationClass;
        this.guid = guid;
        this.tags = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getSourceRole() {
        return sourceRole;
    }

    @Override
    public String getDestRole() {
        return destinationRole;
    }

    @Override
    public EAElement getSource() {
        return source;
    }

    @Override
    public EAElement getDestination() {
        return destination;
    }

    @Override
    public EAElement getAssociationClass() {
        return associationClass;
    }

    @Override
    public String getSourceCardinality() {
        return sourceCardinality;
    }

    @Override
    public String getDestinationCardinality() {
        return targetCardinality;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public List<EATag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    @Override
    public String getPath() {
        if (getName() != null)
            return getSource().getPath() + ":" + getName();
        else
            return getSource().getPath() + ":(" + getSource().getName() + " -> " + getDestination().getName() + ")";
    }

    int getConnectorId() {
        return connectorId;
    }

    List<EATag> getTagsOrig() {
        return tags;
    }
}

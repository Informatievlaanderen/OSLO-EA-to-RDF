package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

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
    private EAElement source;
    private EAElement destination;
    private EAElement associationClass;
    private String guid;
    private ListMultimap<String, String> tags;

    MemoryEAConnector(int connectorId, String name, Direction direction, String notes, String type,
                             String sourceRole, String destinationRole, EAElement source, EAElement destination,
                             EAElement associationClass, String guid) {
        this.connectorId = connectorId;
        this.name = name;
        this.direction = direction;
        this.notes = notes;
        this.type = type;
        this.sourceRole = sourceRole;
        this.destinationRole = destinationRole;
        this.source = source;
        this.destination = destination;
        this.associationClass = associationClass;
        this.guid = guid;
        this.tags = ArrayListMultimap.create();
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
    public String getGuid() {
        return guid;
    }

    @Override
    public ListMultimap<String, String> getTags() {
        return Multimaps.unmodifiableListMultimap(tags);
    }

    int getConnectorId() {
        return connectorId;
    }

    ListMultimap<String, String> getTagsOrig() {
        return tags;
    }
}

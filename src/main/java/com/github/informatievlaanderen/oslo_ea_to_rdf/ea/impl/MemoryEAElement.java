package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAAttribute;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dieter De Paepe
 */
class MemoryEAElement implements EAElement {
    private int objectID;
    private String name;
    private String notes;
    private String guid;
    private String stereotype;
    private Type type;
    private EAPackage containingPackage;
    private List<EAConnector> connectors;
    private List<EAAttribute> attributes;
    private ListMultimap<String, String> tags;

    MemoryEAElement(int objectID, String name, String notes, String guid, String stereotype,
                    Type type, EAPackage containingPackage) {
        this.objectID = objectID;
        this.name = name;
        this.notes = notes;
        this.guid = guid;
        this.stereotype = stereotype;
        this.type = type;
        this.containingPackage = containingPackage;
        this.connectors = new ArrayList<>();
        this.attributes = new ArrayList<>();
        this.tags = ArrayListMultimap.create();
    }

    int getObjectID() {
        return objectID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public String getStereoType() {
        return stereotype;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ListMultimap<String, String> getTags() {
        return Multimaps.unmodifiableListMultimap(tags);
    }

    @Override
    public EAPackage getPackage() {
        return containingPackage;
    }

    @Override
    public List<EAAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    @Override
    public List<EAConnector> getConnectors() {
        return Collections.unmodifiableList(connectors);
    }

    List<EAConnector> getConnectorsOrig() {
        return connectors;
    }

    List<EAAttribute> getAttributesOrig() {
        return attributes;
    }

    ListMultimap<String, String> getTagsOrig() {
        return tags;
    }
}

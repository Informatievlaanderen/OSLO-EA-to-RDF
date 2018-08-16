package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dieter De Paepe
 */
class MemoryEAElement implements EAElement {
    private int objectID;
    @Expose
    private String name;
    private String notes;
    @Expose
    private String guid;
    private String stereotype;
    private Type type;
    private EAPackage containingPackage;
    private List<EAConnector> connectors;
    private List<EAAttribute> attributes;
    private List<EATag> tags;

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
        this.tags = new ArrayList<>();
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
    public List<EATag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    @Override
    public String getPath() {
        return getPackage().getPath() + ":" + getName();
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

    List<EATag> getTagsOrig() {
        return tags;
    }
}

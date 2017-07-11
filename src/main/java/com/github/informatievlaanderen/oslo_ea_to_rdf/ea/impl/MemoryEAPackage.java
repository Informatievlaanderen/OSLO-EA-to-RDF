package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EADiagram;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dieter De Paepe
 */
class MemoryEAPackage implements EAPackage {
    private String name;
    private String guid;
    private String stereoType;
    private String notes;
    private int objectID;
    private int packageID;
    private EAPackage parent;
    private List<EAPackage> packages;
    private List<EAElement> elements;
    private List<EADiagram> diagrams;
    private List<EATag> tags;

    MemoryEAPackage(String name, String guid, String stereoType, String notes, int objectID, int packageID) {
        this.name = name;
        this.guid = guid;
        this.stereoType = stereoType;
        this.notes = notes;
        this.objectID = objectID;
        this.packageID = packageID;
        this.parent = null;
        this.packages = new ArrayList<>();
        this.elements = new ArrayList<>();
        this.diagrams = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public String getStereoType() {
        return stereoType;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public List<EAPackage> getPackages() {
        return Collections.unmodifiableList(packages);
    }

    List<EAPackage> getPackagesOrig() {
        return packages;
    }

    @Override
    public EAPackage getParent() {
        return parent;
    }

    @Override
    public List<EAElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public List<EADiagram> getDiagrams() {
        return Collections.unmodifiableList(diagrams);
    }

    @Override
    public List<EATag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    @Override
    public String getPath() {
        if (getParent() != null)
            return getParent().getPath() + "." + getName();
        else
            return getName();
    }

    List<EADiagram> getDiagramsOrig() {
        return diagrams;
    }

    List<EAElement> getElementsOrig() {
        return elements;
    }

    int getObjectID() {
        return objectID;
    }

    int getPackageID() {
        return packageID;
    }

    void setParent(EAPackage parent) {
        this.parent = parent;
    }

    List<EATag> getTagsOrig() {
        return tags;
    }
}

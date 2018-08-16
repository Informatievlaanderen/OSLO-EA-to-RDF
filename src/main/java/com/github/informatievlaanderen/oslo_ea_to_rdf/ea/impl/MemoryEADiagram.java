package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EADiagram;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import com.google.gson.annotations.Expose;

import java.util.Collections;
import java.util.List;

/**
 * @author Dieter De Paepe
 */
class MemoryEADiagram implements EADiagram {
    private int diagramId;
    @Expose
    private String name;
    @Expose
    private String guid;
    private String notes;
    private EAPackage containingPackage;
    private List<MemoryDiagramElement> classes;

    MemoryEADiagram(int diagramId, String name, String guid, String notes, EAPackage containingPackage, List<MemoryDiagramElement> classes) {
        this.diagramId = diagramId;
        this.name = name;
        this.guid = guid;
        this.notes = notes;
        this.containingPackage = containingPackage;
        this.classes = classes;
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
    public String getNotes() {
        return notes;
    }

    @Override
    public EAPackage getPackage() {
        return containingPackage;
    }

    @Override
    public List<MemoryDiagramElement> getElements() {
        return Collections.unmodifiableList(classes);
    }

    List<MemoryDiagramElement> getClassesOrig() {
        return classes;
    }

    int getDiagramId() {
        return diagramId;
    }
}

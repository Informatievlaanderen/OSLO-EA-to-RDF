package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAAttribute;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

/**
 * @author Dieter De Paepe
 */
class MemoryEAAttribute implements EAAttribute {
    private EAElement element;
    private String guid;
    private String name;
    private String notes;
    private String type;
    private int attributeID;
    private ListMultimap<String, String> tags;

    MemoryEAAttribute(EAElement element, String guid, String name, String notes, String type, int attributeID) {
        this.element = element;
        this.guid = guid;
        this.name = name;
        this.notes = notes;
        this.type = type;
        this.attributeID = attributeID;
        this.tags = ArrayListMultimap.create();
    }

    @Override
    public EAElement getElement() {
        return element;
    }

    @Override
    public String getGuid() {
        return guid;
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
    public String getType() {
        return type;
    }

    @Override
    public ListMultimap<String, String> getTags() {
        return Multimaps.unmodifiableListMultimap(tags);
    }

    ListMultimap<String, String> getTagsOrig() {
        return tags;
    }

    int getAttributeID() {
        return attributeID;
    }
}

package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * A virtual connector that represents one of two parts of a (non-virtual) connector with an association class.
 *
 * Regular connectors can have an association class, effectively linking 3 elements together. In order to have a more
 * streamlined workflow, this class can be used to split such a connector into 2 instances of this class, effectively
 * acting as a wrapper. Most methods are forwarded to the original connector, except those relating to source, target
 * and associations. Connector tags are a filtered version of the original tags.
 */
public class NormalizedEAConnector implements EAConnector {
    private EAConnector inner;
    private ConnectionPart part;
    private String tagPrefix;

    /**
     * Creates a new connector to represent one of two parts of a connector with an association class.
     * @param inner the base connector
     * @param part the part of the base connector to be represented by this connector
     * @param tagPrefix the prefix identifying all tags of the original connector that should occur in this connector
     */
    public NormalizedEAConnector(EAConnector inner, ConnectionPart part, String tagPrefix) {
        Objects.requireNonNull(inner.getAssociationClass(), "The provided connector does not have an association class.");
        this.inner = inner;
        this.part = part;
        this.tagPrefix = tagPrefix;
    }

    @Override
    public String getName() {
        return inner.getName();
    }

    @Override
    public Direction getDirection() {
        return inner.getDirection();
    }

    @Override
    public String getNotes() {
        return inner.getNotes();
    }

    @Override
    public String getType() {
        return inner.getType();
    }

    @Override
    public String getSourceRole() {
        if (part == ConnectionPart.SOURCE_TO_ASSOCIATION)
            return inner.getSourceRole();
        return null;
    }

    @Override
    public String getDestRole() {
        if (part == ConnectionPart.ASSOCIATION_TO_DESTINATION)
            return inner.getDestRole();
        return null;
    }

    @Override
    public EAElement getSource() {
        if (part == ConnectionPart.SOURCE_TO_ASSOCIATION)
            return inner.getSource();
        else
            return inner.getAssociationClass();
    }

    @Override
    public EAElement getDestination() {
        if (part == ConnectionPart.SOURCE_TO_ASSOCIATION)
            return inner.getAssociationClass();
        else
            return inner.getDestination();
    }

    @Override
    public EAElement getAssociationClass() {
        // Always null
        return null;
    }

    @Override
    public String getGuid() {
        return inner.getGuid();
    }

    @Override
    public ListMultimap<String, String> getTags() {
        ListMultimap<String, String> filteredTags = ArrayListMultimap.create();

        Map<String, Collection<String>> innerTags = inner.getTags().asMap();
        for (String key : innerTags.keySet()) {
            if (key.startsWith(tagPrefix)) {
                String newKey = key.substring(tagPrefix.length());
                filteredTags.putAll(newKey, innerTags.get(key));
            }
        }

        return Multimaps.unmodifiableListMultimap(filteredTags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NormalizedEAConnector that = (NormalizedEAConnector) o;
        return Objects.equals(inner, that.inner) &&
                part == that.part &&
                Objects.equals(tagPrefix, that.tagPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner, part, tagPrefix);
    }

    public enum ConnectionPart {
        SOURCE_TO_ASSOCIATION,
        ASSOCIATION_TO_DESTINATION
    }
}

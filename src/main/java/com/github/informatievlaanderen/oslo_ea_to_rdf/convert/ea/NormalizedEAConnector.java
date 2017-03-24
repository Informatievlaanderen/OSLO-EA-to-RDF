package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl.MemoryEATag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A virtual connector that represents one of four parts of a (non-virtual) connector with an association class.
 *
 * Regular connectors can have an association class, effectively linking 3 elements together. In order to have a more
 * streamlined workflow, this class can be used to split such a connector into 4 instances of this class, effectively
 * acting as a wrapper. Most methods are forwarded to the original connector, except those relating to source, target
 * and associations. Connector tags are a filtered version of the original tags.
 */
public class NormalizedEAConnector implements EAConnector {
    private EAConnector inner;
    private ConnectionPart part;
    private String tagPrefix;

    /**
     * Creates a new connector to represent one of four parts of a connector with an association class.
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
        if (part == ConnectionPart.ASSOCIATION_TO_SOURCE || part == ConnectionPart.DESTINATION_TO_ASSOCIATION) {
            if (inner.getDirection() == Direction.DEST_TO_SOURCE)
                return Direction.SOURCE_TO_DEST;
            if (inner.getDirection() == Direction.SOURCE_TO_DEST)
                return Direction.DEST_TO_SOURCE;
            return inner.getDirection();
        }

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
        if (part == ConnectionPart.DESTINATION_TO_ASSOCIATION)
            return inner.getDestRole();
        return null;
    }

    @Override
    public String getDestRole() {
        if (part == ConnectionPart.ASSOCIATION_TO_DESTINATION)
            return inner.getDestRole();
        if (part == ConnectionPart.ASSOCIATION_TO_SOURCE)
            return inner.getSourceRole();
        return null;
    }

    @Override
    public EAElement getSource() {
        if (part == ConnectionPart.SOURCE_TO_ASSOCIATION)
            return inner.getSource();
        if (part == ConnectionPart.DESTINATION_TO_ASSOCIATION)
            return inner.getDestination();
        return inner.getAssociationClass();
    }

    @Override
    public EAElement getDestination() {
        if (part == ConnectionPart.ASSOCIATION_TO_DESTINATION)
            return inner.getDestination();
        if (part == ConnectionPart.ASSOCIATION_TO_SOURCE)
            return inner.getSource();
        return inner.getAssociationClass();
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
    public List<EATag> getTags() {
        List<EATag> filteredTags = new ArrayList<>();

        for (EATag tag : inner.getTags()) {
            if (tag.getKey().startsWith(tagPrefix))
                filteredTags.add(new MemoryEATag(tag.getKey().substring(tagPrefix.length()), tag.getValue(), tag.getNotes()));
        }

        return filteredTags;
    }

    @Override
    public String getSourceCardinality() {
        if (part == ConnectionPart.SOURCE_TO_ASSOCIATION)
            return inner.getSourceCardinality();
        if (part == ConnectionPart.DESTINATION_TO_ASSOCIATION)
            return inner.getDestinationCardinality();
        return null;
    }

    @Override
    public String getDestinationCardinality() {
        if (part == ConnectionPart.ASSOCIATION_TO_DESTINATION)
            return inner.getDestinationCardinality();
        if (part == ConnectionPart.ASSOCIATION_TO_SOURCE)
            return inner.getSourceCardinality();
        return null;
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
        ASSOCIATION_TO_SOURCE,
        DESTINATION_TO_ASSOCIATION,
        ASSOCIATION_TO_DESTINATION
    }
}

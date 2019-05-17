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
public class RoleEAConnector implements EAConnector {
    private EAConnector inner;
    private ConnectionPart part;

    /**
     * Creates a new connector to represent one of four parts of a connector with an association class.
     * @param inner the base connector
     * @param part the part of the base connector to be represented by this connector
     */
    public RoleEAConnector(EAConnector inner, ConnectionPart part) {
        this.inner = inner;
        this.part = part;
    }

    @Override
    public String getName() {
        return inner.getName();
    }

    @Override
    public Direction getDirection() {
        return Direction.SOURCE_TO_DEST;
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
        if (part == ConnectionPart.SOURCE_TO_DEST)
            return inner.getSourceRole();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            return inner.getDestRole();
        return null;
    }

    @Override
    public String getDestRole() {
        if (part == ConnectionPart.SOURCE_TO_DEST)
            return inner.getDestRole();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            return inner.getSourceRole();
        return null;
    }

    @Override
    public EAElement getSource() {
	EAElement result = null;
        if (part == ConnectionPart.SOURCE_TO_DEST)
            result = inner.getSource();
	else {
            result = inner.getDestination();
	};
	return result;
    }

    @Override
    public EAElement getDestination() {
	EAElement result = null;
        if (part == ConnectionPart.SOURCE_TO_DEST)
            result = inner.getDestination();
	else {
            result = inner.getSource();
	};
	return result;
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
        return inner.getTags();
    }

    @Override
    public String getPath() {
        if (getName() != null)
            return getSource().getPath() + ":" + getName();
        else
            return getSource().getPath() + ":(" + getSource().getName() + " -> " + getDestination().getName() + ")";
    }

    @Override
    public String getSourceCardinality() {
        if (part == ConnectionPart.SOURCE_TO_DEST)
            return inner.getSourceCardinality();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            return inner.getDestinationCardinality();
        throw new IllegalStateException();
    }

    @Override
    public String getDestinationCardinality() {
        if (part == ConnectionPart.SOURCE_TO_DEST)
            return inner.getDestinationCardinality();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            return inner.getSourceCardinality();
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleEAConnector that = (RoleEAConnector) o;
        return Objects.equals(inner, that.inner) &&
                part == that.part ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner, part);
    }

    public enum ConnectionPart {
        SOURCE_TO_DEST,
        DEST_TO_SOURCE
    }
}

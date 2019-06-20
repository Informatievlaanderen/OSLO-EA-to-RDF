package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl.MemoryEATag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A directed connector derived from another connector
 *
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
	// for a role connector is the name the role
        return this.getDestRole();
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
        String role = null;
        if (part == ConnectionPart.SOURCE_TO_DEST)
            role = inner.getSourceRole();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            role = inner.getDestRole();
	if (role == null)
	    role = inner.getName(); // does not uses the tags-label
        return role;
    }

    @Override
    public List<EATag> getSourceRoleTags() {
        if (part == ConnectionPart.SOURCE_TO_DEST)
            return inner.getSourceRoleTags();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            return inner.getDestRoleTags();
        return inner.getSourceRoleTags();
    }

    @Override
    public String getDestRole() {
        String role = null;
        if (part == ConnectionPart.SOURCE_TO_DEST)
            role = inner.getDestRole();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            role = inner.getSourceRole();
	if (role == null)
	    role = inner.getName(); // does not uses the tags label

        return role;
    }

    @Override
    public List<EATag> getDestRoleTags() {
        if (part == ConnectionPart.SOURCE_TO_DEST)
            return inner.getDestRoleTags();
        if (part == ConnectionPart.DEST_TO_SOURCE)
            return inner.getSourceRoleTags();
        return inner.getDestRoleTags();
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
    // for a Role connector are the tags those of the Role and not of the main one
    // we could consider an overwrite approach TODO XXX
    public List<EATag> getTags() {
        return this.getDestRoleTags();
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

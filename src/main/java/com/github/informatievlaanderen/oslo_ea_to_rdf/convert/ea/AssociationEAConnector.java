package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl.MemoryEATag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A association connector created for the AssociationClass connected with the assocation <inner>
 *
 */
public class AssociationEAConnector implements EAConnector {
    private EAConnector inner;
    private EAElement asource;
    private EAElement atarget;
    private String aname;
    private String asourcecard;
    private String atargetcard;
    private String aguid;

    private final Logger LOGGER = LoggerFactory.getLogger(RoleEAConnector.class);

    /**
     * Creates a new connector to represent one of four parts of a connector with an association class.
     * @param inner the base connector
     * @param part the part of the base connector to be represented by this connector
     */
    public AssociationEAConnector(EAConnector inner, EAElement dsource, EAElement dtarget, String dname, String dsourcecard, String dtargetcard) {
	
        this.inner   = inner;
        this.asource = dsource;
        this.atarget = dtarget;
        this.aname   = dname;
        this.asourcecard = dsourcecard;
        this.atargetcard = dtargetcard;
        this.aguid = "Derived:"+ asource.getGuid() + "->" + atarget.getGuid();
    }

    @Override
    public String getName() {
        return this.asource.getEffectiveName();
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
        return role;
    }

    @Override
    public List<EATag> getSourceRoleTags() {
        return null;
    }

    @Override
    public String getDestRole() {
        String role = null;
        return role;
    }

    @Override
    public List<EATag> getDestRoleTags() {
        return null;
    }

    @Override
    public EAElement getSource() {
	return asource;
    }

    @Override
    public EAElement getDestination() {
	return atarget;
    }

    @Override
    public EAElement getAssociationClass() {
        // Always null
        return null;
    }

    @Override
    public String getGuid() {
        return aguid;
    }

    @Override
    // for a Role connector are the tags those of the Role and not of the main one
    // we could consider an overwrite approach TODO XXX
    public List<EATag> getTags() {
        List<EATag> result = new ArrayList<>();
        String value = "verbindt de associatieklasse behorende bij de basiseigenschap met de domein/range klasse van de basiseigenschap.";
        String usage = "";
        EATag label = new MemoryEATag("label-nl",this.getName() + "(" + this.aname + ")","");
        EATag aplabel = new MemoryEATag("ap-label-nl",this.getName() + "(" + this.aname + ")","");
        EATag definition = new MemoryEATag("definition-nl",value,"");
        EATag apdefinition = new MemoryEATag("ap-definition-nl",value,"");
        EATag usageNote = new MemoryEATag("usageNote-nl",usage,"");
        EATag apusageNote = new MemoryEATag("ap-usageNote-nl",usage,"");
        result.add(label);
        result.add(aplabel);
        result.add(definition);
        result.add(apdefinition);
        result.add(usageNote);
        result.add(apusageNote);
        return result;
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
        return asourcecard;
    }

    @Override
    public String getDestinationCardinality() {
        return atargetcard;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssociationEAConnector that = (AssociationEAConnector) o;
        return Objects.equals(inner, that.inner) &&
                aguid == that.aguid ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inner, aguid);
    }

}

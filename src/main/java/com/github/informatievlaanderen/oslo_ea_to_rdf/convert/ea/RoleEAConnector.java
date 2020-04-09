package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ea;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.Tag;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TagHelper;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.URIObject;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl.MemoryEATag;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.Tag;
// import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TagHelper;

/** A directed connector derived from another connector */
public class RoleEAConnector implements EAConnector, URIObject {
  private EAConnector inner;
  private ConnectionPart part;
  private List<EATag> newlabels;
  private String myuri;
  private String myef;
  private TagHelper tagHelper;

  private final Logger LOGGER = LoggerFactory.getLogger(RoleEAConnector.class);

  /**
   * Creates a new connector to represent one of four parts of a connector with an association
   * class.
   *
   * @param inner the base connector
   * @param part the part of the base connector to be represented by this connector
   */
  public RoleEAConnector(EAConnector inner, ConnectionPart part, TagHelper dtagHelper) {

    this.inner = inner;
    this.part = part;
    this.tagHelper = dtagHelper;
  }

  public RoleEAConnector(
      EAConnector inner, ConnectionPart part, List<EATag> newlabels, TagHelper dtagHelper) {

    this.inner = inner;
    this.part = part;
    this.newlabels = newlabels;
    this.tagHelper = dtagHelper;
  }

  @Override
  public String getName() {
    // for a role connector is the name the role
    // The name is used to construct the URI, if not given
    // in case derived from a connector without a direction, add the domain class first
    // otherwise use the role name with the first character lowercase

    if (part == ConnectionPart.UNSPEC_DEST_TO_SOURCE) return inner.getName();
    //	    return inner.getDestination().getName() + "." +
    // StringUtils.uncapitalize(inner.getName());

    if (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST) return inner.getName();
    //	    return inner.getSource().getName() + "." + StringUtils.uncapitalize(inner.getName());
    // return StringUtils.uncapitalize(this.getDestRole());
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
    if (part == ConnectionPart.SOURCE_TO_DEST) role = inner.getSourceRole();
    if (part == ConnectionPart.DEST_TO_SOURCE) role = inner.getDestRole();
    if (role == null) role = inner.getName(); // does not uses the tags-label
    return role;
  }

  @Override
  public List<EATag> getSourceRoleTags() {
    if (part == ConnectionPart.SOURCE_TO_DEST) return inner.getSourceRoleTags();
    if (part == ConnectionPart.DEST_TO_SOURCE) return inner.getDestRoleTags();
    if ((part == ConnectionPart.UNSPEC_DEST_TO_SOURCE)
        || (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST)) return null;
    return inner.getSourceRoleTags();
  }

  @Override
  public String getDestRole() {
    String role = null;
    if (part == ConnectionPart.SOURCE_TO_DEST) role = inner.getDestRole();
    if (part == ConnectionPart.DEST_TO_SOURCE) role = inner.getSourceRole();
    if (role == null) role = inner.getName(); // does not uses the tags label

    return role;
  }

  @Override
  public List<EATag> getDestRoleTags() {
    if (part == ConnectionPart.SOURCE_TO_DEST) return inner.getDestRoleTags();
    if (part == ConnectionPart.DEST_TO_SOURCE) return inner.getSourceRoleTags();
    if ((part == ConnectionPart.UNSPEC_DEST_TO_SOURCE)
        || (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST)) return inner.getTags();
    return inner.getDestRoleTags();
  }

  @Override
  public EAElement getSource() {
    EAElement result = null;
    if ((part == ConnectionPart.SOURCE_TO_DEST) || (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST))
      result = inner.getSource();
    else {
      result = inner.getDestination();
    }
    ;
    return result;
  }

  @Override
  public EAElement getDestination() {
    EAElement result = null;
    if ((part == ConnectionPart.SOURCE_TO_DEST) || (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST))
      result = inner.getDestination();
    else {
      result = inner.getSource();
    }
    ;
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
    String destName = this.getDestination().getName();
    String sourceName = this.getSource().getName();
    if (destName == sourceName) {
      if (part == ConnectionPart.UNSPEC_DEST_TO_SOURCE) {
        String cname = tagHelper.getOptionalTag(this.inner, Tag.LABELNL, "");
        String cnameap = tagHelper.getOptionalTag(this.inner, Tag.APLABELNL, "");
        LOGGER.debug("role labels {} - {} ", cname, cnameap);
        String scname = cname + " (target)";
        String scnameap = "";
        if (cnameap != "") scnameap = cnameap + " (target)";
        LOGGER.debug("disambiguated role labels {} - {} ", scname, scnameap);
        EATag scnameTag = new MemoryEATag("label-nl", scname, "");
        EATag scnameapTag = null;
        List<EATag> ts = new ArrayList<>();
        if (inner.getTags() != null) ts.addAll(inner.getTags());
        ts.add(scnameTag);
        if (scnameap != "") {
          scnameapTag = new MemoryEATag("ap-label-nl", scnameap, "");
          ts.add(scnameapTag);
        }
        ;
        return ts;

      } else {
        if (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST) {
          //      List<EATag> ts = new ArrayList<>();
          //      if (inner.getTags() != null) ts.addAll(inner.getTags());
          //      if (newlabels != null) ts.addAll(newlabels);
          //      return ts;
          String cname = tagHelper.getOptionalTag(this.inner, Tag.LABELNL, "");
          String cnameap = tagHelper.getOptionalTag(this.inner, Tag.APLABELNL, "");
          LOGGER.debug("role labels {} - {} ", cname, cnameap);
          String scname = cname + " (source)";
          String scnameap = "";
          if (cnameap != "") scnameap = cnameap + " (source)";
          LOGGER.debug("disambiguated role labels {} - {} ", scname, scnameap);
          EATag scnameTag = new MemoryEATag("label-nl", scname, "");
          EATag scnameapTag = null;
          List<EATag> ts = new ArrayList<>();
          if (inner.getTags() != null) ts.addAll(inner.getTags());
          ts.add(scnameTag);
          if (scnameap != "") {
            scnameapTag = new MemoryEATag("ap-label-nl", scnameap, "");
            ts.add(scnameapTag);
          }
          ;
          return ts;
        } else {
          if (this.getDestRoleTags() == null) {
            LOGGER.debug("No Dest Role tags found, fallback to inner.getTags()");

            if (this.inner.getTags() == null) {
              LOGGER.debug("No tags found for {} ", this.inner.getPath());
              List<EATag> ts = new ArrayList<>();
              return ts;
            } else {
              return this.inner.getTags();
            }
          } else {
            return this.getDestRoleTags();
          }
        }
      }
    } else {
      return this.getDestRoleTags();
    }
  }

  @Override
  public String getPath() {
    if (getName() != null) return getSource().getPath() + ":" + getName();
    else
      return getSource().getPath()
          + ":("
          + getSource().getName()
          + " -> "
          + getDestination().getName()
          + ")";
  }

  @Override
  public String getSourceCardinality() {
    if ((part == ConnectionPart.SOURCE_TO_DEST) || (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST))
      return inner.getSourceCardinality();
    if ((part == ConnectionPart.DEST_TO_SOURCE) || (part == ConnectionPart.UNSPEC_DEST_TO_SOURCE))
      return inner.getDestinationCardinality();
    throw new IllegalStateException();
  }

  @Override
  public String getDestinationCardinality() {
    if ((part == ConnectionPart.SOURCE_TO_DEST) || (part == ConnectionPart.UNSPEC_SOURCE_TO_DEST))
      return inner.getDestinationCardinality();
    if ((part == ConnectionPart.DEST_TO_SOURCE) || (part == ConnectionPart.UNSPEC_DEST_TO_SOURCE))
      return inner.getSourceCardinality();
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RoleEAConnector that = (RoleEAConnector) o;
    return Objects.equals(inner, that.inner) && part == that.part;
  }

  @Override
  public int hashCode() {
    return Objects.hash(inner, part);
  }

  public enum ConnectionPart {
    SOURCE_TO_DEST,
    DEST_TO_SOURCE,
    UNSPEC_SOURCE_TO_DEST,
    UNSPEC_DEST_TO_SOURCE
  }

  @Override
  public String getURI() {
    return myuri;
  }

  @Override
  public void setURI(String mu) {
    this.myuri = mu;
  }

  public String getEffectiveName() {
    return myef;
  }

  public void setEffectiveName(String ef) {
    this.myef = ef;
  }
}

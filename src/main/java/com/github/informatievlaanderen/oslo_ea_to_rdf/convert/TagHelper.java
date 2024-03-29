package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.JSONLDOntology.LanguageStringDescription;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Configuration;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Mapping;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAObject;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object that knows which Enterprise Architect tags are relevant and how they should be
 * outputted.
 *
 * @author Dieter De Paepe
 */
public class TagHelper {
  private static final Joiner JOINER = Joiner.on(", ");
  private static final Logger LOGGER = LoggerFactory.getLogger(TagHelper.class);
  /**
   * Tag value that indicates the tag note is to be used instead. This allows users to use text
   * longer than the 256 characters allowed in the tag value.
   */
  public static final String USE_NOTE_VALUE = "NOTE";

  private Configuration config;

  public TagHelper(Configuration config) {
    this.config = config;
  }

  /**
   * Collects all relevant information from the tags of the specified object.
   *
   * @param source source of the tags
   * @param scope the scope to be outputted
   * @return never {@code null}
   */
  public List<TagData> getTagDataFor(EAObject source, Scope scope) {
    if (scope == Scope.NOTHING) return Collections.emptyList();

    List<Mapping> mappings;
    if (scope == Scope.FULL_DEFINITON) mappings = config.getInternalMappings();
    else mappings = config.getExternalMappings();

    List<TagData> result = new ArrayList<>();
    for (Mapping mapping : mappings) {
      String value;
      if (mapping.isMandatory()) value = getSingleValue(source, mapping.getTag(), "TODO", true);
      else value = getSingleValue(source, mapping.getTag(), null, false);

      if (value == null) continue;

      if (RDFS.Resource.getURI().equals(mapping.getType())) {
        result.add(
            new TagData(
                mapping.getTag(),
                mapping.getProperty(),
                ResourceFactory.createResource(value),
                value));
      } else if (Strings.isNullOrEmpty(mapping.getType())
          || RDF.dtLangString.getURI().equals(mapping.getType())) {
        result.add(
            new TagData(
                mapping.getTag(),
                mapping.getProperty(),
                ResourceFactory.createLangLiteral(value, mapping.getLang()),
                value));
      } else {
        RDFDatatype datatype = NodeFactory.getType(mapping.getType());
        result.add(
            new TagData(
                mapping.getTag(),
                mapping.getProperty(),
                ResourceFactory.createTypedLiteral(value, datatype),
                value));
      }
    }

    return result;
  }

  /**
   * Collects all values for each of the specified mappings for the given object.
   *
   * @param object the object from which to extract the tags
   * @param mappings all mappings to include
   * @return never {@code null}
   */
  public List<TagData> getTagDataFor(EAObject object, Iterable<Mapping> mappings) {
    List<TagData> result = new ArrayList<>();
    for (Mapping mapping : mappings) {
      List<String> tagValues = getTagValues(object.getTags(), mapping.getTag());
      LOGGER.debug("search tag {}", mapping.getTag());
      // fallback tags field should not be empty for this debug line: LOGGER.debug("fallback {}",
      // mapping.getFallbackTags().toString());

      Iterator<String> backupIterator =
          mapping.getFallbackTags() != null
              ? mapping.getFallbackTags().iterator()
              : Collections.emptyIterator();
      String b = "";
      while (tagValues.isEmpty() && backupIterator.hasNext()) {
        b = backupIterator.next();
        tagValues = getTagValues(object.getTags(), b);
        LOGGER.debug(
            "found tag value {} {} in object {} using {}",
            mapping.getTag(),
            tagValues.toString(),
            object.getTags().toString(),
            b);
      }
      ;

      if (tagValues.isEmpty() && mapping.isMandatory()) {
        LOGGER.warn("Missing \"{}\" tag for \"{}\".", mapping.getTag(), object.getPath());
        tagValues = Collections.singletonList("TODO");
      }

      LOGGER.debug("found tagvalues {}", tagValues.toString());
      if (RDFS.Resource.getURI().equals(mapping.getType())) {
        for (String tagValue : tagValues)
          result.add(
              new TagData(
                  mapping.getTag(),
                  mapping.getProperty(),
                  ResourceFactory.createResource(tagValue),
                  tagValue));
      } else if (Strings.isNullOrEmpty(mapping.getType())
          || RDF.dtLangString.getURI().equals(mapping.getType())) {
        for (String tagValue : tagValues) {
          if (tagValue != null) {
            result.add(
                new TagData(
                    mapping.getTag(),
                    mapping.getProperty(),
                    ResourceFactory.createLangLiteral(tagValue, mapping.getLang()),
                    tagValue));
          }
          ;
        }
        ;
      } else {
        RDFDatatype datatype = NodeFactory.getType(mapping.getType());
        for (String tagValue : tagValues)
          result.add(
              new TagData(
                  mapping.getTag(),
                  mapping.getProperty(),
                  ResourceFactory.createTypedLiteral(tagValue, datatype),
                  tagValue));
      }
    }

    LOGGER.debug("tags found {}", result.toString());
    return result;
  }

  /**
   * Collects all values and return as a list of stringified JsonObjects for each of the specified
   * mappings for the given object.
   *
   * @param object the object from which to extract the tags
   * @param mappings all mappings to include
   * @return never {@code null}
   */
  public List<String> getTagDataForJson(EAObject object, Iterable<Mapping> mappings) {
    List<String> result = new ArrayList<>();
    HashMap<String, List<LanguageStringDescription>> langresult = new HashMap<>();
    for (Mapping mapping : mappings) {
      List<String> tagValues = getTagValues(object.getTags(), mapping.getTag());
      LOGGER.debug("search tag {}", mapping.getTag());
      // fallback tags field should not be empty for this debug line: LOGGER.debug("fallback {}",
      // mapping.getFallbackTags().toString());

      Iterator<String> backupIterator =
          mapping.getFallbackTags() != null
              ? mapping.getFallbackTags().iterator()
              : Collections.emptyIterator();
      String b = "";
      while (tagValues.isEmpty() && backupIterator.hasNext()) {
        b = backupIterator.next();
        tagValues = getTagValues(object.getTags(), b);
        LOGGER.debug(
            "found tag value {} {} in object {} using {}",
            mapping.getTag(),
            tagValues.toString(),
            object.getTags().toString(),
            b);
      }
      ;

      if (tagValues.isEmpty() && mapping.isMandatory()) {
        LOGGER.warn("Missing \"{}\" tag for \"{}\".", mapping.getTag(), object.getPath());
        tagValues = Collections.singletonList("TODO");
      }

      LOGGER.debug("found tagvalues {}", tagValues.toString());
      /* no grouping done per tag */
      List<LanguageStringDescription> initl = new ArrayList<>();
      if (mapping.getLang() != null) {
        for (String tagValue : tagValues) {
          if (langresult.containsKey(mapping.getTag())) {
            initl = langresult.get(mapping.getTag());
          } else {
            initl.clear();
          }
          ;
          initl.add(new LanguageStringDescription(mapping.getLang(), tagValue));
          langresult.put(mapping.getTag(), initl);
          /*
          			result.add("\"" + mapping.getTag() + "\" : {"  +
                                             "\"" + mapping.getLang() + "\": \"" + StringEscapeUtils.escapeJson(tagValue) + "\"}");
          */
        }
        ;
      } else {
        for (String tagValue : tagValues) {
          result.add(
              "\"" + mapping.getTag() + "\" : \"" + StringEscapeUtils.escapeJson(tagValue) + "\"");
        }
        ;
      }
      ;
    }
    ;
    for (String i : langresult.keySet()) {
      List<String> lres = new ArrayList<>();
      for (LanguageStringDescription lsd : langresult.get(i)) {
        lres.add(
            "\""
                + lsd.getLanguage()
                + "\": \""
                + StringEscapeUtils.escapeJson(lsd.getValue())
                + "\"");
      }
      ;
      String lresl = JOINER.join(lres);
      result.add("\"" + i + "\" : {" + lresl + "}");
    }

    LOGGER.debug("tags found {}", result.toString());
    return result;
  }

  /**
   * Collects the {@link Mapping#getTag()} values.
   *
   * @return never {@code null}
   */
  public List<String> getTagNames(List<Mapping> mappings) {
    return mappings.stream().map(Mapping::getTag).collect(Collectors.toList());
  }

  public String getTagKey(Tag tag) {
    return config.getBuiltinTags().getOrDefault(tag, tag.getDefaultTagName());
  }

  public String getOptionalTag(EAObject pack, Tag tag, String backup) {
    return getOptionalTag(pack, getTagKey(tag), backup);
  }

  public String getOptionalTag(EAObject pack, String key, String backup) {
    return getSingleValue(pack, key, backup, false);
  }

  public String getSingleValue(EAObject object, Tag tag, String backup, boolean warnIfMissing) {
    return getSingleValue(object, getTagKey(tag), backup, warnIfMissing);
  }

  public String getSingleValue(EAObject object, String tag, String backup, boolean warnIfMissing) {
    List<String> values = getTagValues(object.getTags(), tag);

    if (values.isEmpty()) {
      if (warnIfMissing) {
        LOGGER.warn("Missing \"{}\" tag for \"{}\".", tag, object.getPath());
      }
      return backup;
    } else if (values.size() > 1) {
      LOGGER.warn(
          "Multiple occurrences of tag \"{}\" where only one was expected for \"{}\".",
          tag,
          object.getPath());
      return values.get(0);
    } else {
      return values.get(0);
    }
  }

  /** Gathers the values of all tags with the given key. */
  private List<String> getTagValues(List<EATag> tags, String key) {
    return tags.stream()
        .filter(t -> key.equals(t.getKey()))
        .map(
            t ->
                USE_NOTE_VALUE.equals(t.getValue())
                    ? (t.getNotes() == null
                        ? "TODO"
                        : StringUtils.remove(t.getNotes(), "NOTE$ea_notes="))
                    : t.getValue())
        .collect(Collectors.toList());
  }

  // NOTE$ea_notes=

  public List<Mapping> getOntologyMappings() {
    return config.getOntologyMappings();
  }

  public List<Mapping> getContentMappings(Scope scope) {
    if (scope == Scope.NOTHING) return Collections.emptyList();

    if (scope == Scope.FULL_DEFINITON) return config.getInternalMappings();
    else return config.getExternalMappings();
  }
}

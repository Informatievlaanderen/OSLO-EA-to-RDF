package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Configuration;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Mapping;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAObject;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EATag;
import com.google.common.base.Strings;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An object that knows which Enterprise Architect tags are relevant and how they should be outputted.
 *
 * @author Dieter De Paepe
 */
public class TagHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagHelper.class);
    /**
     * Tag value that indicates the tag note is to be used instead. This allows users to use text longer than
     * the 256 characters allowed in the tag value.
     */
    public static final String USE_NOTE_VALUE = "NOTE";

    private Configuration config;

    public TagHelper(Configuration config) {
        this.config = config;
    }

    /**
     * Collects all relevant information from the tags of the specified object.
     * @param source source of the tags
     * @param scope the scope to be outputted
     * @return never {@code null}
     */
    public List<TagData> getTagDataFor(EAObject source, Scope scope) {
        if (scope == Scope.NOTHING)
            return Collections.emptyList();

        List<Mapping> mappings;
        if (scope == Scope.FULL_DEFINITON)
            mappings = config.getInternalMappings();
        else
            mappings = config.getExternalMappings();

        List<TagData> result = new ArrayList<>();
        for (Mapping mapping : mappings) {
            String value;
            if (mapping.isMandatory())
                value = getSingleValue(source, mapping.getTag(), "TODO", true);
            else
                value = getSingleValue(source, mapping.getTag(), null, false);

            if (value == null)
                continue;

            if (RDFS.Resource.getURI().equals(mapping.getType())) {
                result.add(new TagData(mapping.getTag(), mapping.getProperty(), ResourceFactory.createResource(value)));
            } else if (Strings.isNullOrEmpty(mapping.getType()) || RDF.dtLangString.getURI().equals(mapping.getType())) {
                result.add(new TagData(mapping.getTag(), mapping.getProperty(), ResourceFactory.createLangLiteral(value, mapping.getLang())));
            } else {
                RDFDatatype datatype = NodeFactory.getType(mapping.getType());
                result.add(new TagData(mapping.getTag(), mapping.getProperty(), ResourceFactory.createTypedLiteral(value, datatype)));
            }
        }

        return result;
    }

    /**
     * Collects all values for each of the specified mappings for the given object.
     * @param object the object from which to extract the tags
     * @param mappings all mappings to include
     * @return never {@code null}
     */
    public List<TagData> getTagDataFor(EAObject object, Iterable<Mapping> mappings) {
        List<TagData> result = new ArrayList<>();
        for (Mapping mapping : mappings) {
            List<String> tagValues = getTagValues(object.getTags(), mapping.getTag());

            Iterator<String> backupIterator = mapping.getFallbackTags() != null ? mapping.getFallbackTags().iterator() : Collections.emptyIterator();
            while (tagValues.isEmpty() && backupIterator.hasNext())
                tagValues = getTagValues(object.getTags(), backupIterator.next());

            if (tagValues.isEmpty() && mapping.isMandatory()) {
                LOGGER.warn("Missing \"{}\" tag for \"{}\".", mapping.getTag(), object.getPath());
                tagValues = Collections.singletonList("TODO");
            }

            if (RDFS.Resource.getURI().equals(mapping.getType())) {
                for (String tagValue : tagValues)
                    result.add(new TagData(mapping.getTag(), mapping.getProperty(), ResourceFactory.createResource(tagValue)));
            } else if (Strings.isNullOrEmpty(mapping.getType()) || RDF.dtLangString.getURI().equals(mapping.getType())) {
                for (String tagValue : tagValues)
                    result.add(new TagData(mapping.getTag(), mapping.getProperty(), ResourceFactory.createLangLiteral(tagValue, mapping.getLang())));
            } else {
                RDFDatatype datatype = NodeFactory.getType(mapping.getType());
                for (String tagValue : tagValues)
                    result.add(new TagData(mapping.getTag(), mapping.getProperty(), ResourceFactory.createTypedLiteral(tagValue, datatype)));
            }
        }

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
            LOGGER.warn("Multiple occurrences of tag \"{}\" where only one was expected for \"{}\".", tag, object.getPath());
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    /**
     * Gathers the values of all tags with the given key.
     */
    private List<String> getTagValues(List<EATag> tags, String key) {
        return tags.stream()
                .filter(t -> key.equals(t.getKey()))
                .map(t -> USE_NOTE_VALUE.equals(t.getValue()) ? (t.getNotes() == null ? "TODO" : t.getNotes()) : t.getValue())
                .collect(Collectors.toList());
    }

    public List<Mapping> getOntologyMappings() {
        return config.getOntologyMappings();
    }

    public List<Mapping> getContentMappings(Scope scope) {
        if (scope == Scope.NOTHING)
            return Collections.emptyList();

        if (scope == Scope.FULL_DEFINITON)
            return config.getInternalMappings();
        else
            return config.getExternalMappings();
    }
}

package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Configuration;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Mapping;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAObject;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
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
                value = getMandatoryTag(source, mapping.getTag(), "TODO");
            else
                value = getOptionalTag(source, mapping.getTag(), null);

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
     * Collects all relevant information from the tags of the specified package.
     *
     * @param eaPackage the package whose tags are to be extracted
     * @return never {@code null}
     */
    public List<TagData> getTagDataFor(EAPackage eaPackage) {
        List<TagData> result = new ArrayList<>();
        for (Mapping mapping : config.getOntologyMappings()) {
            String value;
            if (mapping.isMandatory())
                value = getMandatoryTag(eaPackage, mapping.getTag(), "TODO");
            else
                value = getOptionalTag(eaPackage, mapping.getTag(), null);

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
     * Gets all the names of relevant tags for the given scope.
     * @return never {@code null}
     */
    public List<String> getTagNamesFor(Scope scope) {
        if (scope == Scope.NOTHING)
            return Collections.emptyList();

        List<Mapping> mappings;
        if (scope == Scope.FULL_DEFINITON)
            mappings = config.getInternalMappings();
        else
            mappings = config.getExternalMappings();

        return mappings.stream().map(Mapping::getTag).collect(Collectors.toList());
    }

    public String getTagKey(Tag tag) {
        return config.getBuiltinTags().getOrDefault(tag, tag.getDefaultTagName());
    }

    public String getMandatoryTag(EAObject obj, Tag tag, String backup) {
        return getMandatoryTag(obj, getTagKey(tag), backup);
    }

    public String getMandatoryTag(EAObject pack, String key, String backup) {
        List<String> tags = pack.getTags().stream()
                .filter(t -> key.equals(t.getKey()))
                .map(t -> USE_NOTE_VALUE.equals(t.getValue()) ? t.getNotes() : t.getValue())
                .collect(Collectors.toList());

        if (tags.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for \"{}\".", key, Util.getFullName(pack));
            return backup;
        } else if (tags.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for \"{}\".", key, Util.getFullName(pack));
            return tags.get(0);
        } else {
            return tags.get(0);
        }
    }

    public String getMandatoryTag(EAPackage pack, Tag tag, String backup) {
        return getMandatoryTag(pack, getTagKey(tag), backup);
    }

    public String getMandatoryTag(EAPackage pack, String key, String backup) {
        List<String> tags = pack.getTags().stream()
                .filter(t -> key.equals(t.getKey()))
                .map(t -> USE_NOTE_VALUE.equals(t.getValue()) ? t.getNotes() : t.getValue())
                .collect(Collectors.toList());

        if (tags.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for package \"{}\".", key, Util.getFullName(pack));
            return backup;
        } else if (tags.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for package \"{}\".", key, Util.getFullName(pack));
            return tags.get(0);
        } else {
            return tags.get(0);
        }
    }

    public String getOptionalTag(EAObject obj, Tag tag, String backup) {
        return getOptionalTag(obj, getTagKey(tag), backup);
    }

    public String getOptionalTag(EAObject pack, String key, String backup) {
        List<String> values = pack.getTags().stream()
                .filter(t -> key.equals(t.getKey()))
                .map(t -> USE_NOTE_VALUE.equals(t.getValue()) ? t.getNotes() : t.getValue())
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for \"{}\".", key, Util.getFullName(pack));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public String getOptionalTag(EAPackage pack, Tag tag, String backup) {
        return getOptionalTag(pack, getTagKey(tag), backup);
    }

    public String getOptionalTag(EAPackage pack, String key, String backup) {
        List<String> values = pack.getTags().stream()
                .filter(t -> key.equals(t.getKey()))
                .map(t -> USE_NOTE_VALUE.equals(t.getValue()) ? t.getNotes() : t.getValue())
                .collect(Collectors.toList());

        if (values.isEmpty()) {
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for package \"{}\".", key, Util.getFullName(pack));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }
}

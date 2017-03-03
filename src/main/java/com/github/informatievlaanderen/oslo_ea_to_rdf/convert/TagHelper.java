package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Configuration;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Mapping;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAObject;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import org.apache.jena.rdf.model.ResourceFactory;
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

            if (value != null)
                result.add(new TagData(mapping.getTag(), mapping.getProperty(), ResourceFactory.createLangLiteral(value, mapping.getLang())));
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
        List<String> values = pack.getTags().get(key);

        if (values.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for \"{}\".", key, Util.getFullName(pack));
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for \"{}\".", key, Util.getFullName(pack));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public String getMandatoryTag(EAPackage pack, Tag tag, String backup) {
        return getMandatoryTag(pack, getTagKey(tag), backup);
    }

    public String getMandatoryTag(EAPackage pack, String key, String backup) {
        List<String> values = pack.getTags().get(key);

        if (values.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for package \"{}\".", key, Util.getFullName(pack));
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for package \"{}\".", key, Util.getFullName(pack));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public String getOptionalTag(EAObject obj, Tag tag, String backup) {
        return getOptionalTag(obj, getTagKey(tag), backup);
    }

    public String getOptionalTag(EAObject pack, String key, String backup) {
        List<String> values = pack.getTags().get(key);

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
        List<String> values = pack.getTags().get(key);

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

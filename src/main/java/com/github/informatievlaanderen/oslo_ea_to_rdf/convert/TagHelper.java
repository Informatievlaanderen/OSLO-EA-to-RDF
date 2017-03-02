package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Configuration;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config.Mapping;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAObject;
import org.apache.jena.rdf.model.ResourceFactory;

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
                value = Util.getMandatoryTag(source, mapping.getTag(), "TODO");
            else
                value = Util.getOptionalTag(source, mapping.getTag(), null);

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
}

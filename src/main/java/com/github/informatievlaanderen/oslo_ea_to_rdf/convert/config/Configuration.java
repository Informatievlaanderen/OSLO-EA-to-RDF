package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config;

import org.apache.jena.rdf.model.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for the converter.
 *
 * The main purpose is to have a flexible way to determine which tags map to which RDF terms.
 *
 * @author Dieter De Paepe
 */
public class Configuration {
    private Map<String, Resource> prefixes;
    private List<Mapping> internalMappings;
    private List<Mapping> externalMappings;

    public Configuration(Map<String, Resource> prefixes, List<Mapping> internalMappings, List<Mapping> externalMappings) {
        this.prefixes = prefixes;
        this.internalMappings = internalMappings;
        this.externalMappings = externalMappings;
    }

    /**
     * Gets the mapping of prefixes to RDF namespaces that was defined in the configuration.
     * @return never {@code null}
     */
    public Map<String, Resource> getPrefixes() {
        if (prefixes == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(prefixes);
    }

    /**
     * Gets the mappings intended for terms that were defined by the user.
     * @return never {@code null}
     */
    public List<Mapping> getInternalMappings() {
        if (internalMappings == null)
            return Collections.emptyList();
        return Collections.unmodifiableList(internalMappings);
    }

    /**
     * Gets the mappings intended for existing, external terms.
     * @return never {@code null}
     */
    public List<Mapping> getExternalMappings() {
        if (externalMappings == null)
            return Collections.emptyList();
        return Collections.unmodifiableList(externalMappings);
    }
}

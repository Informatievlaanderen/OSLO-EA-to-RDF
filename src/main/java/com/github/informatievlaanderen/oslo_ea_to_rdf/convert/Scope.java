package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

/**
 * Describes which information should be outputted by the output handler of the converter.
 *
 * @author Dieter De Paepe
 */
public enum Scope {
    /**
     * All information should be included.
     */
    FULL_DEFINITON,
    /**
     * Only translations should be included.
     */
    TRANSLATIONS_ONLY,
    /**
     * No output.
     */
    NOTHING
}

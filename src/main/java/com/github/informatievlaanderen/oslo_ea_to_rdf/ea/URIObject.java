package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import java.util.List;

/**
 * An EA object with a uri assigned
 */
public interface URIObject {
    /**
     * Gets the uri of the object.
     *
     * @return the uri, or {@code null}
     */
    String getURI();

    /**
     * Sets the uri of the object.
     *
     * @param uri is the full uri to be set
     * @return void
     */
    void setURI(String uri);

}

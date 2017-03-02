package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.InvalidPropertyURIException;

import java.io.IOException;

/**
 * GSON adapter for parsing {@link Property}.
 *
 * @author Dieter De Paepe
 */
public class PropertyTypeAdapter extends TypeAdapter<Property> {
    @Override
    public void write(JsonWriter out, Property value) throws IOException {
        if (value == null || value.isAnon()) {
            out.nullValue();
            return;
        }

        out.value(value.getURI());
    }

    @Override
    public Property read(JsonReader in) throws IOException {
        String property = in.nextString();
        try {
            return ResourceFactory.createProperty(property);
        } catch (InvalidPropertyURIException e) {
            throw new IOException("Invalid property URI: " + property);
        }
    }
}

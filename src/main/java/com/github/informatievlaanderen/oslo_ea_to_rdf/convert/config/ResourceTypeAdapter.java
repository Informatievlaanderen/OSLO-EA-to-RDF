package com.github.informatievlaanderen.oslo_ea_to_rdf.convert.config;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.io.IOException;

/**
 * GSON adapter for parsing {@link Resource}.
 *
 * @author Dieter De Paepe
 */
public class ResourceTypeAdapter extends TypeAdapter<Resource> {
    @Override
    public void write(JsonWriter jsonWriter, Resource resource) throws IOException {
        if (resource == null || resource.isAnon()) {
            jsonWriter.nullValue();
            return;
        }

        jsonWriter.value(resource.getURI());
    }

    @Override
    public Resource read(JsonReader jsonReader) throws IOException {
        String resource = jsonReader.nextString();
        return ResourceFactory.createResource(resource);
    }
}

# Configurations

This page describes the tag names that were agreed upon for the OSLO project.

## Core vocabulary terms

- `label-nl`
- `label-en`
- `definition-nl`
- `definition-en`
- `usageNote-nl`
- `usageNote-en`

Configuration:

    {
      prefixes: {
        cpsv: "http://purl.org/vocab/cpsv#",
        eu: "http://data.europa.eu/m8g/",
        rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        rdfs: "http://www.w3.org/2000/01/rdf-schema#",
        rov: "http://www.w3.org/ns/regorg#",
        locn: "http://www.w3.org/ns/locn#",
        org: "http://www.w3.org/ns/org#",
        owl: "http://www.w3.org/2002/07/owl#",
        person: "http://www.w3.org/ns/person#",
        skos: "http://www.w3.org/2004/02/skos/core#",
        vann: "http://purl.org/vocab/vann/",
        xsd: "http://www.w3.org/2001/XMLSchema#"
      },
      internalMappings: [
        {
          tag: "label-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          mandatory: true,
          lang: "nl"
        },
        {
          tag: "definition-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#comment",
          mandatory: true,
          lang: "nl"
        },
        {
          tag: "usageNote-nl",
          property: "http://purl.org/vocab/vann/usageNote",
          lang: "nl"
        },
        {
          tag: "label-en",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          mandatory: true,
          lang: "en"
        },
        {
          tag: "definition-en",
          property: "http://www.w3.org/2000/01/rdf-schema#comment",
          mandatory: true,
          lang: "en"
        },
        {
          tag: "usageNote-en",
          property: "http://purl.org/vocab/vann/usageNote",
          lang: "en"
        }
      ],
      externalMappings: [
        {
          tag: "label-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          mandatory: true,
          lang: "nl"
        },
        {
          tag: "definition-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#comment",
          mandatory: true,
          lang: "nl"
        }
      ]
    }
    
## Application profile terms


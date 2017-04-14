# OSLO Configuration

The (first version?) of the OSLO² ontologies were published [online](http://data.vlaanderen.be/ns/)
on 2017-03-31. The ontologies were created using `v1.0` of this tool and using the configuration
listed below.

## Core vocabulary terms

- `label-nl`
- `label-en`
- `definition-nl`
- `definition-en`
- `usageNote-nl`
- `usageNote-en`
- `equivalent`

Configuration:

    {
      prefixes: {
        cpsv: "http://purl.org/vocab/cpsv#",
        dcterms: "http://purl.org/dc/terms/",
        eu: "http://data.europa.eu/m8g/",
        foaf: "http://xmlns.com/foaf/0.1/",
        rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        rdfs: "http://www.w3.org/2000/01/rdf-schema#",
        rov: "http://www.w3.org/ns/regorg#",
        locn: "http://www.w3.org/ns/locn#",
        org: "http://www.w3.org/ns/org#",
        owl: "http://www.w3.org/2002/07/owl#",
        person: "http://www.w3.org/ns/person#",
        prov: "http://www.w3.org/ns/prov#",
        schema: "http://schema.org/",
        skos: "http://www.w3.org/2004/02/skos/core#",
        vann: "http://purl.org/vocab/vann/",
        xsd: "http://www.w3.org/2001/XMLSchema#"
      },
      builtinTags: {
      },
      ontologyMappings: [
        {
          tag: "title-nl",
          property: "http://purl.org/dc/terms/title",
          mandatory: true,
          lang: "nl"
        },
        {
          tag: "title-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          mandatory: true,
          lang: "nl"
        },
        {
          tag: "title-en",
          property: "http://purl.org/dc/terms/title",
          mandatory: true,
          lang: "en"
        },
        {
          tag: "title-en",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          mandatory: true,
          lang: "en"
        },
        {
          tag: "issued",
          property: "http://purl.org/dc/terms/issued",
          mandatory: true,
          type: "http://www.w3.org/2001/XMLSchema#date"
        }
      ],
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
          lang: "en"
        },
        {
          tag: "definition-en",
          property: "http://www.w3.org/2000/01/rdf-schema#comment",
          lang: "en"
        },
        {
          tag: "usageNote-en",
          property: "http://purl.org/vocab/vann/usageNote",
          lang: "en"
        },
    	{
          tag: "equivalent",
          property: "http://www.w3.org/2002/07/owl#equivalentClass",
          type: "http://www.w3.org/2000/01/rdf-schema#Resource"
        }
      ],
      externalMappings: [
        {
          tag: "label-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          mandatory: false,
          lang: "nl"
        },
        {
          tag: "definition-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#comment",
          mandatory: false,
          lang: "nl"
        }
      ]
    }
    
## Application profile terms

- `ap-label-nl`
- `ap-definition-nl`
- `ap-usageNote-nl`
- `ap-codelist`

Configuration:

    {
      prefixes: {
        cpsv: "http://purl.org/vocab/cpsv#",
        dcterms: "http://purl.org/dc/terms/",
        eu: "http://data.europa.eu/m8g/",
        foaf: "http://xmlns.com/foaf/0.1/",
        rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        rdfs: "http://www.w3.org/2000/01/rdf-schema#",
        rov: "http://www.w3.org/ns/regorg#",
        locn: "http://www.w3.org/ns/locn#",
        org: "http://www.w3.org/ns/org#",
        owl: "http://www.w3.org/2002/07/owl#",
        person: "http://www.w3.org/ns/person#",
        prov: "http://www.w3.org/ns/prov#",
        schema: "http://schema.org/",
        skos: "http://www.w3.org/2004/02/skos/core#",
        vann: "http://purl.org/vocab/vann/",
        xsd: "http://www.w3.org/2001/XMLSchema#"
      },
      builtinTags: {
      },
      internalMappings: [
        {
          tag: "ap-label-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          lang: "nl"
        },
        {
          tag: "ap-definition-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#comment",
          mandatory: true,
          lang: "nl"
        },
        {
          tag: "ap-usageNote-nl",
          property: "http://purl.org/vocab/vann/usageNote",
          lang: "nl"
        },
        {
          tag: "ap-codelist",
          property: "http://www.w3.org/2000/01/rdf-schema#seeAlso"
        }
      ],
      externalMappings: [
        {
          tag: "ap-label-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#label",
          lang: "nl"
        },
        {
          tag: "ap-definition-nl",
          property: "http://www.w3.org/2000/01/rdf-schema#comment",
          lang: "nl"
        },
        {
          tag: "ap-usageNote-nl",
          property: "http://purl.org/vocab/vann/usageNote",
          lang: "nl"
        },
        {
          tag: "ap-codelist",
          property: "http://www.w3.org/2000/01/rdf-schema#seeAlso"
        }
      ]
    }
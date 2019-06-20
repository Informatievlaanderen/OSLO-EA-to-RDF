package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.InvalidPropertyURIException;
import org.apache.commons.text.CaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.github.informatievlaanderen.oslo_ea_to_rdf.convert.Tag.*;

/**
 * Functionality to assign URIs to all components from an EA repository.
 *
 * @author Dieter De Paepe
 */

/**
 * Nota Bert Van Nuffelen
 * the interpretation of the ignore flags should better not be done here.
 * it is mixing 2 concerns.
 * probably it is a way to have less error reporting.
 */
public class UriAssigner {
    private final Logger LOGGER = LoggerFactory.getLogger(UriAssigner.class);

    private TagHelper tagHelper;

    public UriAssigner(TagHelper tagHelper) {
        this.tagHelper = tagHelper;
    }

    public Result assignURIs(Iterable<EAPackage> packages, Multimap<String, EAPackage> nameToPackages,
                             Map<EAConnector, EAConnector.Direction> connectorDirections) {
        Map<EAPackage, String> packageURIs = new HashMap<>();
        Map<EAPackage, String> ontologyURIs = new HashMap<>();
        Map<EAElement, String> elementURIs = new HashMap<>();
        Map<EAAttribute, String> attributeURIs = new HashMap<>();
        Map<EAAttribute, String> instanceURIs = new HashMap<>();
        Map<EAConnector, String> connectorURIs = new HashMap<>();
        Map<EAConnector, EAPackage> definingPackages = new HashMap<>();

        assignPackageURIs(packages, packageURIs, ontologyURIs);
        assignNonConnectorURIs(packages, packageURIs, elementURIs, attributeURIs, instanceURIs, nameToPackages);

        // A connector can reference a package as its defining package, meaning it takes on the base URI of that package.
        // This means connectors need to be handled after all package names are assigned an URI.
        assignConnectorURIs(packages, nameToPackages, packageURIs, connectorURIs, definingPackages, connectorDirections);

        // Build indexes for packages and elements
        ListMultimap<String, EAPackage> packageIndex = Multimaps.index(packageURIs.keySet(), packageURIs::get);
        ListMultimap<String, EAElement> elementIndex = Multimaps.index(elementURIs.keySet(), elementURIs::get);
        ListMultimap<String, EAAttribute> instanceIndex = Multimaps.index(instanceURIs.keySet(), instanceURIs::get);

        // Build indexes for properties
        ImmutableListMultimap.Builder<String, Object> builder = ImmutableListMultimap.builder();
        for (Map.Entry<EAAttribute, String> entry : attributeURIs.entrySet())
            builder.put(entry.getValue(), entry.getKey());
        for (Map.Entry<EAConnector, String> entry : connectorURIs.entrySet())
            builder.put(entry.getValue(), entry.getKey());
        ListMultimap<String, Object> propertyIndex = builder.build();

        // Check things having the same URI
        Set<String> claimedKeys = new HashSet<>(packageIndex.keys());
        for (Map.Entry<String, Collection<EAPackage>> entry : packageIndex.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                Iterable<String> conflictingPackages = Iterables.transform(entry.getValue(), EAObject::getPath);
                LOGGER.warn("The following packages map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(conflictingPackages));
//                for (EAPackage pack : entry.getValue()) {
//                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
//                    claimedKeys.add(newKey);
//                    packageURIs.put(pack, newKey);
//                }
            }
        }

        claimedKeys = new HashSet<>(elementIndex.keys());
        for (Map.Entry<String, Collection<EAElement>> entry : elementIndex.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                Iterable<String> conflictingElements = Iterables.transform(entry.getValue(), EAObject::getPath);
                LOGGER.warn("The following elements map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(conflictingElements));
//                for (EAElement element : entry.getValue()) {
//                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
//                    claimedKeys.add(newKey);
//                    elementURIs.put(element, newKey);
//                }
            }
        }

        claimedKeys = new HashSet<>(instanceIndex.keys());
        for (Map.Entry<String, Collection<EAAttribute>> entry : instanceIndex.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                Iterable<String> conflictingElements = Iterables.transform(entry.getValue(), EAObject::getPath);
                LOGGER.warn("The following instances map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(conflictingElements));
//                for (EAAttribute instance : entry.getValue()) {
//                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
//                    claimedKeys.add(newKey);
//                    instanceURIs.put(instance, newKey);
//                }
            }
        }

        claimedKeys = new HashSet<>(propertyIndex.keys());
        for (Map.Entry<String, Collection<Object>> entry : propertyIndex.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                Iterable<String> conflictingAttributes = Iterables.transform(Iterables.filter(entry.getValue(), EAAttribute.class), EAObject::getPath);
                Iterable<String> conflictingConnectors = Iterables.transform(Iterables.filter(entry.getValue(), EAConnector.class), EAObject::getPath);
                LOGGER.warn("The following properties (attribute or connector) map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(Iterables.concat(conflictingAttributes, conflictingConnectors)));
//                for (Object attributeOrConnection : entry.getValue()) {
//                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
//                    claimedKeys.add(newKey);
//                    if (attributeOrConnection instanceof EAAttribute) {
//                        attributeURIs.put((EAAttribute) attributeOrConnection, newKey);
//                    } else {
//                        connectorURIs.put(((EAConnector) attributeOrConnection), newKey);
//                    }
//                }
            }
        }

        return new Result(packageURIs, ontologyURIs, elementURIs, attributeURIs, connectorURIs, instanceURIs, definingPackages);
    }

    private void assignPackageURIs(Iterable<EAPackage> packages, Map<EAPackage, String> packageURIs,
                                   Map<EAPackage, String> ontologyURIs) {
        for (EAPackage eaPackage : packages) {
            if (Boolean.valueOf(tagHelper.getOptionalTag(eaPackage, Tag.IGNORE, "false")))
                continue;

            String packageURI = tagHelper.getSingleValue(eaPackage, PACKAGE_BASE_URI, "http://fixme.com#", true);
            String namespace = packageURI.substring(0, packageURI.length() - 1);
            String ontologyURI = tagHelper.getOptionalTag(eaPackage, PACKAGE_ONTOLOGY_URI, namespace);
            packageURIs.put(eaPackage, packageURI);
            ontologyURIs.put(eaPackage, ontologyURI);
        }
    }

    private void assignNonConnectorURIs(Iterable<EAPackage> packages, Map<EAPackage, String> packageURIs,
                                        Map<EAElement, String> elementURIs,
                                        Map<EAAttribute, String> attributeURIs, Map<EAAttribute, String> instanceURIs,
                                        Multimap<String, EAPackage> nameToPackages) {
        for (EAPackage eaPackage : packages) {
            if (Boolean.valueOf(tagHelper.getOptionalTag(eaPackage, Tag.IGNORE, "false")))
                continue;

            String packageURI = packageURIs.get(eaPackage);

            for (EAElement element : eaPackage.getElements()) {
                if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false")))
                    continue;

                String elementPackageURI = packageURI;

                String packageName = tagHelper.getOptionalTag(element, Tag.DEFINING_PACKAGE, null);
                if (packageName != null) {
                    Collection<EAPackage> referencedPackages = nameToPackages.get(packageName);
                    if (referencedPackages.size() == 0) {
                        LOGGER.warn("Specified package \"{}\" for element \"{}\" was not found.", packageName, element.getPath());
                    } else if (referencedPackages.size() == 1) {
                        elementPackageURI = packageURIs.get(referencedPackages.iterator().next());
                    } else {
                        LOGGER.warn("Ambiguous package name \"{}\" specified for element \"{}\", it matches multiple packages in the project.", packageName, element.getPath());
                        elementPackageURI = packageURIs.get(referencedPackages.iterator().next());
                    }
                }
                elementURIs.put(element, extractURI(element, elementPackageURI));

                for (EAAttribute attribute : element.getAttributes()) {
                    if (Boolean.valueOf(tagHelper.getOptionalTag(attribute, Tag.IGNORE, "false")))
                        continue;

                    String attributePackageURI = packageURI;

                    packageName = tagHelper.getOptionalTag(attribute, Tag.DEFINING_PACKAGE, null);
                    if (packageName != null) {
                        Collection<EAPackage> referencedPackages = nameToPackages.get(packageName);
                        if (referencedPackages.size() == 0) {
                            LOGGER.warn("Specified package \"{}\" for attribute \"{}\" was not found.", packageName, attribute.getPath());
                        } else if (referencedPackages.size() == 1) {
                            attributePackageURI = packageURIs.get(referencedPackages.iterator().next());
                        } else {
                            LOGGER.warn("Ambiguous package name \"{}\" specified for attribute \"{}\", it matches multiple packages in the project.", packageName, element.getPath());
                            attributePackageURI = packageURIs.get(referencedPackages.iterator().next());
                        }
                    }

                    if (element.getType() == EAElement.Type.ENUMERATION) {
                        String namespace = attributePackageURI;
                        if (namespace.endsWith("/") || namespace.endsWith("#"))
                            namespace = namespace.substring(0, attributePackageURI.length() - 1);
                        String instanceNamespace = namespace + "/" + tagHelper.getOptionalTag(element, LOCALNAME, element.getName()) + "/";
                        instanceURIs.put(attribute, extractURI(attribute, instanceNamespace));
                    } else {
                        String uri = extractURI(attribute, attributePackageURI);
                        try {
                            ResourceFactory.createProperty(uri);
                            attributeURIs.put(attribute, uri);
                        } catch (InvalidPropertyURIException e) {
                            LOGGER.error("Invalid property URI \"{}\", will ignore attribute {}.", uri, attribute.getPath());
                        }
                    }
                }
            }
        }
    }

    private void assignConnectorURIs(Iterable<EAPackage> packages, Multimap<String, EAPackage> nameToPackages,
                                     Map<EAPackage, String> packageURIs, Map<EAConnector, String> connectorURIs,
                                     Map<EAConnector, EAPackage> definingPackages, Map<EAConnector, EAConnector.Direction> connectorDirections) {
        Set<EAConnector> normalisedConnectors = new HashSet<>();
        for (EAPackage eaPackage : packages) {
            if (Boolean.valueOf(tagHelper.getOptionalTag(eaPackage, Tag.IGNORE, "false")))
                continue;

            for (EAElement element : eaPackage.getElements()) {
                if (Boolean.valueOf(tagHelper.getOptionalTag(element, Tag.IGNORE, "false")))
                    continue;

                for (EAConnector connector : element.getConnectors()) {
                    // Connectors not in the diagram will not occur in this map.
                    EAConnector.Direction direction = connectorDirections.getOrDefault(connector, EAConnector.Direction.UNSPECIFIED);
                    normalisedConnectors.addAll(Util.extractAssociationElement2(connector, direction));
                }
            }
        }

        for (EAConnector connector : normalisedConnectors) {
            if (Boolean.valueOf(tagHelper.getOptionalTag(connector, Tag.IGNORE, "false")))
                continue;

            if (Boolean.valueOf(tagHelper.getOptionalTag(connector.getSource(), Tag.IGNORE, "false")))
                continue;

            if (Boolean.valueOf(tagHelper.getOptionalTag(connector.getDestination(), Tag.IGNORE, "false")))
                continue;

            // Inheritance related connectors don't get an URI
            if (EAConnector.TYPE_GENERALIZATION.equals(connector.getType()))
                continue;

            // Determine in which package (= ontology) this connector (= property) is defined
            EAPackage definingPackage = null;
            String connectorURI = tagHelper.getOptionalTag(connector, Tag.EXTERNAL_URI, null);

            String packageName = tagHelper.getOptionalTag(connector, Tag.DEFINING_PACKAGE, null);
            Collection<EAPackage> connectionPackage = nameToPackages.get(packageName);
            if (connectionPackage.size() >= 2) {
                LOGGER.warn("Ambiguous package name specified for connector \"{}\", it matches multiple packages in the project.", connector.getPath());
                definingPackage = connectionPackage.iterator().next();
            } else if (connectionPackage.size() == 1) {
                definingPackage = connectionPackage.iterator().next();
            } else {
                EAPackage srcPackage = connector.getSource().getPackage();
                EAPackage dstPackage = connector.getDestination().getPackage();
                if (srcPackage.equals(dstPackage)) {
                    definingPackage = srcPackage;
                    LOGGER.info("Assuming connector \"{}\" belongs to package \"{}\" based on source and target definition.", connector.getPath(), definingPackage.getName());
                }
            }

            if (connectorURI == null) {
                if (definingPackage == null) {
                    LOGGER.warn("Ignoring connector \"{}\" since it lacks a defining package.", connector.getPath());
                    continue;
                }

                String packageURI = packageURIs.get(definingPackage);
                if (packageURI == null) {
                    LOGGER.warn("Connector \"{}\" is defined on an non existing package, it will be ignored.", connector.getPath());
                    continue;
                }

                String localName0 = tagHelper.getOptionalTag(connector, LOCALNAME, connector.getName());
/*
    	  	String localName  = CaseUtils.toCamelCase(localName0, false, null);
                if (localName == null) {
                    LOGGER.warn("Connector \"{}\" does not have a name, it will be ignored.", connector.getPath());
                    continue;
                }
		if (localName0 != localName) {
                    LOGGER.warn("Connector \"{}\" has not a name in camelCase: {}.", connector.getPath(), localName0);
		}
*/

                connectorURI = packageURI + localName0;
            }
            LOGGER.debug("Connector \"{}\" has uri <{}>.", connector.getPath(), connectorURI);

            try {
                ResourceFactory.createProperty(connectorURI);
                definingPackages.put(connector, definingPackage);
                connectorURIs.put(connector, connectorURI);
            } catch (InvalidPropertyURIException e) {
                LOGGER.error("Invalid property URI \"{}\", will ignore connector {}.", connectorURI, connector.getPath());
	    } catch (Exception e) {
               LOGGER.debug("Exception \"{}\" has happend", e);
	       throw e;
            }
        }
    }

    private String extractURI(EAObject element, String packageURI) {
        String temp = tagHelper.getOptionalTag(element, Tag.EXTERNAL_URI, null);
        if (temp != null)
            return temp;

        temp = tagHelper.getOptionalTag(element, Tag.LOCALNAME, null);
        if (temp != null)
            return packageURI + temp;
        else
            return packageURI + element.getName();
    }

    private String findUniqueKey(String startKey, Set<String> claimedKeys) {
        int counter = 1;
        while (claimedKeys.contains(startKey + "-" + counter))
            counter++;
        return startKey + "-" + counter;
    }

    private static class ChosenURI {
        public final String uri;
        public final boolean locked;

        public ChosenURI(String uri, boolean locked) {
            this.uri = uri;
            this.locked = locked;
        }
    }

    public static class Result {
        /**
         * For each package (= ontology), the corresponding base URI to be used for the defined terms.
         */
        public final Map<EAPackage, String> packageURIs;
        /**
         * For each package, the URI of the corresponding owl:Ontology.
         */
        public final Map<EAPackage, String> ontologyURIs;
        /**
         * For each element, the corresponding URI to be used.
         */
        public final Map<EAElement, String> elementURIs;
        /**
         * For each attribute, the corresponding URI to be used.
         */
        public final Map<EAAttribute, String> attributeURIs;
        /**
         * For each connector, the corresponding URI to be used. Each key present here is also present in {@code definingPackages}.
         */
        public final Map<EAConnector, String> connectorURIs;
        /**
         * For each instance, the corresponding URI to be used.
         */
        public final Map<EAAttribute, String> instanceURIs;
        /**
         * Maps each connector to the package it was assigned to. Contains the keys that {@code connectorURIs} contains.
         */
        public final Map<EAConnector, EAPackage> definingPackages;

        public Result(Map<EAPackage, String> packageURIs, Map<EAPackage, String> ontologyURISs, Map<EAElement, String> elementURIs,
                      Map<EAAttribute, String> attributeURIs, Map<EAConnector, String> connectorURIs,
                      Map<EAAttribute, String> instanceURIs, Map<EAConnector, EAPackage> definingPackages) {
            this.packageURIs = packageURIs;
            this.ontologyURIs = ontologyURISs;
            this.elementURIs = elementURIs;
            this.attributeURIs = attributeURIs;
            this.connectorURIs = connectorURIs;
            this.instanceURIs = instanceURIs;
            this.definingPackages = definingPackages;
        }
    }
}

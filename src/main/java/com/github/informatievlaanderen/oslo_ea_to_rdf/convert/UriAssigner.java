package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.*;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TagNames.LOCALNAME;
import static com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TagNames.PACKAGE_BASE_URI;
import static com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TagNames.PACKAGE_ONTOLOGY_URI;

/**
 * Functionality to assign URIs to all components from an EA repository.
 *
 * @author Dieter De Paepe
 */
public class UriAssigner {
    private final Logger LOGGER = LoggerFactory.getLogger(UriAssigner.class);

    public Result assignURIs(Iterable<EAPackage> packages, Multimap<String, EAPackage> nameToPackages) {
        Map<EAPackage, String> packageURIs = new HashMap<>();
        Map<EAPackage, String> ontologyURIs = new HashMap<>();
        Map<EAElement, String> elementURIs = new HashMap<>();
        Map<EAAttribute, String> attributeURIs = new HashMap<>();
        Map<EAAttribute, String> instanceURIs = new HashMap<>();
        Map<EAConnector, String> connectorURIs = new HashMap<>();
        Map<EAConnector, EAPackage> definingPackages = new HashMap<>();

        assignNonConnectorURIs(packages, packageURIs, ontologyURIs, elementURIs, attributeURIs, instanceURIs);

        // A connector can reference a package as its defining package, meaning it takes on the base URI of that package.
        // This means connectors need to be handled after all package names are assigned an URI.
        assignConnectorURIs(packages, nameToPackages, packageURIs, connectorURIs, definingPackages);

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
                Iterable<String> conflictingPackages = Iterables.transform(entry.getValue(), e -> Util.getFullName(e));
                LOGGER.warn("The following packages map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(conflictingPackages));
                for (EAPackage pack : entry.getValue()) {
                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
                    claimedKeys.add(newKey);
                    packageURIs.put(pack, newKey);
                }
            }
        }

        claimedKeys = new HashSet<>(elementIndex.keys());
        for (Map.Entry<String, Collection<EAElement>> entry : elementIndex.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                Iterable<String> conflictingElements = Iterables.transform(entry.getValue(), e -> Util.getFullName(e));
                LOGGER.warn("The following elements map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(conflictingElements));
                for (EAElement element : entry.getValue()) {
                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
                    claimedKeys.add(newKey);
                    elementURIs.put(element, newKey);
                }
            }
        }

        claimedKeys = new HashSet<>(instanceIndex.keys());
        for (Map.Entry<String, Collection<EAAttribute>> entry : instanceIndex.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                Iterable<String> conflictingElements = Iterables.transform(entry.getValue(), e -> Util.getFullName(e));
                LOGGER.warn("The following instances map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(conflictingElements));
                for (EAAttribute instance : entry.getValue()) {
                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
                    claimedKeys.add(newKey);
                    instanceURIs.put(instance, newKey);
                }
            }
        }

        claimedKeys = new HashSet<>(propertyIndex.keys());
        for (Map.Entry<String, Collection<Object>> entry : propertyIndex.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                Iterable<String> conflictingAttributes = Iterables.transform(Iterables.filter(entry.getValue(), EAAttribute.class), e -> Util.getFullName(e));
                Iterable<String> conflictingConnectors = Iterables.transform(Iterables.filter(entry.getValue(), EAConnector.class), e -> Util.getFullName(e));
                LOGGER.warn("The following properties (attribute or connector) map to the same URI ({}): {}",
                        entry.getKey(),
                        Joiner.on(", ").join(Iterables.concat(conflictingAttributes, conflictingConnectors)));
                for (Object attributeOrConnection : entry.getValue()) {
                    String newKey = findUniqueKey(entry.getKey(), claimedKeys);
                    claimedKeys.add(newKey);
                    if (attributeOrConnection instanceof EAAttribute) {
                        attributeURIs.put((EAAttribute) attributeOrConnection, newKey);
                    } else {
                        connectorURIs.put(((EAConnector) attributeOrConnection), newKey);
                    }
                }
            }
        }

        return new Result(packageURIs, ontologyURIs, elementURIs, attributeURIs, connectorURIs, instanceURIs, definingPackages);
    }

    private void assignNonConnectorURIs(Iterable<EAPackage> packages, Map<EAPackage, String> packageURIs,
                                        Map<EAPackage, String> ontologyURIs, Map<EAElement, String> elementURIs,
                                        Map<EAAttribute, String> attributeURIs, Map<EAAttribute, String> instanceURIs) {
        for (EAPackage eaPackage : packages) {
            if (Boolean.valueOf(Util.getOptionalTag(eaPackage, TagNames.IGNORE, "false")))
                continue;

            String packageURI = Util.getMandatoryTag(eaPackage, PACKAGE_BASE_URI, "http://fixme.com#");
            String ontologyURI = Util.getOptionalTag(eaPackage, PACKAGE_ONTOLOGY_URI, packageURI.substring(0, packageURI.length() - 1));
            packageURIs.put(eaPackage, packageURI);
            ontologyURIs.put(eaPackage, ontologyURI);

            for (EAElement element : eaPackage.getElements()) {
                if (Boolean.valueOf(Util.getOptionalTag(element, TagNames.IGNORE, "false")))
                    continue;

                String localName = Util.getOptionalTag(element, LOCALNAME, element.getName());
                String elementURI = Util.getOptionalTag(element, TagNames.EXPLICIT_URI, packageURI + localName);

                elementURIs.put(element, elementURI);

                for (EAAttribute attribute : element.getAttributes()) {
                    if (Boolean.valueOf(Util.getOptionalTag(attribute, TagNames.IGNORE, "false")))
                        continue;

                    localName = Util.getOptionalTag(attribute, LOCALNAME, attribute.getName());

                    if (element.getType() == EAElement.Type.ENUMERATION) {
                        String attributeURI = Util.getOptionalTag(attribute, TagNames.EXPLICIT_URI,
                                ontologyURI + "/" + Util.getOptionalTag(element, LOCALNAME, element.getName()) + "/" + localName);
                        instanceURIs.put(attribute, attributeURI);
                    } else {
                        String attributeURI = Util.getOptionalTag(attribute, TagNames.EXPLICIT_URI, packageURI + localName);
                        attributeURIs.put(attribute, attributeURI);
                    }

                }
            }
        }
    }

    private void assignConnectorURIs(Iterable<EAPackage> packages, Multimap<String, EAPackage> nameToPackages,
                                     Map<EAPackage, String> packageURIs, Map<EAConnector, String> connectorURIs,
                                     Map<EAConnector, EAPackage> definingPackages) {
        for (EAPackage eaPackage : packages) {
            if (Boolean.valueOf(Util.getOptionalTag(eaPackage, TagNames.IGNORE, "false")))
                continue;

            for (EAElement element : eaPackage.getElements()) {
                if (Boolean.valueOf(Util.getOptionalTag(element, TagNames.IGNORE, "false")))
                    continue;

                for (EAConnector connector : element.getConnectors()) {
                    if (Boolean.valueOf(Util.getOptionalTag(connector, TagNames.IGNORE, "false")))
                        continue;

                    // Inheritance related connectors don't get an URI
                    if (EAConnector.TYPE_GENERALIZATION.equals(connector.getType()))
                        continue;

                    // Don't process connectors twice
                    if (connectorURIs.containsKey(connector))
                        continue;

                    // Determine in which package (= ontology) this connector (= property) is defined
                    EAPackage definingPackage = null;
                    String packageName = Util.getOptionalTag(connector, TagNames.DEFINING_PACKAGE, null);
                    Collection<EAPackage> connectionPackage = nameToPackages.get(packageName);
                    if (connectionPackage.size() >= 2) {
                        LOGGER.warn("Ambiguous package name specified for connector \"{}\", it matches multiple packages in the project.", Util.getFullName(connector));
                        definingPackage = connectionPackage.iterator().next();
                    } else if (connectionPackage.size() == 1) {
                        definingPackage = connectionPackage.iterator().next();
                    } else {
                        EAPackage srcPackage = connector.getSource().getPackage();
                        EAPackage dstPackage = connector.getDestination().getPackage();
                        if (srcPackage.equals(dstPackage)) {
                            definingPackage = srcPackage;
                            LOGGER.info("Assuming connector \"{}\" belongs to package \"{}\" based on source and target definition.", Util.getFullName(connector), definingPackage.getName());
                        }
                    }

                    if (definingPackage == null) {
                        LOGGER.warn("Ignoring connector \"{}\" since it lacks a defining package.", Util.getFullName(connector));
                        continue;
                    }

                    String packageURI = packageURIs.get(definingPackage);
                    if (packageURI == null) {
                        LOGGER.warn("Connector \"{}\" is defined on an ignored package (but isn't ignored itself), it will be ignored.", Util.getFullName(connector));
                        continue;
                    }

                    definingPackages.put(connector, definingPackage);

                    if (EAConnector.TYPE_GENERALIZATION.equals(connector.getType()))
                        continue;

                    String localName = Util.getOptionalTag(connector, LOCALNAME, connector.getName());
                    String connectorURI = Util.getOptionalTag(connector, TagNames.EXPLICIT_URI, null);
                    if (connectorURI == null) {
                        if (localName == null) {
                            LOGGER.error("Connector \"{}\" does not have a name, it will be ignored.", Util.getFullName(connector));
                            continue;
                        }
                        connectorURI = packageURI + localName;
                    }

                    connectorURIs.put(connector, connectorURI);
                }
            }
        }
    }

    private String findUniqueKey(String startKey, Set<String> claimedKeys) {
        int counter = 1;
        while (claimedKeys.contains(startKey + "-" + counter))
            counter++;
        return startKey + "-" + counter;
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
         * Maps each connector to the package it was assigned to. Contains the keys that {@code connectorURIs} contains,
         * plus any generalization connections.
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

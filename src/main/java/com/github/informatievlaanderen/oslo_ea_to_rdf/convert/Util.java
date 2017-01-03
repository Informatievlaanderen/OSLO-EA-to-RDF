package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAAttribute;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAConnector;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility method container class.
 *
 * @author Dieter De Paepe
 */
public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private Util() {}

    public static String getFullName(EAPackage pack) {
        String name = pack.getName();
        while (pack.getParent() != null) {
            pack = pack.getParent();
            name = pack.getName() + "." + name;
        }
        return name;
    }

    public static String getFullName(EAElement element) {
        return getFullName(element.getPackage()) + ":" + element.getName();
    }

    public static String getFullName(EAAttribute attribute) {
        return getFullName(attribute.getElement()) + ":" + attribute.getName();
    }

    public static String getFullName(EAConnector connector) {
        return getFullName(connector.getSource())
                + ":" + MoreObjects.firstNonNull(connector.getName(), connector.getGuid());
    }



    public static String getMandatoryTag(EAPackage pack, String key, String backup) {
        List<String> values = pack.getTags().get(key);

        if (values.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for package \"{}\".", key, getFullName(pack));
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for package \"{}\".", key, getFullName(pack));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public static String getMandatoryTag(EAElement element, String key, String backup) {
        List<String> values = element.getTags().get(key);

        if (values.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for package \"{}\".", key, getFullName(element));
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for element \"{}\".", key, getFullName(element));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public static String getMandatoryTag(EAAttribute attribute, String key, String backup) {
        List<String> values = attribute.getTags().get(key);

        if (values.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for package \"{}\".", key, getFullName(attribute));
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for attribute \"{}\".", key, getFullName(attribute));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public static String getMandatoryTag(EAConnector conn, String key, String backup) {
        List<String> values = conn.getTags().get(key);

        if (values.isEmpty()) {
            LOGGER.warn("Missing \"{}\" tag for connector \"{}\".", key, getFullName(conn));
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for connector \"{}\".", key, getFullName(conn));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public static String getOptionalTag(EAPackage pack, String key, String backup) {
        List<String> values = pack.getTags().get(key);

        if (values.isEmpty()) {
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for package \"{}\".", key, getFullName(pack));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public static String getOptionalTag(EAElement element, String key, String backup) {
        List<String> values = element.getTags().get(key);

        if (values.isEmpty()) {
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for element \"{}\".", key, getFullName(element));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public static String getOptionalTag(EAAttribute attribute, String key, String backup) {
        List<String> values = attribute.getTags().get(key);

        if (values.isEmpty()) {
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for attribute \"{}\".", key, getFullName(attribute));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }

    public static String getOptionalTag(EAConnector connector, String key, String backup) {
        List<String> values = connector.getTags().get(key);

        if (values.isEmpty()) {
            return backup;
        } else if (values.size() > 1) {
            LOGGER.warn("Multiple occurrences of tag \"{}\" for connector \"{}\".", key, getFullName(connector));
            return values.get(0);
        } else {
            return values.get(0);
        }
    }
}

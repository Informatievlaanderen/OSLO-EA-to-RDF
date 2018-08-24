package com.github.informatievlaanderen.oslo_ea_to_rdf.convert;

/**
 * Describes whether a property being written to output is from a known package or not.
 *
 * @author Dieter De Paepe
 */
public enum PackageExported {
	/**
	 * We know the package the property belongs to: it is the package being exported.
	 */
	ACTIVE_PACKAGE,
	/**
	 * We know the package the property belongs to: it is not the package being exported.
	 */
	OTHER_PACKAGE,
	/**
	 * We do not know the package the property belongs to.
	 */
	UNKNOWN
}

package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import java.util.List;

/**
 * A package in an Enterprise Architect project. Packages are used to structure the project in a tree structure.
 * A package bundles {@link EAElement}s, {@link EADiagram}s and other {@code EAPackage}s.
 *
 * @author Dieter De Paepe
 */
public interface EAPackage {
    /**
     * Gets the name of this package.
     * @return the name
     */
    String getName();

    /**
     * Gets the GUID of the package.
     * @return the GUID
     */
    String getGuid();

    /**
     * Gets the primary stereotype of this package
     * @return the stereotype, or {@code null}
     */
    String getStereoType();

    /**
     * Gets the notes for this package.
     * @return the notes, or {@code null}
     */
    String getNotes();

    /**
     * Gets the packages contained in this package.
     * @return an immutable list
     */
    List<? extends EAPackage> getPackages();

    /**
     * Gets the package in which this package is contained.
     * @return {@code null} if this is the root package
     */
    EAPackage getParent();

    /**
     * Gets the elements contained in this package.
     * @return an immutable list
     */
    List<? extends EAElement> getElements();

    /**
     * Gets the diagrams contained in this package.
     * @return an immutable list
     */
    List<? extends EADiagram> getDiagrams();

    /**
     * Get the tags associated with this package.
     * @return never {@code null}
     */
    List<EATag> getTags();
}

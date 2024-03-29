package com.github.informatievlaanderen.oslo_ea_to_rdf.ea;

import java.util.List;

/** An object used in Enterprise Architect for modelling purposes. */
public interface EAObject {
  /**
   * Gets the name of the object.
   *
   * @return the name, or {@code null}
   */
  String getName();

  /**
   * Gets the notes of this object.
   *
   * @return the notes, or {@code null}
   */
  String getNotes();

  /**
   * Gets the GUID of this object.
   *
   * @return the GUID, or {@code null}
   */
  String getGuid();

  /**
   * Gets the tags linked to this object.
   *
   * @return never {@code null}
   */
  List<EATag> getTags();

  /**
   * Gets a user-friendly string that can be used to identify the object.
   *
   * <p>The string is built using the names of the {@code EAObject}s in a path-like way (eg:
   * "package1.package2.classX").
   *
   * @return a string
   */
  String getPath();
}

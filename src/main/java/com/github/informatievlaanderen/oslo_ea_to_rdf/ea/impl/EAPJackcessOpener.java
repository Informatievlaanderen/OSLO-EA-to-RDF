package com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import net.ucanaccess.jdbc.JackcessOpenerInterface;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Helper class for opening Enterprise Architect projects using Jackcess.
 */
public class EAPJackcessOpener implements JackcessOpenerInterface {
    public Database open(File fl, String pwd) throws IOException {
        DatabaseBuilder dbd = new DatabaseBuilder(fl);
        dbd.setCharset(Charset.forName("ISO-8859-1"));
        dbd.setAutoSync(false);
        try {
            dbd.setReadOnly(false);
            return dbd.open();
        } catch (IOException e) {
            dbd.setReadOnly(true);
            return dbd.open();

        }
    }
}

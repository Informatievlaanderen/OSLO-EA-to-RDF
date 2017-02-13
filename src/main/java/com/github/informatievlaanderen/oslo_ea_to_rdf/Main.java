package com.github.informatievlaanderen.oslo_ea_to_rdf;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ConversionException;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.Converter;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.RDFOutputHandler;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EADiagram;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EARepository;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl.MemoryRepositoryBuilder;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entry class for command line.
 *
 * @author Dieter De Paepe
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] rawArgs) {
        Args args = new Args();
        JCommander jCommander = new JCommander(args);
        jCommander.setProgramName("java -jar <jarfile>");
        ListArgs listArgs = new ListArgs();
        jCommander.addCommand("list", listArgs);
        ConvertDiagramArgs convertArgs = new ConvertDiagramArgs();
        jCommander.addCommand("convert", convertArgs);

        try {
            jCommander.parse(rawArgs);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            jCommander.usage();
            System.exit(-1);
        }

        if (args.help) {
            jCommander.usage();
            return;
        }

        try {
            if ("list".equals(jCommander.getParsedCommand())) {
                EARepository repo = new MemoryRepositoryBuilder().build(listArgs.eaFile);
                new StructurePrinter().execute(repo, listArgs.printElements);
            } else if ("convert".equals(jCommander.getParsedCommand())) {
                EARepository repo  = new MemoryRepositoryBuilder().build(convertArgs.eaFile);
                List<String> languages = convertArgs.mandatoryLanguages;
                if (convertArgs.includeNoLanguageAttribute)
                    languages.add("");
                RDFOutputHandler rdfOutputHandler = new RDFOutputHandler();
                if (convertArgs.base != null)
                    rdfOutputHandler.addToModel(convertArgs.base.toPath());
                new Converter(repo, MoreObjects.firstNonNull(convertArgs.mandatoryLanguages, Collections.emptyList()), rdfOutputHandler)
                        .convertDiagram(findByName(repo, convertArgs.diagramName));
                rdfOutputHandler.writeToFile(convertArgs.outputFile.toPath());
            } else {
                jCommander.usage();
            }
        } catch (SQLException e) {
            LOGGER.error("An error occurred while reading the EA model.", e);
        } catch (ConversionException | IOException e) {
            LOGGER.error("An error occurred during conversion.",  e);
        }
    }

    private static EADiagram findByName(EARepository repo, String name) throws ConversionException {
        Objects.requireNonNull(name);
        Collection<EADiagram> diagrams = Collections2.filter(repo.getDiagrams(), diagram -> name.equals(diagram.getName()));
        if (diagrams.size() > 1)
            throw new ConversionException("Multiple diagrams share the name \"" + name + "\" - cannot continue.");
        else if (diagrams.isEmpty())
            throw new ConversionException("Diagram not found: " + name + ".");

        return diagrams.iterator().next();
    }

    private static class Args {
        @Parameter(names = {"-h", "--help"}, help = true, hidden = true)
        boolean help;
    }

    @Parameters(commandDescription = "List the structure of the EA file.")
    private static class ListArgs {
        @Parameter(names = {"-i", "--input"}, required = true, description = "The EA project file.")
        File eaFile;

        @Parameter(names = {"--full"}, description = "Also print classes, enumerations and datatypes.")
        boolean printElements;
    }

    @Parameters(commandDescription = "Convert a diagram from an EA file to a RDF turtle file.")
    private static class ConvertDiagramArgs {
        @Parameter(names = {"-i", "--input"}, required = true, description = "The EA project file.")
        File eaFile;

        @Parameter(names = {"-b", "--base"}, required = false, description = "Turtle file containing starting statements.")
        File base;

        @Parameter(names = {"-d", "--diagram"}, required = true, description = "The name of the diagram to convert.")
        String diagramName;

        @Parameter(names = {"-o", "--output"}, required = true, description = "Output file name.")
        File outputFile;

        @Parameter(names = {"--lang"}, variableArity = true, description = "The languages to be read from the model.")
        List<String> mandatoryLanguages;

        @Parameter(names = {"--includeNoLang"}, description = "Also generate string properties without language tag.")
        boolean includeNoLanguageAttribute;
    }
}

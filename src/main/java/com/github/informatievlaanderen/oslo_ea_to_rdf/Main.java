package com.github.informatievlaanderen.oslo_ea_to_rdf;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.TSVOutputHandler;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.ConversionException;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.Converter;
import com.github.informatievlaanderen.oslo_ea_to_rdf.convert.RDFOutputHandler;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EADiagram;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EARepository;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.impl.MemoryRepositoryBuilder;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;

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
        ConvertDiagramToRDFArgs convertRDFArgs = new ConvertDiagramToRDFArgs();
        jCommander.addCommand("convert", convertRDFArgs);
        ConvertDiagramToTSVArgs convertTSVArgs = new ConvertDiagramToTSVArgs();
        jCommander.addCommand("tsv", convertTSVArgs);

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
                EARepository repo  = new MemoryRepositoryBuilder().build(convertRDFArgs.eaFile);
                List<String> languages = convertRDFArgs.mandatoryLanguages;
                if (convertRDFArgs.includeNoLanguageAttribute)
                    languages.add("");
                List<String> externalTermLanguages = new ArrayList<>(languages);
                externalTermLanguages.retainAll(convertRDFArgs.externalTermLanguages);
                RDFOutputHandler rdfOutputHandler = new RDFOutputHandler(externalTermLanguages);
                if (convertRDFArgs.base != null)
                    rdfOutputHandler.addToModel(convertRDFArgs.base.toPath());
                new Converter(repo, MoreObjects.firstNonNull(convertRDFArgs.mandatoryLanguages, Collections.emptyList()), rdfOutputHandler)
                        .convertDiagram(findByName(repo, convertRDFArgs.diagramName));
                rdfOutputHandler.writeToFile(convertRDFArgs.outputFile.toPath());
            } else if ("tsv".equals(jCommander.getParsedCommand())) {
                EARepository repo = new MemoryRepositoryBuilder().build(convertTSVArgs.eaFile);
                List<String> languages = convertTSVArgs.mandatoryLanguages;
                if (convertTSVArgs.includeNoLanguageAttribute)
                    languages.add("");

                try (BufferedWriter writer = Files.newBufferedWriter(convertTSVArgs.outputFile.toPath(), Charsets.UTF_8)) {
                    TSVOutputHandler tsvOutputHandler = new TSVOutputHandler(writer, languages);
                    new Converter(repo, MoreObjects.firstNonNull(convertTSVArgs.mandatoryLanguages, Collections.emptyList()), tsvOutputHandler)
                            .convertDiagram(findByName(repo, convertTSVArgs.diagramName));
                }
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
    private static class ConvertDiagramToRDFArgs {
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

        @Parameter(names = {"--extlang"}, variableArity = true, description = "The languages to be outputted for reused terms (subset of --lang).")
        List<String> externalTermLanguages;

        @Parameter(names = {"--includeNoLang"}, description = "Also generate string properties without language tag.")
        boolean includeNoLanguageAttribute;
    }

    @Parameters(commandDescription = "Create a TSV table of all term information.")
    private static class ConvertDiagramToTSVArgs {
        @Parameter(names = {"-i", "--input"}, required = true, description = "The EA project file.")
        File eaFile;

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

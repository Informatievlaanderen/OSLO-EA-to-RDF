package com.github.informatievlaanderen.oslo_ea_to_rdf;

import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EADiagram;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAElement;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EAPackage;
import com.github.informatievlaanderen.oslo_ea_to_rdf.ea.EARepository;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Command for listing the structure of an Enterprise Architect file.
 *
 * @author Dieter De Paepe
 */
public class StructurePrinter {
    public void execute(EARepository repo, boolean listElements) {
        EAPackage root = repo.getRootPackage();

        recursivePrint(root, 0, listElements);
    }

    private void recursivePrint(EAPackage pack, int indent, boolean listElements) {
        String whitespace = Strings.repeat("  ", indent);
        System.out.printf("%s%s %s %s\n",
                whitespace,
                "Package",
                pack.getName(),
                pack.getGuid());

        printDiagrams(pack, indent + 1);

        if (listElements)
            printElements(pack, indent + 1);

        for (EAPackage child : pack.getPackages()) {
            recursivePrint(child, indent + 1, listElements);
        }
    }

    private void printDiagrams(EAPackage pack, int indent) {
        String whitespace = Strings.repeat("  ", indent);
        List<? extends EADiagram> diagrams = new ArrayList<>(pack.getDiagrams());

        Collections.sort(diagrams, (o1, o2) -> ComparisonChain.start()
                .compare(o1.getName(), o2.getName())
                .compare(o1.getGuid(), o2.getGuid())
                .result());

        for (EADiagram eaDiagram : diagrams) {
            System.out.printf("%s%s %s %s\n",
                    whitespace,
                    "Diagram",
                    eaDiagram.getName(),
                    eaDiagram.getGuid());
        }
    }

    private void printElements(EAPackage pack, int indent) {
        String whitespace = Strings.repeat("  ", indent);
        List<EAElement> elements = new ArrayList<>(pack.getElements());
        Collections.sort(elements, ((o1, o2) -> ComparisonChain.start()
                .compare(o1.getType(), o2.getType())
                .compare(o1.getName(), o2.getName())
                .compare(o1.getGuid(), o2.getGuid())
                .result()));

        for (EAElement element : elements) {
            String type;
            switch (element.getType()) {
                case CLASS: type = "Class"; break;
                case ENUMERATION: type = "Enumeration"; break;
                case DATATYPE: type = "DataType"; break;
                default: type = "?";
            }
            System.out.printf("%s%s %s %s\n",
                    whitespace,
                    type,
                    element.getName(),
                    element.getGuid());
        }
    }
}

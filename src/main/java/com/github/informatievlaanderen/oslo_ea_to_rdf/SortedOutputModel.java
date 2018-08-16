package com.github.informatievlaanderen.oslo_ea_to_rdf;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.ResIteratorImpl;

import java.util.ArrayList;

/**
 * A Jena-compatible model that outputs its elements in alphabetical order.
 */
public class SortedOutputModel extends ModelCom {
    public SortedOutputModel() {
        super(org.apache.jena.graph.Factory.createGraphMem());
    }

    @Override
    public ResIterator listSubjects() {
        ResIterator resIterator = super.listSubjects();

        ArrayList<Resource> subjects = Lists.newArrayList(resIterator);
        subjects.sort((o1, o2) -> ComparisonChain.start()
                .compare(o1.getNameSpace(), o2.getNameSpace(), Ordering.natural().nullsLast())
                .compare(o1.getLocalName(), o2.getLocalName(), Ordering.natural().nullsLast())
                .result());

        return new ResIteratorImpl(subjects.iterator());
    }
}

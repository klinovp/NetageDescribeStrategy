package nl.netage.stardog.describe;

import java.util.stream.Stream;

import com.complexible.common.base.CloseableIterator;
import com.complexible.common.base.Streams;
import com.complexible.common.openrdf.query.ImmutableDataset;
import com.complexible.common.rdf.model.Namespaces;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.index.IndexReader;
import com.complexible.stardog.plan.describe.DescribeStrategy;
import com.complexible.stardog.query.QueryFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQueryResult;

/**
 * Example {@link DescribeStrategy} which returns all statements where the node occurs as a subject or as an object.
 *
 * @author  Netage
 */
public final class NetageDescribeStrategy implements DescribeStrategy {


	@SuppressWarnings("unchecked")
	public Stream<Statement> describe(final QueryFactory theFactory, final Dataset theDataset, final Resource theValue) {
		// This class can be simplified by extending SingleQueryDescribeStrategy but we're showing the full version
		// in case you want to do more than just run a single query.
		Preconditions.checkArgument(theValue != null, "The described value should not be null");

		// The SPARQL spec doesn't define the dataset for DESCRIBE queries.
		// FROM [NAMED] clauses in DESCRIBE queries define it for the WHERE pattern but
		// it doesn't say whether the same dataset should be used for describing matched resources.
		// It should be otherwise queries like DESCRIBE :A FROM :g don't make sense but one may want to
		// describe resources matched in G1 based on information in G2.
		Dataset aDataset = ImmutableDataset.builder()
		                                   .namedGraphs(Iterables.concat(theDataset.getDefaultGraphs(), theDataset.getNamedGraphs()))
		                                   .build();
		final GraphQueryResult aResults = theFactory.graph("construct {graph ?g { ?subject ?p ?object } } where { " +
		                                             "graph ?g { " +
		                                             "{ ?s ?p ?o . bind (?s as ?subject) bind(?o as ?object) } " +
		                                             "union " +
		                                             "{ ?o ?p ?s . bind (?o as ?subject) bind(?s as ?object) } } }",
		                                             Namespaces.STARDOG)
		                                      .dataset(aDataset)
		                                      .parameter("s", theValue)
		                                      .execute();
		
		final GraphQueryResult bResults = theFactory.graph("CONSTRUCT {graph ?g {?o ?p2 ?o2}} WHERE {" 
				+ "graph ?g {?s ?p ?o . " 
				+ "?o ?p2 ?o2"
				+ "filter(STRSTARTS(str(?o),str(?s)}}",                                                                                
                Namespaces.STARDOG)
         .dataset(aDataset)
         .parameter("s", theValue)
         .execute();
		
		CloseableIterator<Statement> aResultsIter = new CloseableIterator.AbstractCloseableIterator<Statement>() {
			public void close() {
				aResults.close();
			}

			@Override
			protected Statement computeNext() {
				if (aResults.hasNext()) {
					return aResults.next();
				}

				return endOfData();
			}
		};
		
		CloseableIterator<Statement> bResultsIter = new CloseableIterator.AbstractCloseableIterator<Statement>() {
			public void close() {
				bResults.close();
			}

			@Override
			protected Statement computeNext() {
				if (bResults.hasNext()) {
					return bResults.next();
				}

				return endOfData();
			}
		};
		
		return Streams.concat(Streams.stream(aResultsIter), Streams.stream(bResultsIter));
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "Describe(Example)";
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof NetageDescribeStrategy;
	}

	public Model describe(IndexReader arg0, long[] arg1, Value arg2) throws StardogException {
		// TODO Auto-generated method stub
		return null;
	}
}
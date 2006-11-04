package edu.mit.simile.babel.generic;

import java.io.Reader;
import java.io.Writer;
import java.util.Locale;
import java.util.Properties;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.sail.Namespace;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.util.iterator.CloseableIterator;

import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.BabelWriter;
import edu.mit.simile.babel.format.RdfXmlFormat;
import edu.mit.simile.babel.format.SerializationFormat;
import edu.mit.simile.babel.type.GenericType;
import edu.mit.simile.babel.type.SemanticType;

public class RdfXmlConverter implements BabelReader, BabelWriter {

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getLabel(java.util.Locale)
	 */
	public String getLabel(Locale locale) {
		return "Serializes generic data to RDF/XML";
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getDescription(java.util.Locale)
	 */
	public String getDescription(Locale locale) {
		return "Serializes generic data to RDF/XML";
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getSemanticType()
	 */
	public SemanticType getSemanticType() {
		return GenericType.s_singleton;
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getSerializationFormat()
	 */
	public SerializationFormat getSerializationFormat() {
		return RdfXmlFormat.s_singleton;
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#read(java.io.Reader, org.openrdf.sail.Sail, java.util.Properties)
	 */
	public void read(Reader reader, Sail sail, Properties properties)
			throws Exception {

		RDFXMLParser parser = new RDFXMLParser();
		parser.setRDFHandler(new RDFHandler() {
			SailConnection m_connection;
			
			public void startRDF() throws RDFHandlerException {
				// nothing
			}
		
			public void handleStatement(Statement s) throws RDFHandlerException {
				try {
					m_connection.addStatement(s.getSubject(), s.getPredicate(), s.getObject(), s.getContext());
				} catch (SailException e) {
					throw new RDFHandlerException(e);
				}
			}
		
			public void handleNamespace(String prefix, String name) throws RDFHandlerException {
				try {
					m_connection.setNamespace(prefix, name);
				} catch (SailException e) {
					throw new RDFHandlerException(e);
				}
			}
		
			public void handleComment(String arg0) throws RDFHandlerException {
				// nothing
			}
		
			public void endRDF() throws RDFHandlerException {
				try {
					m_connection.commit();
				} catch (SailException e) {
					throw new RDFHandlerException(e);
				}
			}
			
			public RDFHandler initialize(SailConnection c) {
				m_connection = c;
				return this;
			}
		}.initialize(sail.getConnection()));
		
		parser.parse(reader, "");
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelWriter#write(java.io.Writer, org.openrdf.sail.Sail, java.util.Properties)
	 */
	public void write(Writer writer, Sail sail, Properties properties)
			throws Exception {
		
		SailConnection connection = sail.getConnection();
		
		RDFXMLWriter rdfWriter = new RDFXMLWriter(writer);
		
		rdfWriter.startRDF();
		CloseableIterator<? extends Namespace> n = sail.getConnection().getNamespaces();
		while (n.hasNext()) {
			Namespace ns = n.next();
			rdfWriter.handleNamespace(ns.getPrefix(), ns.getName());
		}
		
		CloseableIterator<? extends Statement> i = 
			sail.getConnection().getStatements(null, null, null, false);
		while (i.hasNext()) {
			rdfWriter.handleStatement(i.next()); 
		}
		rdfWriter.endRDF();
		
		connection.commit();
	}

}

package edu.mit.simile.babel.generic;

import info.aduna.collections.iterators.CloseableIterator;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.turtle.TurtleParser;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.BabelWriter;
import edu.mit.simile.babel.format.N3Format;
import edu.mit.simile.babel.SerializationFormat;
import edu.mit.simile.babel.GenericType;
import edu.mit.simile.babel.SemanticType;

public class N3Converter implements BabelReader, BabelWriter {

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getLabel(java.util.Locale)
	 */
	public String getLabel(Locale locale) {
		return "Serializes generic data to N3";
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getDescription(java.util.Locale)
	 */
	public String getDescription(Locale locale) {
		return "Serializes generic data to N3";
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
		return N3Format.s_singleton;
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#takesReader()
	 */
	public boolean takesReader() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#read(java.io.InputStream, org.openrdf.sail.Sail, java.util.Properties, java.util.Locale)
	 */
	public void read(InputStream inputStream, Sail sail, Properties properties, Locale locale) throws Exception {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#read(java.io.Reader, org.openrdf.sail.Sail, java.util.Properties, java.util.Locale)
	 */
	public void read(Reader reader, Sail sail, Properties properties, Locale locale)
			throws Exception {

		TurtleParser parser = new TurtleParser();
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
	 * @see edu.mit.simile.babel.BabelWriter#takesWriter()
	 */
	public boolean takesWriter() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelWriter#write(java.io.OutputStream, org.openrdf.sail.Sail, java.util.Properties, java.util.Locale)
	 */
	public void write(OutputStream outputStream, Sail sail, Properties properties, Locale locale) throws Exception {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelWriter#write(java.io.Writer, org.openrdf.sail.Sail, java.util.Properties, java.util.Locale)
	 */
	public void write(Writer writer, Sail sail, Properties properties, Locale locale)
			throws Exception {
		
		SailConnection connection = sail.getConnection();
		
		N3Writer n3Writer = new N3Writer(writer);
		
		n3Writer.startRDF();
			CloseableIterator<? extends Namespace> n = sail.getConnection().getNamespaces();
			try {
				while (n.hasNext()) {
					Namespace ns = n.next();
					n3Writer.handleNamespace(ns.getPrefix(), ns.getName());
				}
			} finally {
				n.close();
			}
			
			CloseableIterator<? extends Statement> i = 
				sail.getConnection().getStatements(null, null, null, false);
			try {
				while (i.hasNext()) {
					n3Writer.handleStatement(i.next()); 
				}
			} finally {
				i.close();
			}
		n3Writer.endRDF();
		
		connection.commit();
	}

}

package edu.mit.simile.babel.generic;

import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.NotImplementedException;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.util.iterator.CloseableIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.mit.simile.babel.BabelWriter;
import edu.mit.simile.babel.format.RSS1p0Format;
import edu.mit.simile.babel.SerializationFormat;
import edu.mit.simile.babel.GenericType;
import edu.mit.simile.babel.SemanticType;

public class RSS1p0Writer implements BabelWriter {

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getLabel(java.util.Locale)
	 */
	public String getLabel(Locale locale) {
		return "Serializes generic data to RSS 1.0";
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getDescription(java.util.Locale)
	 */
	public String getDescription(Locale locale) {
		return "Serializes generic data to RSS 1.0";
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
		return RSS1p0Format.s_singleton;
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
        String url = properties.getProperty("url");
        if (url == null) {
            url = "http://www.example.com/";
        }
        
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        
        Element rootElement = document.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF");
        {
            rootElement.setAttribute("xmlns", "http://purl.org/rss/1.0/");
            rootElement.setAttribute("xmlns:rss", "http://purl.org/rss/1.0/");
            rootElement.setAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
            rootElement.setAttribute("xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            rootElement.setAttribute("xmlns:rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            
            document.appendChild(rootElement);
        }
        
        Element channelElement = document.createElement("channel");
        {
            channelElement.setAttribute("rdf:about", url);
            channelElement.appendChild(_createElementWithText(document, "title", "Exhibit Data"));
            channelElement.appendChild(_createElementWithText(document, "link", url));
            channelElement.appendChild(_createElementWithText(document, "description", "Exhibit data at " + url));
            
            rootElement.appendChild(channelElement);
        }
        
        List<String> itemURIs = new ArrayList<String>();
		
		SailConnection connection = sail.getConnection();
		try {
            CloseableIterator<? extends Statement> statements =
                connection.getStatements(null, RDF.TYPE, null, true);
            try {
                while (statements.hasNext()) {
                    Statement statement = statements.next();
                    
                    Resource subject = statement.getSubject();
                    String subjectURI = subject.toString();
                    
                    Element itemElement = document.createElement("item");
                    {
                        itemElement.setAttribute("rdf:about", subjectURI);
                        itemElement.appendChild(_createElementWithText(
                            document, "title", _getObjectString(subject, RDFS.LABEL, connection)));
                        itemElement.appendChild(_createElementWithText(document, "link", subjectURI));
                        
                        StringBuffer stringBuffer = new StringBuffer();
                        {
                            CloseableIterator<? extends Statement> statements2 =
                                connection.getStatements(subject, null, null, true);
                            try {
                                while (statements2.hasNext()) {
                                    Statement statement2 = statements2.next();
                                    Value value = statement2.getObject();
                                    
                                    stringBuffer.append(value.toString());
                                    stringBuffer.append('\n');
                                }
                            } finally {
                                statements2.close();
                            }
                            
                            itemElement.appendChild(_createElementWithText(
                                document, "description", stringBuffer.toString()));
                        }
                        
                        rootElement.appendChild(itemElement);
                    }
                    
                    itemURIs.add(subjectURI);
                }
            } finally {
                statements.close();
            }
        } finally {
            connection.close();
        }
        
        Element seq = document.createElement("rdf:Seq");
        {
            
            for (String itemURI : itemURIs) {
                Element li = document.createElement("rdf:li");
                li.setAttribute("rdf:resource", itemURI);
                
                seq.appendChild(li);
            }
            
            channelElement.appendChild(seq);
        }
        
        // Write it out
        {
            TransformerFactory  transformerFactory = TransformerFactory.newInstance();
            Transformer         transformer = transformerFactory.newTransformer();
            DOMSource           source = new DOMSource(document);
            StreamResult        result = new StreamResult(writer);
            
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            
            writer.flush();
        }
	}

    static protected Element _createElementWithText(Document document, String tagName, String text) {
        Element element = document.createElement(tagName);
        element.setTextContent(text);
        return element;
    }
    
    static protected String _getObjectString(Resource subject, URI predicate, SailConnection c) {
        Value v = _getObject(subject, predicate, c);
        return v instanceof Literal ? ((Literal) v).getLabel() : null;
    }
    
    static protected Value _getObject(Resource subject, URI predicate, SailConnection c) {
        CloseableIterator<? extends Statement> i = c.getStatements(subject, predicate, null, true);
        try {
            if (i.hasNext()) {
                return i.next().getObject();
            } else {
                return null;
            }
        } finally {
            i.close();
        }
    }
}

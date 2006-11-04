package edu.mit.simile.babel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openrdf.sail.memory.MemoryStore;

import edu.mit.simile.babel.type.SemanticType;
import edu.mit.simile.babel.util.Util;

public class TranslatorServlet extends HttpServlet {
	final static private long serialVersionUID = 2083937775584527297L;
	final static private Logger s_logger = Logger.getLogger(TranslatorServlet.class);
	
	final static private Map<String, BabelConverter> s_converters = 
			new HashMap<String, BabelConverter>();
	
	final static private void addConverter(String name, String className) {
		try {
			s_converters.put(name, (BabelConverter) Class.forName(className).newInstance());
		} catch (Exception e) {
			s_logger.error("Failed to add converter" + name + " of type " + className, e);
		}
	}
	static {
		addConverter("raw", "edu.mit.simile.babel.writers.raw.RawRdfXmlWriter");
		addConverter("bibtex", "edu.mit.simile.babel.readers.bibtex.BibtexReader");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		Properties 	readerProperties = new Properties();
		Properties 	writerProperties = new Properties();
		String		readerName = null;
		String		writerName = null;
		String[]	urls = null;
		
		Enumeration<String> names = getParameterNames(request);
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String[] values = request.getParameterValues(name);
			
			if (name.startsWith("in-")) {
				readerProperties.setProperty(name.substring(3), Util.join(values, ';'));
			} else if (name.startsWith("out-")) {
				writerProperties.setProperty(name.substring(4), Util.join(values, ';'));
			} else if (name.equals("url")) {
				urls = values;
			} else if (name.equals("reader")) {
				readerName = values[0];
			} else if (name.equals("writer")) {
				writerName = values[0];
			}
		}
		
		if (readerName == null) {
			s_logger.warn("No reader name in request");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} else if (writerName == null) {
			s_logger.warn("No writer name in request");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		BabelConverter reader = s_converters.get(readerName); 
		BabelConverter writer = s_converters.get(writerName); 
		if (reader == null) {
			s_logger.warn("No reader of name " + readerName);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} else if (writer == null) {
			s_logger.warn("No writer of name " + writerName);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		SemanticType readerType = reader.getSemanticType();
		SemanticType writerType = writer.getSemanticType();
		if (!writerType.getClass().isInstance(readerType)) {
			s_logger.warn(
				"Writer class " + writerType.getClass().getName() + 
				" cannot take input from reader class " + readerType.getClass().getName()
			);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Enumeration<String> getParameterNames(HttpServletRequest request) {
		return (Enumeration<String>) request.getParameterNames();
	}
	
	protected void internalPost(
		BabelConverter 		reader, 
		BabelConverter 		writer, 
		Properties			readerProperties,
		Properties			writerProperties,
		HttpServletRequest 	request, 
		HttpServletResponse response
	) throws Exception {
		response.setCharacterEncoding("UTF-8");
		response.setContentType(writer.getSerializationFormat().getMimetype());
		
		MemoryStore store = new MemoryStore();
		store.initialize(); {
			Writer bufferedWriter = new BufferedWriter(
				new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
			
			reader.read(request.getReader(), store, readerProperties);
			writer.write(bufferedWriter, store, writerProperties);
			
			bufferedWriter.close();
		} store.shutDown();
		
		response.setStatus(HttpServletResponse.SC_OK);
	}
}

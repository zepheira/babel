package edu.mit.simile.babel;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class Babel {
	final static private Logger s_logger = Logger.getLogger(TranslatorServlet.class);
	
	final static public Map<String, BabelConverter> s_converters = 
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
}

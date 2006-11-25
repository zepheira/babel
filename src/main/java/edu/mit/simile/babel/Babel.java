package edu.mit.simile.babel;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;

public class Babel {
	final static private Logger s_logger = Logger.getLogger(TranslatorServlet.class);
	
	final static public Map<String, BabelReader> s_readers = 
		new HashMap<String, BabelReader>();
	
	final static public Map<String, BabelWriter> s_writers = 
		new HashMap<String, BabelWriter>();

	final static public Map<String, String> s_previewTemplates = 
		new HashMap<String, String>();
	
	final static private void addReader(String name, String className) {
		try {
			s_readers.put(name, (BabelReader) Class.forName(className).newInstance());
		} catch (Exception e) {
			s_logger.error("Failed to add reader " + name + " of type " + className, e);
		}
	}
	final static private void addWriter(String name, String className) {
		try {
			s_writers.put(name, (BabelWriter) Class.forName(className).newInstance());
		} catch (Exception e) {
			s_logger.error("Failed to add writer " + name + " of type " + className, e);
		}
	}
	
	static {
		addReader("rdf-xml", "edu.mit.simile.babel.generic.RdfXmlConverter");
		addReader("n3", "edu.mit.simile.babel.generic.N3Converter");
		
		addWriter("rdf-xml", "edu.mit.simile.babel.generic.RdfXmlConverter");
		addWriter("n3", "edu.mit.simile.babel.generic.N3Converter");
		
		addReader("bibtex", "edu.mit.simile.babel.bibtex.BibtexReader");
		addWriter("exhibit-json", "edu.mit.simile.babel.exhibit.ExhibitJsonWriter");
		addWriter("bibtex-exhibit-json", "edu.mit.simile.babel.exhibit.BibtexExhibitJsonWriter");
		addReader("tsv", "edu.mit.simile.babel.tsv.TSVReader");
		
		s_previewTemplates.put("exhibit-json", "exhibit.vt");
		s_previewTemplates.put("bibtex-exhibit-json", "bibtex-exhibit.vt");
	}
	
    final static String s_urlEncoding = "UTF-8";
    final static URLCodec s_codec = new URLCodec();
    
    final static String decode(String s) {
        try {
            return s_codec.decode(s, s_urlEncoding);
        } catch (Exception e) {
            throw new RuntimeException("Exception decoding " + s + " with " + s_urlEncoding + " encoding.");
        }
    }
}

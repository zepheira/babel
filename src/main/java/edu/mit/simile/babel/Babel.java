package edu.mit.simile.babel;

import java.util.HashMap;
import java.util.Map;

public class Babel {
	final static public Map<String, String> s_readers = 
		new HashMap<String, String>();
	
	final static public Map<String, String> s_writers = 
		new HashMap<String, String>();

	final static public Map<String, String> s_previewTemplates = 
		new HashMap<String, String>();
	
	final static private void addReader(String name, String className) {
		s_readers.put(name, className);
	}
	final static private void addWriter(String name, String className) {
		s_writers.put(name, className);
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
	
	static BabelReader getReader(String name) {
		try {
			return (BabelReader) Class.forName(s_readers.get(name)).newInstance();
		} catch (Exception e) {
			return null;
		}
	}
	static BabelWriter getWriter(String name) {
		try {
			return (BabelWriter) Class.forName(s_writers.get(name)).newInstance();
		} catch (Exception e) {
			return null;
		}
	}
}

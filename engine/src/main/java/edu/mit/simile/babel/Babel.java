package edu.mit.simile.babel;

import java.util.HashMap;
import java.util.Map;

public class Babel {

    final static public Map<String, String> s_readers = new HashMap<String, String>();
    final static public Map<String, String> s_writers =  new HashMap<String, String>();

    final static public Map<String, String> s_readersFromMimeType = new HashMap<String, String>();
    final static public Map<String, String> s_writersFromMimeType =  new HashMap<String, String>();

    final static public Map<String, String> s_previewTemplates = new HashMap<String, String>();

    static {
        s_readers.put("rdf-xml", "edu.mit.simile.babel.generic.RdfXmlConverter");
        s_readers.put("n3", "edu.mit.simile.babel.generic.N3Converter");
        s_readers.put("tsv", "edu.mit.simile.babel.tsv.TSVReader");
        s_readers.put("xls", "edu.mit.simile.babel.xls.XLSReader");
        s_readers.put("bibtex", "edu.mit.simile.babel.bibtex.BibtexReader");
        s_readers.put("exhibit-json", "edu.mit.simile.babel.exhibit.ExhibitJsonReader");
        s_readers.put("exhibit-html", "edu.mit.simile.babel.exhibit.ExhibitWebPageReader");

        s_readersFromMimeType.put("application/rdf+xml", "edu.mit.simile.babel.generic.RdfXmlConverter");
        s_readersFromMimeType.put("application/rdf+n3", "edu.mit.simile.babel.generic.N3Converter");
        s_readersFromMimeType.put("application/rdf+turtle", "edu.mit.simile.babel.generic.N3Converter");
        s_readersFromMimeType.put("text/tab-separated-values", "edu.mit.simile.babel.tsv.TSVReader");
        s_readersFromMimeType.put("application/vnd.ms-excel", "edu.mit.simile.babel.xls.XLSReader");
        s_readersFromMimeType.put("text/x-bibtex", "edu.mit.simile.babel.bibtex.BibtexReader");
        s_readersFromMimeType.put("application/json+exhibit", "edu.mit.simile.babel.exhibit.ExhibitJsonReader");
        s_readersFromMimeType.put("text/html+exhibit", "edu.mit.simile.babel.exhibit.ExhibitWebPageReader");
                
        s_writers.put("rdf-xml", "edu.mit.simile.babel.generic.RdfXmlConverter");
        s_writers.put("n3", "edu.mit.simile.babel.generic.N3Converter");
        s_writers.put("rss1.0", "edu.mit.simile.babel.generic.RSS1p0Writer");
        s_writers.put("exhibit-json", "edu.mit.simile.babel.exhibit.ExhibitJsonWriter");
        s_writers.put("exhibit-jsonp", "edu.mit.simile.babel.exhibit.ExhibitJsonpWriter");
        s_writers.put("bibtex-exhibit-json", "edu.mit.simile.babel.exhibit.BibtexExhibitJsonWriter");
        s_writers.put("bibtex-exhibit-jsonp", "edu.mit.simile.babel.exhibit.BibtexExhibitJsonpWriter");

        s_writersFromMimeType.put("application/rdf+xml", "edu.mit.simile.babel.generic.RdfXmlConverter");
        s_writersFromMimeType.put("application/rdf+n3", "edu.mit.simile.babel.generic.N3Converter");
        s_writersFromMimeType.put("application/rdf+turtle", "edu.mit.simile.babel.generic.N3Converter");
        s_writersFromMimeType.put("application/json+exhibit", "edu.mit.simile.babel.exhibit.ExhibitJsonWriter");
        s_writersFromMimeType.put("application/jsonp+exhibit", "edu.mit.simile.babel.exhibit.ExhibitJsonpWriter");
        
        s_previewTemplates.put("exhibit-json", "exhibit.vt");
        s_previewTemplates.put("bibtex-exhibit-json", "bibtex-exhibit.vt");
    }

    static public BabelReader getReader(String name) {
        try {
            return (BabelReader) Class.forName(s_readers.get(name)).newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    static public BabelWriter getWriter(String name) {
        try {
            return (BabelWriter) Class.forName(s_writers.get(name)).newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    
    static public BabelReader getReaderFromMimeType(String mimeType) {
        try {
            return (BabelReader) Class.forName(s_readersFromMimeType.get(mimeType)).newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    static public BabelWriter getWriterFromMimeType(String mimeType) {
        try {
            return (BabelWriter) Class.forName(s_writersFromMimeType.get(mimeType)).newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    
}

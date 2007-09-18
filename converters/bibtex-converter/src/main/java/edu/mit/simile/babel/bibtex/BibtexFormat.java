package edu.mit.simile.babel.bibtex;

import java.util.Locale;

import edu.mit.simile.babel.SerializationFormat;

/**
 * @author dfhuynh
 *
 */
public class BibtexFormat implements SerializationFormat {
	final static public BibtexFormat s_singleton = new BibtexFormat();
	
	protected BibtexFormat() {
		// nothing
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.format.SerializationFormat#getLabel(java.util.Locale)
	 */
	public String getLabel(Locale locale) {
		return "Bibtex";
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.format.SerializationFormat#getDescription(java.util.Locale)
	 */
	public String getDescription(Locale locale) {
		return "Bibtex";
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.format.SerializationFormat#getMimetype()
	 */
	public String getMimetype() {
		return "text/plain";
	}
}

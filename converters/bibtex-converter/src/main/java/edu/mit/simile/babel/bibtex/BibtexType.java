package edu.mit.simile.babel.bibtex;

import java.util.Locale;

import edu.mit.simile.babel.GenericType;

public class BibtexType extends GenericType {
	final static public BibtexType s_singleton = new BibtexType();
	
	protected BibtexType() {
		// nothing
	}

	public String getDescription(Locale locale) {
		return "Bibtex format";
	}

	public String getLabel(Locale locale) {
		return "Bibtex format";
	}

}

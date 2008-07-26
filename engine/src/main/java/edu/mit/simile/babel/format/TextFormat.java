package edu.mit.simile.babel.format;

import java.util.Locale;

import edu.mit.simile.babel.SerializationFormat;

/**
 * @author stefanom
 *
 */
public class TextFormat implements SerializationFormat {
	final static public TextFormat s_singleton = new TextFormat();
	
	protected TextFormat() {
		// nothing
	}

	public String getLabel(Locale locale) {
		return "Text";
	}
	
	public String getDescription(Locale locale) {
		return "Text";
	}
	
	public String getMimetype() {
		return "text/plain";
	}
}

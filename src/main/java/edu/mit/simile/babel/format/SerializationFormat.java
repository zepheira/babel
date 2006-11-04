package edu.mit.simile.babel.format;

import java.util.Locale;

/**
 * @author dfhuynh
 *
 */
public interface SerializationFormat {
	/**
	 * @param locale
	 * @return
	 */
	public String getLabel(Locale locale);
	
	/**
	 * @param locale
	 * @return
	 */
	public String getDescription(Locale locale);
	
	/**
	 * @return
	 */
	public String getMimetype();
}

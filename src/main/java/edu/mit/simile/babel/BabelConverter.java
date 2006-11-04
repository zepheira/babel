package edu.mit.simile.babel;

import java.io.Reader;
import java.io.Writer;
import java.util.Locale;
import java.util.Properties;

import org.openrdf.sail.Sail;

import edu.mit.simile.babel.format.SerializationFormat;
import edu.mit.simile.babel.type.SemanticType;

public interface BabelConverter {
	public String getLabel(Locale locale);
	public String getDescription(Locale locale);
	
	public SemanticType getSemanticType();
	public SerializationFormat getSerializationFormat();
	
	public void read(Reader reader, Sail sail, Properties properties) throws Exception;
	public void write(Writer writer, Sail sail, Properties properties) throws Exception;
}

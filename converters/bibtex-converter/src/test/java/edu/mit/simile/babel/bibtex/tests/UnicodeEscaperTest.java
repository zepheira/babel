package edu.mit.simile.babel.bibtex.tests;

import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.mit.simile.babel.bibtex.BibtexUtils;

public class UnicodeEscaperTest {

    static Logger logger = Logger.getLogger(UnicodeEscaperTest.class);
    
    String[] original = {
        "Perù",
        "Viégas",
    };
    
    String[] converted = {
        "Per\\\\u00f9",
        "Vi\\\\u00e9gas",
    };

    @Test public void testEscaper() throws Exception {
        int counter = 0;
        for (int i = 0; i < original.length; i++) {
            try {
                Reader o = new StringReader(original[i]);
                Reader e = BibtexUtils.unescapeUnicode(o);
                StringWriter w = new StringWriter();
                IOUtils.copy(e, w);
                w.close();
                String escaped = w.toString();
                logger.info(original[i] + " -> " + escaped + " [" + converted[i] + "]");
                if (converted[i].equals(escaped)) {
                    counter++;
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
        
        assertTrue(counter == original.length);
    }
    
}

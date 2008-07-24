package edu.mit.simile.babel.bibtex.tests;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import org.junit.*;
import static org.junit.Assert.*;

import edu.mit.simile.babel.bibtex.BibtexCleanerReader;
import edu.mit.simile.babel.bibtex.BibtexUtils;
import edu.mit.simile.babel.bibtex.internal.BibtexGrammar;

public class BibtexParserTest {

    static Logger logger = Logger.getLogger(BibtexParserTest.class);
    
    String[] files = {
        "allstrings.bib",
        "comments.bib",
        "dups.bib",
        "simple.bib",
        "zotero.bib",
        "main.bib",
        "karger.bib",
        "demaine.bib",
        "endnote.bib",
        "edge_cases.bib"
    };
    
    
    @Test public void testParser() throws Exception {
        int counter = 0;
        for (int i = 0; i < files.length; i++) {
            logger.info("Parsing: " + files[i]);
            InputStream stream = null;
            try {
                stream = this.getClass().getClassLoader().getResourceAsStream(files[i]);
                BibtexGrammar p = new BibtexGrammar(
                    new BibtexCleanerReader(
                        new BufferedReader(
                            BibtexUtils.unescapeUnicode(
                                new InputStreamReader(stream, "US-ASCII")
                            )
                        )
                    )
                );
                p.parse();
                p.printout();
                counter++;
            } catch (Exception e) {
                logger.error(e);
                stream.close();
            }
        }
        
        assertTrue(counter == files.length);
    }
    
}

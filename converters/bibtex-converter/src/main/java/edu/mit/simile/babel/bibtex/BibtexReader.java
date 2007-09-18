/*
 *  (c) Copyright The SIMILE Project 2003-2004. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package edu.mit.simile.babel.bibtex;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

import edu.mit.simile.babel.BabelReader;
import edu.mit.simile.babel.SerializationFormat;
import edu.mit.simile.babel.bibtex.internal.BibtexGrammar;
import edu.mit.simile.babel.SemanticType;
import edu.mit.simile.babel.util.ListMap;

/**
 * @author matsakis
 * @author dfhuynh
 *
 */
public final class BibtexReader implements BabelReader {
	final static private Logger s_logger = Logger.getLogger(BibtexReader.class);
	
	final static private String s_namespace = "http://simile.mit.edu/2006/11/bibtex#";
	static private long 		s_idGenerator = 0;
	
    final static String s_urlEncoding = "UTF-8";
    final static URLCodec s_codec = new URLCodec();

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getLabel(java.util.Locale)
	 */
	public String getLabel(Locale locale) {
		return "Bibtex Reader";
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getDescription(java.util.Locale)
	 */
	public String getDescription(Locale locale) {
		return "Reads bibtex format";
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#getOutputSemanticType()
	 */
	public SemanticType getSemanticType() {
		return BibtexType.s_singleton;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelConverter#getSerializationFormat()
	 */
	public SerializationFormat getSerializationFormat() {
		return BibtexFormat.s_singleton;
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelConverter#getOutputMimetype()
	 */
	public String getOutputMimetype() {
		return "text/plain";
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#takesReader()
	 */
	public boolean takesReader() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#read(java.io.InputStream, org.openrdf.sail.Sail, java.util.Properties, java.util.Locale)
	 */
	public void read(InputStream inputStream, Sail sail, Properties properties, Locale locale) throws Exception {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see edu.mit.simile.babel.BabelReader#read(java.io.Reader, org.openrdf.sail.Sail, java.util.Properties, java.util.Locale)
	 */
	public void read(Reader reader, Sail sail, Properties properties, Locale locale) throws Exception {
		String ourNamespace = properties.getProperty("namespace");
		
		reader = unescapeUnicode(reader);
		
		BibtexGrammar parser = new BibtexGrammar(new BibtexCleanerReader(reader));
		parser.parse();
		
		List<BibMap> records = parser.getRecords();
		ListMap keymap = new ListMap();
		preprocess(records, keymap);
		
		/*
		 * Set namespaces
		 */
		SailConnection c = sail.getConnection();
		try {
			c.setNamespace("bibtex", s_namespace);
			c.commit();
		} catch (SailException e) {
			c.rollback();
			c.close();
			throw e;
		}

		/*
		 * Assert as RDF
		 */
		try {
			URI publicationType = new URIImpl(s_namespace + "Publication");
			URI authorType = new URIImpl(s_namespace + "Author");
			URI keyPredicate = new URIImpl(s_namespace + "key");
			URI publicationTypePredicate = new URIImpl(s_namespace + "type");
			
			ListMap processedKeys = new ListMap();
			Map<String, String> authorLastNames = new HashMap<String, String>();
			Map<String, String> authorOriginalNames = new HashMap<String, String>();
			
			for (BibMap rec : records) {
				String URI = rec.getURI();
				String key = rec.getKey();
				
				if (processedKeys.count(key) > 0) {
					s_logger.warn(processedKeys.check(key, URI) ?
						"Found identical entry with key " + key :
						"Found alternate entry with key " + key);
				}
				processedKeys.put(key, URI);
				
				Resource record = new URIImpl(URI);
				
				/*
				 * Assert basic properties
				 */
				c.addStatement(record, RDF.TYPE, publicationType);
				c.addStatement(record, keyPredicate, new LiteralImpl(key));
				c.addStatement(record, publicationTypePredicate, new LiteralImpl(rec.getType()));
				
				try {
					/*
					 * Assert the remaining properties of the record
					 */
					Iterator predicates = rec.keySet().iterator();			
					while (predicates.hasNext()) {
						String p = URLEncoder.encode((String) predicates.next(), "UTF-8");
						String v = (String) rec.get(p);
						
						if (v == null) {
							continue;
						} else {
							v = unescapeUnicode(v).replaceAll("\\s+", " ");;
						}
						
						URIImpl predicate = new URIImpl(s_namespace + p);
						
						/*
						 * Split authors into a sequence.
						 */
						if ("author".equals(p)) {
							List<String> elements = new LinkedList<String>();
							
							String[] segments = StringUtils.splitByWholeSeparator(v, " and ");
							for (int x = 0; x < segments.length; x++) {
								String originalName = unescapeBibtexSpecialCharacters(segments[x].trim());
								if (originalName.startsWith("and ")) {
									originalName = originalName.substring(4);
								}
								String fullName = originalName;
								String lastName = null;
								
								int comma = fullName.indexOf(',');
								if (comma > 0) {
									lastName = fullName.substring(0, comma).trim();
								} else {
									int space = fullName.lastIndexOf(' ');
									if (space > 0) {
										lastName = fullName.substring(space + 1);
										fullName = lastName + ", " + fullName.substring(0, space).trim();
									} else {
										lastName = fullName;
									}
								}
								
								elements.add(fullName);
								authorLastNames.put(fullName, lastName);
								authorOriginalNames.put(fullName, originalName);
							}
							
							Resource sequence = new BNodeImpl("seq" + s_idGenerator++);
							c.addStatement(sequence, RDF.TYPE, RDF.SEQ);
							c.addStatement(record, predicate, sequence);
							
							int count = 0;
							for (String s : elements) {
								c.addStatement(
									sequence, 
									new URIImpl(RDF.NAMESPACE + "_" + (++count)), 
									new URIImpl(ourNamespace + s_codec.encode(s, s_urlEncoding))
								);
							}
						} else {
							Value value = (p.equals("crossref") && keymap.containsKey(v))
								? (Value) new URIImpl(((BibMap) keymap.get(v)).getURI())
								: (Value) new LiteralImpl(unescapeBibtexSpecialCharacters(v));
								
							c.addStatement(record, predicate, value);	
						}
					}
				} catch (UnsupportedEncodingException e) {
					// won't happen, but quiets the compiler
				}
			}
			
			URIImpl lastNamePredicate = new URIImpl(s_namespace + "last-name");
			URIImpl originalNamePredicate = new URIImpl(s_namespace + "original-name");
			for (String fullName : authorLastNames.keySet()) {
				URI resource = new URIImpl(ourNamespace + s_codec.encode(fullName, s_urlEncoding));
				
				c.addStatement(resource, RDF.TYPE, authorType);
				c.addStatement(resource, RDFS.LABEL, new LiteralImpl(fullName));
				c.addStatement(resource, lastNamePredicate, new LiteralImpl(authorLastNames.get(fullName)));
				c.addStatement(resource, originalNamePredicate, new LiteralImpl(authorOriginalNames.get(fullName)));
			}
			
			c.commit();
		} catch (Exception e) {
			c.rollback();
			throw e;
		} finally {
			c.close();
		}
	}
	
	private void preprocess(List<BibMap> records, ListMap keymap) {
		/*
		 * fill a map of keys to records, normalizing strings at the same time
		 */ 
		for (BibMap record : records) {
			record.normalizeStrings();
			keymap.put(record.getKey(), record);
		}

		/*
		 * resolve cross-references
		 */
		for (BibMap record : records) {
			String key = record.getKey();
			
			if (record.containsKey("crossref")){
				String crosskey = (String) record.get("crossref");
				
				if (!keymap.containsKey(crosskey)) {
					s_logger.warn("Record " + key + " contains undefined crossref " + crosskey);
 				} else {
					if (keymap.count(crosskey) > 1) {
						s_logger.warn("Ambiguous crossref " + crosskey + " in " + key);
					}
					
					Map ref = (Map) keymap.get(crosskey);
					if (ref.containsKey("crossref")) {
						s_logger.warn("Ignoring nested crossref in " + crosskey + 
								" while resolving crossrefs for " + key);
					}
					
					for (Object rkey : ref.keySet()) {
						if(!record.containsKey(rkey) && !rkey.equals("crossref")) {
							record.put(rkey, ref.get(rkey));
						}
					}
				}
			}
		}

		/*
		 * Mint URIs
		 */ 
		for (BibMap record : records) {
			record.createURI();
		}
	}
	
	private Reader unescapeUnicode(Reader reader) throws IOException {
		StringWriter writer = new StringWriter();
		
		char[] chars = new char[1024];
		int l = 0;
		while ((l = reader.read(chars)) > 0) {
			for (int k = 0; k < l; k++) {
				char c = chars[k];
				
	            if ((c & 0xFF80) == 0) { // 2-digit hex
	            	writer.append(c);
	            } else { // Unicode.
	                writer.append("\\u");
	                
		            // append hexadecimal form of c left-padded with 0
		            for (int shift = (4 - 1) * 4; shift >= 0; shift -= 4) {
		                int digit = 0xf & (c >> shift);
		                int hc = (digit < 10) ? '0' + digit : 'a' - 10 + digit;
		                writer.append((char)hc);
		            }
	            }
			}
        }
		writer.close();
		
		String input = writer.toString();
		
		return new StringReader(input);
	}
	
	private String unescapeUnicode(String s) {
		if (s.indexOf("\\u") < 0) { 
			return s;
		}
		
		StringBuffer sb = new StringBuffer(s.length());
		int l = 0;
		int u = s.indexOf("\\u", l);
		while (u >= 0) {
			sb.append(s.substring(l, u));
			try {
				sb.append((char) Integer.parseInt(s.substring(u + 2, u + 6), 16));
				l = u + 6;
			} catch (Exception e) {
				sb.append("\\u");
				l = u + 2;
			}
			u = s.indexOf("\\u", l);
		}
		sb.append(s.substring(l));
		
		return sb.toString();
	}
	
	private String unescapeBibtexSpecialCharacters(String s) {
		s = s.replace('~', ' ');
		
		if (s.indexOf('\\') < 0) {
			return s;
		}
		
		StringBuffer sb = new StringBuffer(s.length());
		int current = 0;
		int next;
		
		outer:while ((next = s.indexOf('\\', current)) >= 0) {
			if (next > current && s.charAt(next - 1) == '{') { // cases like {\'a} and {\aa} 
				sb.append(s.subSequence(current, next - 1));
				
				if (next + 3 < s.length() && s.charAt(next + 3) == '}') { // cases like {\'a}
					char c1 = s.charAt(next + 1);
					char c2 = s.charAt(next + 2);
					
					for (int i = 0; i < s_accentedCharacters.length; i++) {
						if (c1 == s_accentedCharacters[i][0] && c2 == s_accentedCharacters[i][1]) {
							sb.append(s_accentedCharacters[i][2]);
							current = next + 4;
							continue outer;
						}
					}
				}
				
				for (int i = 0; i < s_nationalSymbols.length; i++) {
					String code = s_nationalSymbols[i][0];
					int l = code.length();
					int end = next + l + 1;
					if (end < s.length() && s.charAt(end) == '}' && s.substring(next + 1, end).equals(code)) {
						sb.append(s_nationalSymbols[i][1]);
						current = end + 1;
						continue outer;
					}
				}
				
				sb.append(s.subSequence(next - 1, next + 1));
				current = next + 1;
			} else { // cases like \'{a} and \aa
				sb.append(s.subSequence(current, next));
				
				if (next + 4 < s.length() && 
					s.charAt(next + 2) == '{' && 
					s.charAt(next + 4) == '}') { // cases like \'{a}
					
					char c1 = s.charAt(next + 1);
					char c2 = s.charAt(next + 3);
					
					for (int i = 0; i < s_accentedCharacters.length; i++) {
						if (c1 == s_accentedCharacters[i][0] && c2 == s_accentedCharacters[i][1]) {
							sb.append(s_accentedCharacters[i][2]);
							current = next + 5;
							continue outer;
						}
					}
				}
				
				for (int i = 0; i < s_nationalSymbols.length; i++) {
					String code = s_nationalSymbols[i][0];
					int l = code.length();
					int end = next + l;
					if (end < s.length() && s.substring(next + 1, end + 1).equals(code)) {
						sb.append(s_nationalSymbols[i][1]);
						current = end + 1;
						continue outer;
					}
				}
				
				sb.append(s.charAt(next));
				current = next + 1;
			}
		}
		sb.append(s.subSequence(current, s.length()));
		
		return sb.toString();
	}
	
	final static private char[][] s_accentedCharacters = new char[][] {
		{ 	'`', 	'a', 	'\u00e0' },
		{	'\'',	'a',	'\u00e1' },
		{	'^',	'a',	'\u00e2' },
		{	'~',	'a',	'\u00e3' },
		{	'"',	'a',	'\u00e4' },
		{	'=',	'a',	'\u0101' },
		{	'u',	'a',	'\u0103' },
		
		{ 	'`', 	'A', 	'\u00c0' },
		{	'\'',	'A',	'\u00c1' },
		{	'^',	'A',	'\u00c2' },
		{	'~',	'A',	'\u00c3' },
		{	'"',	'A',	'\u00c4' },
		{	'=',	'A',	'\u0100' },
		{	'u',	'A',	'\u0102' },
		
		{	'c',	'c',	'\u00e7' },
		{	'c',	'C',	'\u00c7' },
		
		{ 	'`', 	'e', 	'\u00e8' },
		{	'\'',	'e',	'\u00e9' },
		{	'^',	'e',	'\u00ea' },
		{	'"',	'e',	'\u00eb' },
		{	'v',	'e',	'\u011b' },
		{	'=',	'e',	'\u0113' },
		{	'u',	'e',	'\u0115' },

		{ 	'`', 	'E', 	'\u00c8' },
		{	'\'',	'E',	'\u00c9' },
		{	'^',	'E',	'\u00ca' },
		{	'"',	'E',	'\u00cb' },
		{	'v',	'E',	'\u011b' },
		{	'=',	'E',	'\u0112' },
		{	'u',	'E',	'\u0114' },

		{ 	'`', 	'i', 	'\u00ec' },
		{	'\'',	'i',	'\u00ed' },
		{	'^',	'i',	'\u00ee' },
		{	'"',	'i',	'\u00ef' },
		{	'~',	'i',	'\u0129' },
		{	'=',	'i',	'\u012b' },
		{	'u',	'i',	'\u012d' },
		
		{ 	'`', 	'I', 	'\u00cc' },
		{	'\'',	'I',	'\u00cd' },
		{	'^',	'I',	'\u00ce' },
		{	'"',	'I',	'\u00cf' },
		{	'~',	'I',	'\u0128' },
		{	'=',	'I',	'\u012a' },
		{	'u',	'I',	'\u012c' },
		
		{ 	'`', 	'o', 	'\u00f2' },
		{	'\'',	'o',	'\u00f3' },
		{	'^',	'o',	'\u00f4' },
		{	'~',	'o',	'\u00f5' },
		{	'\"',	'o',	'\u00f6' },
		{	'=',	'o',	'\u014d' },
		{	'u',	'o',	'\u014f' },
		{	'H',	'o',	'\u0151' },
		
		{ 	'`', 	'O', 	'\u00d2' },
		{	'\'',	'O',	'\u00d3' },
		{	'^',	'O',	'\u00d4' },
		{	'~',	'O',	'\u00d5' },
		{	'"',	'O',	'\u00d6' },
		{	'=',	'O',	'\u014c' },
		{	'u',	'O',	'\u014e' },
		{	'H',	'O',	'\u0150' },
		
		{ 	'`', 	'u', 	'\u00f9' },
		{	'\'',	'u',	'\u00fa' },
		{	'^',	'u',	'\u00fb' },
		{	'"',	'u',	'\u00fc' },
		{	'~',	'u',	'\u0169' },
		{	'=',	'u',	'\u016b' },
		{	'u',	'u',	'\u016d' },
		{	'H',	'u',	'\u0171' },
		
		{ 	'`', 	'U', 	'\u00d9' },
		{	'\'',	'U',	'\u00da' },
		{	'^',	'U',	'\u00db' },
		{	'"',	'U',	'\u00dc' },
		{	'~',	'U',	'\u0168' },
		{	'=',	'U',	'\u016a' },
		{	'u',	'U',	'\u016c' },
		{	'H',	'U',	'\u0170' },
		
	};
	final static private String[][] s_nationalSymbols = new String[][] {
		{	"aa",	"\u00e5" },
		{	"ae",	"\u00e6" },
		{	"oe",	"\u0153" },
		
		{	"AA",	"\u00c5" },
		{	"AE",	"\u00c6" },
		{	"OE",	"\u0152" },
	};
}


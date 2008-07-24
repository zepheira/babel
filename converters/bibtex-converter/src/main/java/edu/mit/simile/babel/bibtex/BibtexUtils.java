/*
 *  (c) Copyright The SIMILE Project 2003-2008. All rights reserved.
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public class BibtexUtils {

    public static Reader unescapeUnicode(Reader reader) throws IOException {
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
        
        String input = writer.toString().replace("\\u", "\\\\u");
        
        return new StringReader(input);
    }
    
    public static String unescapeUnicode(String s) {
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
        
        return sb.toString().replace("\\\\u", "\\u");
    }
    
    public static String unescapeBibtexSpecialCharacters(String s) {
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
        
        return sb.toString().replace("\\\\u", "\\u");
    }
    
    final static private char[][] s_accentedCharacters = new char[][] {
        {   '`',    'a',    '\u00e0' },
        {   '\'',   'a',    '\u00e1' },
        {   '^',    'a',    '\u00e2' },
        {   '~',    'a',    '\u00e3' },
        {   '"',    'a',    '\u00e4' },
        {   '=',    'a',    '\u0101' },
        {   'u',    'a',    '\u0103' },
        
        {   '`',    'A',    '\u00c0' },
        {   '\'',   'A',    '\u00c1' },
        {   '^',    'A',    '\u00c2' },
        {   '~',    'A',    '\u00c3' },
        {   '"',    'A',    '\u00c4' },
        {   '=',    'A',    '\u0100' },
        {   'u',    'A',    '\u0102' },
        
        {   'c',    'c',    '\u00e7' },
        {   'c',    'C',    '\u00c7' },
        
        {   '`',    'e',    '\u00e8' },
        {   '\'',   'e',    '\u00e9' },
        {   '^',    'e',    '\u00ea' },
        {   '"',    'e',    '\u00eb' },
        {   'v',    'e',    '\u011b' },
        {   '=',    'e',    '\u0113' },
        {   'u',    'e',    '\u0115' },

        {   '`',    'E',    '\u00c8' },
        {   '\'',   'E',    '\u00c9' },
        {   '^',    'E',    '\u00ca' },
        {   '"',    'E',    '\u00cb' },
        {   'v',    'E',    '\u011b' },
        {   '=',    'E',    '\u0112' },
        {   'u',    'E',    '\u0114' },

        {   '`',    'i',    '\u00ec' },
        {   '\'',   'i',    '\u00ed' },
        {   '^',    'i',    '\u00ee' },
        {   '"',    'i',    '\u00ef' },
        {   '~',    'i',    '\u0129' },
        {   '=',    'i',    '\u012b' },
        {   'u',    'i',    '\u012d' },
        
        {   '`',    'I',    '\u00cc' },
        {   '\'',   'I',    '\u00cd' },
        {   '^',    'I',    '\u00ce' },
        {   '"',    'I',    '\u00cf' },
        {   '~',    'I',    '\u0128' },
        {   '=',    'I',    '\u012a' },
        {   'u',    'I',    '\u012c' },
        
        {   '`',    'o',    '\u00f2' },
        {   '\'',   'o',    '\u00f3' },
        {   '^',    'o',    '\u00f4' },
        {   '~',    'o',    '\u00f5' },
        {   '\"',   'o',    '\u00f6' },
        {   '=',    'o',    '\u014d' },
        {   'u',    'o',    '\u014f' },
        {   'H',    'o',    '\u0151' },
        
        {   '`',    'O',    '\u00d2' },
        {   '\'',   'O',    '\u00d3' },
        {   '^',    'O',    '\u00d4' },
        {   '~',    'O',    '\u00d5' },
        {   '"',    'O',    '\u00d6' },
        {   '=',    'O',    '\u014c' },
        {   'u',    'O',    '\u014e' },
        {   'H',    'O',    '\u0150' },
        
        {   '`',    'u',    '\u00f9' },
        {   '\'',   'u',    '\u00fa' },
        {   '^',    'u',    '\u00fb' },
        {   '"',    'u',    '\u00fc' },
        {   '~',    'u',    '\u0169' },
        {   '=',    'u',    '\u016b' },
        {   'u',    'u',    '\u016d' },
        {   'H',    'u',    '\u0171' },
        
        {   '`',    'U',    '\u00d9' },
        {   '\'',   'U',    '\u00da' },
        {   '^',    'U',    '\u00db' },
        {   '"',    'U',    '\u00dc' },
        {   '~',    'U',    '\u0168' },
        {   '=',    'U',    '\u016a' },
        {   'u',    'U',    '\u016c' },
        {   'H',    'U',    '\u0170' },
        
    };

    final static private String[][] s_nationalSymbols = new String[][] {
        {   "aa",   "\u00e5" },
        {   "ae",   "\u00e6" },
        {   "oe",   "\u0153" },
        
        {   "AA",   "\u00c5" },
        {   "AE",   "\u00c6" },
        {   "OE",   "\u0152" },
    };    
}

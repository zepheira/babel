package edu.mit.simile.babel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.Part;

/**
 * @author dfhuynh
 *
 */
public class ValidatorServlet extends HttpServlet {
	private static final long serialVersionUID = -5216314675436973678L;
	final static private Logger s_logger = Logger.getLogger(ValidatorServlet.class);

	private VelocityEngine m_ve;
	
	@Override
	public void init() throws ServletException {
		super.init();
		
        try {
    		File webapp = new File(getServletContext().getRealPath("/"));
    		
            Properties velocityProperties = new Properties();
            velocityProperties.setProperty(
                    RuntimeConstants.FILE_RESOURCE_LOADER_PATH, 
                    new File(new File(webapp, "WEB-INF"), "templates").getAbsolutePath());
    		
            m_ve = new VelocityEngine();
			m_ve.init(velocityProperties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		MultipartParser parser = new MultipartParser(request, 5 * 1024 * 1024);
		
		Part part = null;
		while ((part = parser.readNextPart()) != null) {
			if (part.isFile()) {
				FilePart filePart = (FilePart) part;
				Reader reader = new InputStreamReader(filePart.getInputStream());
				
				internalHandle(request, response, readerToString(reader));
				break;
			}
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String url = null;
		String expression = null;
		
		/*
		 * Parse parameters
		 */
        String[] params = StringUtils.splitPreserveAllTokens(request.getQueryString(), '&');
        if (params != null) {
	        for (int i = 0; i < params.length; i++) {
	            String param = params[i];
	            int equalIndex = param.indexOf('=');
	
	            if (equalIndex >= 0) {
	                String rawName = param.substring(0, equalIndex);
	                String rawValue = param.substring(equalIndex + 1);
	
	                String name = Babel.decode(rawName);
	                String value = Babel.decode(rawValue);
	
					if (name.equals("url")) {
						url = value;
						break;
					} else if (name.equals("expression")) {
						expression = value;
						break;
					}
	            }
			}
        }
        
		/*
		 * Load source from URL if any
		 */
        if (url != null) {
			URLConnection connection = null; 
			try {
				connection = new URL(url).openConnection();
				connection.setConnectTimeout(5000);
				connection.connect();
			} catch (Exception e) {
				s_logger.error(e);
				return;
			}
			
			InputStream inputStream = connection.getInputStream();
			String encoding = connection.getContentEncoding();
			
			Reader reader = new InputStreamReader(
				inputStream, (encoding == null) ? "ISO-8859-1" : encoding);
			
        	internalHandle(request, response, readerToString(reader));
        } else if (expression != null) {
        	internalHandle(request, response, expression);
        } else {
            try {
                VelocityContext vcContext = new VelocityContext();
   	            vcContext.put("hasCode", new Boolean(false));
   	            
   	            response.setContentType("text/html");
	            m_ve.mergeTemplate("validator.vt", vcContext, response.getWriter());
	        } catch (Throwable t) {
	        	throw new ServletException(t);
	        }
        }
	}
	
	protected void internalHandle(
		HttpServletRequest request, 
		HttpServletResponse response,
		String code
	) throws ServletException, IOException {
        EvaluatorException e = null;
        try {
	        Context c = Context.enter();
	        c.compileString(code, "", 1, null);
        } catch (EvaluatorException e2) {
        	e = e2;
        }
        
        try {
            VelocityContext vcContext = new VelocityContext();
            {
	            StringWriter writer = new StringWriter();
	            
	            vcContext.put("hasCode", new Boolean(true));
	            vcContext.put("hasError", e != null);
	            if (e != null) {
	            	String line = e.lineSource();
	            	int lineNumber = e.lineNumber() - 1;
	            	int colNumber = e.columnNumber();
	            	
	            	StringBuffer preceedingLines = new StringBuffer();
	            	StringBuffer succeedingLines = new StringBuffer();
	            	{
	            		int startLine = Math.max(0, lineNumber - 5);
	            		int endLine = e.lineNumber() + 5;
	            		
	            		LineNumberReader reader = new LineNumberReader(new StringReader(code));
	            		
	            		int l = 0;
	            		while (l < startLine) {
	            			reader.readLine();
	            			l++;
	            		}
	            		
	            		while (l < lineNumber) {
	            			preceedingLines.append(reader.readLine());
	            			preceedingLines.append("\n");
	            			l++;
	            		}
	            		
	            		reader.readLine();
	            		
	            		while (l < endLine) {
	            			String s = reader.readLine();
	            			if (s != null) {
		            			succeedingLines.append(s);
		            			succeedingLines.append("\n");
		            			l++;
	            			} else {
	            				break;
	            			}
	            		}
	            	}	            	
	            	
	            	vcContext.put("message", e.details());
	            	vcContext.put("line", new Integer(e.lineNumber()));
	            	vcContext.put("preceedingLines", preceedingLines.toString());
	            	vcContext.put("succeedingLines", succeedingLines.toString());
	            	if (colNumber > 0) {
	            		vcContext.put("prefix", line.substring(0, colNumber - 1));
	            		vcContext.put("highlight", line.substring(colNumber - 1, colNumber));
	            		vcContext.put("suffix", line.substring(colNumber));
	            	} else {
	            		vcContext.put("prefix", "");
	            		vcContext.put("highlight", line);
	            		vcContext.put("suffix", "");
	            	}
	            }
	            
	            writer.close();
            }
            
            m_ve.mergeTemplate("validator.vt", vcContext, response.getWriter());
        } catch (Throwable t) {
        	throw new ServletException(t);
        }
	}
	
	protected String readerToString(Reader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
		sb.append("(");
		{
			char[] chars = new char[1024];
			int c;
			try {
				while ((c = reader.read(chars)) > 0) {
					sb.append(chars, 0, c);
				}
			} finally {
				reader.close();
			}
		}
		sb.append(")");
		
		return sb.toString();
	}
}
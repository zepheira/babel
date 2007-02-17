package edu.mit.simile.babel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
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
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

import edu.mit.simile.babel.SemanticType;
import edu.mit.simile.babel.util.Util;

/**
 * @author dfhuynh
 *
 */
public class TranslatorServlet extends HttpServlet {
	final static private long serialVersionUID = 2083937775584527297L;
	final static private Logger s_logger = Logger.getLogger(TranslatorServlet.class);
	
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
		
        String[] params = StringUtils.splitPreserveAllTokens(request.getQueryString(), '&');
		Writer writer = new BufferedWriter(
			new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
		try {
			internalDoPost(request, response, params, writer);
		} finally {
			writer.close();
		}
	}
	
	protected boolean internalDoPost(
		HttpServletRequest 	request, 
		HttpServletResponse response,
		String[]			params,
		Writer				writer
	) throws ServletException, IOException {
		Properties 		readerProperties = new Properties();
		Properties 		writerProperties = new Properties();
		String			readerName = null;
		String			writerName = null;
		String			mimetype = null;
		
		/*
		 * Parse parameters
		 */
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            int equalIndex = param.indexOf('=');

            if (equalIndex >= 0) {
                String rawName = param.substring(0, equalIndex);
                String rawValue = param.substring(equalIndex + 1);

                String name = Util.decode(rawName);
                String value = Util.decode(rawValue);

				if (name.startsWith("in-")) {
					readerProperties.setProperty(name.substring(3), value);
				} else if (name.startsWith("out-")) {
					writerProperties.setProperty(name.substring(4), value);
				} else if (name.equals("reader")) {
					readerName = value;
				} else if (name.equals("writer")) {
					writerName = value;
				} else if (name.equals("mimetype")) {
					mimetype = value;
				}
            }
		}
		
		/*
		 * Instantiate converters
		 */
		if (readerName == null) {
			s_logger.warn("No reader name in request");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return false;
		} else if (writerName == null) {
			s_logger.warn("No writer name in request");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		
		BabelReader babelReader = Babel.getReader(readerName); 
		BabelWriter babelWriter = Babel.getWriter(writerName); 
		if (babelReader == null) {
			s_logger.warn("No reader of name " + readerName);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return false;
		} else if (babelWriter == null) {
			s_logger.warn("No writer of name " + writerName);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		
		/*
		 * Check compatibility
		 */
		SemanticType readerType = babelReader.getSemanticType();
		SemanticType writerType = babelWriter.getSemanticType();
		if (!writerType.getClass().isInstance(readerType)) {
			s_logger.warn(
				"Writer " + writerType.getClass().getName() + 
				" cannot take input from reader " + readerType.getClass().getName()
			);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return false;
		}
		
		/*
		 * Read in data, convert, and write result out
		 */
		MemoryStore store = new MemoryStore();
		Locale locale = request.getLocale();
		try {
			store.initialize();
			try {
				readAndConvert(babelReader, store, readerProperties, request, locale);
				
				setContentEncodingAndMimetype(babelWriter, response, mimetype);
				
				writeResult(babelWriter, store, writerProperties, writer, locale);
				
				return true;
			} finally {
				store.shutDown();
			}
		} catch (Exception e) {
			s_logger.error(e);
            
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            response.setContentType("text/html");
            writeError(writer, e);
		}
		return false;
	}
	
	protected void readAndConvert(
		BabelReader 		converter,
		Sail				sail,
		Properties			readerProperties,
		HttpServletRequest	request,
		Locale				locale
	) throws BabelException {
		try {
			MultipartParser parser = new MultipartParser(request, 5 * 1024 * 1024);
			
			Part part = null;
			while ((part = parser.readNextPart()) != null) {
                readerProperties.setProperty("namespace", generateNamespace(request));
                readerProperties.setProperty("url", "");
                
				if (part.isFile()) {
					FilePart filePart = (FilePart) part;
					if (converter.takesReader()) {
						Reader reader = new InputStreamReader(filePart.getInputStream());
						try {
							converter.read(reader, sail, readerProperties, locale);
						} finally {
							reader.close();
						}
					} else {
						InputStream inputStream = filePart.getInputStream();
						try {
							converter.read(inputStream, sail, readerProperties, locale);
						} finally {
							inputStream.close();
						}
					}
				} else if (part.isParam()) {
					ParamPart paramPart = (ParamPart) part;
					String paramName = paramPart.getName();
					if (paramName.equals("raw-text")) {
						if (converter.takesReader()) {
							StringReader reader = new StringReader(paramPart.getStringValue());
							try {
								converter.read(reader, sail, readerProperties, locale);
							} finally {
								reader.close();
							}
						}
					} else if (paramName.equals("url")) {
						String url = paramPart.getStringValue();
						if (url.length() > 0) {
							URLConnection connection = null; 
							try {
								connection = new URL(url).openConnection();
								connection.setConnectTimeout(5000);
								connection.connect();
							} catch (Exception e) {
								s_logger.error(e);
								continue;
							}
							
                            readerProperties.setProperty("namespace", makeIntoNamespace(url));
                            readerProperties.setProperty("url", url);

							InputStream inputStream = connection.getInputStream();
							if (converter.takesReader()) {
								String encoding = connection.getContentEncoding();
								
								Reader reader = new InputStreamReader(
									inputStream, (encoding == null) ? "ISO-8859-1" : encoding);
											
								try {
									converter.read(reader, sail, readerProperties, locale);
								} finally {
									reader.close();
								}
							} else {
								try {
									converter.read(inputStream, sail, readerProperties, locale);
								} finally {
									inputStream.close();
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			s_logger.error(e);
			throw new BabelException(e);
		}
	}
	
	protected void writeResult(
		BabelWriter 		babelWriter, 
		Sail 				sail, 
		Properties 			writerProperties,
		Writer				writer,
		Locale				locale
	) throws BabelException {
		try {
			babelWriter.write(writer, sail, writerProperties, locale);
		} catch (Exception e) {
			s_logger.error(e);
			throw new BabelException(e);
		}
	}
    
    protected void writeError(Writer writer, Exception e) throws ServletException {
        try {
            VelocityContext vcContext = new VelocityContext();
            vcContext.put("exception", e);
            vcContext.put("isBabelException", Boolean.valueOf(e instanceof BabelException));
            {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                printWriter.close();
                
                vcContext.put("stackTrace", stringWriter.toString());
            }
            
            m_ve.mergeTemplate("error.vt", vcContext, writer);
        } catch (Throwable e2) {
            throw new ServletException(e2);
        }
    }
	
	protected void setContentEncodingAndMimetype(
			BabelWriter writer, HttpServletResponse response, String mimetype) {
		response.setCharacterEncoding("UTF-8");
		response.setContentType(
			(mimetype == null || mimetype.equals("default")) ? 
					writer.getSerializationFormat().getMimetype() :
					mimetype
		);
	}
	
    static protected String generateNamespace(HttpServletRequest request) {
    	return makeIntoNamespace("http://" + request.getRemoteAddr() + "/");
    }
    
    static protected String makeIntoNamespace(String s) {
    	if (s.endsWith("#")) {
    		return s;
    	} else if (s.endsWith("/")) {
    		return s;
    	} else {
    		return s + "#";
    	}
    }
}

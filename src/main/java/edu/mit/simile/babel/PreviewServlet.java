package edu.mit.simile.babel;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

public class PreviewServlet extends TranslatorServlet {
	private static final long serialVersionUID = -2862110707968976815L;

	//final static private Logger s_logger = Logger.getLogger(PreviewServlet.class);
	
	private VelocityEngine m_ve;
	
	@Override
	public void init() throws ServletException {
		super.init();
		
        try {
    		File webapp = new File(getServletContext().getRealPath("/"));
    		
            Properties velocityProperties = new Properties();
            velocityProperties.setProperty(
                    RuntimeConstants.FILE_RESOURCE_LOADER_PATH, 
                    new File(webapp.getParentFile(), "templates").getAbsolutePath());
    		
            m_ve = new VelocityEngine();
			m_ve.init(velocityProperties);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String template = null;
            
            String[] params = StringUtils.splitPreserveAllTokens(request.getQueryString(), '&');
            for (int i = 0; i < params.length; i++) {
                String param = params[i];
                int equalIndex = param.indexOf('=');

                if (equalIndex >= 0) {
                    String name = param.substring(0, equalIndex);
                    if ("template".equals(name)) {
                    	template = decode(param.substring(equalIndex + 1));
                    	break;
                    }
                }
            }
            
            VelocityContext vcContext = new VelocityContext();
            {
	            StringWriter writer = new StringWriter();
	            
	            internalDoPost(request, response, params, writer);
	            
	            vcContext.put("data", writer.toString());
	            
	            writer.close();
            }
            
            m_ve.mergeTemplate(template, vcContext, response.getWriter());
        } catch (Throwable e) {
        	throw new ServletException(e);
        }
	}
	
	@Override
	protected void setContentEncodingAndMimetype(BabelWriter writer, HttpServletResponse response, String mimetype) {
		// do nothing
	}
}

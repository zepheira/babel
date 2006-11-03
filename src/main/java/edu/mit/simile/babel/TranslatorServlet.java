package edu.mit.simile.babel;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

public class TranslatorServlet extends HttpServlet {
	final static private Logger s_logger = Logger.getLogger(TranslatorServlet.class);
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		super.doPost(request, response);
		
	}
}

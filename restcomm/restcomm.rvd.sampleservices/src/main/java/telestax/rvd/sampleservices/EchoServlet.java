package telestax.rvd.sampleservices;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class EchoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public EchoServlet() {
        super();
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	    
	      response.setContentType("application/json");
	      Gson gson = new GsonBuilder().create();
	      
	      /*
	       * This is simple echo servlet to be used for testing. All parameters passed to it on the URL are returned as a 
	       * JSON object.
	       */
	      
	      JsonObject params = new JsonObject();
	      Enumeration<String> e = request.getParameterNames();
	      while ( e.hasMoreElements() ) {
	          String name = (String) e.nextElement();
	          params.addProperty( name, request.getParameter(name));
	      }

	      response.getWriter().print( gson.toJson( params ) );
	}
}

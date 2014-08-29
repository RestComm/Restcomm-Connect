package telestax.rvd.sampleservices;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CheckSubscriberCodeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    public CheckSubscriberCodeServlet() {
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    response.setContentType("application/json");
	    
	    /*
	     * This is a simple routing external service. A servlet that given some parameters decides what should be the 
	     * next RVD module.
	     */
	    
	    Gson gson = new GsonBuilder().create();
	    
	    // If customer code is 123 accept him otherwise goodbye
	    if ( "123".equals(request.getParameter("code")) )
	        response.getWriter().print( gson.toJson("SubscribersRoom"));
	    else
	        response.getWriter().print( gson.toJson("Goodbye"));
	}

}

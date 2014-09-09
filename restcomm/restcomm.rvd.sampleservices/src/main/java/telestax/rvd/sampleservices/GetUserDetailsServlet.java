package telestax.rvd.sampleservices;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class GetUserDetailsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // If the user is found return this
    public static class UserExistsResponse {
        // business data
        String firstname;
        String lastname;

        // flow data
        String nextModule;

        public UserExistsResponse(String firstname, String lastname, String nextModule) {
            super();
            this.firstname = firstname;
            this.lastname = lastname;
            this.nextModule = nextModule;
        }
    }

    // If the user was not found return this
    public static class UserNotFoundResponse {
        // flow data only
        String nextModule;

        public UserNotFoundResponse(String nextModule) {
            super();
            this.nextModule = nextModule;
        }
    }

    // In an application exception (or any other exception could be the case) was thrown
    public static class ApplicationExceptionResponse {
        String error;
        String nextModule;

        public ApplicationExceptionResponse(String error, String nextModule) {
            super();
            this.error = error;
            this.nextModule = nextModule;
        }
    }


    public GetUserDetailsServlet() {
        super();
    }


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");

        /* Return customer details for the customerId specified. In this example, a static mapping is used but a query to
         * a database could do as well.
         */

        // Since we use JSON to format our response, Gson comes in handy
        Gson gson = new GsonBuilder().create();

        // Get service input parameters
        //String customerId = request.getParameter("customerId");

        String jsonResponse;
        try {

            Integer customerId = Integer.parseInt(request.getParameter("customerId"));


            if ( customerId == 1 )
            {
                UserExistsResponse existsResponse = new UserExistsResponse("Mickey", "Mouse", "Members");
                jsonResponse = gson.toJson(existsResponse);
            } else
            if ( customerId == 2 )
            {
                UserExistsResponse existsResponse = new UserExistsResponse("Donald","Duck","Members");
                jsonResponse = gson.toJson(existsResponse);
            } else
            {
                // No 'firstname', 'lastname' variables if the user is not a registered customer
                UserNotFoundResponse notFoundResponse = new UserNotFoundResponse("Guests");
                jsonResponse = gson.toJson(notFoundResponse);
            }
        } catch (NumberFormatException e) {
            if ( "".equals(request.getParameter("customerId")) ) {
                UserNotFoundResponse notFoundResponse = new UserNotFoundResponse("Guests");
                jsonResponse = gson.toJson(notFoundResponse);
            } else {
                ApplicationExceptionResponse exceptionResponse = new ApplicationExceptionResponse(e.getClass().getSimpleName(), "Error");
                jsonResponse = gson.toJson(exceptionResponse);
            }
        }
        response.getWriter().print( jsonResponse );
    }

}

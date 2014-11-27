package org.mobicents.servlet.restcomm.rvd.model.steps.es;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.ESRequestException;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.BadExternalServiceResponse;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.ErrorParsingExternalServiceUrl;
import org.mobicents.servlet.restcomm.rvd.interpreter.exceptions.RemoteServiceError;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.client.UrlParam;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;


public class ExternalServiceStep extends Step {

    static final Logger logger = Logger.getLogger(ExternalServiceStep.class.getName());

    private String url; // supports RVD variable expansion when executing the HTTP request
    private String method;
    private String username;
    private String password;
    private List<UrlParam> urlParams;
    private List<Assignment> assignments;
    private String next;
    private String nextVariable;
    private Boolean doRouting;
    private String nextType;
    private ValueExtractor nextValueExtractor;
    private List<RouteMapping> routeMappings;
    //private String defaultNext;
    private String exceptionNext;


    public ValueExtractor getNextValueExtractor() {
        return nextValueExtractor;
    }

    public void setNextValueExtractor(ValueExtractor nextValueExtractor) {
        this.nextValueExtractor = nextValueExtractor;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<RouteMapping> getRouteMappings() {
        return routeMappings;
    }

    public void setRouteMappings(List<RouteMapping> routeMappings) {
        this.routeMappings = routeMappings;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public Boolean getDoRouting() {
        return doRouting;
    }

    public void setDoRouting(Boolean doRouting) {
        this.doRouting = doRouting;
    }

    public String getNextType() {
        return nextType;
    }

    public void setNextType(String nextType) {
        this.nextType = nextType;
    }

    public String getNextVariable() {
        return nextVariable;
    }

    public void setNextVariable(String nextVariable) {
        this.nextVariable = nextVariable;
    }
    public List<UrlParam> getUrlParams() {
        return this.urlParams;
    }

    public String getMethod() {
        return method;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


    public String getExceptionNext() {
        return exceptionNext;
    }

    public void setExceptionNext(String exceptionNext) {
        this.exceptionNext = exceptionNext;
    }

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @returns String - The module name to continue rendering with. null, to continue processing the existing module
     */
    @Override
    public String process(Interpreter interpreter, HttpServletRequest httpRequest ) throws InterpreterException {

        //ExternalServiceStep esStep = (ExternalServiceStep) step;
        String next = null;
        try {

            // *** Build the request uri ***

            URI url;
            try {
                URIBuilder uri_builder = new URIBuilder(interpreter.populateVariables(getUrl()) ); // supports RVD variable expansion

                // if this is a relative url fill in missing fields from the request
                if (uri_builder.getHost() == null ) {
                    logger.debug("External Service: Relative url is used. Will override from http request to RVD controller");
                    uri_builder.setScheme(httpRequest.getScheme());
                    uri_builder.setHost(httpRequest.getServerName());
                    uri_builder.setPort(httpRequest.getServerPort());
                    if (  ! uri_builder.getPath().startsWith("/") )
                        uri_builder.setPath("/" + uri_builder.getPath());
                }

                // Add url parameters for GET requests
                if ( getMethod() == null || "GET".equals(getMethod()) )
                    for ( UrlParam urlParam : getUrlParams() )
                        uri_builder.addParameter(urlParam.getName(), interpreter.populateVariables(urlParam.getValue()) );

                url = uri_builder.build();
            } catch (URISyntaxException e) {
                throw new ErrorParsingExternalServiceUrl( "URL: " + getUrl(), e);
            }


            // *** Make the request and get a status code and a response. Build a JsonElement from the response  ***

            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response;
            int statusCode;
            JsonElement response_element = null;

            logger.info("Requesting from url: " + url);
            logger.debug("Requesting from url: " + url);
            if ( interpreter.getRvdContext().getProjectSettings().getLogging() )
                interpreter.getProjectLogger().log("Requesting from url: " + url).tag("app",interpreter.getAppName()).tag("ES").tag("REQUEST").done();
            if ( "POST".equals(getMethod()) ) {
                HttpPost post = new HttpPost(url);
                List <NameValuePair> values = new ArrayList <NameValuePair>();
                for ( UrlParam urlParam : getUrlParams() )
                    values.add(new BasicNameValuePair(urlParam.getName(), interpreter.populateVariables(urlParam.getValue()) ));
                post.setEntity(new UrlEncodedFormEntity(values));
                post.addHeader("Authorization", "Basic " + RvdUtils.buildHttpAuthorizationToken(getUsername(), getPassword()));
                response = client.execute( post );
            } else
            if ( getMethod() == null || getMethod().equals("GET") ) {
                HttpGet get = new HttpGet( url );
                get.addHeader("Authorization", "Basic " + RvdUtils.buildHttpAuthorizationToken(getUsername(), getPassword()));
                response = client.execute( get );
            } else
                throw new InterpreterException("Unknonwn HTTP method specified: " + getMethod() );

            statusCode = response.getStatusLine().getStatusCode();

            // In  case of error in the service no need to proceed. Just continue the "onException" module if set
            if ( statusCode >= 400 && statusCode < 600 ) {
                logger.info("Remote service failed with: " + response.getStatusLine());
                if ( ! RvdUtils.isEmpty( getExceptionNext()) )
                    return getExceptionNext();
                else
                    throw new RemoteServiceError("Service " + url + " failed with: " + response.getStatusLine() +". Throwing an error since no 'On Remote Exception' has been defined.");
            }

            // Parse the response if (a) there are assignments or (b) there is dynamic or mapped routing
            if ( getAssignments() != null && getAssignments().size() > 0
                    || getDoRouting() && ("responseBased".equals(getNextType()) || "mapped".equals(getNextType())) ) {
                HttpEntity entity = response.getEntity();
                if ( entity != null ) {
                    JsonParser parser = new JsonParser();
                    String entity_string = EntityUtils.toString(entity);
                    //logger.info("ES: Received " + entity_string.length() + " bytes");
                    //logger.debug("ES Response: " + entity_string);
                    if ( interpreter.getRvdContext().getProjectSettings().getLogging() )
                        interpreter.getProjectLogger().log(entity_string).tag("app",interpreter.getAppName()).tag("ES").tag("RESPONSE").done();
                    response_element = parser.parse(entity_string);
                }
            } else
                logger.debug("ES: No parsing will be done to the response");

            // *** Determine what to do next. Find the next module name or whether to continue in the current module ***

            if ( getDoRouting() ) {
                if ( "fixed".equals( getNextType() ) )
                    next = getNext();
                else
                if ( "responseBased".equals(getNextType()) || "mapped".equals(getNextType())) {
                    String nextValue = interpreter.evaluateExtractorExpression(getNextValueExtractor(), response_element);

                    if ( "responseBased".equals(getNextType()) ) {
                        next = interpreter.getNodeNameByLabel( nextValue );
                    } else
                    if ( "mapped".equals(getNextType()) ) {
                        if ( getRouteMappings() != null ) {
                            for ( RouteMapping mapping : getRouteMappings() ) {
                                if ( nextValue != null && nextValue.equals(mapping.getValue()) ) {
                                    next = mapping.getNext();
                                    break;
                                }
                            }
                        }
                    }
                }
                // if no next route has been found throw an error
                if ( "fixed".equals(getNextType()) && RvdUtils.isEmpty(next) ) {
                    throw new InterpreterException("No valid module could be found for ES routing"); // use a general exception for now.
                }
                logger.info( "Routing enabled. Chosen target: " + next);
            }


            // *** Perform the assignments ***

            try {
                if ( getDoRouting() && ("responseBased".equals(getNextType()) || "mapped".equals(getNextType())) ) {
                    for ( Assignment assignment : getAssignments() ) {
                        logger.debug("working on variable " + assignment.getDestVariable() );
                        logger.debug( "moduleNameScope: " + assignment.getModuleNameScope());
                        if ( assignment.getModuleNameScope() == null || assignment.getModuleNameScope().equals(next) ) {
                            String value = null;
                            try {
                                value = interpreter.evaluateExtractorExpression(assignment.getValueExtractor(), response_element);
                            } catch ( BadExternalServiceResponse e ) {
                                logger.error("Could not parse variable "  + assignment.getDestVariable() + ". Variable not found in response");
                                throw e;
                            }

                            if ( "application".equals(assignment.getScope()) )
                                interpreter.putStickyVariable(assignment.getDestVariable(), value);
                            interpreter.putVariable(assignment.getDestVariable(), value );
                        } else
                            logger.debug("skipped assignment to " + assignment.getDestVariable() );
                    }
                }  else {
                    for ( Assignment assignment : getAssignments() ) {
                        logger.debug("working on variable " + assignment.getDestVariable() );
                        String value = null;
                        try {
                            value = interpreter.evaluateExtractorExpression(assignment.getValueExtractor(), response_element);
                        } catch ( BadExternalServiceResponse e ) {
                            logger.error("Could not parse variable "  + assignment.getDestVariable() + ". Variable not found in response");
                            throw e;
                        }

                        if ( "application".equals(assignment.getScope()) )
                            interpreter.putStickyVariable(assignment.getDestVariable(), value);

                        interpreter.putVariable(assignment.getDestVariable(), value );
                    }
                }
                logger.debug("variables after processing ExternalService step: " + interpreter.getVariables().toString() );
            } catch (JsonSyntaxException e) {
                throw new BadExternalServiceResponse("External Service request received a malformed JSON response" );
            }

        } catch (IOException e) {
            throw new ESRequestException("Error processing ExternalService step " + getName(), e);
        }
        return next;
    }

}

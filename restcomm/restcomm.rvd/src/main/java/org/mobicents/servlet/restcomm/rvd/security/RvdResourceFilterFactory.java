package org.mobicents.servlet.restcomm.rvd.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.security.annotations.RvdAuth;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

public class RvdResourceFilterFactory implements ResourceFilterFactory {
    static final Logger logger = Logger.getLogger(ResourceFilterFactory.class.getName());

    public RvdResourceFilterFactory() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<ResourceFilter> create(AbstractMethod method) {
        List<ResourceFilter> filters = new ArrayList<ResourceFilter>();

        if ( method.getMethod().getAnnotation(RvdAuth.class) != null ) {
            filters.add(new AutheticationFilter() );
            filters.add(new SessionKeepAliveFilter());
            //logger.info("RvdAuth annotation FOUND");
        }
        // apply filter only to the project manager services
        /*PathValue pathValue = method.getResource().getPath();
        if ( pathValue != null && pathValue.getValue() != null && pathValue.getValue().startsWith("projects") ) {
            filters.add(new RvdResourceFilter() );
        }
        */

        return filters;
    }

}

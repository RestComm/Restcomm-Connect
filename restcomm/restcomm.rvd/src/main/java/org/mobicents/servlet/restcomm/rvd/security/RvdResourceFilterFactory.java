package org.mobicents.servlet.restcomm.rvd.security;

import java.util.ArrayList;
import java.util.List;

import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.PathValue;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

public class RvdResourceFilterFactory implements ResourceFilterFactory {

    public RvdResourceFilterFactory() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<ResourceFilter> create(AbstractMethod method) {
        List<ResourceFilter> filters = new ArrayList<ResourceFilter>();

        // apply filter only to the project manager services
        PathValue pathValue = method.getResource().getPath();
        if ( pathValue != null && pathValue.getValue() != null && pathValue.getValue().startsWith("/manager/projects") ) {
            filters.add(new RvdResourceFilter() );
        }

        return filters;
    }

}

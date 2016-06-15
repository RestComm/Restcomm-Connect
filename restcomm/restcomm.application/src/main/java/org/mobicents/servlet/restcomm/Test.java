package org.mobicents.servlet.restcomm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

// TODO: REMOVE THIS CLASS AFTER TESTING
// THIS IS A TEST CLASS, for some quick local tests, MUST BE IGNORED
public class Test {

    public static void main(String[] args) {
        Configuration xml = null;
        //XMLConfiguration xml =null;

        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.setAttributeSplittingDisabled(true);
            xmlConfiguration.load("src/main/webapp/WEB-INF/conf/restcomm.xml");
            xml = xmlConfiguration;
        } catch (final ConfigurationException exception) {
            System.out.println(exception);
        }

        /*List<Object> mgcpMediaServers = xml.getList("media-server-manager.mgcp-servers.mgcp-server.local-address");
        int mgcpMediaServerListSize = mgcpMediaServers.size();
        System.out.println("mgcpMediaServerListSize: "+mgcpMediaServerListSize);*/

        /*Configuration settings ;
        settings = xml.subset("media-server-manager");
        HierarchicalConfiguration settingInXml = (HierarchicalConfiguration)settings;
        List<Object> mgcpMediaServers2 = settingInXml.getList("mgcp-servers.mgcp-server.local-address");
        int mgcpMediaServerListSize2 = mgcpMediaServers2.size();
        System.out.println("mgcpMediaServerListSize: "+mgcpMediaServerListSize2);*/

        Configuration settings ;
        settings = xml.subset("media-server-manager");
        List<Object> mgcpMediaServers2 = settings.getList("mgcp-servers.mgcp-server.local-address");
        int mgcpMediaServerListSize2 = mgcpMediaServers2.size();
        System.out.println("mgcpMediaServerListSize: "+mgcpMediaServerListSize2);

        List<String>mediaGateways = new ArrayList<String>();
        mediaGateways.add("MS1");
        mediaGateways.add("MS2");
        mediaGateways.add("MS3");
        mediaGateways.add("MS4");
        mediaGateways.add("MS5");

        for (int i =0; i<10;i++){
            System.out.println(mediaGateways.get(Robin.getInstance(mediaGateways.size()).getNextMediaGatewayIndex()));
        }
    }

}

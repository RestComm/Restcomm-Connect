package org.restcomm.connect.testsuite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.restcomm.connect.commons.Version;


public class WebArchiveUtil {
    
    public static File tweakFilePorts(String filePath, Map<String,String> portReplaments){
        try {
            InputStream resourceAsStream = WebArchiveUtil.class.getClassLoader().getResourceAsStream(filePath);
            StringWriter writer = new StringWriter();
            IOUtils.copy(resourceAsStream, writer, java.nio.charset.Charset.defaultCharset());         
            String confStr = writer.toString();
            for (String key : portReplaments.keySet()) {
                confStr = confStr.replace(key, portReplaments.get(key));
            }
            String targetFilePath = "target/" + filePath;
            if (System.getProperty("arquillian_sip_port") != null) {
                targetFilePath = targetFilePath + System.getProperty("arquillian_sip_port");
            }
            File f = new File(targetFilePath);
            FileUtils.writeStringToFile(f, confStr);
            return f;
        } catch (IOException ex) {
            return null;
        }
    }   
    
    public static WebArchive createWebArchiveNoGw(String restcommConf, String dbScript, Map<String,String> replacements) {
        return createWebArchiveNoGw(restcommConf, dbScript, new ArrayList(), replacements);
    }
    public static WebArchive createWebArchiveNoGw(String restcommConf, String dbScript, List<String> resources, Map<String,String> replacements) {
        Map<String,String> webInfResources = new HashMap();
        webInfResources.put(restcommConf, "conf/restcomm.xml");
        webInfResources.put(dbScript, "data/hsql/restcomm.script");
        webInfResources.put("akka_application.conf", "classes/application.conf");
        return createWebArchiveNoGw(webInfResources, resources, replacements);
    }
    public static WebArchive createWebArchiveNoGw(Map<String,String> webInfResources, List<String> resources, Map<String,String> replacements) {
    
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + Version.getVersion()).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.delete("/WEB-INF/classes/application.conf");
        archive.addAsWebInfResource("sip.xml");
        for (String webdInfFile : webInfResources.keySet()) {
            File f = WebArchiveUtil.tweakFilePorts(webdInfFile,replacements );
            archive.addAsWebInfResource(f, webInfResources.get(webdInfFile));
        }
        archive.addAsWebInfResource("akka_application.conf", "classes/application.conf");
        for (String rAux: resources) {
            File rFile = WebArchiveUtil.tweakFilePorts(rAux,replacements );
            archive.addAsWebResource(rFile, rAux);
        }
        return archive;
    }    
}

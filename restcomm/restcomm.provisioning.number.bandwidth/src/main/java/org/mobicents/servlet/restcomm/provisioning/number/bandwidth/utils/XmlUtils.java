package org.mobicents.servlet.restcomm.provisioning.number.bandwidth.utils;

import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;

/**
 * Created by sbarstow on 10/14/14.
 */
public class XmlUtils {

    private static final Logger LOG = Logger.getLogger(XmlUtils.class);

    private static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    public static Object fromXml(String responseBody, Class c) throws JAXBException, XMLStreamException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(responseBody.getBytes());
        JAXBContext jaxbContext = JAXBContext.newInstance(c);
        XMLStreamReader xsr = xmlInputFactory.createXMLStreamReader(inputStream);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return jaxbUnmarshaller.unmarshal(xsr);
    }

    public static String toXml(Object o) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(o.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(o, writer);
        LOG.debug("toXml: " + writer.toString());
        return writer.toString();
    }

}
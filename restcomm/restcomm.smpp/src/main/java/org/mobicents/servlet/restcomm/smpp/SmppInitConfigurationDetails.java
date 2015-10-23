package org.mobicents.servlet.restcomm.smpp;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;

import akka.actor.ActorSystem;

public class SmppInitConfigurationDetails {

    private static ActorSystem system;
    private static Configuration configuration;
    private static SipFactory factory;
    private static DaoManager storage;
    private static ServletContext servletContext;

    public SmppInitConfigurationDetails(final ActorSystem system, final Configuration configuration, final SipFactory factory,
            final DaoManager storage, final ServletContext servletContext){


        SmppInitConfigurationDetails.system = system;
        SmppInitConfigurationDetails.configuration = configuration;
        SmppInitConfigurationDetails.factory = factory;
        SmppInitConfigurationDetails.storage = storage;
        SmppInitConfigurationDetails.servletContext = servletContext;
    }


    public static ActorSystem getSystem(){
        return system;
    }

    public static Configuration getConfiguration(){
        return configuration;
    }

    public static SipFactory getSipFactory(){
        return factory;
    }

    public static DaoManager getStorage(){
        return storage;
    }

    public static ServletContext getServletContext(){
        return servletContext;
    }


}

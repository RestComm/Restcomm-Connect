package org.mobicents.servlet.restcomm.sms;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.dao.DaoManager;

import akka.actor.ActorSystem;

public class SmsInitConfigurationDetails {

    private static ActorSystem system;
    private static Configuration configuration;
    private static SipFactory factory;
    private static DaoManager storage;
    private static ServletContext servletContext;

    public SmsInitConfigurationDetails(final ActorSystem system, final Configuration configuration, final SipFactory factory,
            final DaoManager storage, final ServletContext servletContext){


        SmsInitConfigurationDetails.system = system;
        SmsInitConfigurationDetails.configuration = configuration;
        SmsInitConfigurationDetails.factory = factory;
        SmsInitConfigurationDetails.storage = storage;
        SmsInitConfigurationDetails.servletContext = servletContext;
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

    public static ServletContext servletContext(){
        return servletContext;
    }


}

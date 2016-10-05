package org.restcomm.connect.extension.controller;

import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.RestcommExtension;
import org.restcomm.connect.extension.api.RestcommExtensionGeneric;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by gvagenas on 21/09/16.
 */
public class ExtensionController {
    private static Logger logger = Logger.getLogger(ExtensionController.class);

    private static ExtensionController instance;
    private List callManagerExtensions;
    private List smsSessionExtensions;
    private List ussdCallManagerExtensions;

    private ExtensionController(){
        this.callManagerExtensions = new CopyOnWriteArrayList();
        this.smsSessionExtensions = new CopyOnWriteArrayList();
        this.ussdCallManagerExtensions = new CopyOnWriteArrayList();
    }

    public static ExtensionController getInstance() {
        if (instance == null) {
            instance = new ExtensionController();
        }
        return instance;
    }

    public List<RestcommExtensionGeneric> getExtensions(final ExtensionType type) {
        //Check the sender's class and return the extensions that are supported for this class
        if (type.equals(ExtensionType.CallManager) && (callManagerExtensions != null && callManagerExtensions.size() > 0)) {
            return callManagerExtensions;
        } else if (type.equals(ExtensionType.SmsSession) && (smsSessionExtensions != null && smsSessionExtensions.size() > 0)) {
            return smsSessionExtensions;
        } else if (type.equals(ExtensionType.UssdCallManager) && (ussdCallManagerExtensions != null && ussdCallManagerExtensions.size() > 0)) {
            return ussdCallManagerExtensions;
        } else {
            return null;
        }
    }

    public void registerExtension(final RestcommExtensionGeneric extension) {
        //scan the annotation to see what this extension supports
        ExtensionType[] types = extension.getClass().getAnnotation(RestcommExtension.class).type();
        for (ExtensionType type : types) {
            if (type.equals(ExtensionType.CallManager)) {
                callManagerExtensions.add(extension);
            }
            if (type.equals(ExtensionType.SmsSession)) {
                smsSessionExtensions.add(extension);
            }
            if (type.equals(ExtensionType.UssdCallManager)) {
                ussdCallManagerExtensions.add(extension);
            }
        }
    }
}

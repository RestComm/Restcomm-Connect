package org.restcomm.connect.extension.controller;

import org.restcomm.connect.extension.api.ApiRequest;
import org.restcomm.connect.extension.api.ExtensionResponse;
import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.IExtensionRequest;
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
    private List restApiExtensions;
    private List featureAccessControlExtensions;

    private ExtensionController(){
        this.callManagerExtensions = new CopyOnWriteArrayList();
        this.smsSessionExtensions = new CopyOnWriteArrayList();
        this.ussdCallManagerExtensions = new CopyOnWriteArrayList();
        this.restApiExtensions = new CopyOnWriteArrayList();
        this.featureAccessControlExtensions = new CopyOnWriteArrayList();
    }

    public static ExtensionController getInstance() {
        if (instance == null) {
            instance = new ExtensionController();
        }
        return instance;
    }

    /**
     * allow to reset the singleton. Mainly for testing purposes.
     * TODO should we reset the singleton if app is shutdown...?
     */
    public void reset() {
        instance = null;
    }

    public List<RestcommExtensionGeneric> getExtensions(final ExtensionType type) {
        //Check the sender's class and return the extensions that are supported for this class
        if (type.equals(ExtensionType.CallManager) && (callManagerExtensions != null && callManagerExtensions.size() > 0)) {
            return callManagerExtensions;
        } else if (type.equals(ExtensionType.SmsService) && (smsSessionExtensions != null && smsSessionExtensions.size() > 0)) {
            return smsSessionExtensions;
        } else if (type.equals(ExtensionType.UssdCallManager) && (ussdCallManagerExtensions != null && ussdCallManagerExtensions.size() > 0)) {
            return ussdCallManagerExtensions;
        } else if (type.equals(ExtensionType.RestApi) && (restApiExtensions != null && restApiExtensions.size() > 0)) {
            return restApiExtensions;
        } else if (type.equals(ExtensionType.FeatureAccessControl) && (featureAccessControlExtensions != null && featureAccessControlExtensions.size() > 0)) {
          return featureAccessControlExtensions;
        } else {
            return null;
        }
    }

    public void registerExtension(final RestcommExtensionGeneric extension) {
        //scan the annotation to see what this extension supports
        ExtensionType[] types = extension.getClass().getAnnotation(RestcommExtension.class).type();
        String extensionName = extension.getClass().getName();
        for (ExtensionType type : types) {
            if (type.equals(ExtensionType.CallManager)) {
                callManagerExtensions.add(extension);
                if (logger.isDebugEnabled()) {
                    logger.debug("CallManager extension added: "+extensionName);
                }
            }
            if (type.equals(ExtensionType.SmsService)) {
                smsSessionExtensions.add(extension);
                if (logger.isDebugEnabled()) {
                    logger.debug("SmsService extension added: "+extensionName);
                }
            }
            if (type.equals(ExtensionType.UssdCallManager)) {
                ussdCallManagerExtensions.add(extension);
                if (logger.isDebugEnabled()) {
                    logger.debug("UssdCallManager extension added: "+extensionName);
                }
            }
            if (type.equals(ExtensionType.RestApi)) {
                restApiExtensions.add(extension);
                if (logger.isDebugEnabled()) {
                    logger.debug("RestApi extension added: "+extensionName);
                }
            }
            if (type.equals(ExtensionType.FeatureAccessControl)) {
                featureAccessControlExtensions.add(extension);
                if (logger.isDebugEnabled()) {
                    logger.debug("FeatureAccesControl extension added: "+extensionName);
                }
            }
        }
    }

    public ExtensionResponse executePreOutboundAction(final IExtensionRequest ier, List<RestcommExtensionGeneric> extensions) {
        //FIXME: if we have more than one extension in chain
        // and all of them are successful, we only receive the last
        // extensionResponse
        ExtensionResponse response = new ExtensionResponse();
        if (extensions != null && extensions.size() > 0) {

            for (RestcommExtensionGeneric extension : extensions) {
                if(logger.isInfoEnabled()) {
                    logger.info( extension.getName()+" is enabled="+extension.isEnabled());
                }
                if (extension.isEnabled()) {
                    try {
                        ExtensionResponse tempResponse = extension.preOutboundAction(ier);
                        if (tempResponse != null) {
                            response = tempResponse;
                            //fail fast
                            if (!tempResponse.isAllowed()) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        if (logger.isDebugEnabled()) {
                            String msg = String.format("There was an exception while executing preInboundAction from extension %s", extension.getName());
                            logger.debug(msg, t);
                        }
                    }
                }
            }
        }
        return response;
    }

    public ExtensionResponse executePostOutboundAction(final IExtensionRequest er, List<RestcommExtensionGeneric> extensions) {
        ExtensionResponse response = new ExtensionResponse();
        if (extensions != null && extensions.size() > 0) {

            for (RestcommExtensionGeneric extension : extensions) {
                if(logger.isInfoEnabled()) {
                    logger.info( extension.getName()+" is enabled="+extension.isEnabled());
                }
                if (extension.isEnabled()) {
                    try {
                        ExtensionResponse tempResponse = extension.postOutboundAction(er);
                        if (tempResponse != null) {
                            response = tempResponse;
                            //fail fast
                            if (!tempResponse.isAllowed()) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        if (logger.isDebugEnabled()) {
                            String msg = String.format("There was an exception while executing preInboundAction from extension %s", extension.getName());
                            logger.debug(msg, t);
                        }
                    }
                }
            }
        }
        return response;
    }

    public ExtensionResponse executePreInboundAction(final IExtensionRequest er, List<RestcommExtensionGeneric> extensions) {
        ExtensionResponse response = new ExtensionResponse();
        if (extensions != null && extensions.size() > 0) {
            for (RestcommExtensionGeneric extension : extensions) {
                if(logger.isInfoEnabled()) {
                    logger.info( extension.getName()+" is enabled="+extension.isEnabled());
                }
                if (extension.isEnabled()) {
                    try {
                        ExtensionResponse tempResponse = extension.preInboundAction(er);
                        if (tempResponse != null) {
                            response = tempResponse;
                            //fail fast
                            if (!tempResponse.isAllowed()) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        if (logger.isDebugEnabled()) {
                            String msg = String.format("There was an exception while executing preInboundAction from extension %s", extension.getName());
                            logger.debug(msg, t);
                        }
                    }
                }
            }
        }
        return response;
    }

    public ExtensionResponse executePostInboundAction(final IExtensionRequest er,  List<RestcommExtensionGeneric> extensions) {
        ExtensionResponse response = new ExtensionResponse();
        if (extensions != null && extensions.size() > 0) {
            for (RestcommExtensionGeneric extension : extensions) {
                if(logger.isInfoEnabled()) {
                    logger.info( extension.getName()+" is enabled="+extension.isEnabled());
                }
                if (extension.isEnabled()) {
                    try {
                        ExtensionResponse tempResponse = extension.postInboundAction(er);
                        if (tempResponse != null) {
                            response = tempResponse;
                            //fail fast
                            if (!tempResponse.isAllowed()) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        if (logger.isDebugEnabled()) {
                            String msg = String.format("There was an exception while executing preInboundAction from extension %s", extension.getName());
                            logger.debug(msg, t);
                        }
                    }
                }
            }
        }
        return response;
    }

    public ExtensionResponse executePreApiAction(final ApiRequest apiRequest, List<RestcommExtensionGeneric> extensions) {
        ExtensionResponse response = new ExtensionResponse();

        if (extensions != null && extensions.size() > 0) {
            for (RestcommExtensionGeneric extension : extensions) {
                if(logger.isInfoEnabled()) {
                    logger.info( extension.getName()+" is enabled="+extension.isEnabled());
                }
                if (extension.isEnabled()) {
                    try {
                        ExtensionResponse tempResponse = extension.preApiAction(apiRequest);
                        if (tempResponse != null) {
                            response = tempResponse;
                            //fail fast
                            if (!tempResponse.isAllowed()) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        if (logger.isDebugEnabled()) {
                            String msg = String.format("There was an exception while executing preInboundAction from extension %s", extension.getName());
                            logger.debug(msg, t);
                        }
                    }
                }
            }
        }
        return response;
    }

    public ExtensionResponse executePostApiAction(final ApiRequest apiRequest, List<RestcommExtensionGeneric> extensions) {
        ExtensionResponse response = new ExtensionResponse();

        if (extensions != null && extensions.size() > 0) {
            for (RestcommExtensionGeneric extension : extensions) {
                if(logger.isInfoEnabled()) {
                    logger.info( extension.getName()+" is enabled="+extension.isEnabled());
                }
                if (extension.isEnabled()) {
                    try {
                        ExtensionResponse tempResponse = extension.postApiAction(apiRequest);
                        if (tempResponse != null) {
                            response = tempResponse;
                            //fail fast
                            if (!tempResponse.isAllowed()) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        if (logger.isDebugEnabled()) {
                            String msg = String.format("There was an exception while executing preInboundAction from extension %s", extension.getName());
                            logger.debug(msg, t);
                        }
                    }
                }
            }
        }
        return response;
    }
}

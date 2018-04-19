package org.restcomm.connect.extension.controller;

import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.AccountsDao;
import org.restcomm.connect.dao.ClientsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.dao.OrganizationsDao;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.dao.entities.Client;
import org.restcomm.connect.dao.entities.Organization;
import org.restcomm.connect.extension.api.ApiRequest;
import org.restcomm.connect.extension.api.ExtensionConfiguration;
import org.restcomm.connect.extension.api.ExtensionResponse;
import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.ExtensionContext;
import org.restcomm.connect.extension.api.IExtensionRequest;
import org.restcomm.connect.extension.api.RestcommExtension;
import org.restcomm.connect.extension.api.RestcommExtensionGeneric;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletContext;

/**
 * Created by gvagenas on 21/09/16.
 */
public class ExtensionController implements ExtensionContext{
    private static Logger logger = Logger.getLogger(ExtensionController.class);

    private static ExtensionController instance;
    private List callManagerExtensions;
    private List smsSessionExtensions;
    private List ussdCallManagerExtensions;
    private List restApiExtensions;
    private List featureAccessControlExtensions;

    private DaoManager daoManager;

    private ServletContext context;

    private ExtensionController(){
        this.callManagerExtensions = new CopyOnWriteArrayList();
        this.smsSessionExtensions = new CopyOnWriteArrayList();
        this.ussdCallManagerExtensions = new CopyOnWriteArrayList();
        this.restApiExtensions = new CopyOnWriteArrayList();
        this.featureAccessControlExtensions = new CopyOnWriteArrayList();
    }

    public void init(ServletContext context) {
        this.context = context;
        daoManager = (DaoManager)context.getAttribute(DaoManager.class.getName());
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

    @Override
    public ExtensionConfiguration getEffectiveConfiguration(String extensionSid, String scopeSid) {
        ExtensionsConfigurationDao ecd = daoManager.getExtensionsConfigurationDao();
        ClientsDao cd = daoManager.getClientsDao();
        AccountsDao ad = daoManager.getAccountsDao();
        OrganizationsDao od = daoManager.getOrganizationsDao();

        Sid sid = new Sid(scopeSid);
        Client client = cd.getClient(sid);
        Account account = ad.getAccount(sid);
        Organization organization = od.getOrganization(sid);

        //FIXME: might not be optimized
        //preliminary check for all scopes: client, acc, org
        ExtensionConfiguration extCfg = ecd.getAccountExtensionConfiguration(scopeSid, extensionSid);

        //the scopeSid was a client
        if(client!= null && extCfg==null) {
            account = ad.getAccount(client.getAccountSid());
        }

        //the scopeSid was an account
        if(account!=null && extCfg==null) {
            extCfg = ecd.getAccountExtensionConfiguration(account.getSid().toString(), extensionSid);
            if(extCfg==null) {
                List<String> lineage = ad.getAccountLineage(sid);
                for(String currSid : lineage) {
                    if(logger.isInfoEnabled()) {
                        logger.info("checking "+ currSid);
                    }
                    extCfg = ecd.getAccountExtensionConfiguration(currSid, extensionSid);
                    if(extCfg != null) {
                        break;
                    }
                }
                organization = od.getOrganization(account.getOrganizationSid());
            }
        }

        //the scopeSid was an org
        if(organization!= null && extCfg==null) {
            //it is assumed that lineage will always have identical org, so we only check the originating account
            extCfg = ecd.getAccountExtensionConfiguration(organization.getSid().toString(), extensionSid);
            if(logger.isInfoEnabled()) {
                logger.info("checking "+ account.getOrganizationSid().toString());
            }
        }

        //check default
        if(extCfg==null) {
            //if no account specific entry is defined, we use the extension config
            extCfg = ecd.getConfigurationBySid(new Sid(extensionSid));
        }
        return extCfg;
    }
}

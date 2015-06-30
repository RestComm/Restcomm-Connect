/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.provisioning.number.voxbone;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.provisioning.number.api.ContainerConfiguration;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumber;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberParameters;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager;
import org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberSearchFilters;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * @author jean.deruelle@telestax.com
 */
public class VoxbonePhoneNumberProvisioningManager implements PhoneNumberProvisioningManager {
    private static final Logger logger = Logger.getLogger(VoxbonePhoneNumberProvisioningManager.class);
    private static final String COUNTRY_CODE_PARAM = "countryCodeA3";
    private static final String PAGE_SIZE = "pageSize";
    private static final String PAGE_NUMBER = "pageNumber";
    private static final String CONTENT_TYPE = "application/json";

    protected Boolean telestaxProxyEnabled;
    protected String uri, username, password;
    protected String searchURI, createCartURI, voiceURI, updateURI, cancelURI, countriesURI, listDidsURI;
    protected String voiceUriId;
    protected Configuration activeConfiguration;
    protected ContainerConfiguration containerConfiguration;

    public VoxbonePhoneNumberProvisioningManager() {}

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#init(org.apache.commons.configuration
     * .Configuration, boolean)
     */
    @Override
    public void init(Configuration phoneNumberProvisioningConfiguration, Configuration telestaxProxyConfiguration, ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
        telestaxProxyEnabled = telestaxProxyConfiguration.getBoolean("enabled", false);
        if (telestaxProxyEnabled) {
            uri = telestaxProxyConfiguration.getString("uri");
            username = telestaxProxyConfiguration.getString("username");
            password = telestaxProxyConfiguration.getString("password");
            activeConfiguration = telestaxProxyConfiguration;
        } else {
            Configuration voxboneConfiguration = phoneNumberProvisioningConfiguration.subset("voxbone");
            uri = voxboneConfiguration.getString("uri");
            username = voxboneConfiguration.getString("username");
            password = voxboneConfiguration.getString("password");
            activeConfiguration = voxboneConfiguration;
        }
        searchURI = uri + "/inventory/didgroup";
        createCartURI = uri + "/ordering/cart";
        voiceURI = uri + "/configuration/voiceuri";
        updateURI = uri + "/configuration/configuration";
        cancelURI = uri + "/ordering/cancel";
        countriesURI = uri + "/inventory/country";
        listDidsURI = uri + "/inventory/did";

        Configuration callbackUrlsConfiguration = phoneNumberProvisioningConfiguration.subset("callback-urls");
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

        WebResource webResource = jerseyClient.resource(voiceURI);
        String body = "{\"voiceUri\":{\"voiceUriProtocol\":\"SIP\",\"uri\":\"" + callbackUrlsConfiguration.getString("voice[@url]") + "\"}}";
        ClientResponse clientResponse = webResource.accept(CONTENT_TYPE).type(CONTENT_TYPE).put(ClientResponse.class,body);

        String voiceURIResponse = clientResponse.getEntity(String.class);
        if(logger.isDebugEnabled())
            logger.debug("response " + voiceURIResponse);

        JsonParser parser = new JsonParser();
        JsonObject jsonVoiceURIResponse = parser.parse(voiceURIResponse).getAsJsonObject();

        if (clientResponse.getClientResponseStatus() == Status.OK) {
            JsonObject voxVoiceURI = jsonVoiceURIResponse.get("voiceUri").getAsJsonObject();
            voiceUriId = voxVoiceURI.get("voiceUriId").getAsString();
        } else if (clientResponse.getClientResponseStatus() == Status.UNAUTHORIZED) {
            JsonObject error = jsonVoiceURIResponse.get("errors").getAsJsonArray().get(0).getAsJsonObject();
            throw new IllegalArgumentException(error.get("apiErrorMessage").getAsString());
        } else {
            webResource = jerseyClient.resource(voiceURI);
            clientResponse = webResource.queryParam(PAGE_NUMBER,"0").queryParam(PAGE_SIZE,"300").accept(CONTENT_TYPE).type(CONTENT_TYPE).get(ClientResponse.class);

            String listVoiceURIResponse = clientResponse.getEntity(String.class);
            if(logger.isDebugEnabled())
                logger.debug("response " + listVoiceURIResponse );

            JsonObject jsonListVoiceURIResponse = parser.parse(listVoiceURIResponse).getAsJsonObject();
            // TODO go through the list of voiceURI id and check which one is matching
            JsonObject voxVoiceURI = jsonListVoiceURIResponse.get("voiceUris").getAsJsonArray().get(0).getAsJsonObject();
            voiceUriId = voxVoiceURI.get("voiceUriId").getAsString();
        }
    }

    private List<PhoneNumber> toAvailablePhoneNumbers(final JsonArray phoneNumbers, PhoneNumberSearchFilters listFilters) {
        Pattern searchPattern = listFilters.getFilterPattern();
        final List<PhoneNumber> numbers = new ArrayList<PhoneNumber>();
        for (int i = 0; i < phoneNumbers.size(); i++) {
            JsonObject number = phoneNumbers.get(i).getAsJsonObject();
            if(Integer.parseInt(number.get("stock").toString()) > 0) {
                //we only return the number if there is any stock for it
                String countryCode = number.get(COUNTRY_CODE_PARAM).getAsString();
                String features = null;
                if(number.get("features") != null) {
                    features = number.get("features").toString();
                }
                boolean isVoiceCapable = true;
                boolean isFaxCapable = false;
                boolean isSmsCapable = false;
                if(features.contains("VoxSMS")) {
                    isSmsCapable = true;
                }
                if(features.contains("VoxFax")) {
                    isFaxCapable = true;
                }
                String friendlyName = number.get("countryCodeA3").getAsString();
                if(number.get("cityName") != null && !(number.get("cityName") instanceof JsonNull)) {
                    friendlyName = friendlyName + "-" + number.get("cityName").getAsString();
                }
                if(number.get("areaCode") != null && !(number.get("areaCode") instanceof JsonNull)) {
                    friendlyName = friendlyName + "-" + number.get("areaCode").getAsString();
                }
                final PhoneNumber phoneNumber = new PhoneNumber(
                        friendlyName,
                        number.get("didGroupId").getAsString(),
                        null, null, null, null, null, null,
                        countryCode, isVoiceCapable, isSmsCapable, null, isFaxCapable, null);
                numbers.add(phoneNumber);
            }
        }
        return numbers;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mobicents.servlet.restcomm.provisioning.number.api.PhoneNumberProvisioningManager#searchForNumbers(java.lang.String,
     * java.lang.String, java.util.regex.Pattern, boolean, boolean, boolean, boolean, int, int)
     */
    @Override
    public List<PhoneNumber> searchForNumbers(String country, PhoneNumberSearchFilters listFilters) {
        Locale locale = new Locale("en",country);
        String iso3Country = locale.getISO3Country();
        if(logger.isDebugEnabled()) {
            logger.debug("searchPattern " + listFilters.getFilterPattern());
        }

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

        WebResource webResource = jerseyClient.resource(searchURI);

        // https://developers.voxbone.com/docs/v3/inventory#path__didgroup.html
        webResource = webResource.queryParam(COUNTRY_CODE_PARAM,iso3Country);
//        Pattern filterPattern = listFilters.getFilterPattern();
//        if(filterPattern != null) {
//            webResource = webResource.queryParam("cityNamePattern" , filterPattern.toString());
//        }
        if(listFilters.getAreaCode() != null) {
            webResource = webResource.queryParam("areaCode", listFilters.getAreaCode());
        }
        if(listFilters.getInRateCenter() != null) {
            webResource = webResource.queryParam("rateCenter", listFilters.getInRateCenter());
        }
        if(listFilters.getSmsEnabled() != null) {
            webResource = webResource.queryParam("featureIds", "6");
        }
        if(listFilters.getFaxEnabled() != null) {
            webResource = webResource.queryParam("featureIds", "25");
        }
        if(listFilters.getRangeIndex() != -1) {
            webResource = webResource.queryParam(PAGE_NUMBER, "" + listFilters.getRangeIndex());
        } else {
            webResource = webResource.queryParam(PAGE_NUMBER, "0");
        }
        if(listFilters.getRangeSize() != -1) {
            webResource = webResource.queryParam(PAGE_SIZE, "" + listFilters.getRangeSize());
        } else {
            webResource = webResource.queryParam(PAGE_SIZE, "50");
        }
        ClientResponse clientResponse = webResource.accept(CONTENT_TYPE).type(CONTENT_TYPE)
                .get(ClientResponse.class);

        String response = clientResponse.getEntity(String.class);
        if(logger.isDebugEnabled())
            logger.debug("response " + response);

        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

//        long count = jsonResponse.getAsJsonPrimitive("count").getAsLong();
//        if(logger.isDebugEnabled()) {
//            logger.debug("Number of numbers found : "+count);
//        }
        JsonArray voxboneNumbers = jsonResponse.getAsJsonArray("didGroups");
        final List<PhoneNumber> numbers = toAvailablePhoneNumbers(voxboneNumbers, listFilters);
        return numbers;
//        } else {
//            logger.warn("Couldn't reach uri for getting Phone Numbers. Response status was: "+response.getStatusLine().getStatusCode());
//        }
//
//        return new ArrayList<PhoneNumber>();
    }

    @Override
    public boolean buyNumber(PhoneNumber phoneNumberObject, PhoneNumberParameters phoneNumberParameters) {
        String phoneNumber = phoneNumberObject.getPhoneNumber();
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

        WebResource webResource = jerseyClient.resource(createCartURI);
        ClientResponse clientResponse = webResource.accept(CONTENT_TYPE).type(CONTENT_TYPE).put(ClientResponse.class,"{}");

        String createCartResponse = clientResponse.getEntity(String.class);
        if(logger.isDebugEnabled())
            logger.debug("createCartResponse " + createCartResponse);

        JsonParser parser = new JsonParser();
        JsonObject jsonCreateCartResponse = parser.parse(createCartResponse).getAsJsonObject();
        JsonObject voxCart = jsonCreateCartResponse.get("cart").getAsJsonObject();
        String cartIdentifier = voxCart.get("cartIdentifier").getAsString();

        try {
            Client addToCartJerseyClient = Client.create();
            addToCartJerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

            WebResource addToCartWebResource = addToCartJerseyClient.resource(createCartURI + "/" + cartIdentifier + "/product");
            String addToCartBody = "{\"didCartItem\":{\"didGroupId\":\"" + phoneNumber + "\",\"quantity\":\"1\"}}";
            ClientResponse addToCartResponse = addToCartWebResource.accept(CONTENT_TYPE).type(CONTENT_TYPE).post(ClientResponse.class,addToCartBody);

            if (addToCartResponse.getClientResponseStatus() == Status.OK) {
                String addToCartResponseString = addToCartResponse.getEntity(String.class);
                if(logger.isDebugEnabled())
                    logger.debug("addToCartResponse " + addToCartResponseString);

                JsonObject jsonAddToCartResponse = parser.parse(addToCartResponseString).getAsJsonObject();

                if(jsonAddToCartResponse.get("status").getAsString().equalsIgnoreCase("SUCCESS")) {
                    Client checkoutCartJerseyClient = Client.create();
                    checkoutCartJerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

                    WebResource checkoutCartWebResource = checkoutCartJerseyClient.resource(createCartURI + "/" + cartIdentifier + "/checkout");
                    ClientResponse checkoutCartResponse = checkoutCartWebResource.queryParam("cartIdentifier", cartIdentifier).accept(CONTENT_TYPE).type(CONTENT_TYPE).get(ClientResponse.class);

                    if (checkoutCartResponse.getClientResponseStatus() == Status.OK) {
                        String checkoutCartResponseString = checkoutCartResponse.getEntity(String.class);
                        if(logger.isDebugEnabled())
                            logger.debug("checkoutCartResponse " + checkoutCartResponseString);

                        JsonObject jsonCheckoutCartResponse = parser.parse(checkoutCartResponseString).getAsJsonObject();
                        if(jsonCheckoutCartResponse.get("status").getAsString().equalsIgnoreCase("SUCCESS")) {
                            String orderReference = jsonCheckoutCartResponse.get("productCheckoutList").getAsJsonArray().get(0).getAsJsonObject().get("orderReference").getAsString();
                            Client listDidsJerseyClient = Client.create();
                            listDidsJerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

                            WebResource listDidsWebResource = listDidsJerseyClient.resource(listDidsURI);
                            ClientResponse listDidsResponse = listDidsWebResource.
                                    queryParam("orderReference", orderReference).
                                    queryParam(PAGE_NUMBER, "0").
                                    queryParam(PAGE_SIZE, "50").
                                    accept(CONTENT_TYPE).
                                    type(CONTENT_TYPE).
                                    get(ClientResponse.class);

                            if (listDidsResponse.getClientResponseStatus() == Status.OK) {
                                String listDidsResponseString = listDidsResponse.getEntity(String.class);
                                if(logger.isDebugEnabled())
                                    logger.debug("listDidsResponse " + listDidsResponseString);

                                JsonObject jsonListDidsResponse = parser.parse(listDidsResponseString).getAsJsonObject();
                                JsonObject dids = jsonListDidsResponse.get("dids").getAsJsonArray().get(0).getAsJsonObject();
                                String didId = dids.get("didId").getAsString();
                                String e164 = dids.get("e164").getAsString();
                                phoneNumberObject.setFriendlyName(didId);
                                phoneNumberObject.setPhoneNumber(e164);
                                updateNumber(phoneNumberObject, phoneNumberParameters);
                            } else {
                                return false;
                            }
                        } else {
                            // Handle not enough credit on the account
                            return false;
                        }
                    } else {
                        return false;
                    }
                    // we always return true as the phone number was bought
                    return true;
                } else {
                    return false;
                }
            } else {
                if(logger.isDebugEnabled())
                    logger.debug("Couldn't buy Phone Number " + phoneNumber + ". Response status was: "+ addToCartResponse.getClientResponseStatus());
            }
        } catch (final Exception e) {
            logger.warn("Couldn't reach uri for buying Phone Numbers" + uri, e);
        }

        return false;
    }

    @Override
    public boolean updateNumber(PhoneNumber phoneNumberObj, PhoneNumberParameters phoneNumberParameters) {
        String phoneNumber = phoneNumberObj.getFriendlyName();
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

        WebResource webResource = jerseyClient.resource(updateURI);
        String body = "{\"didIds\":[\"" + phoneNumber + "\"],\"voiceUriId\":\"" + voiceUriId + "\"}";
        ClientResponse clientResponse = webResource.accept(CONTENT_TYPE).type(CONTENT_TYPE).post(ClientResponse.class,body);

        String voiceURIResponse = clientResponse.getEntity(String.class);
        if(logger.isDebugEnabled())
            logger.debug("response " + voiceURIResponse);

//        JsonParser parser = new JsonParser();
//        JsonObject jsonCreateCartResponse = parser.parse(voiceURIResponse).getAsJsonObject();

        if (clientResponse.getClientResponseStatus() == Status.OK) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean cancelNumber(PhoneNumber phoneNumberObj) {
        String phoneNumber = phoneNumberObj.getFriendlyName();
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

        WebResource webResource = jerseyClient.resource(cancelURI);

        String body = "{\"didIds\":[\"" + phoneNumber + "\"]}";
        ClientResponse clientResponse = webResource.accept(CONTENT_TYPE).type(CONTENT_TYPE).post(ClientResponse.class,body);

        String response = clientResponse.getEntity(String.class);
        if(logger.isDebugEnabled())
            logger.debug("response " + response);

//        JsonParser parser = new JsonParser();
//        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

        return clientResponse.getClientResponseStatus() == Status.OK;
    }

    @Override
    public List<String> getAvailableCountries() {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(username, password));

        WebResource webResource = jerseyClient.resource(countriesURI);

        // http://www.voxbone.com/apidoc/resource_InventoryServiceRest.html#path__country.html
        ClientResponse clientResponse = webResource.queryParam(PAGE_NUMBER,"0").queryParam(PAGE_SIZE,"300").accept(CONTENT_TYPE).type(CONTENT_TYPE)
                .get(ClientResponse.class);

        String response = clientResponse.getEntity(String.class);
        if(logger.isDebugEnabled())
            logger.debug("response " + response);

        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        JsonArray voxCountries = jsonResponse.get("countries").getAsJsonArray();
        List<String> countries = new ArrayList<String>();
        for (int i = 0; i < voxCountries.size(); i++) {
            JsonObject country = voxCountries.get(i).getAsJsonObject();
            String countryCode = country.get(COUNTRY_CODE_PARAM).getAsString();
            countries.add(countryCode);
        }

        return countries;
    }
}

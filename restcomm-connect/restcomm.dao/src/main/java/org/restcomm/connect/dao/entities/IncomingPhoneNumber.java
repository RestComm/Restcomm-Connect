/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.restcomm.connect.dao.entities;

import java.net.URI;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class IncomingPhoneNumber {
    private Sid sid;
    private DateTime dateCreated;
    private DateTime dateUpdated;
    private String friendlyName;
    private Sid accountSid;
    private String phoneNumber;
    private String cost;
    private String apiVersion;
    private Boolean hasVoiceCallerIdLookup;
    private URI voiceUrl;
    private String voiceMethod;
    private URI voiceFallbackUrl;
    private String voiceFallbackMethod;
    private URI statusCallback;
    private String statusCallbackMethod;
    private Sid voiceApplicationSid;
    private String voiceApplicationName;
    private URI smsUrl;
    private String smsMethod;
    private URI smsFallbackUrl;
    private String smsFallbackMethod;
    private Sid smsApplicationSid;
    private String smsApplicationName;
    private URI uri;
    private URI ussdUrl;
    private String ussdMethod;
    private URI ussdFallbackUrl;
    private String ussdFallbackMethod;
    private Sid ussdApplicationSid;
    private String ussdApplicationName;
    private URI referUrl;
    private String referMethod;
    private Sid referApplicationSid;
    private String referApplicationName;

    // Capabilities
    private Boolean voiceCapable;
    private Boolean smsCapable;
    private Boolean mmsCapable;
    private Boolean faxCapable;
    private Boolean pureSip;

    public IncomingPhoneNumber(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated,
            final String friendlyName, final Sid accountSid, final String phoneNumber, final String cost, final String apiVersion,
            final Boolean hasVoiceCallerIdLookup, final URI voiceUrl, final String voiceMethod, final URI voiceFallbackUrl,
            final String voiceFallbackMethod, final URI statusCallback, final String statusCallbackMethod,
            final Sid voiceApplicationSid, final URI smsUrl, final String smsMethod, final URI smsFallbackUrl,
            final String smsFallbackMethod, final Sid smsApplicationSid, final URI uri, final URI ussdUrl, final String ussdMethod, final URI ussdFallbackUrl,
            final String ussdFallbackMethod, final Sid ussdApplicationSid,
            final URI referUrl, final String referMethod, final Sid referApplicationSid) {
        this(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, cost, apiVersion, hasVoiceCallerIdLookup,
                voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod,
                voiceApplicationSid, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri, ussdUrl, ussdMethod, ussdFallbackUrl, ussdFallbackMethod, ussdApplicationSid, referUrl, referMethod, referApplicationSid, null, null, null, null, null);
    }

    public IncomingPhoneNumber(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated,
                               final String friendlyName, final Sid accountSid, final String phoneNumber, final String cost, final String apiVersion,
                               final Boolean hasVoiceCallerIdLookup, final URI voiceUrl, final String voiceMethod, final URI voiceFallbackUrl,
                               final String voiceFallbackMethod, final URI statusCallback, final String statusCallbackMethod,
                               final Sid voiceApplicationSid, final URI smsUrl, final String smsMethod, final URI smsFallbackUrl,
                               final String smsFallbackMethod, final Sid smsApplicationSid, final URI uri, final URI ussdUrl, final String ussdMethod, final URI ussdFallbackUrl,
                               final String ussdFallbackMethod, final Sid ussdApplicationSid,
                               final URI referUrl, final String referMethod, final Sid referApplicationSid,
                               final Boolean voiceCapable,
                               final Boolean smsCapable, final Boolean mmsCapable, final Boolean faxCapable, final Boolean pureSip) {
        this(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, cost, apiVersion, hasVoiceCallerIdLookup,
                voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod,
                voiceApplicationSid, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri, ussdUrl, ussdMethod, ussdFallbackUrl, ussdFallbackMethod, ussdApplicationSid,
                        referUrl, referMethod, referApplicationSid,
                voiceCapable, smsCapable, mmsCapable, faxCapable, pureSip, null, null, null, null);
    }

    public IncomingPhoneNumber(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated,
            final String friendlyName, final Sid accountSid, final String phoneNumber, final String cost, final String apiVersion,
            final Boolean hasVoiceCallerIdLookup, final URI voiceUrl, final String voiceMethod, final URI voiceFallbackUrl,
            final String voiceFallbackMethod, final URI statusCallback, final String statusCallbackMethod,
            final Sid voiceApplicationSid, final URI smsUrl, final String smsMethod, final URI smsFallbackUrl,
            final String smsFallbackMethod, final Sid smsApplicationSid, final URI uri, final URI ussdUrl, final String ussdMethod, final URI ussdFallbackUrl,
            final String ussdFallbackMethod, final Sid ussdApplicationSid,
            final URI referUrl, final String referMethod, final Sid referApplicationSid,
            final Boolean voiceCapable,
            final Boolean smsCapable, final Boolean mmsCapable, final Boolean faxCapable, final Boolean pureSip, final String voiceApplicationName, final String smsApplicationName, final String ussdApplicationName, final String referApplicationName) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.friendlyName = friendlyName;
        this.accountSid = accountSid;
        this.phoneNumber = phoneNumber;
        this.cost = cost;
        this.apiVersion = apiVersion;
        this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
        this.voiceUrl = voiceUrl;
        this.voiceMethod = voiceMethod;
        this.voiceFallbackUrl = voiceFallbackUrl;
        this.voiceFallbackMethod = voiceFallbackMethod;
        this.statusCallback = statusCallback;
        this.statusCallbackMethod = statusCallbackMethod;
        this.voiceApplicationSid = voiceApplicationSid;
        this.smsUrl = smsUrl;
        this.smsMethod = smsMethod;
        this.smsFallbackUrl = smsFallbackUrl;
        this.smsFallbackMethod = smsFallbackMethod;
        this.smsApplicationSid = smsApplicationSid;
        this.uri = uri;
        this.ussdUrl = ussdUrl;
        this.ussdMethod = ussdMethod;
        this.ussdFallbackUrl = ussdFallbackUrl;
        this.ussdFallbackMethod = ussdFallbackMethod;
        this.ussdApplicationSid = ussdApplicationSid;
        this.referUrl = referUrl;
        this.referMethod = referMethod;
        this.referApplicationSid = referApplicationSid;
        this.voiceCapable = voiceCapable;
        this.smsCapable = smsCapable;
        this.mmsCapable = mmsCapable;
        this.faxCapable = faxCapable;
        this.pureSip = pureSip;
        this.voiceApplicationName = voiceApplicationName;
        this.smsApplicationName = smsApplicationName;
        this.ussdApplicationName = ussdApplicationName;
        this.referApplicationName = referApplicationName;
    }

    /**
     * @return the sid
     */
    public Sid getSid() {
        return sid;
    }

    /**
     * @param sid the sid to set
     */
    public void setSid(Sid sid) {
        this.sid = sid;
    }

    /**
     * @return the dateCreated
     */
    public DateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(DateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the dateUpdated
     */
    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(DateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the friendlyName
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * @param friendlyName the friendlyName to set
     */
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    /**
     * @return the accountSid
     */
    public Sid getAccountSid() {
        return accountSid;
    }

    /**
     * @param accountSid the accountSid to set
     */
    public void setAccountSid(Sid accountSid) {
        this.accountSid = accountSid;
    }

    /**
     * @return the phoneNumber
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * @param phoneNumber the phoneNumber to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * @return the cost
     */
    public String getCost() {
        return cost;
    }

    /**
     * @param cost the cost to set
     */
    public void setCost(String cost) {
        this.cost = cost;
    }

    /**
     * @return the apiVersion
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * @param apiVersion the apiVersion to set
     */
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * @return the hasVoiceCallerIdLookup
     */
    public Boolean hasVoiceCallerIdLookup() {
        return hasVoiceCallerIdLookup;
    }

    /**
     * @param hasVoiceCallerIdLookup the hasVoiceCallerIdLookup to set
     */
    public void setHasVoiceCallerIdLookup(Boolean hasVoiceCallerIdLookup) {
        this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
    }

    /**
     * @return the voiceUrl
     */
    public URI getVoiceUrl() {
        return voiceUrl;
    }

    /**
     * @param voiceUrl the voiceUrl to set
     */
    public void setVoiceUrl(URI voiceUrl) {
        this.voiceUrl = voiceUrl;
    }

    /**
     * @return the voiceMethod
     */
    public String getVoiceMethod() {
        return voiceMethod;
    }

    /**
     * @param voiceMethod the voiceMethod to set
     */
    public void setVoiceMethod(String voiceMethod) {
        this.voiceMethod = voiceMethod;
    }

    /**
     * @return the voiceFallbackUrl
     */
    public URI getVoiceFallbackUrl() {
        return voiceFallbackUrl;
    }

    /**
     * @param voiceFallbackUrl the voiceFallbackUrl to set
     */
    public void setVoiceFallbackUrl(URI voiceFallbackUrl) {
        this.voiceFallbackUrl = voiceFallbackUrl;
    }

    /**
     * @return the voiceFallbackMethod
     */
    public String getVoiceFallbackMethod() {
        return voiceFallbackMethod;
    }

    /**
     * @param voiceFallbackMethod the voiceFallbackMethod to set
     */
    public void setVoiceFallbackMethod(String voiceFallbackMethod) {
        this.voiceFallbackMethod = voiceFallbackMethod;
    }

    /**
     * @return the statusCallback
     */
    public URI getStatusCallback() {
        return statusCallback;
    }

    /**
     * @param statusCallback the statusCallback to set
     */
    public void setStatusCallback(URI statusCallback) {
        this.statusCallback = statusCallback;
    }

    /**
     * @return the statusCallbackMethod
     */
    public String getStatusCallbackMethod() {
        return statusCallbackMethod;
    }

    /**
     * @param statusCallbackMethod the statusCallbackMethod to set
     */
    public void setStatusCallbackMethod(String statusCallbackMethod) {
        this.statusCallbackMethod = statusCallbackMethod;
    }

    /**
     * @return the voiceApplicationSid
     */
    public Sid getVoiceApplicationSid() {
        return voiceApplicationSid;
    }

    /**
     * @param voiceApplicationSid the voiceApplicationSid to set
     */
    public void setVoiceApplicationSid(Sid voiceApplicationSid) {
        this.voiceApplicationSid = voiceApplicationSid;
    }

    /**
     * @return the smsUrl
     */
    public URI getSmsUrl() {
        return smsUrl;
    }

    /**
     * @param smsUrl the smsUrl to set
     */
    public void setSmsUrl(URI smsUrl) {
        this.smsUrl = smsUrl;
    }

    /**
     * @return the smsMethod
     */
    public String getSmsMethod() {
        return smsMethod;
    }

    /**
     * @param smsMethod the smsMethod to set
     */
    public void setSmsMethod(String smsMethod) {
        this.smsMethod = smsMethod;
    }

    /**
     * @return the smsFallbackUrl
     */
    public URI getSmsFallbackUrl() {
        return smsFallbackUrl;
    }

    /**
     * @param smsFallbackUrl the smsFallbackUrl to set
     */
    public void setSmsFallbackUrl(URI smsFallbackUrl) {
        this.smsFallbackUrl = smsFallbackUrl;
    }

    /**
     * @return the smsFallbackMethod
     */
    public String getSmsFallbackMethod() {
        return smsFallbackMethod;
    }

    /**
     * @param smsFallbackMethod the smsFallbackMethod to set
     */
    public void setSmsFallbackMethod(String smsFallbackMethod) {
        this.smsFallbackMethod = smsFallbackMethod;
    }

    /**
     * @return the smsApplicationSid
     */
    public Sid getSmsApplicationSid() {
        return smsApplicationSid;
    }

    /**
     * @param smsApplicationSid the smsApplicationSid to set
     */
    public void setSmsApplicationSid(Sid smsApplicationSid) {
        this.smsApplicationSid = smsApplicationSid;
    }

    /**
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * @return the ussdUrl
     */
    public URI getUssdUrl() {
        return ussdUrl;
    }

    /**
     * @param ussdUrl the ussdUrl to set
     */
    public void setUssdUrl(URI ussdUrl) {
        this.ussdUrl = ussdUrl;
    }

    /**
     * @return the ussdMethod
     */
    public String getUssdMethod() {
        return ussdMethod;
    }

    /**
     * @param ussdMethod the ussdMethod to set
     */
    public void setUssdMethod(String ussdMethod) {
        this.ussdMethod = ussdMethod;
    }

    /**
     * @return the ussdFallbackUrl
     */
    public URI getUssdFallbackUrl() {
        return ussdFallbackUrl;
    }

    /**
     * @param ussdFallbackUrl the ussdFallbackUrl to set
     */
    public void setUssdFallbackUrl(URI ussdFallbackUrl) {
        this.ussdFallbackUrl = ussdFallbackUrl;
    }

    /**
     * @return the ussdFallbackMethod
     */
    public String getUssdFallbackMethod() {
        return ussdFallbackMethod;
    }

    /**
     * @param ussdFallbackMethod the ussdFallbackMethod to set
     */
    public void setUssdFallbackMethod(String ussdFallbackMethod) {
        this.ussdFallbackMethod = ussdFallbackMethod;
    }

    /**
     * @return the ussdApplicationSid
     */
    public Sid getUssdApplicationSid() {
        return ussdApplicationSid;
    }

    /**
     * @param ussdApplicationSid the ussdApplicationSid to set
     */
    public void setUssdApplicationSid(Sid ussdApplicationSid) {
        this.ussdApplicationSid = ussdApplicationSid;
    }

    /**
     * @return the referUrl
     */
    public URI getReferUrl() {
        return referUrl;
    }

    /**
     * @param referUrl the referUrl to set
     */
    public void setReferUrl(URI referUrl) {
        this.referUrl = referUrl;
    }

    /**
     * @return the referMethod
     */
    public String getReferMethod() {
        return referMethod;
    }

    /**
     * @param referMethod the referMethod to set
     */
    public void setReferMethod(String referMethod) {
        this.referMethod = referMethod;
    }

    /**
     * @return the referApplicationSid
     */
    public Sid getReferApplicationSid() {
        return referApplicationSid;
    }

    /**
     * @param referApplicationSid the referApplicationSid to set
     */
    public void setReferApplicationSid(Sid referApplicationSid) {
        this.referApplicationSid = referApplicationSid;
    }

    /**
     * @return the referApplicationName
     */
    public String getReferApplicationName() {
        return referApplicationName;
    }

    /**
     * @param referApplicationName the referApplicationName to set
     */
    public void setReferApplicationName(String referApplicationName) {
        this.referApplicationName = referApplicationName;
    }

    /**
     * @return the voiceCapable
     */
    public Boolean isVoiceCapable() {
        return voiceCapable;
    }

    /**
     * @param voiceCapable the voiceCapable to set
     */
    public void setVoiceCapable(Boolean voiceCapable) {
        this.voiceCapable = voiceCapable;
    }

    /**
     * @return the smsCapable
     */
    public Boolean isSmsCapable() {
        return smsCapable;
    }

    /**
     * @param smsCapable the smsCapable to set
     */
    public void setSmsCapable(Boolean smsCapable) {
        this.smsCapable = smsCapable;
    }

    /**
     * @return the mmsCapable
     */
    public Boolean isMmsCapable() {
        return mmsCapable;
    }

    /**
     * @param mmsCapable the mmsCapable to set
     */
    public void setMmsCapable(Boolean mmsCapable) {
        this.mmsCapable = mmsCapable;
    }

    /**
     * @return the faxCapable
     */
    public Boolean isFaxCapable() {
        return faxCapable;
    }

    /**
     * @param faxCapable the faxCapable to set
     */
    public void setFaxCapable(Boolean faxCapable) {
        this.faxCapable = faxCapable;
    }

    public Boolean isPureSip() {
        return pureSip;
    }

    public void setPureSip(Boolean pureSip) {
        this.pureSip = pureSip;
    }

    public void setVoiceApplicationName(String voiceApplicationName) {
        this.voiceApplicationName = voiceApplicationName;
    }

    public void setSmsApplicationName(String smsApplicationName) {
        this.smsApplicationName = smsApplicationName;
    }

    public void setUssdApplicationName(String ussdApplicationName) {
        this.ussdApplicationName = ussdApplicationName;
    }

    public String getVoiceApplicationName() {
        return voiceApplicationName;
    }

    public String getSmsApplicationName() {
        return smsApplicationName;
    }

    public String getUssdApplicationName() {
        return ussdApplicationName;
    }

    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private String friendlyName;
        private Sid accountSid;
        private String phoneNumber;
        private String cost;
        private String apiVersion;
        private Boolean hasVoiceCallerIdLookup;
        private URI voiceUrl;
        private String voiceMethod;
        private URI voiceFallbackUrl;
        private String voiceFallbackMethod;
        private URI statusCallback;
        private String statusCallbackMethod;
        private Sid voiceApplicationSid;
        private URI smsUrl;
        private String smsMethod;
        private URI smsFallbackUrl;
        private String smsFallbackMethod;
        private Sid smsApplicationSid;
        private URI uri;

        private URI ussdUrl;
        private String ussdMethod;
        private URI ussdFallbackUrl;
        private String ussdFallbackMethod;
        private Sid ussdApplicationSid;

        private URI referUrl;
        private String referMethod;
        private Sid referApplicationSid;

        // Capabilities
        private Boolean voiceCapable;
        private Boolean smsCapable;
        private Boolean mmsCapable;
        private Boolean faxCapable;
        private Boolean pureSip;


        private Builder() {
            super();
        }

        public IncomingPhoneNumber build() {
            final DateTime now = DateTime.now();
            return new IncomingPhoneNumber(sid, now, now, friendlyName, accountSid, phoneNumber, cost, apiVersion,
                    hasVoiceCallerIdLookup, voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback,
                    statusCallbackMethod, voiceApplicationSid, smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod,
                    smsApplicationSid, uri, ussdUrl, ussdMethod, ussdFallbackUrl, ussdFallbackMethod, ussdApplicationSid,
                    referUrl, referMethod, referApplicationSid,
                    voiceCapable, smsCapable, mmsCapable, faxCapable, pureSip);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setFriendlyName(final String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public void setAccountSid(final Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setPhoneNumber(final String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public void setCost(final String cost) {
            this.cost = cost;
        }

        public void setApiVersion(final String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setHasVoiceCallerIdLookup(final boolean hasVoiceCallerIdLookup) {
            this.hasVoiceCallerIdLookup = hasVoiceCallerIdLookup;
        }

        public void setVoiceUrl(final URI voiceUrl) {
            this.voiceUrl = voiceUrl;
        }

        public void setVoiceMethod(final String voiceMethod) {
            this.voiceMethod = voiceMethod;
        }

        public void setVoiceFallbackUrl(final URI voiceFallbackUrl) {
            this.voiceFallbackUrl = voiceFallbackUrl;
        }

        public void setVoiceFallbackMethod(final String voiceFallbackMethod) {
            this.voiceFallbackMethod = voiceFallbackMethod;
        }

        public void setStatusCallback(final URI statusCallback) {
            this.statusCallback = statusCallback;
        }

        public void setStatusCallbackMethod(final String statusCallbackMethod) {
            this.statusCallbackMethod = statusCallbackMethod;
        }

        public void setVoiceApplicationSid(final Sid voiceApplicationSid) {
            this.voiceApplicationSid = voiceApplicationSid;
        }

        public void setSmsUrl(final URI smsUrl) {
            this.smsUrl = smsUrl;
        }

        public void setSmsMethod(final String smsMethod) {
            this.smsMethod = smsMethod;
        }

        public void setSmsFallbackUrl(final URI smsFallbackUrl) {
            this.smsFallbackUrl = smsFallbackUrl;
        }

        public void setSmsFallbackMethod(final String smsFallbackMethod) {
            this.smsFallbackMethod = smsFallbackMethod;
        }

        public void setSmsApplicationSid(final Sid smsApplicationSid) {
            this.smsApplicationSid = smsApplicationSid;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }

        public void setUssdUrl(final URI ussdUrl) {
            this.ussdUrl = ussdUrl;
        }

        public void setUssdMethod(final String ussdMethod) {
            this.ussdMethod = ussdMethod;
        }

        public void setUssdFallbackUrl(final URI ussdFallbackUrl) {
            this.ussdFallbackUrl = ussdFallbackUrl;
        }

        public void setUssdFallbackMethod(final String ussdFallbackMethod) {
            this.ussdFallbackMethod = ussdFallbackMethod;
        }

        public void setUssdApplicationSid(final Sid ussdApplicationSid) {
            this.ussdApplicationSid = ussdApplicationSid;
        }

        public URI getReferUrl() {
            return referUrl;
        }

        public void setReferUrl(URI referUrl) {
            this.referUrl = referUrl;
        }

        public String getReferMethod() {
            return referMethod;
        }

        public void setReferMethod(String referMethod) {
            this.referMethod = referMethod;
        }

        public Sid getReferApplicationSid() {
            return referApplicationSid;
        }

        public void setReferApplicationSid(Sid referApplicationSid) {
            this.referApplicationSid = referApplicationSid;
        }

        public void setVoiceCapable(Boolean voiceCapable) {
            this.voiceCapable = voiceCapable;
        }

        public void setSmsCapable(Boolean smsCapable) {
            this.smsCapable = smsCapable;
        }

        public void setMmsCapable(Boolean mmsCapable) {
            this.mmsCapable = mmsCapable;
        }

        public void setFaxCapable(Boolean faxCapable) {
            this.faxCapable = faxCapable;
        }

        public void setPureSip(Boolean pureSip) {
            this.pureSip = pureSip;
        }

        public static Builder builder() {
            return new Builder();
        }
    }
}

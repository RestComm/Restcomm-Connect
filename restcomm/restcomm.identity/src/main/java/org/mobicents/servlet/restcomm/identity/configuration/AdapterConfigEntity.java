package org.mobicents.servlet.restcomm.identity.configuration;

import com.google.gson.annotations.SerializedName;

public class AdapterConfigEntity {
    String realm;

    @SerializedName("realm-public-key")
    String realmPublicKey;

    @SerializedName("auth-server-url")
    String authServerUrl;

    @SerializedName("bearer-only")
    Boolean bearerOnly;

    @SerializedName("ssl-required")
    String sslRequired;

    String resource;

    @SerializedName("public-client")
    Boolean publicClient;

    @SerializedName("use-resource-role-mappings")
    Boolean useResourceRoleMappings;

    public AdapterConfigEntity() {
        // TODO Auto-generated constructor stub
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealmPublicKey() {
        return realmPublicKey;
    }

    public void setRealmPublicKey(String realmPublicKey) {
        this.realmPublicKey = realmPublicKey;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public Boolean getBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(Boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }


    public String getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(String sslRequired) {
        this.sslRequired = sslRequired;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Boolean getPublicClient() {
        return publicClient;
    }

    public void setPublicClient(Boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Boolean getUseResourceRoleMappings() {
        return useResourceRoleMappings;
    }

    public void setUseResourceRoleMappings(Boolean useResourceRoleMappings) {
        this.useResourceRoleMappings = useResourceRoleMappings;
    }

}

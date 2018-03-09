/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.connect.extension.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.HashMap;

import java.util.Map;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.extension.api.ConfigurationException;
import org.restcomm.connect.extension.api.ExtensionConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class DefaultExtensionConfiguration {
    public enum PropertyType {
        VERSION("version");
        private String value;

        private PropertyType(String value) {
            this.value = value;
        }
    };

    private static final Logger logger = Logger.getLogger(DefaultExtensionConfiguration.class);
    private boolean workingWithLocalConf;
    private ExtensionsConfigurationDao extensionConfigurationDao;
    private ExtensionConfiguration extensionConfiguration;
    private JsonObject configurationJsonObj;
    private JsonParser jsonParser;
    private DaoManager daoManager;
    private Gson gson;
    private JsonObject defaultConfigurationJsonObj;
    private String extensionName;
    private DefaultArtifactVersion defVersion;
    private HashMap<String, String> specificConfigurationMap;
    private Sid sid;
    private String localConfigPath;

    public DefaultExtensionConfiguration() {
        this.sid = new Sid("EX00000000000000000000000000000001");
        this.localConfigPath = "";
    }

    public DefaultExtensionConfiguration(final DaoManager daoManager, String extensionName, String localConfigPath) {
        try {
            init(daoManager, extensionName, localConfigPath);
        } catch (Exception e) {
            logger.error("Exception initializing");
        }
    }

    public void init(final DaoManager daoManager, String extensionName, String localConfigPath) throws ConfigurationException {
        try {
            this.setDaoManager(daoManager);
            this.extensionConfigurationDao = daoManager.getExtensionsConfigurationDao();

            if (extensionName.isEmpty() && localConfigPath.isEmpty()) {
                throw new ConfigurationException("extensionName or local config cant be empty");
            }
            if (!extensionName.isEmpty()) {
                this.extensionName = extensionName;
            }
            if (!localConfigPath.isEmpty()) {
                // Load the default extensionConfiguration from file
                this.defaultConfigurationJsonObj = loadDefaultConfiguration(localConfigPath);

                configurationJsonObj = this.defaultConfigurationJsonObj;

                // Get the extension name from default extensionConfiguration
                String temp = defaultConfigurationJsonObj.get("extension_name").getAsString();
                if (!temp.isEmpty()) {
                    extensionName = temp;
                }
                defVersion = new DefaultArtifactVersion(defaultConfigurationJsonObj.get("version").getAsString());
            }
        // Load extensionConfiguration from DB
        extensionConfiguration = extensionConfigurationDao.getConfigurationByName(extensionName);

            // try fetch sid from name
            if (extensionConfiguration == null) {
                // If extensionConfiguration from DB is null then add the default values to DB
                this.sid = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
                extensionConfiguration = new ExtensionConfiguration(sid, this.extensionName, true,
                        defaultConfigurationJsonObj.toString(), ExtensionConfiguration.configurationType.JSON, DateTime.now());
                extensionConfigurationDao.addConfiguration(extensionConfiguration);

            } else {
                // Get configuration object
                this.sid = extensionConfiguration.getSid();
                // try get default config data
                JsonObject dbConfiguration = null;

                DefaultArtifactVersion currentVersion = null;
                try {
                    dbConfiguration = (JsonObject) jsonParser.parse((String) extensionConfiguration.getConfigurationData());
                    if (dbConfiguration.get("version") != null) {
                        currentVersion = new DefaultArtifactVersion(dbConfiguration.get("version").getAsString());
                    }

                    if (dbConfiguration != null && (currentVersion == null || currentVersion.compareTo(defVersion) < 0)) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Configuration found in the DB is older version than the default one: "
                                    + defVersion.toString());
                        }

                        for (Map.Entry<String, JsonElement> jsonElementEntry : defaultConfigurationJsonObj.entrySet()) {
                            if (!jsonElementEntry.getKey().equalsIgnoreCase("specifics_configuration")
                                    && dbConfiguration.get(jsonElementEntry.getKey()) == null) {
                                dbConfiguration.add(jsonElementEntry.getKey(), jsonElementEntry.getValue());
                            }
                        }
                        if (dbConfiguration.get("version") != null) {
                            dbConfiguration.remove("version");
                        }
                        dbConfiguration.addProperty("version", defaultConfigurationJsonObj.get("version").getAsString());

                        extensionConfiguration = new ExtensionConfiguration(extensionConfiguration.getSid(), extensionName,
                                extensionConfiguration.isEnabled(), dbConfiguration.toString(),
                                ExtensionConfiguration.configurationType.JSON, DateTime.now());
                        extensionConfigurationDao.updateConfiguration(extensionConfiguration);
                    }
                    configurationJsonObj = dbConfiguration;
                    // Load Specific Configuration Map
                    // loadSpecificConfigurationMap(configurationJsonObj);
                } catch (Exception e) {
                }

            }
            if (logger.isInfoEnabled()) {
                logger.info("Finished loading configuration for extension: " + extensionName);
            }
        } catch (ConfigurationException configurationException) {
            String errorMessage = "Exception during " + this.getClass() + " Configuration constructor ";
            if (logger.isDebugEnabled()) {
                logger.debug(errorMessage + configurationException);
            }
            throw new ConfigurationException(errorMessage);
        } catch (PersistenceException persistenceException) {
            if (logger.isDebugEnabled()) {
                logger.debug("PersistenceException during " + this.getClass() + " init, will fallback to default configuration");
            }
            workingWithLocalConf = true;
        } catch (IOException e) {
            logger.debug("IOException during " + this.getClass());
        }
    }

    public JsonObject loadDefaultConfiguration(String localConfigFilePath) throws IOException {
        JsonObject jsonObj = null;
        jsonParser = new JsonParser();
        InputStream in = (InputStream) getClass().getResourceAsStream(localConfigFilePath);
        BufferedReader inReader = new BufferedReader(new InputStreamReader(in));
        JsonReader reader = new JsonReader(inReader);
        JsonElement jsonElement = jsonParser.parse(reader);
        jsonObj = (JsonObject) jsonElement;
        in.close();
        inReader.close();
        reader.close();
        return jsonObj;
    }

    public void reloadConfiguration() {
        if (!workingWithLocalConf) {
            if (extensionConfigurationDao.isLatestVersionByName(extensionName, extensionConfiguration.getDateUpdated())) {
                extensionConfiguration = extensionConfigurationDao.getConfigurationByName(extensionName);
                String updatedConf = (String) extensionConfiguration.getConfigurationData();
                configurationJsonObj = (JsonObject) jsonParser.parse(updatedConf);
                // loadSpecificConfigurationMap(configurationJsonObj);
                if (logger.isInfoEnabled()) {
                    logger.info(this.extensionName + " extension configuration reloaded");
                }
            }
        }
    }

    public boolean isEnabled() {
        reloadConfiguration();
        if (extensionConfiguration != null) {
            return extensionConfiguration.isEnabled();
        } else {
            return true;
        }
    }

    public String getVersion() {
        reloadConfiguration();
        String ver = configurationJsonObj.get(PropertyType.VERSION.value).getAsString();
        return ver;
    }

    public Sid getSid() {
        return this.sid;
    }

    public void loadSpecificConfigurationMap(final JsonObject json) {
        JsonArray specificConfJsonArray = json.getAsJsonArray();
        // JsonArray specificConfJsonArray = json.getAsJsonArray("specifics_configuration");
        // if (specificConfJsonArray != null) {
        // specificConfigurationMap = new HashMap<String,String>();
        // Iterator<JsonElement> iter = specificConfJsonArray.iterator();
        // while (iter.hasNext()) {
        // JsonElement elem = iter.next();
        // if (elem.getAsJsonObject().get("sid") != null) {
        // specificConfigurationMap.put(sid, value);
        // }
        // }
        // }
        // return map
    }

    // getConfigAsJson
    // getConfigAsConfiguration
    // getConfigAsHashMap
    /*public void getSpecificConfigurationMapAsXml() {
    }*/

    /**
     * @return the daoManager
     */
    public DaoManager getDaoManager() {
        return daoManager;
    }

    /**
     * @param daoManager the daoManager to set
     */
    public void setDaoManager(DaoManager daoManager) {
        this.daoManager = daoManager;
    }

    public JsonObject getCurrentConf() {
        return configurationJsonObj;
    }
}

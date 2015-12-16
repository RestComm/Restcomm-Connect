/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.servlet.restcomm;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.ApplicationsDao;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.IncomingPhoneNumbersDao;
import org.mobicents.servlet.restcomm.dao.NotificationsDao;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Application;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.Notification;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.thoughtworks.xstream.XStream;

/**
 * This class was designed to be used with exclusivity by
 * {@link org.mobicents.servlet.restcomm.RvdProjectsMigrator}, once that Restcomm should not interact
 * with RVD's workspace as a typical operation given that the migration
 * process forms a very specific scenario.
 *
 * @author guilherme.jansen@telestax.com
 */
public class RvdProjectsMigrationHelper {

    private static final String CONTEXT_NAME_RVD = "restcomm-rvd.war";
    private static final String WORKSPACE_DIRECTORY_NAME = "workspace";
    private static final String PROTO_DIRECTORY_PREFIX = "_proto";
    private static final Pattern RVD_PROJECT_URL = Pattern.compile("^\\/restcomm-rvd.*\\/(.*)\\/controller$");
    private static final String ACCOUNT_NOTIFICATIONS_SID = "ACae6e420f425248d6a26948c17a9e2acf";

    private Configuration configuration;
    private String workspacePath;
    private StateHeader currentStateHeader;
    private HashMap<String, String> projectsApplications;
    private final ApplicationsDao applicationDao;
    private final AccountsDao accountsDao;
    private final IncomingPhoneNumbersDao didsDao;
    private final ClientsDao clientsDao;
    private final NotificationsDao notificationsDao;
    private List<IncomingPhoneNumber> dids;
    private List<Client> clients;

    public RvdProjectsMigrationHelper(ServletContext servletContext, Configuration configuration) throws FileNotFoundException {
        defineWorkspacePath(servletContext);
        this.configuration = configuration;
        final DaoManager storage = (DaoManager) servletContext.getAttribute(DaoManager.class.getName());
        this.projectsApplications = new HashMap<String, String>();
        this.applicationDao = storage.getApplicationsDao();
        this.accountsDao = storage.getAccountsDao();
        this.didsDao = storage.getIncomingPhoneNumbersDao();
        this.clientsDao = storage.getClientsDao();
        this.notificationsDao = storage.getNotificationsDao();
    }

    private void defineWorkspacePath(ServletContext servletContext) throws FileNotFoundException {
        // Obtain RVD context root path
        String contextRootPath = servletContext.getRealPath("/");
        String contextPathRvd = contextRootPath + "../" + CONTEXT_NAME_RVD + "/";

        // Load RVD configuration and check workspace path
        FileInputStream input = new FileInputStream(contextPathRvd + "WEB-INF/rvd.xml");
        XStream xstream = new XStream();
        xstream.alias("rvd", RvdConfig.class);
        RvdConfig rvdConfig = (RvdConfig) xstream.fromXML(input);
        String workspaceBasePath = contextPathRvd + WORKSPACE_DIRECTORY_NAME;
        if (rvdConfig.getWorkspaceLocation() != null && !"".equals(rvdConfig.getWorkspaceLocation())) {
            if (rvdConfig.getWorkspaceLocation().startsWith("/"))
                workspaceBasePath = rvdConfig.getWorkspaceLocation(); // this is an absolute path
            else
                workspaceBasePath = contextPathRvd + rvdConfig.getWorkspaceLocation(); // this is a relative path hooked under
                                                                                       // RVD context
        }
        this.workspacePath = workspaceBasePath;
    }

    public List<String> listProjects() throws RvdProjectsMigrationException {
        List<String> items = new ArrayList<String>();
        File workspaceDir = new File(workspacePath);
        if (workspaceDir.exists()) {
            File[] entries = workspaceDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File anyfile) {
                    if (anyfile.isDirectory() && !anyfile.getName().startsWith(PROTO_DIRECTORY_PREFIX))
                        return true;
                    return false;
                }
            });
            Arrays.sort(entries, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    File statefile1 = new File(f1.getAbsolutePath() + File.separator + "state");
                    File statefile2 = new File(f2.getAbsolutePath() + File.separator + "state");
                    if (statefile1.exists() && statefile2.exists())
                        return Long.valueOf(statefile2.lastModified()).compareTo(statefile1.lastModified());
                    else
                        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                }
            });
            for (File entry : entries) {
                items.add(entry.getName());
            }
        } else {
            throw new RvdProjectsMigrationException("[ERROR-CODE:1] Error while loading the list of projects from workspace", 1);
        }
        return items;
    }

    public String renameProjectUsingNewConvention(String projectName) throws RvdProjectsMigrationException {
        Sid projectSid = Sid.generate(Sid.Type.PROJECT);
        try {
            renameProject(projectName, projectSid.toString());
        } catch (Exception e) {
            throw new RvdProjectsMigrationException("[ERROR-CODE:2] Error while renaming the project '" + projectName
                    + "' to '"
                    + projectSid.toString() + "'"
 + e.getMessage(), 2);
        }
        return projectSid.toString();
    }

    public void renameProject(String source, String dest) throws IOException {
        File sourceDir = new File(workspacePath + File.separator + source);
        File destDir = new File(workspacePath + File.separator + dest);
        FileUtils.moveDirectory(sourceDir, destDir);
    }

    public void loadProjectState(String projectName) throws RvdProjectsMigrationException {
        try {
            String pathName = workspacePath + File.separator + projectName + File.separator + "state";
            File file = new File(pathName);
            if (!file.exists()) {
                throw new RvdProjectsMigrationException("File " + file.getPath() + "does not exist");
            }
            String data = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            JsonParser parser = new JsonParser();
            JsonElement headerElement = parser.parse(data).getAsJsonObject().get("header");
            if (headerElement == null) {
                throw new RvdProjectsMigrationException();
            }
            Gson gson = new Gson();
            currentStateHeader = gson.fromJson(headerElement, StateHeader.class);
        } catch (Exception e) {
            throw new RvdProjectsMigrationException("[ERROR-CODE:3] Error loading state file from project '" + projectName
                    + "' " + e.getMessage(), 3);
        }
    }

    public boolean projectUsesNewNamingConvention(String projectName) {
        return Sid.pattern.matcher(projectName).matches();
    }

    public void createOrUpdateApplicationEntity(String projectSid, String projectName) throws RvdProjectsMigrationException {
        Application app = null;
        try {
            app = applicationDao.getApplication(projectName);
            if (app != null && app.getProjectSid() == null) {
                // Update application
                app = app.setRcmlUrl(URI.create("/restcomm-rvd/services/apps/" + projectSid + "/controller"));
                app = app.setProjectSid(new Sid(projectSid));
                applicationDao.updateApplication(app);
                projectsApplications.put(projectSid, app.getSid().toString());
            } else if (app == null) {
                // Create new application
                Account account = accountsDao.getAccount(currentStateHeader.getOwner());
                if (account == null) {
                    throw new Exception("Error locating the owner account for project \"" + projectSid + "\"");
                }
                final Application.Builder builder = Application.builder();
                final Sid sid = Sid.generate(Sid.Type.APPLICATION);
                builder.setSid(sid);
                builder.setFriendlyName(projectName);
                builder.setAccountSid(account.getAccountSid());
                builder.setApiVersion(configuration.getString("api-version"));
                builder.setHasVoiceCallerIdLookup(false);
                String rootUri = configuration.getString("root-uri");
                rootUri = StringUtils.addSuffixIfNotPresent(rootUri, "/");
                final StringBuilder buffer = new StringBuilder();
                buffer.append(rootUri).append(configuration.getString("api-version")).append("/Accounts/")
                        .append(account.toString()).append("/Applications/").append(sid.toString());
                builder.setUri(URI.create(buffer.toString()));
                builder.setRcmlUrl(URI.create("/restcomm-rvd/services/apps/" + projectSid + "/controller"));
                builder.setKind(Application.Kind.getValueOf(currentStateHeader.getProjectKind()));
                builder.setProjectSid(new Sid(projectSid));
                app = builder.build();
                applicationDao.addApplication(app);
                projectsApplications.put(projectSid, app.getSid().toString());
            }
        } catch (Exception e) {
            String suffix = app != null ? "with the application '" + app.getSid().toString() + "' " : "";
            throw new RvdProjectsMigrationException("[ERROR-CODE:4] Error while synchronizing the project '" + projectSid
                    + "' " + suffix + e.getMessage(), 4);
        }
    }

    public void updateIncomingPhoneNumbers(String projectSid, String projectName) throws RvdProjectsMigrationException {
        try {

            if (dids == null) {
                dids = didsDao.getAllIncomingPhoneNumbers();
            }
        } catch (Exception e) {
            throw new RvdProjectsMigrationException(
                    "[ERROR-CODE:5] Error while loading the IncomingPhoneNumbers list for updates with project '"
                    + projectSid
                            + "' " + e.getMessage(), 5);
        }
        for (int i = 0; i < dids.size(); i++) {
            IncomingPhoneNumber did = dids.get(i);
            try {
                Application.Kind kind = Application.Kind.getValueOf(currentStateHeader.getProjectKind());
                switch (kind) {
                    case SMS:
                        if (hasUrlReference(did.getSmsUrl().toString(), projectName)) {
                            Sid smsApplicationSid = new Sid(String.valueOf(projectsApplications.get(projectSid)));
                            IncomingPhoneNumber updateSmsDid = new IncomingPhoneNumber(did.getSid(), did.getDateCreated(),
                                    did.getDateUpdated(), did.getFriendlyName(), did.getAccountSid(), did.getPhoneNumber(),
                                    did.getCost(), did.getApiVersion(), did.hasVoiceCallerIdLookup(), did.getVoiceUrl(),
                                    did.getVoiceMethod(), did.getVoiceFallbackUrl(), did.getVoiceFallbackMethod(),
                                    did.getStatusCallback(), did.getStatusCallbackMethod(), did.getVoiceApplicationSid(), null,
                                    null, did.getSmsFallbackUrl(), did.getSmsFallbackMethod(), smsApplicationSid, did.getUri(),
                                    did.getUssdUrl(), did.getUssdMethod(), did.getUssdFallbackUrl(),
                                    did.getUssdFallbackMethod(), did.getUssdApplicationSid(), did.isVoiceCapable(),
                                    did.isSmsCapable(), did.isMmsCapable(), did.isFaxCapable(), did.isPureSip());
                            didsDao.updateIncomingPhoneNumber(updateSmsDid);
                            dids.set(i, updateSmsDid);
                        }
                        break;
                    case USSD:
                        if (hasUrlReference(did.getUssdUrl().toString(), projectName)) {
                            Sid ussdApplicationSid = new Sid(String.valueOf(projectsApplications.get(projectSid)));
                            IncomingPhoneNumber updateUssdDid = new IncomingPhoneNumber(did.getSid(), did.getDateCreated(),
                                    did.getDateUpdated(), did.getFriendlyName(), did.getAccountSid(), did.getPhoneNumber(),
                                    did.getCost(), did.getApiVersion(), did.hasVoiceCallerIdLookup(), did.getVoiceUrl(),
                                    did.getVoiceMethod(), did.getVoiceFallbackUrl(), did.getVoiceFallbackMethod(),
                                    did.getStatusCallback(), did.getStatusCallbackMethod(), did.getVoiceApplicationSid(),
                                    did.getSmsUrl(), did.getSmsMethod(), did.getSmsFallbackUrl(), did.getSmsFallbackMethod(),
                                    did.getSmsApplicationSid(), did.getUri(), null, null, did.getUssdFallbackUrl(),
                                    did.getUssdFallbackMethod(), ussdApplicationSid, did.isVoiceCapable(), did.isSmsCapable(),
                                    did.isMmsCapable(), did.isFaxCapable(), did.isPureSip());
                            didsDao.updateIncomingPhoneNumber(updateUssdDid);
                            dids.set(i, updateUssdDid);
                        }
                        break;
                    case VOICE:
                        if (hasUrlReference(did.getVoiceUrl().toString(), projectName)) {
                            Sid voiceApplicationSid = new Sid(String.valueOf(projectsApplications.get(projectSid)));
                            IncomingPhoneNumber updateVoiceDid = new IncomingPhoneNumber(did.getSid(), did.getDateCreated(),
                                    did.getDateUpdated(), did.getFriendlyName(), did.getAccountSid(), did.getPhoneNumber(),
                                    did.getCost(), did.getApiVersion(), did.hasVoiceCallerIdLookup(), null, null,
                                    did.getVoiceFallbackUrl(), did.getVoiceFallbackMethod(), did.getStatusCallback(),
                                    did.getStatusCallbackMethod(), voiceApplicationSid, did.getSmsUrl(), did.getSmsMethod(),
                                    did.getSmsFallbackUrl(), did.getSmsFallbackMethod(), did.getSmsApplicationSid(),
                                    did.getUri(), did.getUssdUrl(), did.getUssdMethod(), did.getUssdFallbackUrl(),
                                    did.getUssdFallbackMethod(), did.getUssdApplicationSid(), did.isVoiceCapable(),
                                    did.isSmsCapable(), did.isMmsCapable(), did.isFaxCapable(), did.isPureSip());
                            didsDao.updateIncomingPhoneNumber(updateVoiceDid);
                            dids.set(i, updateVoiceDid);
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                throw new RvdProjectsMigrationException("[ERROR-CODE:6] Error while updating IncomingPhoneNumber '"
                        + did.getSid().toString() + "' with the Application '" + projectsApplications.get(projectSid) + "' "
                        + e.getMessage(), 6);
            }
        }
    }

    private boolean hasUrlReference(String url, String projectName) throws UnsupportedEncodingException {
        if (url != null && !url.isEmpty()) {
            Matcher m = RVD_PROJECT_URL.matcher(url);
            if (m.find()) {
                String result = m.group(1);
                result = URLDecoder.decode(result, "UTF-8");
                return projectName.equals(result);
            }
        }
        return false;
    }

    public void updateClients(String projectSid, String projectName) throws RvdProjectsMigrationException {
        try {
            if (clients == null) {
                clients = clientsDao.getAllClients();
            }

        } catch (Exception e) {
            throw new RvdProjectsMigrationException(
                    "[ERROR-CODE:7] Error while loading the Clients list for updates with project '" + projectSid
                    + "'. "
                            + e.getMessage(), 7);
        }
        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            try {
                Application.Kind kind = Application.Kind.getValueOf(currentStateHeader.getProjectKind());
                if (kind == Application.Kind.VOICE) {
                    if (hasUrlReference(client.getVoiceUrl().toString(), projectName)) {
                        Sid voiceApplicationSid = new Sid(String.valueOf(projectsApplications.get(projectSid)));
                        client = client.setVoiceApplicationSid(voiceApplicationSid);
                        client = client.setVoiceMethod(null);
                        client = client.setVoiceUrl(null);
                        clientsDao.updateClient(client);
                        clients.set(i, client);
                    }
                }
            } catch (Exception e) {
                throw new RvdProjectsMigrationException("[ERROR-CODE:8] Error while updating Client '"
                        + client.getSid().toString() + "' with the Application '" + projectsApplications.get(projectSid) + "' "
                        + e.getMessage(), 8);
            }
        }
    }

    public void storeWorkspaceStatus(boolean migrationSucceeded) throws RvdProjectsMigrationException {
        String pathName = workspacePath + File.separator + ".workspaceStatus";
        File file = new File(pathName);
        Gson gson = new Gson();
        WorkspaceStatus ws = new WorkspaceStatus(migrationSucceeded);
        String data = gson.toJson(ws);
        try {
            FileUtils.writeStringToFile(file, data, "UTF-8");
        } catch (IOException e) {
            throw new RvdProjectsMigrationException("[ERROR-CODE:9] Error creating file in storage: " + file + e.getMessage(),
                    9);
        }
    }

    public boolean isMigrationSucceeded() {
        String pathName = workspacePath + File.separator + ".workspaceStatus";
        File file = new File(pathName);
        if (!file.exists()) {
            return false;
        }
        String data;
        try {
            data = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
        } catch (IOException e) {
            return false;
        }
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(data).getAsJsonObject();
        if (element != null) {
            Gson gson = new Gson();
            WorkspaceStatus ws = gson.fromJson(element, WorkspaceStatus.class);
            return ws.getNamingMigrationSucceeded();
        } else {
            return false;
        }
    }

    public void addNotification(String message, Integer level) throws URISyntaxException {
        Notification.Builder builder = Notification.builder();
        Sid sid = Sid.generate(Sid.Type.NOTIFICATION);
        builder.setSid(sid);
        builder.setAccountSid(new Sid(ACCOUNT_NOTIFICATIONS_SID));
        builder.setApiVersion(configuration.getString("api-version"));
        builder.setLog(level);
        builder.setMoreInfo(new URI("http://docs.telestax.com/restcomm-pages/"));
        builder.setMessageText(message);
        builder.setMessageDate(DateTime.now());
        StringBuilder buffer = new StringBuilder();
        buffer.append("/").append(configuration.getString("api-version")).append("/Accounts/");
        buffer.append(ACCOUNT_NOTIFICATIONS_SID).append("/Notifications/");
        buffer.append(sid.toString());
        final URI uri = URI.create(buffer.toString());
        builder.setUri(uri);

        Notification notification = builder.build();
        notificationsDao.addNotification(notification);
    }

    public boolean isMigrationEnabled() {
        Boolean value = new Boolean(configuration.getString("rvd-workspace-migration-enabled"));
        return value;
    }

    public String getApplicationSidByProjectSid(String projectSid) {
        return projectsApplications.get(projectSid);
    }

    private class WorkspaceStatus {

        boolean namingMigrationSucceeded;

        public WorkspaceStatus() {

        }

        public WorkspaceStatus(boolean namingMigrationSucceeded) {
            super();
            this.namingMigrationSucceeded = namingMigrationSucceeded;
        }

        public boolean getNamingMigrationSucceeded() {
            return namingMigrationSucceeded;
        }

    }

    private class RvdConfig {
        private String workspaceLocation;
        private String restcommPublicIp;
        private String sslMode;

        public RvdConfig() {
        }

        public RvdConfig(String workspaceLocation, String restcommPublicIp, String sslMode) {
            super();
            this.workspaceLocation = workspaceLocation;
            this.restcommPublicIp = restcommPublicIp;
            this.sslMode = sslMode;
        }

        public String getWorkspaceLocation() {
            return workspaceLocation;
        }

        public String getRestcommPublicIp() {
            return restcommPublicIp;
        }

        public String getSslMode() {
            return sslMode;
        }

    }

    private class StateHeader {
        // application logging settings for this project. If not null logging is enabled.
        // We are using an object instead of a boolean to easily add properties in the future
        public class Logging {
        }

        String projectKind;
        String startNodeName;
        String version;
        String owner; // the Restcomm user id that owns the project or null if it has no owner at all. Added in 7.1.6 release

        // Logging logging; - moved to the separate 'settings' file
        public StateHeader() {
        }

        public StateHeader(String projectKind, String startNodeName, String version) {
            super();
            this.projectKind = projectKind;
            this.startNodeName = startNodeName;
            this.version = version;
        }

        public StateHeader(String projectKind, String startNodeName, String version, String owner) {
            super();
            this.projectKind = projectKind;
            this.startNodeName = startNodeName;
            this.version = version;
            this.owner = owner;
        }

        public String getProjectKind() {
            return projectKind;
        }

        public String getStartNodeName() {
            return startNodeName;
        }

        public String getVersion() {
            return version;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner2) {
            this.owner = owner2;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}

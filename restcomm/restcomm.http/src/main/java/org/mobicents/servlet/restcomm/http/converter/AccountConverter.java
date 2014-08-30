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
package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.util.StringUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class AccountConverter extends AbstractConverter implements JsonSerializer<Account> {
    private final String apiVersion;
    private final String rootUri;

    public AccountConverter(final Configuration configuration) {
        super(configuration);
        this.apiVersion = configuration.getString("api-version");
        rootUri = StringUtils.addSuffixIfNotPresent(configuration.getString("root-uri"), "/");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Account.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final Account account = (Account) object;
        writer.startNode("Account");
        writeSid(account.getSid(), writer);
        writeFriendlyName(account.getFriendlyName(), writer);
        writeStatus(account.getStatus().toString(), writer);
        writeType(account.getType().toString(), writer);
        writeDateCreated(account.getDateCreated(), writer);
        writeDateUpdated(account.getDateUpdated(), writer);
        writeAuthToken(account, writer);
        writeUri(account.getUri(), writer);
        writeSubResourceUris(account, writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Account account, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(account.getSid(), object);
        writeFriendlyName(account.getFriendlyName(), object);
        writeType(account.getType().toString(), object);
        writeStatus(account.getStatus().toString(), object);
        writeDateCreated(account.getDateCreated(), object);
        writeDateUpdated(account.getDateUpdated(), object);
        writeAuthToken(account, object);
        writeUri(account.getUri(), object);
        writeSubResourceUris(account, object);
        return object;
    }

    private String prefix(final Account account) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(rootUri).append(apiVersion).append("/Accounts/").append(account.getSid().toString());
        return buffer.toString();
    }

    private void writeAuthToken(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("AuthToken");
        writer.setValue(account.getAuthToken());
        writer.endNode();
    }

    private void writeAuthToken(final Account account, final JsonObject object) {
        object.addProperty("auth_token", account.getAuthToken());
    }

    private void writeAvailablePhoneNumbers(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("AvailablePhoneNumbers");
        writer.setValue(prefix(account) + "/AvailablePhoneNumbers");
        writer.endNode();
    }

    private void writeAvailablePhoneNumbers(final Account account, final JsonObject object) {
        object.addProperty("available_phone_numbers", prefix(account) + "/AvailablePhoneNumbers.json");
    }

    private void writeCalls(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("Calls");
        writer.setValue(prefix(account) + "/Calls");
        writer.endNode();
    }

    private void writeCalls(final Account account, final JsonObject object) {
        object.addProperty("calls", prefix(account) + "/Calls.json");
    }

    private void writeConferences(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("Conferences");
        writer.setValue(prefix(account) + "/Conferences");
        writer.endNode();
    }

    private void writeConferences(final Account account, final JsonObject object) {
        object.addProperty("conferences", prefix(account) + "/Conferences.json");
    }

    private void writeIncomingPhoneNumbers(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("IncomingPhoneNumbers");
        writer.setValue(prefix(account) + "/IncomingPhoneNumbers");
        writer.endNode();
    }

    private void writeIncomingPhoneNumbers(final Account account, final JsonObject object) {
        object.addProperty("incoming_phone_numbers", prefix(account) + "/IncomingPhoneNumbers.json");
    }

    private void writeNotifications(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("Notifications");
        writer.setValue(prefix(account) + "/Notifications");
        writer.endNode();
    }

    private void writeNotifications(final Account account, final JsonObject object) {
        object.addProperty("notifications", prefix(account) + "/Notifications.json");
    }

    private void writeOutgoingCallerIds(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("OutgoingCallerIds");
        writer.setValue(prefix(account) + "/OutgoingCallerIds");
        writer.endNode();
    }

    private void writeOutgoingCallerIds(final Account account, final JsonObject object) {
        object.addProperty("outgoing_caller_ids", prefix(account) + "/OutgoingCallerIds.json");
    }

    private void writeRecordings(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("Recordings");
        writer.setValue(prefix(account) + "/Recordings");
        writer.endNode();
    }

    private void writeRecordings(final Account account, final JsonObject object) {
        object.addProperty("recordings", prefix(account) + "/Recordings.json");
    }

    private void writeSandBox(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("Sandbox");
        writer.setValue(prefix(account) + "/Sandbox");
        writer.endNode();
    }

    private void writeSandBox(final Account account, final JsonObject object) {
        object.addProperty("sandbox", prefix(account) + "/Sandbox.json");
    }

    private void writeSmsMessages(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("SMSMessages");
        writer.setValue(prefix(account) + "/SMS/Messages");
        writer.endNode();
    }

    private void writeSmsMessages(final Account account, final JsonObject object) {
        object.addProperty("sms_messages", prefix(account) + "/SMS/Messages.json");
    }

    private void writeSubResourceUris(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("SubresourceUris");
        writeAvailablePhoneNumbers(account, writer);
        writeCalls(account, writer);
        writeConferences(account, writer);
        writeIncomingPhoneNumbers(account, writer);
        writeNotifications(account, writer);
        writeOutgoingCallerIds(account, writer);
        writeRecordings(account, writer);
        writeSandBox(account, writer);
        writeSmsMessages(account, writer);
        writeTranscriptions(account, writer);
        writer.endNode();
    }

    private void writeSubResourceUris(final Account account, final JsonObject object) {
        final JsonObject other = new JsonObject();
        writeAvailablePhoneNumbers(account, other);
        writeCalls(account, other);
        writeConferences(account, other);
        writeIncomingPhoneNumbers(account, other);
        writeNotifications(account, other);
        writeOutgoingCallerIds(account, other);
        writeRecordings(account, other);
        writeSandBox(account, other);
        writeSmsMessages(account, other);
        writeTranscriptions(account, other);
        object.add("subresource_uris", other);
    }

    private void writeTranscriptions(final Account account, final HierarchicalStreamWriter writer) {
        writer.startNode("Transcriptions");
        writer.setValue(prefix(account) + "/Transcriptions");
        writer.endNode();
    }

    private void writeTranscriptions(final Account account, final JsonObject object) {
        object.addProperty("transcriptions", prefix(account) + "/Transcriptions.json");
    }
}

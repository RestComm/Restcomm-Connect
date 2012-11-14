package org.mobicents.servlet.sip.restcomm.entities;

import java.net.URI;

import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.Immutable;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@Immutable
public final class Announcement {
	private Sid sid;
	private DateTime dateCreated;
	private Sid accountSid;
	private String gender;
	private String language;
	private String text;
	private URI uri;

	public Announcement(Sid sid, Sid accountSid, String gender, String language, String text, URI uri) {
		this.sid = sid;
		this.dateCreated = DateTime.now();
		this.accountSid = accountSid;
		this.gender = gender;
		this.language = language;
		this.text = text;
		this.uri = uri;
	}
	
	public Announcement(Sid sid, DateTime dateCreated, Sid accountSid, String gender, String language, String text, URI uri) {
		this.sid = sid;
		this.dateCreated = dateCreated;
		this.accountSid = accountSid;
		this.gender = gender;
		this.language = language;
		this.text = text;
		this.uri = uri;
	}
	
	public Sid getSid() {
		return sid;
	}

	public DateTime getDateCreated() {
		return dateCreated;
	}

	public Sid getAccountSid() {
		return accountSid;
	}

	public String getGender() {
		return gender;
	}

	public String getLanguage() {
		return language;
	}

	public String getText() {
		return text;
	}

	public URI getUri() {
		return uri;
	}
}

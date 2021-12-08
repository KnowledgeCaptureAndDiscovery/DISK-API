package org.diskproject.server.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.mail.Address;

public class Mail {
	private String subject;
	private String content;
	private Address[] replyTo;
	private String results;
	private Date timeUpdated;
	private String hypothesisId;

	public Mail(String subject, String content, Address[] replyTo) {
		this.subject = subject;
		this.content = content;
		this.replyTo = replyTo;
	}

	public Mail() {
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Address[] getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(Address[] replyTo) {
		this.replyTo = replyTo;
	}
	
	public void setResults(String results) {
		System.out.println("Results saved. "+ results);
		this.results = results;
	}
	
	public Date getTimeUpdated(){
		return timeUpdated;
	}
	
	public Date setTimeUpdated(Date timeUpdated){
		return timeUpdated;
	}
	
	public String getResults() {
		return results;
	}
	
	public String getHypothesisId(){
		return hypothesisId;
	}
	
	public void setHypothesisId(String hypothesisId){
		this.hypothesisId = hypothesisId;
	}

	public String toString() {
		return "Subject: " + getSubject() + " From: "
				+ Arrays.toString(getReplyTo()) + " Body: "+getContent();
	}
	
	public int hashCode(){
		return Arrays.toString(getReplyTo()).length();
	}

	@Override
	public boolean equals(Object o) {
		Mail m = (Mail) o;
		if (m.getSubject().equals(getSubject()))
			if (m.getContent().equals(getContent())) {
				List<String> m1 = new ArrayList<String>();
				List<String> m2 = new ArrayList<String>();
				for (Address a : getReplyTo())
					m1.add(a.toString());
				for (Address a : m.getReplyTo())
					m2.add(a.toString());
				Collections.sort(m1);
				Collections.sort(m2);
				return m1.equals(m2);
			}
		return false;
	}
}
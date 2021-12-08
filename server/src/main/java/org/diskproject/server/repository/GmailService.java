package org.diskproject.server.repository;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.diskproject.shared.classes.common.Graph;
import org.diskproject.shared.classes.common.TreeItem;
import org.diskproject.shared.classes.common.Triple;
import org.diskproject.shared.classes.common.TripleUtil;
import org.diskproject.shared.classes.hypothesis.Hypothesis;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.server.util.Config;
import org.diskproject.server.util.Mail;
import org.diskproject.server.util.gmail.OAuth2Authenticator;
import org.diskproject.shared.classes.util.GUID;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.sun.mail.imap.IMAPSSLStore;
import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

public class GmailService {

	private static String USER_NAME;
	static ScheduledExecutorService monitor;
	static MailMonitor mailThread;
	static Set<Mail> emails;
	static GmailService gmail;
	static boolean created = false;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/**
	 * When using this, https://myaccount.google.com/lesssecureapps?pli=1 set Allow
	 * less secure apps: ON Otherwise, it will not connect
	 * 
	 * If reading list of emails doesn't work, go to Gmail-->Settings-->Settings
	 * -->Forwarding and POP/IMAP-->IMAP Access--> Enable IMAP-->Save Changes
	 */
	public static void main(String[] args) {
		get();
	}

	public static GmailService get() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!created && gmail == null)
			gmail = new GmailService();

		return gmail;
	}

	public void shutdown() {
		if (mailThread != null)
			mailThread.stop();
		if (monitor != null)
			monitor.shutdownNow();
	}

	public void refreshTokens() {
		try {
			HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			GoogleTokenResponse response = new GoogleRefreshTokenRequest(httpTransport, JSON_FACTORY,
					getProperty("gmail.tokens.refresh"), getProperty("gmail.clientId"),
					getProperty("gmail.clientSecret")).execute();
			setProperty("gmail.tokens.access", response.getAccessToken());
			System.out.println("Access Token updated.");
		} catch (Exception e) {
			setProperty("gmail.code", "CODE_HERE");
			e.printStackTrace();
		}
	}

	public void promptCode() {
		String redirectUrl = "urn:ietf:wg:oauth:2.0:oob";
		String url = new GoogleAuthorizationCodeRequestUrl(getProperty("gmail.clientId"), redirectUrl,
				Collections.singleton("https://mail.google.com/")).setAccessType("offline").build();
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("If a url did not automatically open, please open the following in your browser:\n" + url);
		try {
			String start = getProperty("gmail.code");
			for (int i = 0; i < 4; i++) {
				Thread.sleep(20000);
				if (!getProperty("gmail.code").equals(start))
					break;
			}
			if (getProperty("gmail.code").equals(start)) {
				promptCode();
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		GoogleTokenResponse response;
		try {
			response = new GoogleAuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
					getProperty("gmail.clientId"), getProperty("gmail.clientSecret"), getProperty("gmail.code"),
					redirectUrl).execute();
			setProperty("gmail.tokens.refresh", response.getRefreshToken());
			setProperty("gmail.tokens.access", response.getAccessToken());
			System.out.println("Tokens added.");
		} catch (Exception e) {
		}
	}

	private GmailService() {
		USER_NAME = getProperty("gmail.username");
		created = true;
		emails = new HashSet<Mail>();
		monitor = Executors.newScheduledThreadPool(0);
		mailThread = new MailMonitor();
		OAuth2Authenticator.initialize();
	}

	public String getProperty(String property) {
		if (Config.get() == null)
			return "";
		PropertyListConfiguration props = Config.get().getProperties();
		return props.getString(property);
	}

	public void setProperty(String property, String value) {
		if (Config.get() == null)
			return;
		PropertyListConfiguration props = Config.get().getProperties();
		props.setProperty(property, value);
		try {
			props.save(props.getFileName());
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}

	public void saveReplies(String username) {
		try {
			OAuth2Authenticator.initialize();
			Properties props = new Properties();
			props.put("mail.imaps.sasl.enable", "true");
			props.put("mail.imaps.sasl.mechanisms", "XOAUTH2");
			props.put(org.diskproject.server.util.gmail.OAuth2SaslClientFactory.OAUTH_TOKEN_PROP,
					getProperty("gmail.tokens.access"));
			Session session = Session.getInstance(props);
			session.setDebug(false);

			final URLName unusedUrlName = null;
			IMAPSSLStore store = new IMAPSSLStore(session, unusedUrlName);
			final String emptyPassword = "";

			try {
				store.connect("imap.gmail.com", 993, USER_NAME, emptyPassword);
			} catch (AuthenticationFailedException e1) {
				refreshTokens();
				store.close();
				e1.printStackTrace();
				return;
			}
			Folder inbox = store.getFolder("[Gmail]/Sent Mail");
			inbox.open(Folder.READ_WRITE);

			// Fetch sent messages from inbox folder
			Message[] messages;
			messages = inbox.getMessages();
			for (Message message : messages) {
				try {
					if (message.getSubject().trim().toLowerCase().equals("re: hypothesis submission")) {
						saveReply(message);
					}
				} catch (Exception e) {
				}

			}

			inbox.close(true);
			store.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveReply(Message m) {
		try {
			String c;

			if (m.getContent() instanceof MimeMultipart)
				c = getTextFromMimeMultipart((MimeMultipart) m.getContent());
			else
				c = m.getContent().toString();

			if (c.startsWith("Revised Hypotheses: Results of the Hypothesis request")) {

				String[] emailAddresses = new String[m.getAllRecipients().length];
				for (int i = 0; i < emailAddresses.length; i++)
					emailAddresses[i] = ((InternetAddress) (m.getAllRecipients()[i])).getAddress();
				Arrays.sort(emailAddresses);

				for (Mail email : emails) {
					String[] emailAddresses2 = new String[email.getReplyTo().length];
					for (int i = 0; i < emailAddresses2.length; i++)
						emailAddresses2[i] = ((InternetAddress) (email.getReplyTo()[i])).getAddress();
					Arrays.sort(emailAddresses2);
					if (Arrays.equals(emailAddresses, emailAddresses2)) {
						if (email.getContent().equals(c.substring(c.indexOf("(") + 1, c.indexOf(")")))) {
							if (email.getResults() == null
									|| !email.getResults().equals(c.substring(c.indexOf("\n") + 3).trim()))
								email.setResults(c.substring(c.indexOf("\n") + 3).trim());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveMail(Message m, boolean read) throws Exception {
		try {

			Mail mail = toMail(m);
			if (!hypothesisExists(mail))
				addHypothesis(mail);
			if (!read) {
				sendEmail("Re: " + mail.getSubject(), mail.getReplyTo(),
						"Successfully found hypothesis " + (String) mail.getContent() + ".");
			}
			emails.add(mail);
			System.out.println("Added email: " + mail);
		} catch (Exception e) {
			e.printStackTrace();

			String c;
			if (m.getContent() instanceof MimeMultipart)
				c = getTextFromMimeMultipart((MimeMultipart) m.getContent()).replace("<", "").replace(">", "");
			else
				c = m.getContent().toString();

			sendEmail("Re: " + m.getSubject(), m.getReplyTo(),
					"Could not add " + c + ". Please check that the content if formatted accurately.");
			throw new Exception();
		}
	}

	public boolean hypothesisExists(Mail mail) {
		String username = getProperty("username");
		String domain = getProperty("domain");

		TripleUtil util = new TripleUtil();
		List<Triple> triples = new ArrayList<Triple>();

		String[] arr = mail.getContent().split("\n");

		for (String triple : arr) {
			Triple t = util.fromString(triple);
			t.setDetails(null);
			triples.add(t);
		}

		List<TreeItem> hypList = DiskRepository.get().listHypotheses(username, domain);
		for (TreeItem hypothesis : hypList) {
			boolean tripleExists = false;
			Hypothesis hyp = DiskRepository.get().getHypothesis(username, domain, hypothesis.getId());
			if (hyp.getGraph().getTriples().size() == mail.getContent().split("\n").length)
				for (Triple t : hyp.getGraph().getTriples()) {
					tripleExists = false;
					for (Triple temp : triples) {
						if (t.toString().equals(temp.toString())) {
							tripleExists = true;
							break;
						}
					}
					if (!tripleExists)
						break;
				}
			if (tripleExists) {
				mail.setHypothesisId(hypothesis.getId());
				return true;
			}
		}
		return false;

	}

	public Mail toMail(Message m) throws Exception {
		Mail mail = new Mail();
		mail.setSubject(m.getSubject());

		if (m.getContent() instanceof MimeMultipart)
			mail.setContent(getTextFromMimeMultipart((MimeMultipart) m.getContent()).trim());
		else
			mail.setContent(m.getContent().toString().trim());
		mail.setReplyTo(m.getReplyTo());
		return mail;
	}

	public void addHypothesis(Mail mail) throws Exception {
		// Add hypothesis
		Hypothesis hypothesis = new Hypothesis();

		// Set Id
		String id = GUID.randomId("Hypothesis");
		hypothesis.setId(id);
		mail.setHypothesisId(id);

		// Set Graph
		TripleUtil util = new TripleUtil();
		Graph newgraph = new Graph();
		List<Triple> triples = new ArrayList<Triple>();

		String[] arr = mail.getContent().split("\n");

		for (String triple : arr) {
			Triple t = util.fromString(triple);
			t.setDetails(null);
			triples.add(t);
		}

		newgraph.setTriples(triples);
		hypothesis.setGraph(newgraph);

		// Set Name
		String name = "";
		String content = mail.getContent().replace("<", "").replace(">", "").trim();
		arr = content.split("(#)|( )");
		for (int i = 0; i < arr.length; i++)
			if (i % 2 == 1)
				name += arr[i] + " ";
		hypothesis.setName(name.trim());

		// Set Description
		hypothesis.setDescription("Added by DISK Agent. First requested by: "
				+ Arrays.toString(mail.getReplyTo()).replace("[", "").replace("]", "") + ".");

		// Set Parent Id
		hypothesis.setParentId(null);

		// Add Hypothesis
		boolean saved = DiskRepository.get().addHypothesis(getProperty("username"), getProperty("domain"), hypothesis);

		if (saved) {
			addTriggeredLineOfInquiry(mail);
			sendEmail("Re: " + mail.getSubject(), mail.getReplyTo(),
					"Successfully added " + (String) mail.getContent() + ".");
		} else
			throw new Exception();
	}

	/**
	 * TODO: Add querying for vocabulary so client does not have to know namespaces
	 * TODO: Fix what to do if there is a connection issue. It is causing glitches
	 * at the moment.
	 */
	public void fetchMessages(String user, boolean read) {
		try {
			OAuth2Authenticator.initialize();
			Properties props = new Properties();
			props.put("mail.imaps.sasl.enable", "true");
			props.put("mail.imaps.sasl.mechanisms", "XOAUTH2");
			props.put(org.diskproject.server.util.gmail.OAuth2SaslClientFactory.OAUTH_TOKEN_PROP,
					getProperty("gmail.tokens.access"));
			Session session = Session.getInstance(props);
			session.setDebug(false);

			final URLName unusedUrlName = null;
			IMAPSSLStore store = new IMAPSSLStore(session, unusedUrlName);
			final String emptyPassword = "";

			try {
				store.connect("imap.gmail.com", 993, USER_NAME, emptyPassword);
			} catch (AuthenticationFailedException e1) {
				refreshTokens();
				store.close();
				e1.printStackTrace();
				return;
			}
			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_WRITE);

			// Fetch unseen messages from inbox folder
			Message[] messages;
			messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), read));
			for (Message message : messages) {
				// Delete messages that are not necessary
				try {
					message.setFlag(Flags.Flag.SEEN, true);
					if (message.getSubject().trim().toLowerCase().equals("hypothesis submission")) {
						saveMail(message, read);
					} else if (message.getSubject().trim().toLowerCase().equals("help")) {
						try {
							String path = getProperty("help_file");
							sendEmail("Re: Help", message.getReplyTo(),
									new String(Files.readAllBytes(Paths.get(path))));
							Folder other = store.getFolder("Help");
							Message[] m = { message };
							inbox.copyMessages(m, other);
							inbox.setFlags(m, new Flags(Flags.Flag.DELETED), true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else
						throw new Exception();
				} catch (Exception e) {
					Folder other = store.getFolder("Other");
					Message[] m = { message };
					inbox.copyMessages(m, other);
					inbox.setFlags(m, new Flags(Flags.Flag.DELETED), true);

				}

			}

			inbox.close(true);
			store.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addTriggeredLineOfInquiry(Mail mail) {
		List<TriggeredLOI> tloiList = DiskRepository.get().queryHypothesis(getProperty("username"),
				getProperty("domain"), mail.getHypothesisId());
		for (TriggeredLOI tloi : tloiList) {
			DiskRepository.get().addTriggeredLOI(getProperty("username"), getProperty("domain"), tloi);
		}
	}

	private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
		String result = "";
		int count = mimeMultipart.getCount();
		for (int i = 0; i < count; i++) {
			BodyPart bodyPart = mimeMultipart.getBodyPart(i);
			if (bodyPart.isMimeType("text/plain")) {
				if (i != 0)
					result = result + " " + bodyPart.getContent();
				else
					result = result + bodyPart.getContent();

				break; // without break same text appears twice
			} else if (bodyPart.isMimeType("text/html")) {
				String html = (String) bodyPart.getContent();
				if (i != 0)
					result = result + " " + org.jsoup.Jsoup.parse(html).text();
				else
					result = result + org.jsoup.Jsoup.parse(html).text();
			} else if (bodyPart.getContent() instanceof MimeMultipart) {
				result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
			}
		}
		result = result.replace("<", " ").replace(">", " ").trim();
		String[] ar = result.split("\\s+");
		String endResult = "";
		for (int i = 0; i < ar.length; i++) {
			if (i % 3 == 0 && i != 0)
				endResult += "\n";
			endResult += "<" + ar[i] + "> ";
		}
		return endResult.trim();
	}

	/**
	 * TODO: Add date metadata to hypothesis so that requester can get updates
	 * properly
	 */
	private void checkForNewResults() {
		String username = getProperty("username");
		String domain = getProperty("domain");

		TripleUtil util = new TripleUtil();
		for (Mail mail : emails) {
			try {
				String header = "Revised Hypotheses: Results of the Hypothesis request (" + mail.getContent() + ")\n";
				String results;

				if (mail.getHypothesisId() != null) {
					List<TreeItem> hypList = (ArrayList<TreeItem>) ((ArrayList<TreeItem>) DiskRepository.get()
							.listHypotheses(username, domain)).clone();
					Collections.reverse(hypList); // Reverse so that newer hypotheses come first
					for (TreeItem hypothesis : hypList) {
						results = "";
						try {
							Hypothesis hyp = DiskRepository.get().getHypothesis(username, domain, hypothesis.getId());
							if (hyp.getParentId() != null && hyp.getParentId().equals(mail.getHypothesisId())) {
								for (Triple t : hyp.getGraph().getTriples()) {
									results += "\n" + util.toString(t) + " with Confidence Value: "
											+ t.getDetails().getConfidenceValue();
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						results = results.trim();
						if ((mail.getResults() == null && results.length() > 1) || (mail.getResults() != null
								&& results.length() > 1 && !mail.getResults().equals(results))) {
							mail.setResults(results.trim());
							sendEmail("Re: " + mail.getSubject(), mail.getReplyTo(), header + "\n" + results);
							break;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void sendEmail(String subject, Address[] to, String body) {
		try {
			OAuth2Authenticator.initialize();
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.starttls.required", "true");
			props.put("mail.smtp.sasl.enable", "false");
			Session session = Session.getInstance(props);
			session.setDebug(false);
			String from = USER_NAME;

			final URLName unusedUrlName = null;
			SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
			// If the password is non-null, SMTP tries to do AUTH LOGIN.
			final String emptyPassword = null;
			transport.connect("smtp.gmail.com", 587, from, emptyPassword);
			byte[] response = String.format("user=%s\1auth=Bearer %s\1\1", from, getProperty("gmail.tokens.access"))
					.getBytes();
			response = BASE64EncoderStream.encode(response);

			try {
				transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);
			} catch (MessagingException e) {
				refreshTokens();
				sendEmail(subject, to, body);
				transport.close();
				return;
			}

			Message message = new MimeMessage(session);

			message.setFrom(new InternetAddress(from));

			message.setSubject(subject);
			message.setText(body);
			transport.sendMessage(message, to);
			transport.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class MailMonitor implements Runnable {

		boolean stop;
		int load;
		boolean settingCode;

		public MailMonitor() {
			monitor.scheduleWithFixedDelay(this, 20, 20, TimeUnit.SECONDS);
			stop = false;
			load = 0;
			settingCode = false;
		}

		public synchronized void run() {
			try {
				if (stop) {
					while (!Thread.currentThread().isInterrupted()) {
						Thread.currentThread().interrupt();
					}
				} else {
					if (!this.equals(mailThread)) {
						stop();
						return;
					}
					if (load != 0 && !settingCode) {
						if (getProperty("gmail.code").equals("CODE_HERE")) {
							settingCode = true;
							promptCode();
							settingCode = false;
						} else {
							if (emails.size() == 0) {
								fetchMessages(USER_NAME, true); // save read mail

								saveReplies(USER_NAME); // save replies from before so that client does not receive
														// repeat emails
							}
							fetchMessages(USER_NAME, false);
							checkForNewResults();
						}
					} else
						load++;
				}
			} catch (Exception e) {
				while (!Thread.interrupted()) {
					stop = true;
					Thread.currentThread().interrupt();
				}
			}
		}

		public void stop() {
			while (!Thread.interrupted()) {
				stop = true;
				Thread.currentThread().interrupt();
			}
		}
	}
}

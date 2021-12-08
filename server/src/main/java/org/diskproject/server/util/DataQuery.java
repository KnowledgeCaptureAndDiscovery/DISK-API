package org.diskproject.server.util;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataQuery {

	public static void main(String[] args) {
		queryFor("[[Category:Cohort_(E)]][[UsesGenotypePlatform_(E)::TestPurposes]]|?HasLocationOfDataCollection_(E)");
	}

	public static HttpClient apiLogin() {
		try {
			HttpClient httpclient = HttpClients.custom()
					.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
					.build();
			HttpPost httppost = new HttpPost("http://organicdatacuration.org/enigma_new/api.php?");
			// Request parameters and other properties.
			List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("action", "query"));
			params.add(new BasicNameValuePair("meta", "tokens"));
			params.add(new BasicNameValuePair("type", "login"));
			params.add(new BasicNameValuePair("format", "json"));

			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			// Execute and get the response.
			HttpResponse response = httpclient.execute(httppost);
			response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();

			Scanner sc = new Scanner(entity.getContent());
			String token = sc.useDelimiter("\\A").next();
			token = token.substring(token.indexOf("logintoken") + 13, token.lastIndexOf("\"") - 1);
			sc.close();
			// Finished getting token

			httppost = new HttpPost("http://organicdatacuration.org/enigma_new/api.php?");
			// Request parameters and other properties.
			params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("action", "login"));
			//FIXME: This not longer work. Must use a data-adapter!
			params.add(new BasicNameValuePair("lgname", Config.get().getProperties().getString("ENIGMA.username")));
			params.add(new BasicNameValuePair("lgpassword", Config.get().getProperties().getString("ENIGMA.password")));
			params.add(new BasicNameValuePair("lgtoken", token));
			params.add(new BasicNameValuePair("format", "json"));

			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			// Execute and get the response.
			response = httpclient.execute(httppost);
			entity = response.getEntity();

			sc = new Scanner(entity.getContent());
			sc.close();
			return httpclient;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String[] queryFor(String inputQuery) {

		try {
			HttpClient httpclient = apiLogin();
			String saveQuery = inputQuery;
			// Make query machine readable and initialize variables
			//int offset = 0;

			String MachineReadableQuery = "http://organicdatacuration.org/enigma_new/api.php?action=ask&format=json&query="
					+ inputQuery.replace(" ", "%20").replace("|", "%7C");
			// Open query result reader for datasets first
			HttpPost httppost = new HttpPost(MachineReadableQuery);

			// Execute and get the response.
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();

			Scanner sc = new Scanner(entity.getContent());
			String json = sc.useDelimiter("\\A").next();

			// Get all pertaining files
			JsonParser jsonParser = new JsonParser();
			JsonObject expobj = (JsonObject) jsonParser.parse(json);
			JsonObject dataobj = expobj.get("query").getAsJsonObject().get("results").getAsJsonObject();
			Set<Map.Entry<String, JsonElement>> entries = dataobj.entrySet();// will return members of your object
			Set<String> fileNames = new HashSet<String>();
			for (Map.Entry<String, JsonElement> entry : entries) {
				JsonObject queryobj = entry.getValue().getAsJsonObject().get("printouts").getAsJsonObject();
				Set<Map.Entry<String, JsonElement>> properties = queryobj.entrySet();// will return members of your
																						// object
				for (Map.Entry<String, JsonElement> property : properties) {
					JsonArray filesobj = property.getValue().getAsJsonArray();
					for (JsonElement files : filesobj) {
						try {
							fileNames.add(files.getAsString());
						} catch (Exception e) {
							fileNames.add(files.getAsJsonObject().get("fulltext").getAsString());
						}
					}
				}
			}

			String files = "";
			// Get files

			for (String file : fileNames) {
				String requestFileURL = "http://organicdatacuration.org/enigma_new/api.php?action=query"
						+ "&format=json&prop=imageinfo&iiprop=url&titles=" + URLEncoder.encode(file, "UTF-8");
				httppost = new HttpPost(requestFileURL);
				response = httpclient.execute(httppost);
				entity = response.getEntity();
				sc = new Scanner(entity.getContent());
				String requestResult = sc.useDelimiter("\\A").next();
				JsonObject result = jsonParser.parse(requestResult).getAsJsonObject().get("query").getAsJsonObject()
						.get("pages").getAsJsonObject();
				entries = result.entrySet();// will return members of your object

				for (Map.Entry<String, JsonElement> entry : entries) {
					JsonArray urls = entry.getValue().getAsJsonObject().get("imageinfo").getAsJsonArray();
					for (JsonElement urlList : urls) {
						String fileUrl = urlList.getAsJsonObject().get("url").getAsString();

						sc = new Scanner(new URL(fileUrl).openStream(), "UTF-8");
						String out = sc.useDelimiter("\\A").next();
						if (files.length() > 1)
							files = files + "\n\",\"\n";
						files += file.replace("/", "").replace("\\", "").replace(":", "").replace("*", "")
								.replace("?", "").replace("\"", "").replace("<", "").replace(">", "").replace("|", "")
								+ "\n\",\"\n" + out;
					}
				}
			}

			String[] output = new String[2];
			output[0] = saveQuery.replace("/", "").replace("\\", "").replace(":", "").replace("*", "").replace("?", "")
					.replace("\"", "").replace("<", "").replace(">", "").replace("|", ""); // Original query string is
																							// no longer used in code
			output[1] = files; // Change to just this string split by "\n\",\"\n" in the future (code uses this
								// for now)
			sc.close();

			return output;
		} catch (Exception e) {
			e.printStackTrace();
			return new String[] { "", "" };
		}
	}
	
	public static boolean wasUpdatedInLastDay(String query) {
		try {
			String inputQuery = query.substring(0, query.indexOf("]]") + 2) + query.substring(query.indexOf("]]") + 2)
					+ "|?Modification_date#-F[F_d,_y]|sort%3DModification date|order%3Ddesc";
			HttpClient httpclient = apiLogin();
			// Make query machine readable and initialize variables

			String MachineReadableQuery = "http://organicdatacuration.org/enigma_new/api.php?action=ask&format=json&query="
					+ inputQuery.replace(" ", "_").replace("|", "%7C");
			// Open query result reader for datasets first
			HttpPost httppost = new HttpPost(MachineReadableQuery);

			// Execute and get the response.
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();

			Scanner sc = new Scanner(entity.getContent());
			String json = sc.useDelimiter("\\A").next();

			JsonParser jsonParser = new JsonParser();
			JsonObject expobj = jsonParser.parse(json).getAsJsonObject().get("query").getAsJsonObject().get("results")
					.getAsJsonObject();
			JsonObject checkobj = null;
			for (Map.Entry<String, JsonElement> lastModified : expobj.entrySet()) { // 1 item only
				if (checkobj == null)
					checkobj = lastModified.getValue().getAsJsonObject();
			}
			String time = checkobj.get("printouts").getAsJsonObject().get("Modification date").getAsJsonArray().get(0)
					.getAsJsonObject().get("timestamp").getAsString();
			long timeStamp = Long.parseLong(time + "000");
			Calendar lastModified = Calendar.getInstance();
			lastModified.setTimeInMillis(timeStamp);

			Calendar modifiedBy = Calendar.getInstance();
			modifiedBy.add(Calendar.DATE, -1);
			sc.close();
			if (modifiedBy.compareTo(lastModified) <= 0) {
				return true;
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return false;
	}

	/**
	 * Below is a functioning but bad form of querying for files since it could
	 * easily become deprecated
	 */

//	public static String[] queryFor(String args) throws Exception {
//		WebClient webClient = WebClientSetup();
//		return queryMain("", args, webClient);
//	}
//
//	public static boolean wasUpdatedInLastDay(String query) {
//		try {
//
//			String inputQuery = query.substring(0, query.indexOf("]]") + 2) + "[[Modification date::+]]"
//					+ query.substring(query.indexOf("]]") + 2)
//					+ "|?Modification_date#-F[F_d,_y]/sort=Modification date/order=descending";
//			WebClient webClient = WebClientSetup();
//			// Make query machine readable and initialize variables
//			int offset = 0;
//
//			String MachineReadableQuery = "http://organicdatacuration.org/enigma_new/index.php/Special:Ask/"
//					+ toMachineReadableQuery(inputQuery, offset);
//			// Open query result reader for datasets first
//			System.out.println(MachineReadableQuery);
//			TextPage page3 = (TextPage) webClient.getPage(MachineReadableQuery);
//			// System.out.println(MachineReadableQuery);
//			// System.out.println(page3.getWebResponse().getContentAsString());
//			Scanner s = new Scanner(page3.getWebResponse().getContentAsString());
//			DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");
//			Calendar c = Calendar.getInstance();
//			c.add(Calendar.DATE, -1);
//			s.useDelimiter(Pattern.compile("(\\n)|,"));
//			Calendar c2;
//			s.nextLine();
//			while (s.hasNextLine()) {
//				s.next();
//				if (s.next() != null) {
//					c2 = Calendar.getInstance();
//					c2.setTime(dateFormat.parse(s.next().replace("\"", "").replace("\'", "")));
//					if (c.compareTo(c2) <= 0)
//						return true;
//					break;
//				}
//
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return false;
//	}
//
//	public static WebClient WebClientSetup() throws Exception {
//	BrowserVersion browserVersion = new BrowserVersion.BrowserVersionBuilder(
//			BrowserVersion.FIREFOX_52)
//			.setApplicationName("Firefox")
//			.setApplicationVersion(
//					"5.0 (Windows NT 10.0; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0")
//			.setUserAgent(
//					"Mozilla/5.0 (Windows NT 10.0; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0")
//			.build();
//
//	WebClient webClient = new WebClient(browserVersion);
//	HtmlPage page1 = null;
//	// Get the first page
//	for (int i = 0; i < 3; i++)
//		try {
//			page1 = webClient
//					.getPage("http://organicdatacuration.org/enigma_new/index.php?title=Special:UserLogin&wpRemember=1&returnto=Regina+Wang&returntoquery=");
//			break;
//		} catch (Exception e) {
//		}
//	if(page1 == null)
//		page1 = webClient
//		.getPage("http://organicdatacuration.org/enigma_new/index.php?title=Special:UserLogin&wpRemember=1&returnto=Regina+Wang&returntoquery=");
//
//	// Get the form that we are dealing with and within that form,
//	// find the submit button and the field that we want to change.
//	HtmlForm form = page1.getFormByName("userlogin");
//	HtmlTextInput textField = form.getInputByName("wpName");
//
//	// Change the value of the text field
//	textField.type(Config.get().getProperties().getString("ENIGMA.username"));
//	HtmlPasswordInput textField2 = (HtmlPasswordInput) page1
//			.getElementById("wpPassword1");
//	textField2.type(Config.get().getProperties().getString("ENIGMA.password"));
//	HtmlButton button = (HtmlButton) page1.getElementById("wpLoginAttempt");
//
//	// Now submit the form by clicking the button and get back the
//	// second page.
//	HtmlPage page2 = button.click();
//
//	return webClient;
//}
//
//	public static String[] queryMain(String str, String inputQuery, WebClient webClient) throws Exception {
//		String saveQuery = inputQuery;
//		// Make query machine readable and initialize variables
//		int offset = 0;
//
//		String MachineReadableQuery = "http://organicdatacuration.org/enigma_new/index.php/Special:Ask/"
//				+ toMachineReadableQuery(inputQuery, offset);
//		// Open query result reader for datasets first
//
//		TextPage page3 = (TextPage) webClient.getPage(MachineReadableQuery);
//		// System.out.println(MachineReadableQuery);
//		// System.out.println(page3.getWebResponse().getContentAsString());
//		Scanner s = new Scanner(page3.getWebResponse().getContentAsString());
//		s.useDelimiter(",");
//
//		// Set up reading through pages (first line is irrelevant)
//		// (500 datasets per query max)
//		s.nextLine();
//		String SingleResult;
//		try {
//			while ((SingleResult = s.nextLine()) != null) {
//				// Read through query results
//				try {
//					// System.out.println(SingleResult);
//					while (SingleResult != null) {
//						if (SingleResult.indexOf(",") != -1 && SingleResult.indexOf(",") != SingleResult.length() - 1) {
//							SingleResult = SingleResult.substring(SingleResult.indexOf(",") + 1);
//							String data = pageNameToFileInformation(SingleResult, webClient);
//							// Open new file in zip and save text into entry
//							str += data + "\n\",\"\n";
//						}
//						SingleResult = s.nextLine();
//					}
//				} catch (Exception e) {
//				}
//				offset += 10;
//				MachineReadableQuery = "http://organicdatacuration.org/enigma_new/index.php/Special:Ask/"
//						+ toMachineReadableQuery(inputQuery, offset);
//
//				// Get query results for datasets again\
//				// System.out.println(MachineReadableQuery);
//				page3 = (TextPage) webClient.getPage(MachineReadableQuery);
//				s = new Scanner(page3.getWebResponse().getContentAsString());
//				s.nextLine();
//			}
//		} catch (Exception e) {
//		}
//		String[] output = new String[2];
//		try {
//			str = str.substring(0, str.length() - 3);
//		} catch (Exception e) {
//		}
//		saveQuery = saveQuery.replace("/", "").replace("\\", "").replace(":", "").replace("*", "").replace("?", "")
//				.replace("\"", "").replace("<", "").replace(">", "").replace("|", "");
//		output[0] = saveQuery;
//		output[1] = str;
//		s.close();
//		return output;
//		// Clean up
//	}
//
//	/**
//	 * @param original = query provided
//	 * @return link to the query in a csv format
//	 */
//	public static String toMachineReadableQuery(String original, int offset) {
//		try {
//			String query = original.replace("|", "/");
//			query = URLEncoder.encode(query, "UTF-8");
//			query = query.replace("%", "-").replace("-2F", "/");
//			// Make query machine readable and initialize variables
//
//			String MachineReadableQuery = "" + query + "/offset%3D" + offset + "/limit%3D-2010/format=%20csv";
//			return MachineReadableQuery;
//		} catch (Exception e) {
//		}
//		return "";
//	}
//
//	public static String pageNameToFileInformation(String SingleResult, WebClient webClient) {
//		try {
//			// Find dataset link and download file
//			HtmlPage page3 = webClient.getPage("http://organicdatacuration.org/enigma_new/index.php/" + SingleResult);
//			Scanner s = new Scanner(page3.getWebResponse().getContentAsString());
//			String file;
//			while ((file = s.nextLine()).indexOf("<li><a href=\"#filelinks\">") == -1) {
//			}
//			s.close();
//			file = file.substring(file.indexOf("a href=") + 7);
//			file = file.substring(file.indexOf("a href=") + 8);
//			file = file.substring(0, file.indexOf("\""));
//			TextPage pageTxt = (TextPage) webClient.getPage("http://organicdatacuration.org" + file);
//
//			return SingleResult.replace("/", "").replace("\\", "").replace(":", "").replace("*", "").replace("?", "")
//					.replace("\"", "").replace("<", "").replace(">", "").replace("|", "") + "\n\",\"\n"
//					+ pageTxt.getWebResponse().getContentAsString();
//		} catch (Exception e) {
//		}
//		return "";
//	}

}

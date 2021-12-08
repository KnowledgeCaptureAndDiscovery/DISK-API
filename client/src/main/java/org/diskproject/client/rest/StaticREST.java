package org.diskproject.client.rest;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

public class StaticREST {
	public static void getJSObject(String url, final Callback<JavaScriptObject, Throwable> callback) {
		RequestBuilder builder =  new RequestBuilder(RequestBuilder.GET, url);
		//builder.setHeader("Access-Control-Allow-Origin", "*");
		try {
			builder.sendRequest(null, new RequestCallback() {
				@Override
				public void onResponseReceived(Request request, Response response) {
					if (response.getStatusCode() == 200) {
						JavaScriptObject json = JsonUtils.safeEval(response.getText());
						callback.onSuccess(json);
					} else {
						GWT.log("Status code error:" + response.getStatusCode());
					}
				}
				@Override
				public void onError(Request request, Throwable exception) {
					GWT.log("error2");
					// TODO Auto-generated method stub
				}
			});
		} catch (Exception e) {
			GWT.log("some error");
			// TODO: handle exception
		}
	}

	public static void getAsString(String url, final Callback<String, Throwable> callback) {
		RequestBuilder builder =  new RequestBuilder(RequestBuilder.GET, url);
		try {
			builder.sendRequest(null, new RequestCallback() {
				@Override
				public void onResponseReceived(Request request, Response response) {
					if (response.getStatusCode() == 200) {
						callback.onSuccess(response.getText());
					} else {
						GWT.log("error3");
					}
				}
				@Override
				public void onError(Request request, Throwable exception) {
					GWT.log("error2");
					// TODO Auto-generated method stub
				}
			});
		} catch (Exception e) {
			GWT.log("some error");
			// TODO: handle exception
		}
	}
}

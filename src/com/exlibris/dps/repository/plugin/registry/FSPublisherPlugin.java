package com.exlibris.dps.repository.plugin.registry;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.exlibris.core.infra.common.exceptions.logging.ExLogger;
import com.exlibris.dps.sdk.registry.PublisherRegistryPlugin;


/**
 * @author Motip
 * Publisher to FigShare
 */


public class FSPublisherPlugin implements PublisherRegistryPlugin{

	// Publisher initial parameters
	private Map<String, String> initParams = new HashMap<String, String>();
	
	private static final String BASE_URL = "baseUrl";
	private static final String TOKEN = "token";
	private static final String NAME = "name";
	private static final String PUT = "PUT";
	private static final String DELETE = "DELETE";
	private static final String VERSIONS = "/versions/";
	private static final int OK_CODE = 205;
	private static final int DELETE_CODE = 204;
	private static final String SEPARATOR="/";

	private static final String SUPPLEMENTARY = "/supplementary_fields";

	private static final String UNPUBLISH_IE = "unpublish_ie";

	private static final String MESSAGE = "message";
	private String version;
	private String articalId;
	private String pid;
	// ExLibris Logger
	private static ExLogger log = ExLogger.getExLogger(FSPublisherPlugin.class);
	
	@Override
	public boolean publish(String pid, String convertedIE) {

		String[] parts = convertedIE.split(SEPARATOR);
		this.articalId = parts[0]; 
		this.version = parts[1];
		this.pid = pid;
		if(createFigShareRequest(PUT,getUrl(),getPayload(true))){
			return true;
		}
		return false;	
	}

	@Override
	public void initParam(Map<String, String> params) {
		// copy all init params to local variable
		initParams.putAll(params);
	}

	@Override
	public boolean unpublish(String pid) {
		String[] parts = initParams.get(UNPUBLISH_IE).split(SEPARATOR);
		this.articalId = parts[0]; 
		this.version = parts[1];
		String deleteUrl = getUrl() + SUPPLEMENTARY ;
		if(createFigShareRequest(DELETE,deleteUrl,getPayload(false))){
			return true;
		}
		return false;	
	}
	
	/**
	 * createFigShareRequest - Send the request to FigShare using rest API .
	 * @param method - POST/DELETE
	 * @param url - url to FigShare with specific params
	 * @param payload
	 */
	private boolean createFigShareRequest(String method ,String url,String payload){
		URL obj = null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e2) {
            log.error(e2);
        } 
        try{
        	HttpURLConnection  con = (HttpURLConnection ) obj.openConnection();
	        
	        con.setRequestProperty("Content-Type", "application/json");
	        con.setDoOutput(true);
	        con.setRequestProperty("Authorization","token "+initParams.get(TOKEN));
	        con.setRequestMethod(method);
	        //set the payload for put request
	    	DataOutputStream wr = new DataOutputStream(con.getOutputStream());
	   	 	wr.writeBytes(payload);
	        wr.flush();
	        wr.close();
	        con.connect();
	        return checkResponse(con);
	        
        }catch(IOException e){
        	log.error(e.getMessage());
        	return false;
        }
	}

	/**
	 * checkResponse -Check the response code.
	 * @return - Return the response status(success/failed).
	 */
	private boolean checkResponse(HttpURLConnection con) {
      
		InputStream is = null;
    	BufferedReader reader = null;
    	String xmlResponse = "";
    	try {
    		xmlResponse = returnResponseContent(con,is,reader);
		    if(!xmlResponse.isEmpty()){
		    	if(con.getResponseCode() != OK_CODE || con.getResponseCode() != DELETE_CODE){
		    		JSONObject json = new JSONObject(xmlResponse);
		    		log.error(con.getResponseCode() + " -  " + json.get(MESSAGE));  
		    		return false;
		    	}
		    }
		    return true;
		} catch (IOException | JSONException e) {
			log.error(e);
			return false;
		} 
	}
	
	/**
	 * returnRequestContent - return the response content
	 * @return - return the response content
	 */
	private String returnResponseContent(HttpURLConnection con, InputStream is, BufferedReader reader) throws IOException{
		try{
		String xmlResponse = "";
		if(con.getResponseCode() == OK_CODE || con.getResponseCode() == DELETE_CODE){
	        is = con.getInputStream();
	    }else{
	        is = con.getErrorStream();
	    }
		//get the response message
		reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String line = "";
	    while((line = reader.readLine()) != null) {
	    	xmlResponse += line;
        }	
		return xmlResponse;
		}catch (IOException e) {
			log.error(e);
			throw new IOException(e);
		}finally{
			reader.close();
			is.close();
		}
	}
	
	/**
	 * getPayload - Returns the payload value.
	 * @param publishFlag - publish/unpublish - different payload.
	 * @return - return the response content
	 */	
	private String getPayload(boolean publishFlag) {
		
		JSONObject json = new JSONObject();
		JSONObject subJson = new JSONObject(); 
		try {
			if(publishFlag){
				subJson.put("name", (initParams.get(NAME)));
				subJson.put("value",(pid ));
				json.append("supplementary_fields", subJson);
			}else{
				json.put("field", (initParams.get(NAME)));
			}
			
		} catch (JSONException e) {
			log.error("Failed to create the pyaload");
		}			
		return json.toString();
	}

	private String getUrl() {
		//Build the url with the current article and version.
		return initParams.get(BASE_URL)+articalId+VERSIONS+version;
	}

}

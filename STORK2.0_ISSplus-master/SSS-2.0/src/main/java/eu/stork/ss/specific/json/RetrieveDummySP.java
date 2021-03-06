package eu.stork.ss.specific.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import eu.stork.peps.auth.commons.IPersonalAttributeList;
import eu.stork.peps.auth.commons.PEPSValues;
import eu.stork.peps.auth.commons.PersonalAttribute;
import eu.stork.peps.auth.commons.PersonalAttributeList;
import eu.stork.ss.Constants;
import eu.stork.ss.SPUtil;
import eu.stork.ss.RetrievePersonalAttributeList;

public class RetrieveDummySP extends RetrievePersonalAttributeList {
	/**
	 * Unique identifier.
	 */
	private static final long serialVersionUID = -5984353371825874179L;

	//Logger
	static final Logger logger = LoggerFactory.getLogger(RetrieveDummySP.class.getName());

	@Override
	//protected IPersonalAttributeList retrievePersonalAttributeList(String token) {
	public IPersonalAttributeList retrievePersonalAttributeList(String token,String sp) {//ptk
		IPersonalAttributeList pal;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String serviceUrl = "http://localhost/stork2-attributes.php?t=";

		logger.debug("Trying to retrieve the PAL with JSON.");
		
		pal = null;
		try {
			//Load the configuration (URL)
			try {
				Properties configs = SPUtil.loadConfigs(Constants.SP_PROPERTIES);
				
				//serviceUrl = configs.getProperty(Constants.DS_URL);
				serviceUrl = configs.getProperty(sp+Constants.DS_URL);//ptk
			} catch (IOException e) {
			}

			logger.debug("The URL: [" + serviceUrl + "]");
			System.out.println("The URL: [" + serviceUrl + "]");
			
			HttpGet httpGet = new HttpGet(serviceUrl + token);
			System.out.println("The URL: [" + serviceUrl + token + "]");
			CloseableHttpResponse response = httpclient.execute(httpGet);
			
			try {
				String httpJson;
				Gson gson = new Gson();
				RetrieveAttributes outcome;
			    
				HttpEntity entity = response.getEntity();
			    httpJson = EntityUtils.toString(entity);
			    
				outcome = gson.fromJson(httpJson, RetrieveAttributes.class);

				logger.trace("JSON output: [" + outcome + "]");
				System.out.println("JSON output: [" + outcome + "]");
				System.out.println("JSON status: "+outcome.getStatus());
				
				if ( outcome.getStatus().equals("OK") ) {
					PersonalAttribute pa;
					HashMap<String, Attribute> list = outcome.getList();

					pal = new PersonalAttributeList();

					for (Map.Entry<String, Attribute> entry : list.entrySet()) {
					    String key = entry.getKey();
					    Attribute value = entry.getValue();

					    pa = new PersonalAttribute();
					    //Set name
					    pa.setName(key);
					    //Set is required
					    if ( value.getRequired().equals("1") )
					    	pa.setIsRequired(true);
					    else
					    	pa.setIsRequired(false);

					    if ( value.getValue()!=null ) {
					        final String[] vals =
					                value.getValue().split(PEPSValues.ATTRIBUTE_VALUE_SEP.toString());

					        //Set complex or plain values
						    if ( value.getComplex().equals("1") )
						    	pa.setComplexValue(createComplexValue(vals));
						    else
						    	pa.setValue(createValues(vals));
					    }

						pal.add(pa);
					}
					
					logger.debug("The constructed PAL: " + pal.toString());
					System.out.println("The constructed PAL: " + pal.toString());
				}
			} catch(Exception e) {
				//All other Exceptions
				logger.error("Exception: [" + e + "]");
				e.printStackTrace();
			} finally {
			    response.close();
				httpclient.close();
			}
		} catch(IOException ex) {
			//Exception in CloseableHttpResponse
			logger.error("IOException: [" + ex + "]");
			ex.printStackTrace();
		}

		return pal;
	}

	/**
	 * Validates and creates the attribute values.
	 * 
	 * @param vals The attribute values.
	 * 
	 * @return The {@link List} with the attribute values.
	 * 
	 * @see List
	 */
	private List<String> createValues(final String[] vals) {
		final List<String> values = new ArrayList<String>();
		for (final String val : vals) {
			if (StringUtils.isNotEmpty(val)) {
				values.add(val);
			}
		}
		return values;
	}

	/**
	 * Validates and creates the attribute's complex values.
	 * 
	 * @param values The complex values.
	 * 
	 * @return The {@link Map} with the complex values.
	 * 
	 * @see Map
	 */
	private HashMap<String, String> createComplexValue(final String[] values) {
		final HashMap<String, String> complexValue = new HashMap<String, String>();
		for (final String val : values) {
			final String[] tVal = val.split("=");
			if (StringUtils.isNotEmpty(val) && tVal.length == 2) {
				complexValue.put(tVal[0], tVal[1]);
			}
		}
		return complexValue;
	}
}
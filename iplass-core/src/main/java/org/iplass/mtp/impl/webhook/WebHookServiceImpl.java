package org.iplass.mtp.impl.webhook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.iplass.mtp.ManagerLocator;
import org.iplass.mtp.definition.TypedDefinitionManager;
import org.iplass.mtp.impl.definition.AbstractTypedMetaDataService;
import org.iplass.mtp.impl.definition.DefinitionMetaDataTypeMap;
import org.iplass.mtp.impl.http.HttpClientConfig;
import org.iplass.mtp.impl.mail.template.MetaMailTemplate;
import org.iplass.mtp.impl.mail.template.MetaMailTemplate.MailTemplateRuntime;
import org.iplass.mtp.impl.metadata.MetaDataEntryInfo;
import org.iplass.mtp.impl.webhook.template.MetaWebHookTemplate;
import org.iplass.mtp.impl.webhook.template.MetaWebHookTemplate.WebHookTemplateRuntime;
import org.iplass.mtp.mail.SendMailListener;
import org.iplass.mtp.mail.template.definition.MailTemplateDefinition;
import org.iplass.mtp.mail.template.definition.MailTemplateDefinitionManager;
import org.iplass.mtp.spi.Config;
import org.iplass.mtp.tenant.Tenant;
import org.iplass.mtp.webhook.WebHook;
import org.iplass.mtp.webhook.template.definition.WebHookSubscriber;
import org.iplass.mtp.webhook.template.definition.WebHookTemplateDefinition;
import org.iplass.mtp.webhook.template.definition.WebHookTemplateDefinitionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebHookServiceImpl extends AbstractTypedMetaDataService<MetaWebHookTemplate, WebHookTemplateRuntime> implements WebHookService {
	private HttpClientConfig httpClientConfig;
	
	public WebHookServiceImpl() {
	}
	
	public static class TypeMap extends DefinitionMetaDataTypeMap<WebHookTemplateDefinition, MetaWebHookTemplate> {
		public TypeMap() {
			super(getFixedPath(), MetaWebHookTemplate.class, WebHookTemplateDefinition.class);
		}
		@Override
		public TypedDefinitionManager<WebHookTemplateDefinition> typedDefinitionManager() {
			return ManagerLocator.getInstance().getManager(WebHookTemplateDefinitionManager.class);
		}
	}
	

	private static Logger logger = LoggerFactory.getLogger(WebHookServiceImpl.class);
	public static final String WEBHOOK_TEMPLATE_META_PATH = "/webhook/template/";
	
	
	// TODO 
	private Map<String, Object> sendProperties;
	
	
	public static String getFixedPath() {
		return WEBHOOK_TEMPLATE_META_PATH;
	}



	@Override
	public Class<MetaWebHookTemplate> getMetaDataType() {
		return MetaWebHookTemplate.class;
	}

	@Override
	public Class<WebHookTemplateRuntime> getRuntimeType() {
		return WebHookTemplateRuntime.class;
	}



	@Override
	public void init(Config config) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void destroy() {		
	}



	@Override
	public WebHook createWebHook(Tenant tenant, String charset) {
		return new WebHook();
	}



	@Override
	public void sendWebHook(Tenant tenant, WebHook webHook) {
		try {
			CloseableHttpClient httpClient = HttpClients.createDefault();
			try {
				ArrayList<WebHookSubscriber> receivers = new ArrayList<WebHookSubscriber>(webHook.getSubscribers());
				if (receivers == null || receivers.isEmpty()) {
					logger.warn("Tried to send WebHook with out a valid address url.");
					return;
				}
				
				
				Exception ex = null;
				int retryCount = webHook.getRetryLimit();
				int retryInterval = webHook.getRetryInterval();
				for (int i = 0; i <= retryCount; i++) {
					if (retryInterval > 0) {
						Thread.sleep(retryInterval);
					}
					for (int j = 0; j < receivers.size();j++) {
						HttpPost httpPost = new HttpPost(receivers.get(j).getUrl().toURI());
						CloseableHttpResponse response = httpClient.execute(httpPost);
						StatusLine statusLine= response.getStatusLine();
						if (statusLine.getStatusCode() == HttpStatus.SC_OK) {//普通に成功
							receivers.remove(j);
						}
					}
					
				}
				
				
				
			} finally {
				httpClient.close();
			}
		} catch (Exception e) {
			//handleException(WebHook, e);
		} 
		
	}



}

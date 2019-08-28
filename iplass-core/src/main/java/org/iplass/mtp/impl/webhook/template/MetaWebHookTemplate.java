package org.iplass.mtp.impl.webhook.template;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import org.iplass.mtp.impl.core.ExecuteContext;
import org.iplass.mtp.impl.definition.DefinableMetaData;
import org.iplass.mtp.impl.mail.MailService;
import org.iplass.mtp.impl.mail.template.MetaMailTemplate.MailTemplateRuntime;
import org.iplass.mtp.impl.metadata.BaseMetaDataRuntime;
import org.iplass.mtp.impl.metadata.BaseRootMetaData;
import org.iplass.mtp.impl.metadata.MetaData;
import org.iplass.mtp.impl.metadata.MetaDataConfig;
import org.iplass.mtp.impl.metadata.MetaDataRuntime;
import org.iplass.mtp.impl.metadata.RootMetaData;
import org.iplass.mtp.impl.util.ObjectUtil;
import org.iplass.mtp.impl.webhook.WebHookService;
import org.iplass.mtp.spi.ServiceRegistry;
import org.iplass.mtp.webhook.WebHook;
import org.iplass.mtp.webhook.template.definition.WebHookContent;
import org.iplass.mtp.webhook.template.definition.WebHookSubscriber;
import org.iplass.mtp.webhook.template.definition.WebHookTemplateDefinition;

@XmlRootElement
public class MetaWebHookTemplate extends BaseRootMetaData implements DefinableMetaData<WebHookTemplateDefinition> {

	private static final long serialVersionUID = 6383360434482999137L;
	
	/**
	//BaseRootMetaData にいる内容
	private String name;
	private String displayName;
	private String description;
	*/
	
	private WebHookContent contentBody;
	private String sender;
	private String addressUrl;
	
	/** サブスクライバー：このwebhookを要求した方達 */
	private ArrayList<WebHookSubscriber> subscribers;
	
	/**　リトライ関連　*/
	/** 失敗したらやり直ししますか */
	private boolean retry;
	/** やり直しの最大回数 */
	private int retryLimit;
	/** やり直す度の待ち時間(ms)*/
	private int retryInterval;

	@Override
	public WebHookTemplateRuntime createRuntime(MetaDataConfig metaDataConfig) {
		return new WebHookTemplateRuntime();
	}

	@Override
	public MetaWebHookTemplate copy() {
		return ObjectUtil.deepCopy(this);
	}

	//Definition → Meta
	@Override
	public void applyConfig(WebHookTemplateDefinition definition) {
		name = definition.getName();
		displayName = definition.getDisplayName();
		description = definition.getDescription();
		
		contentBody = definition.getContentBody();
		addressUrl = definition.getAddressUrl();
		sender = definition.getSender();

		subscribers = definition.getSubscribers();

		retry = definition.isRetry();
		retryInterval = definition.getRetryInterval();
		retryLimit = definition.getRetryLimit();
		
	}

	//Meta → Definition
	@Override
	public WebHookTemplateDefinition currentConfig() {
		WebHookTemplateDefinition definition = new WebHookTemplateDefinition();
		definition.setName(name);
		definition.setDisplayName(displayName);
		definition.setDescription(description);
		
		definition.setContentBody(contentBody);
		definition.setAddressUrl(addressUrl);
		definition.setSender(sender);

		definition.setSubscribers(subscribers);

		definition.setRetry(retry);
		definition.setRetryInterval(retryInterval);
		definition.setRetryLimit(retryLimit);

		return definition;
	}

	public class WebHookTemplateRuntime extends BaseMetaDataRuntime {

		@Override
		public MetaWebHookTemplate getMetaData() {
			return MetaWebHookTemplate.this;
		}
		
		public WebHook createWebHook() {
			checkState();
			String _charset = contentBody.getCharset();
			WebHookService ws = ServiceRegistry.getRegistry().getService(WebHookService.class);
			ExecuteContext ex = ExecuteContext.getCurrentContext();
			WebHook webHook = ws.createWebHook(ex.getCurrentTenant(), _charset);
			
			//fill up the info to webhook
			webHook.setContent(contentBody);
			webHook.setName(name);
			webHook.setSubscribers(subscribers);//should throw exception if there is no subscribers registered
			webHook.setRetry(retry);
			webHook.setRetryInterval(retryInterval);
			webHook.setRetryLimit(retryLimit);
			
			return webHook;
		}
		
	} 
	
	
	public WebHookContent getContentBody() {
		return contentBody;
	}

	public void setContentBody(WebHookContent contentBody) {
		this.contentBody = contentBody;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getAddressUrl() {
		return addressUrl;
	}

	public void setAddressUrl(String addressUrl) {
		this.addressUrl = addressUrl;
	}

	public ArrayList<WebHookSubscriber> getSubscribers() {
		return subscribers;
	}

	public void setSubscribers(ArrayList<WebHookSubscriber> subscribers) {
		this.subscribers = subscribers;
	}

	public boolean isRetry() {
		return retry;
	}

	public void setRetry(boolean retry) {
		this.retry = retry;
	}

	public int getRetryLimit() {
		return retryLimit;
	}

	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}


}

package org.iplass.mtp.webhook.template.definition;

import java.io.Serializable;
import java.net.URL;

public class WebHookSubscriber implements Serializable{

	private static final long serialVersionUID = 921157612085724142L;

	/** 送り先 */
	private URL url;
	
	/** 申し込んだ方の名前 */
	private String subscriberName;
	
	/**　申し込んだ方のパスワード */
	private String subscriberPassword;
	
	//何らかしらの認証用物、他の設置なと
	//今後要求されたら改変
	
	public WebHookSubscriber() {
	}
	
	public WebHookSubscriber(URL url, String subscriberName, String subscriberPassword) {
		this.url = url;
		this.subscriberName = subscriberName;
		this.subscriberPassword = subscriberPassword;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public String getSubscriberName() {
		return subscriberName;
	}

	public void setSubscriberName(String subscriberName) {
		this.subscriberName = subscriberName;
	}

	public String getSubscriberPassword() {
		return subscriberPassword;
	}

	public void setSubscriberPassword(String subscriberPassword) {
		this.subscriberPassword = subscriberPassword;
	}

}

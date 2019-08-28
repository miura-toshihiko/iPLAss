/**
 * WebHookを表すクラス、WebHookの送り情報などを記録する
 */
package org.iplass.mtp.webhook;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.iplass.mtp.webhook.template.definition.WebHookContent;
import org.iplass.mtp.webhook.template.definition.WebHookSubscriber;
/**
 * @author lisf06
 */
public class WebHook {
	/** このwebhookの名前　*/
	private String name;

	/** 発信者の名前 */
	private String senderName;
	
	/** 記録用のid */
	private int webHookId;
	
	/** 送る内容 */
	private WebHookContent content;
	
	private ArrayList<WebHookSubscriber> subscribers;
	
	
	/** SSL token　、未実装、 */
	//private String SSL;
	
	/** 失敗したらやり直ししますか */
	private boolean retry;
	
	/** やり直しの最大回数 */
	private int retryLimit;
	
	/** やり直す度の待ち時間(ms)*/
	private int retryInterval;
	
	/** headers */
	private HashMap<String, String> headers;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public int getWebHookId() {
		return webHookId;
	}

	public void setWebHookId(int webHookId) {
		this.webHookId = webHookId;
	}

	public WebHookContent getContent() {
		return content;
	}

	public void setContent(WebHookContent content) {
		this.content = content;
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
	
	public HashMap<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(HashMap<String, String> headers) {
		this.headers = headers;
	}
	
	public ArrayList<WebHookSubscriber> getSubscribers() {
		return subscribers;
	}

	public void setSubscribers(ArrayList<WebHookSubscriber> subscriber) {
		this.subscribers = subscriber;
	}
	
	public void addSubscriber(WebHookSubscriber subscriber) {
		this.subscribers.add(subscriber);
	}
}

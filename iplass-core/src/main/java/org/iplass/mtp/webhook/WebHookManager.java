
package org.iplass.mtp.webhook;

import java.util.Map;

import org.iplass.mtp.Manager;

/**
 * @author lisf06
 *
 */
public interface WebHookManager extends Manager {
	/** 送り用のwebhookオブジェを作る　*/
	public WebHook createWebHook();
	
	/** テンプレから送り用のwebhookオブジェを作る、その後は発信のみ、の感じ */
	public WebHook createWebHook(String tmplDefName, Map<String, Object> bindings);
	
	/** 送る */
	public void sendWebHook(WebHook webHookInstance); 
}

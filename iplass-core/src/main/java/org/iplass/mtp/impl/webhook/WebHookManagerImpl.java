package org.iplass.mtp.impl.webhook;

import java.util.Map;

import org.iplass.mtp.SystemException;
import org.iplass.mtp.impl.core.ExecuteContext;
import org.iplass.mtp.impl.webhook.template.MetaWebHookTemplate.WebHookTemplateRuntime;
import org.iplass.mtp.spi.ServiceRegistry;
import org.iplass.mtp.tenant.Tenant;
import org.iplass.mtp.webhook.WebHook;
import org.iplass.mtp.webhook.WebHookManager;

public class WebHookManagerImpl implements WebHookManager {

	private WebHookService webHookService = ServiceRegistry.getRegistry().getService(WebHookService.class);
	public WebHookManagerImpl() {
	}

	@Override
	public WebHook createWebHook() {
		return webHookService.createWebHook(ExecuteContext.getCurrentContext().getCurrentTenant(), null);
	}

	@Override
	public WebHook createWebHook(String tmplDefName, Map<String, Object> bindings) {
		WebHookTemplateRuntime tmpl = webHookService.getRuntimeByName(tmplDefName);
		if (tmpl == null) {
			throw new SystemException("WebHookTemplate:" + tmplDefName + " not found");
		}
		return tmpl.createWebHook();
	}

	@Override
	public void sendWebHook(WebHook webHookInstance) {
		Tenant tenant = ExecuteContext.getCurrentContext().getCurrentTenant();
		if (webHookInstance.getSubscribers() != null && !webHookInstance.getSubscribers().isEmpty()) {
			webHookService.sendWebHook(tenant, webHookInstance);
		} else {
			//TODO: 送り先がないのでexceptionかを作る
		}
		
	}

}

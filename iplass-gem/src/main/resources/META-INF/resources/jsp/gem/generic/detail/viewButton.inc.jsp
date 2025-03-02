<%--
 Copyright (C) 2014 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.

 Unless you have purchased a commercial license,
 the following license terms apply:

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program. If not, see <https://www.gnu.org/licenses/>.
 --%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="m" uri="http://iplass.org/tags/mtp"%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" trimDirectiveWhitespaces="true"%>

<%@ page import="java.util.HashMap"%>
<%@ page import="org.iplass.mtp.ManagerLocator"%>
<%@ page import="org.iplass.mtp.auth.AuthContext"%>
<%@ page import="org.iplass.mtp.entity.permission.EntityPermission" %>
<%@ page import="org.iplass.mtp.entity.Entity"%>
<%@ page import="org.iplass.mtp.entity.EntityManager"%>
<%@ page import="org.iplass.mtp.entity.definition.VersionControlType"%>
<%@ page import="org.iplass.mtp.entity.query.condition.expr.And"%>
<%@ page import="org.iplass.mtp.entity.query.condition.predicate.Equals"%>
<%@ page import="org.iplass.mtp.entity.query.Query"%>
<%@ page import="org.iplass.mtp.util.StringUtil"%>
<%@ page import="org.iplass.mtp.view.generic.element.Button"%>
<%@ page import="org.iplass.mtp.view.generic.element.DisplayType"%>
<%@ page import="org.iplass.mtp.view.generic.DetailFormView"%>
<%@ page import="org.iplass.mtp.view.generic.EntityViewManager"%>
<%@ page import="org.iplass.mtp.view.generic.EntityViewUtil"%>
<%@ page import="org.iplass.mtp.view.generic.OutputType"%>
<%@ page import="org.iplass.mtp.web.template.TemplateUtil"%>
<%@ page import="org.iplass.gem.command.Constants"%>
<%@ page import="org.iplass.gem.command.GemResourceBundleUtil"%>
<%@ page import="org.iplass.gem.command.generic.detail.DetailFormViewData"%>

<%!
	boolean hasLock(String lockId) {
		String userOid = AuthContext.getCurrentContext().getUser().getOid();
		return lockId != null ? userOid.equals(lockId) : false;
	}
%>
<%
	String defName = (String) request.getAttribute(Constants.DEF_NAME);
	DetailFormViewData data = (DetailFormViewData) request.getAttribute(Constants.DATA);

	DetailFormView form = data.getView();
	//コピーボタン表示可否
	boolean isNoneDispCopyButton = form.isNoneDispCopyButton();

	//ロック制御
	String oid = null;
	boolean isLock = false;
	boolean hasLock = false;
	String lockId = null;
	if (data.getEntity() != null) {
		oid = data.getEntity().getOid();
		lockId = data.getEntity().getLockedBy();
		isLock = lockId != null;
		hasLock = hasLock(lockId);
	}
	String lockStyle = isLock ? "display:none;" : "";
	String unlockStyle = !isLock ? "display:none;" : "";

	//ボタンの表示ラベル
	String editDisplayLabel = GemResourceBundleUtil.resourceString("generic.detail.viewButton.edit");
	String localizedEditDisplayLabel = TemplateUtil.getMultilingualString(form.getEditDisplayLabel(), form.getLocalizedEditDisplayLabelList());
	if (StringUtil.isNotBlank(localizedEditDisplayLabel)) {
		editDisplayLabel = localizedEditDisplayLabel;
	}
	String copyDisplayLabel = GemResourceBundleUtil.resourceString("generic.detail.viewButton.copy");
	String localizedCopyDisplayLabel = TemplateUtil.getMultilingualString(form.getCopyDisplayLabel(), form.getLocalizedCopyDisplayLabelList());
	if (StringUtil.isNotBlank(localizedCopyDisplayLabel)) {
		copyDisplayLabel = localizedCopyDisplayLabel;
	}
	String versionupDisplayLabel = GemResourceBundleUtil.resourceString("generic.detail.viewButton.newVersion");
	String localizedVersionupDisplayLabel = TemplateUtil.getMultilingualString(form.getVersionupDisplayLabel(), form.getLocalizedVersionupDisplayLabelList());
	if (StringUtil.isNotBlank(localizedVersionupDisplayLabel)) {
		versionupDisplayLabel = localizedVersionupDisplayLabel;
	}

	//権限確認用
	AuthContext auth = AuthContext.getCurrentContext();
	boolean showEditButton = auth.checkPermission(new EntityPermission(defName, EntityPermission.Action.UPDATE)) || auth.checkPermission(new EntityPermission(defName, EntityPermission.Action.DELETE));

	//ワークフローの権限確認用
	EntityManager em = ManagerLocator.getInstance().getManager(EntityManager.class);
	EntityViewManager evm = ManagerLocator.getInstance().getManager(EntityViewManager.class);
%>
<ul class="list_operation edit-bar">
<%
		if (form.getButtons().size() > 0) {
			for (Button button : form.getButtons()) {
				boolean isDisplayCustomType = false;
				if (button.getDisplayType() == DisplayType.CUSTOM) {
					isDisplayCustomType = evm.isDisplayButton(data.getEntityDefinition().getName(), button.getCustomDisplayTypeScriptKey(), OutputType.VIEW, data.getEntity());
				}
				if (EntityViewUtil.isDisplayElement(defName, button.getElementRuntimeId(), OutputType.VIEW)
						&& (button.getDisplayType() != null
							&& (button.getDisplayType() == DisplayType.VIEW
								|| button.getDisplayType() == DisplayType.BOTH || isDisplayCustomType))) {
					String cssClass = button.isPrimary() ? "gr-btn" : "gr-btn-02";
					if (StringUtil.isNotBlank(button.getStyle())) {
						cssClass = button.getStyle();
					}
					String customStyle = "";
					if (StringUtil.isNotBlank(button.getInputCustomStyle())) {
						Object value = request.getAttribute(Constants.ENTITY_DATA);
						Entity entity = value instanceof Entity ? (Entity) value : null;
						String scriptKey = (String)request.getAttribute(Constants.FORM_SCRIPT_KEY);
						customStyle = EntityViewUtil.getCustomStyle(defName, scriptKey, button.getInputCustomStyleScriptKey(), entity, null);
					}

					String displayLabel = TemplateUtil.getMultilingualString(button.getDisplayLabel(), button.getLocalizedDisplayLabelList());
%>
<%-- XSS対応-メタの設定のため対応なし(button.getOnclickEvent) --%>
<li class="btn"><input type="button" value="<c:out value="<%=displayLabel %>"/>" class="<c:out value="<%=cssClass %>"/>" style="<c:out value="<%=customStyle %>"/>" onclick="<%=button.getOnclickEvent() %>" /></li>
<%
				}
			}
		}
		if (showEditButton) {
			if (!form.isHideLock()) {
%>
<li class="btn lock-btn"><input type="button" value="${m:rs('mtp-gem-messages', 'generic.detail.viewButton.lock')}" class="gr-btn-02" name="lock" style="<c:out value="<%=lockStyle %>"/>" onclick="dataLock()" /></li>
<li class="btn unlock-btn"><input type="button" value="${m:rs('mtp-gem-messages', 'generic.detail.viewButton.unlock')}" class="gr-btn-02" name="unlock" style="<c:out value="<%=unlockStyle %>"/>" onclick="dataUnlock()" /></li>
<%
			}
			if (!form.isHideDetail()) {
				String dispStyle = "";
				if (isLock && !hasLock) {
					dispStyle = "display: none";
				}
%>
<li class="btn edit-btn"><input type="submit" value="<c:out value="<%=editDisplayLabel %>"/>" class="gr-btn" style="<c:out value="<%=dispStyle %>"/>" onclick="onclick_submit()" /></li>
<%
			}
		}
		if (auth.checkPermission(new EntityPermission(defName, EntityPermission.Action.CREATE)) && !isNoneDispCopyButton) {
%>
<li class="btn copy-btn"><input type="button" value="<c:out value="<%=copyDisplayLabel %>"/>" class="gr-btn" onclick="onclick_copy()" /></li>
<%
		}
		if (showEditButton) {
			if (data.getEntityDefinition().getVersionControlType() != VersionControlType.NONE) {
				String dispStyle = "";
				if (isLock && !hasLock) {
					dispStyle = "display: none";
				}
%>
<li class="btn version-btn"><input type="button" value="<c:out value="<%=versionupDisplayLabel %>"/>" class="gr-btn gr-size-03" style="<c:out value="<%=dispStyle %>"/>" onclick="onclick_newversion()" /></li>
<%
			}
		}
%>
<li class="mt05 cancel-link"><a href="javascript:void(0)" onclick="cancel();return false;">${m:rs("mtp-gem-messages", "generic.detail.viewButton.back")}</a></li>
</ul>

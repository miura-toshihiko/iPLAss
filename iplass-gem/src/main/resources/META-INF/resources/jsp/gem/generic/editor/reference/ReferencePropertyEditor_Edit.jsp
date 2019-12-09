<%--
 Copyright (C) 2013 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.

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
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collections"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.function.Supplier"%>
<%@ page import="org.iplass.mtp.ManagerLocator" %>
<%@ page import="org.iplass.mtp.auth.AuthContext" %>
<%@ page import="org.iplass.mtp.entity.permission.EntityPermission" %>
<%@ page import="org.iplass.mtp.entity.permission.EntityPropertyPermission" %>
<%@ page import="org.iplass.mtp.entity.Entity" %>
<%@ page import="org.iplass.mtp.entity.EntityManager" %>
<%@ page import="org.iplass.mtp.entity.LoadOption"%>
<%@ page import="org.iplass.mtp.entity.definition.EntityDefinition"%>
<%@ page import="org.iplass.mtp.entity.definition.EntityDefinitionManager"%>
<%@ page import="org.iplass.mtp.entity.query.PreparedQuery"%>
<%@ page import="org.iplass.mtp.entity.query.Query" %>
<%@ page import="org.iplass.mtp.entity.query.SortSpec"%>
<%@ page import="org.iplass.mtp.entity.query.SortSpec.SortType"%>
<%@ page import="org.iplass.mtp.entity.query.condition.Condition"%>
<%@ page import="org.iplass.mtp.entity.query.condition.expr.And"%>
<%@ page import="org.iplass.mtp.entity.query.condition.predicate.Equals"%>
<%@ page import="org.iplass.mtp.entity.definition.properties.ReferenceProperty"%>
<%@ page import="org.iplass.mtp.entity.definition.properties.VersionControlReferenceType"%>
<%@ page import="org.iplass.mtp.util.StringUtil" %>
<%@ page import="org.iplass.mtp.view.generic.DetailFormView"%>
<%@ page import="org.iplass.mtp.view.generic.EntityView"%>
<%@ page import="org.iplass.mtp.view.generic.EntityViewManager"%>
<%@ page import="org.iplass.mtp.view.generic.EntityViewUtil"%>
<%@ page import="org.iplass.mtp.view.generic.FormViewUtil"%>
<%@ page import="org.iplass.mtp.view.generic.LoadEntityContext"%>
<%@ page import="org.iplass.mtp.view.generic.LoadEntityInterrupter.LoadType"%>
<%@ page import="org.iplass.mtp.view.generic.editor.LinkProperty"%>
<%@ page import="org.iplass.mtp.view.generic.editor.PropertyEditor"%>
<%@ page import="org.iplass.mtp.view.generic.editor.ReferencePropertyEditor" %>
<%@ page import="org.iplass.mtp.view.generic.editor.ReferencePropertyEditor.EditPage"%>
<%@ page import="org.iplass.mtp.view.generic.editor.ReferencePropertyEditor.ReferenceDisplayType" %>
<%@ page import="org.iplass.mtp.view.generic.editor.SelectPropertyEditor.SelectDisplayType"%>
<%@ page import="org.iplass.mtp.view.generic.editor.SelectPropertyEditor"%>
<%@ page import="org.iplass.mtp.web.template.TemplateUtil" %>
<%@ page import="org.iplass.gem.command.generic.detail.DetailCommandContext"%>
<%@ page import="org.iplass.gem.command.generic.detail.DetailViewCommand"%>
<%@ page import="org.iplass.gem.command.generic.detail.LoadEntityInterrupterHandler"%>
<%@ page import="org.iplass.gem.command.generic.reflink.GetReferenceLinkItemCommand"%>
<%@ page import="org.iplass.gem.command.generic.reftree.SearchTreeDataCommand"%>
<%@ page import="org.iplass.gem.command.generic.search.SearchViewCommand"%>
<%@ page import="org.iplass.gem.command.Constants" %>
<%@ page import="org.iplass.gem.command.GemResourceBundleUtil" %>
<%@ page import="org.iplass.gem.command.ViewUtil" %>
<%!
	List<Entity> getSelectItems(ReferencePropertyEditor editor, Condition defaultCondition, Entity entity,
			PropertyEditor upperEditor) {
		Condition condition = defaultCondition;

		boolean doSearch = true;
		LinkProperty linkProperty = editor.getLinkProperty();
		if (linkProperty != null) {
			//連動の場合は上位値を取得して値が設定されている場合のみ検索
			doSearch = false;
			if (entity != null) {
				Object upperValue = entity.getValue(linkProperty.getLinkFromPropertyName());
				if (upperValue != null) {
					//参照元の値を条件に追加
					String upperPropName = linkProperty.getLinkToPropertyName();
					if (upperValue instanceof Entity) {
						upperPropName = upperPropName + "." + Entity.OID;
						upperValue = ((Entity)upperValue).getOid();
					}
					if (condition != null) {
						condition = new And(condition, new Equals(upperPropName, upperValue));
					} else {
						condition = new Equals(upperPropName, upperValue);
					}
					doSearch = true;
				}
			} else {
				//新規時のSelectPropertyEditorのdefaultValueのチェック(ReferencePropertyEditorはdefaultValueはなし)
				if (upperEditor != null) {
					if (upperEditor instanceof SelectPropertyEditor) {
						SelectPropertyEditor spe = (SelectPropertyEditor)upperEditor;
						String defaultValue = spe.getDefaultValue();
						if (defaultValue != null) {
							if (condition != null) {
								condition = new And(condition, new Equals(linkProperty.getLinkToPropertyName(), defaultValue));
							} else {
								condition = new Equals(linkProperty.getLinkToPropertyName(), defaultValue);
							}
							doSearch = true;
						}
					}
				}
			}
		}

		if (doSearch) {
			Query q = new Query();
			q.from(editor.getObjectName());
			q.select(Entity.OID, Entity.NAME, Entity.VERSION);
			if (editor.getDisplayLabelItem() != null) {
				q.select().add(editor.getDisplayLabelItem());
			}
			if (condition != null) {
				q.where(condition);
			}
			if (editor.getSortType() != null) {
				String sortItem = editor.getSortItem() != null ? editor.getSortItem() : Entity.OID;
				if (!Entity.OID.equals(sortItem) && !Entity.NAME.equals(sortItem)) {
					q.select().add(sortItem);
				}
				SortType sortType = SortSpec.SortType.ASC;
				if ("DESC".equals(editor.getSortType().name())) {
					sortType = SortSpec.SortType.DESC;
				}
				q.order(new SortSpec(sortItem, sortType));
			}

			EntityManager em = ManagerLocator.getInstance().getManager(EntityManager.class);
			return em.searchEntity(q).getList();
		} else {
			return Collections.emptyList();
		}
	}

	PropertyEditor getLinkUpperPropertyEditor(String defName, String viewName, LinkProperty linkProperty) {
		EntityViewManager evm = ManagerLocator.getInstance().getManager(EntityViewManager.class);
		return evm.getPropertyEditor(defName, Constants.VIEW_TYPE_DETAIL, viewName, linkProperty.getLinkFromPropertyName());
	}

	String getLinkUpperType(PropertyEditor editor) {
		if (editor != null) {
			if (editor instanceof SelectPropertyEditor) {
				SelectPropertyEditor spe = (SelectPropertyEditor)editor;
				if (spe.getDisplayType() == SelectDisplayType.SELECT) {
					return "select";
				} else if (spe.getDisplayType() == SelectDisplayType.RADIO
						|| spe.getDisplayType() == SelectDisplayType.CHECKBOX) {
					//CheckBoxの場合も多重度が1の場合のみRadioになるのでラジオで(CheckBoxの場合、反応しない)
					return "radio";
				}
			} else if (editor instanceof ReferencePropertyEditor) {
				ReferencePropertyEditor rpe = (ReferencePropertyEditor)editor;
				if (rpe.getDisplayType() == ReferenceDisplayType.SELECT) {
					return "select";
				} else if (rpe.getDisplayType() == ReferenceDisplayType.CHECKBOX) {
					//CheckBoxの場合も多重度が1の場合のみRadioになるのでラジオで(CheckBoxの場合、反応しない)
					return "radio";
				} else if (rpe.getDisplayType() == ReferenceDisplayType.REFCOMBO) {
					return "select";
				}
			}
		}
		return null;
	}

	//Linkタイプ、Labelタイプの場合の参照Entityのチェック
	//初期値として設定された際に、NameやVersionが未指定の場合を考慮して詰め直す
	List<Entity> getLinkTypeItems(Object propValue, ReferenceProperty pd, ReferencePropertyEditor editor) {
		if (propValue == null) {
			return Collections.emptyList();
		}

		EntityManager em = ManagerLocator.getInstance().getManager(EntityManager.class);
		EntityDefinitionManager edm = ManagerLocator.getInstance().getManager(EntityDefinitionManager.class);
		EntityViewManager evm = ManagerLocator.getInstance().getManager(EntityViewManager.class);
		LoadEntityInterrupterHandler handler = getLoadEntityInterrupterHandler(em, edm, evm);

		List<Entity> entityList = new ArrayList<Entity>();
		if (propValue instanceof Entity[]) {
			Entity[] entities = (Entity[]) propValue;
			if (entities != null) {
				for (Entity refEntity : entities) {
					Entity entity = loadItem(refEntity, editor, pd, handler, em);
					if (entity != null) {
						entityList.add(entity);
					}
				}
			}
		} else if (propValue instanceof Entity) {
			Entity refEntity = (Entity) propValue;
			if (refEntity != null) {
				Entity entity = loadItem(refEntity, editor, pd, handler, em);
				if (entity != null) {
					entityList.add(entity);
				}
			}
		}
		return entityList;
	}

	LoadEntityInterrupterHandler getLoadEntityInterrupterHandler(EntityManager em, EntityDefinitionManager edm, EntityViewManager evm) {
		DetailCommandContext context = new DetailCommandContext(TemplateUtil.getRequestContext(), em, edm);//ここでこれを作るのはちょっと微妙だが・・・
		context.setEntityDefinition(edm.get(context.getDefinitionName()));
		context.setEntityView(evm.get(context.getDefinitionName()));
		return context.getLoadEntityInterrupterHandler();
	}

	Entity loadItem(final Entity refEntity, final ReferencePropertyEditor editor, final ReferenceProperty pd, final LoadEntityInterrupterHandler handler, final EntityManager em) {
		//念のためOIDチェック
		if (refEntity.getOid() == null) {
			return null;
		}
		if (getDisplayPropLabel(editor, refEntity) == null || refEntity.getVersion() == null) {
			//name、versionは必須のためどちらかが未指定ならLoadする
			Entity entity = null;
			LoadOption loadOption = new LoadOption(false, false);
			final String refDefName = editor.getObjectName();
			final LoadEntityContext leContext = handler.beforeLoadReference(refDefName, loadOption, pd, LoadType.VIEW);
			if (leContext.isDoPrivileged()) {
				entity = AuthContext.doPrivileged(new Supplier<Entity>() {

					@Override
					public Entity get() {
						return em.load(refEntity.getOid(), refEntity.getVersion(), refDefName, leContext.getLoadOption());
					}
				});
			} else {
				entity = em.load(refEntity.getOid(), refEntity.getVersion(), refDefName, leContext.getLoadOption());
			}
			handler.afterLoadReference(entity, loadOption, pd, LoadType.VIEW);
			return entity;
		} else {
			return refEntity;
		}
	}

	String getTitle(String defName, String viewName) {
		EntityDefinitionManager edm = ManagerLocator.getInstance().getManager(EntityDefinitionManager.class);
		EntityViewManager evm = ManagerLocator.getInstance().getManager(EntityViewManager.class);

		EntityDefinition ed = edm.get(defName);
		EntityView ev = evm.get(defName);
		DetailFormView fv = null;
		if (ev != null) {
			fv = ev.getDetailFormView(viewName);
		}
		if (fv == null) fv = FormViewUtil.createDefaultDetailFormView(ed);

		return TemplateUtil.getMultilingualString(fv.getTitle(), fv.getLocalizedTitleList(), ed.getDisplayName(), ed.getLocalizedDisplayNameList());
	}
	
	String getDisplayPropLabel(ReferencePropertyEditor editor, Entity refEntity) {
		String displayPropName = editor.getDisplayLabelItem();
		if (displayPropName == null) {
			displayPropName = Entity.NAME;
		}
		return refEntity.getValue(displayPropName);
	}
%>
<%
	String contextPath = TemplateUtil.getTenantContextPath();
	AuthContext auth = AuthContext.getCurrentContext();

	//Request情報取得
	ReferencePropertyEditor editor = (ReferencePropertyEditor) request.getAttribute(Constants.EDITOR_EDITOR);

	Entity entity = request.getAttribute(Constants.ENTITY_DATA) instanceof Entity ? (Entity) request.getAttribute(Constants.ENTITY_DATA) : null;
	Object propValue = request.getAttribute(Constants.EDITOR_PROP_VALUE);

	String defName = (String)request.getAttribute(Constants.DEF_NAME);
	String rootDefName = (String)request.getAttribute(Constants.ROOT_DEF_NAME);
	ReferenceProperty pd = (ReferenceProperty) request.getAttribute(Constants.EDITOR_PROPERTY_DEFINITION);
	String scriptKey = (String)request.getAttribute(Constants.SECTION_SCRIPT_KEY);
	String execType = (String) request.getAttribute(Constants.EXEC_TYPE);
	String viewName = request.getParameter(Constants.VIEW_NAME);
	if (viewName == null) {
		viewName = "";
	} else {
		viewName = StringUtil.escapeHtml(viewName);
	}
	boolean isInsert = Constants.EXEC_TYPE_INSERT.equals(execType);
	Boolean nest = (Boolean) request.getAttribute(Constants.EDITOR_REF_NEST);
	if (nest == null) nest = false;

	//本体のEntity
	Entity parentEntity = (Entity) request.getAttribute(Constants.EDITOR_PARENT_ENTITY);
	String parentOid = parentEntity != null ? parentEntity.getOid() : "";
	String parentVersion = parentEntity != null && parentEntity.getVersion() != null ? parentEntity.getVersion().toString() : "";

	//Property情報取得
	boolean isMappedby = pd.getMappedBy() != null;
	boolean isMultiple = pd.getMultiplicity() != 1;

	Boolean isVirtual = (Boolean) request.getAttribute(Constants.IS_VIRTUAL);
	if (isVirtual == null) isVirtual = false;

	//権限チェック
	boolean editable = true;
	if (isVirtual) {
		editable = true;//仮想プロパティは権限チェック要らない
	} else {
		if(isInsert) {
			editable = auth.checkPermission(new EntityPropertyPermission(defName, pd.getName(), EntityPropertyPermission.Action.CREATE));
		} else {
			editable = auth.checkPermission(new EntityPropertyPermission(defName, pd.getName(), EntityPropertyPermission.Action.UPDATE));
		}
	}
	boolean updatable = ((pd == null || pd.isUpdatable()) || isInsert) && editable;

	//Editorの設定値取得
	String refDefName = editor.getObjectName();
	String propName = editor.getPropertyName();
	boolean hideDeleteButton = editor.isHideDeleteButton();
	boolean hideRegistButton = editor.isHideRegistButton();
	boolean hideSelectButton = editor.isHideSelectButton();
	boolean refEdit = editor.isEditableReference();
	boolean editPageDetail = editor.getEditPage() == null || editor.getEditPage() == EditPage.DETAIL;

	//ネストプロパティ内でのネスト表示は禁止、ひとまずSelectに（非表示でもいいか？）
	if (nest && editor.getDisplayType() == ReferenceDisplayType.NESTTABLE) {
		editor.setDisplayType(ReferenceDisplayType.SELECT);
	}

	//Action定義取得
	String _viewName = editor.getViewName() != null ? editor.getViewName() : "";
	String urlPath = ViewUtil.getParamMappingPath(refDefName, _viewName);

	//追加用のAction
	String addAction = "";
	if (StringUtil.isNotBlank(editor.getAddActionName())) {
		addAction = contextPath + "/" + editor.getAddActionName() + urlPath;
	} else {
		addAction = contextPath + "/" + DetailViewCommand.REF_DETAIL_ACTION_NAME + urlPath;
	}

	//選択用のAction
	String selectAction = "";
	if (StringUtil.isNotBlank(editor.getSelectActionName())) {
		selectAction = contextPath + "/" + editor.getSelectActionName() + urlPath;
	} else {
		selectAction = contextPath + "/" + SearchViewCommand.SELECT_ACTION_NAME + urlPath;
	}

	//表示用のAction
	String viewAction = "";
	if (StringUtil.isNotBlank(editor.getViewrefActionName())) {
		viewAction = contextPath + "/" + editor.getViewrefActionName() + urlPath;
	} else {
		viewAction = contextPath + "/" + DetailViewCommand.REF_VIEW_ACTION_NAME + urlPath;
	}

	String urlParam = "";
	if (StringUtil.isNotBlank(editor.getUrlParameterScriptKey())) {
		urlParam = ManagerLocator.getInstance().getManager(EntityViewManager.class).getUrlParameter(rootDefName, editor.getUrlParameterScriptKey(), parentEntity);
	}

	Condition condition = null;
	if (editor.getCondition() != null && !editor.getCondition().isEmpty()) {
		condition = new PreparedQuery(editor.getCondition()).condition(null);
	}

	String pleaseSelectLabel = "";
	if (ViewUtil.isShowPulldownPleaseSelectLabel()) {
		pleaseSelectLabel = GemResourceBundleUtil.resourceString("generic.editor.reference.ReferencePropertyEditor_Edit.pleaseSelect");
	}

	//カスタムスタイル
	String customStyle = "";
	if (StringUtil.isNotEmpty(editor.getInputCustomStyle())) {
		customStyle = EntityViewUtil.getCustomStyle(rootDefName, scriptKey, editor.getInputCustomStyleScriptKey(), entity, propValue);
	}

	if (ViewUtil.isAutocompletionTarget()) {// FIXME テーブルはいらん？
		request.setAttribute(Constants.AUTOCOMPLETION_EDITOR, editor);
		request.setAttribute(Constants.AUTOCOMPLETION_SCRIPT_PATH, "/jsp/gem/generic/editor/reference/ReferencePropertyAutocompletion.jsp");
	}

	//タイプ毎に表示内容かえる
	if (editor.getDisplayType() == ReferenceDisplayType.LINK && updatable && !isMappedby) {
		//リンク
		String ulId = "ul_" + propName;

		if (!editPageDetail) {
			//参照モードなのでカスタムスタイル変更
			if (StringUtil.isNotEmpty(editor.getCustomStyle())) {
				customStyle = EntityViewUtil.getCustomStyle(rootDefName, scriptKey, editor.getOutputCustomStyleScriptKey(), entity, propValue);
			}
		}

		//初期値として設定された際に、NameやVersionが未指定の場合を考慮して詰め直す
		List<Entity> entityList = getLinkTypeItems(propValue, pd, editor);
%>
<ul id="<c:out value="<%=ulId %>"/>" data-deletable="<c:out value="<%=(!hideDeleteButton && updatable) %>"/>" class="mb05">
<%
		for (int i = 0; i < entityList.size(); i++) {
			Entity refEntity = entityList.get(i);
			String liId = "li_" + propName + i;
			String linkId = propName + "_" + refEntity.getOid();
			String key = refEntity.getOid() + "_" + refEntity.getVersion();
			String dispPropLabel = getDisplayPropLabel(editor, refEntity);
%>
<li id="<c:out value="<%=liId %>"/>" class="list-add">
<%
			if (editPageDetail) {
%>
<a href="javascript:void(0)" class="modal-lnk" style="<c:out value="<%=customStyle%>"/>" id="<c:out value="<%=linkId %>"/>" onclick="showReference('<%=StringUtil.escapeJavaScript(viewAction)%>', '<%=StringUtil.escapeJavaScript(refDefName)%>', '<%=StringUtil.escapeJavaScript(refEntity.getOid())%>', '<%=refEntity.getVersion() %>', '<%=StringUtil.escapeJavaScript(linkId)%>', <%=refEdit %>)"><c:out value="<%=dispPropLabel %>" /></a>
<%
				if (!hideDeleteButton && updatable) {
%>
<input type="button" value="${m:rs('mtp-gem-messages', 'generic.editor.reference.ReferencePropertyEditor_Edit.delete')}" class="gr-btn-02 del-btn" onclick="deleteItem('<%=StringUtil.escapeJavaScript(liId)%>')" />
<%				}
			} else {
%>
<a href="javascript:void(0)" class="modal-lnk" style="<c:out value="<%=customStyle%>"/>" id="<c:out value="<%=linkId %>"/>" onclick="showReference('<%=StringUtil.escapeJavaScript(viewAction)%>', '<%=StringUtil.escapeJavaScript(refDefName)%>', '<%=StringUtil.escapeJavaScript(refEntity.getOid())%>', '<%=refEntity.getVersion() %>', '<%=StringUtil.escapeJavaScript(linkId)%>', false)"><c:out value="<%=dispPropLabel %>" /></a>
<%
			}
%>
<input type="hidden" name="<c:out value="<%=propName %>"/>" value="<c:out value="<%=key %>"/>" />
</li>
<%
		}
%>
</ul>
<%
		if (editPageDetail) {
			if (!hideSelectButton) {
				String selBtnId = "sel_btn_" + propName;
				String specVersionKey = "";
				if (pd.getVersionControlType() == VersionControlReferenceType.AS_OF_EXPRESSION_BASE) {
					//特定バージョン指定の場合、画面の項目からパラメータ取得
					if (StringUtil.isNotBlank(editor.getSpecificVersionPropertyName())) {
						if (editor.getSpecificVersionPropertyName().startsWith(".")) {
							specVersionKey = editor.getSpecificVersionPropertyName().replace(".", "");//ルートを対象
						} else {
							//editorのプロパティ名の最後の.から先を置きかえる
							if (editor.getPropertyName().indexOf(".") > -1) {
								//nest、同レベルの他のプロパティを対象にする
								String parentPath = editor.getPropertyName().substring(0, editor.getPropertyName().lastIndexOf(".") + 1);
								specVersionKey = parentPath + editor.getSpecificVersionPropertyName();
							} else {
								//nestではないのでそのまま設定
								specVersionKey = editor.getSpecificVersionPropertyName();
							}
						}
					}
				}

%>
<input type="button" value="${m:rs('mtp-gem-messages', 'generic.editor.reference.ReferencePropertyEditor_Edit.select')}" class="gr-btn-02 modal-btn sel-btn" id="<c:out value="<%=selBtnId %>"/>" data-propName="<c:out value="<%=propName %>"/>" />
<script type="text/javascript">
$(function() {
	var callback = function(entityList, deleteList, propName) {
<%
				if (editor.getSelectActionCallbackScript() != null) {
%>
<%-- XSS対応-メタの設定のため対応なし(editor.getSelectActionCallbackScript) --%>
<%=editor.getSelectActionCallbackScript()%>
<%
				}
%>
	};
	var key = "selectActionCallback_" + new Date().getTime();
	scriptContext[key] = callback;
	var params = {
		selectAction: "<%=StringUtil.escapeJavaScript(selectAction) %>"
		, viewAction: "<%=StringUtil.escapeJavaScript(viewAction) %>"
		, defName: "<%=StringUtil.escapeJavaScript(refDefName) %>"
		, multiplicity: "<%=pd.getMultiplicity() %>"
		, urlParam: "<%=StringUtil.escapeJavaScript(urlParam) %>"
		, refEdit: <%=refEdit %>
		, callbackKey: key
		, specVersionKey: "<%=StringUtil.escapeJavaScript(specVersionKey) %>"
		, viewName: "<%=StringUtil.escapeJavaScript(_viewName) %>"
		, permitConditionSelectAll: <%=editor.isPermitConditionSelectAll()%>
		, parentDefName: "<%=StringUtil.escapeJavaScript(defName)%>"
		, parentViewName: "<%=StringUtil.escapeJavaScript(viewName)%>"
		, viewType: "<%=Constants.VIEW_TYPE_DETAIL %>"
	}
	var $selBtn = $(":button[id='<%=StringUtil.escapeJavaScript(selBtnId) %>']");
	for (key in params) {
		$selBtn.attr("data-" + key, params[key]);
	}
	$selBtn.on("click", function() {
		searchReference(params.selectAction, params.viewAction, params.defName, $(this).attr("data-propName"), params.multiplicity, <%=isMultiple %>,
				 params.urlParam, params.refEdit, callback, this, params.viewName, params.permitConditionSelectAll, params.parentDefName, params.parentViewName, params.viewType);
	});

});
</script>
<%
			}
			if (auth.checkPermission(new EntityPermission(refDefName, EntityPermission.Action.CREATE)) && !hideRegistButton) {
				String insBtnId = "ins_btn_" + propName;
%>
<input type="button" value="${m:rs('mtp-gem-messages', 'generic.editor.reference.ReferencePropertyEditor_Edit.new')}" class="gr-btn-02 modal-btn ins-btn" id="<c:out value="<%=insBtnId %>"/>" />
<script type="text/javascript">
$(function() {
	var callback = function(entity, propName) {
<%
				if (editor.getInsertActionCallbackScript() != null) {
%>
<%-- XSS対応-メタの設定のため対応なし(editor.getInsertActionCallbackScript) --%>
<%=editor.getInsertActionCallbackScript()%>
<%
				}
%>
	};
	var key = "insertActionCallback_" + new Date().getTime();
	scriptContext[key] = callback;
	var params = {
		addAction: "<%=StringUtil.escapeJavaScript(addAction) %>"
		, viewAction: "<%=StringUtil.escapeJavaScript(viewAction) %>"
		, defName: "<%=StringUtil.escapeJavaScript(refDefName) %>"
		, propName: "<%=StringUtil.escapeJavaScript(propName) %>"
		, multiplicity: "<%=pd.getMultiplicity() %>"
		, urlParam: "<%=StringUtil.escapeJavaScript(urlParam) %>"
		, parentOid: "<%=StringUtil.escapeJavaScript(parentOid)%>"
		, parentVersion: "<%=StringUtil.escapeJavaScript(parentVersion)%>"
		, parentDefName: "<%=StringUtil.escapeJavaScript(defName)%>"
		, parentViewName: "<%=StringUtil.escapeJavaScript(viewName)%>"
		, refEdit: <%=refEdit %>
		, callbackKey: key
	}
	var $insBtn = $(":button[id='<%=StringUtil.escapeJavaScript(insBtnId) %>']");
	for (key in params) {
		$insBtn.attr("data-" + key, params[key]);
	}
	$insBtn.on("click", function() {
		insertReference(params.addAction, params.viewAction, params.defName, params.propName, params.multiplicity,
				 params.urlParam, params.parentOid, params.parentVersion, params.parentDefName, params.parentViewName, params.refEdit, callback, this);
	});

});
</script>
<%
			}
		}
	} else if (editor.getDisplayType() == ReferenceDisplayType.SELECT && updatable && !isMappedby) {
		//リスト
		PropertyEditor upperEditor = null;
		String upperType = null;
		if (editor.getLinkProperty() != null) {
			upperEditor = getLinkUpperPropertyEditor(rootDefName, viewName, editor.getLinkProperty());
			upperType = getLinkUpperType(upperEditor);
		}

		List<Entity> entityList = getSelectItems(editor, condition, entity, upperEditor);

		//リスト
		List<String> oid = new ArrayList<String>();
		if (propValue instanceof Entity[]) {
			Entity[] entities = (Entity[]) propValue;
			if (entities != null) {
				for (Entity refEntity : entities) {
					if (refEntity != null && refEntity.getOid() != null) {
						oid.add(refEntity.getOid());
					}
				}
			}
		} else if (propValue instanceof Entity) {
			Entity refEntity = (Entity) propValue;
			if (refEntity != null && refEntity.getOid() != null) {
				oid.add(refEntity.getOid());
			}
		}

		String multiple = isMultiple ? " multiple" : "";
		String size = isMultiple ? "5" : "1";

		if (editor.getLinkProperty() != null && upperType != null && !isMultiple) {
			//連動設定(連動元のタイプがサポートの場合のみ、かつ多重度は1のみサポート)
			LinkProperty link = editor.getLinkProperty();

%>
<select name="<c:out value="<%=propName %>"/>" class="form-size-02 inpbr refLinkSelect" style="<c:out value="<%=customStyle%>"/>" size="<c:out value="<%=size %>"/>"
data-defName="<c:out value="<%=rootDefName %>"/>"
data-viewType="<%=Constants.VIEW_TYPE_DETAIL %>"
data-viewName="<c:out value="<%=viewName %>"/>"
data-propName="<c:out value="<%=pd.getName() %>"/>"
data-linkName="<c:out value="<%=link.getLinkFromPropertyName() %>"/>"
data-prefix=""
data-getItemWebapiName="<%=GetReferenceLinkItemCommand.WEBAPI_NAME %>"
data-upperType="<c:out value="<%=upperType %>"/>"
>
<%
			if (!isMultiple) {
%>
<option value=""><%= pleaseSelectLabel %></option>
<%
			}
			for (Entity refEntity : entityList) {
				String selected = oid.contains(refEntity.getOid()) ? " selected" : "";
				String _value = refEntity.getOid() + "_" + refEntity.getVersion();
				String displayPropLabel = getDisplayPropLabel(editor, refEntity);
%>
<option value="<c:out value="<%=_value %>"/>" <c:out value="<%=selected %>"/>><c:out value="<%=displayPropLabel %>" /></option>
<%
			}
%>
</select>
<%
		} else {
%>
<select name="<c:out value="<%=propName %>"/>" class="form-size-02 inpbr" style="<c:out value="<%=customStyle%>"/>" size="<c:out value="<%=size %>"/>" <c:out value="<%=multiple %>"/>>
<%
			if (!isMultiple) {
%>
<option value=""><%= pleaseSelectLabel %></option>
<%
			}
%>
<%
			for (Entity refEntity : entityList) {
				String selected = oid.contains(refEntity.getOid()) ? " selected" : "";
				String _value = refEntity.getOid() + "_" + refEntity.getVersion();
				String displayPropLabel = getDisplayPropLabel(editor, refEntity);
%>
<option value="<c:out value="<%=_value %>"/>" <c:out value="<%=selected %>"/>><c:out value="<%=displayPropLabel %>" /></option>
<%
			}
		}
%>
</select>
<%
	} else if (editor.getDisplayType() == ReferenceDisplayType.CHECKBOX && updatable && !isMappedby) {
		//チェックボックス
		PropertyEditor upperEditor = null;
		String upperType = null;
		if (editor.getLinkProperty() != null) {
			upperEditor = getLinkUpperPropertyEditor(rootDefName, viewName, editor.getLinkProperty());
			upperType = getLinkUpperType(upperEditor);
		}

		List<Entity> entityList = getSelectItems(editor, condition, entity, upperEditor);

		//リスト
		List<String> oid = new ArrayList<String>();
		if (propValue instanceof Entity[]) {
			Entity[] entities = (Entity[]) propValue;
			if (entities != null) {
				for (Entity refEntity : entities) {
					if (refEntity != null && refEntity.getOid() != null) {
						oid.add(refEntity.getOid());
					}
				}
			}
		} else if (propValue instanceof Entity) {
			Entity refEntity = (Entity) propValue;
			if (refEntity != null && refEntity.getOid() != null) {
				oid.add(refEntity.getOid());
			}
		}
		String cls = "list-check-01";
		if (!isMultiple) cls = "list-radio-01";

		if (editor.getLinkProperty() != null && upperType != null && !isMultiple) {
			//連動設定(連動元のタイプがサポートの場合のみ、かつ多重度は1のみサポート)
			LinkProperty link = editor.getLinkProperty();
%>
<ul class="<c:out value="<%=cls %>"/> refLinkRadio"
data-itemName="<c:out value="<%=propName %>"/>"
data-defName="<c:out value="<%=rootDefName %>"/>"
data-viewType="<%=Constants.VIEW_TYPE_DETAIL %>"
data-viewName="<c:out value="<%=viewName %>"/>"
data-propName="<c:out value="<%=pd.getName() %>"/>"
data-linkName="<c:out value="<%=link.getLinkFromPropertyName() %>"/>"
data-prefix=""
data-getItemWebapiName="<%=GetReferenceLinkItemCommand.WEBAPI_NAME %>"
data-upperType="<c:out value="<%=upperType %>"/>"
data-customStyle="<c:out value="<%=customStyle%>"/>"
>
<%
			for (Entity refEntity : entityList) {
%>
<li><label style="<c:out value="<%=customStyle%>"/>">
<%
				String checked = oid.contains(refEntity.getOid()) ? " checked" : "";
				String _value = refEntity.getOid() + "_" + refEntity.getVersion();
				if (isMultiple) {
%>
<input type="checkbox" name="<c:out value="<%=propName %>"/>" value="<c:out value="<%=_value %>"/>" <c:out value="<%=checked %>"/> /><c:out value="<%=refEntity.getName() %>" />
<%
				} else {
%>
<input type="radio" name="<c:out value="<%=propName %>"/>" value="<c:out value="<%=_value %>"/>" <c:out value="<%=checked %>"/> /><c:out value="<%=refEntity.getName() %>" />
<%
				}
%>
</label></li>
<%
			}
%>
</ul>
<%
		} else {
%>
<ul class="<c:out value="<%=cls %>"/>">
<%
			for (Entity refEntity : entityList) {
%>
<li><label style="<c:out value="<%=customStyle%>"/>">
<%
				String checked = oid.contains(refEntity.getOid()) ? " checked" : "";
				String _value = refEntity.getOid() + "_" + refEntity.getVersion();
				if (isMultiple) {
%>
<input type="checkbox" name="<c:out value="<%=propName %>"/>" value="<c:out value="<%=_value %>"/>" <c:out value="<%=checked %>"/> /><c:out value="<%=refEntity.getName() %>" />
<%
				} else {
%>
<input type="radio" name="<c:out value="<%=propName %>"/>" value="<c:out value="<%=_value %>"/>" <c:out value="<%=checked %>"/> /><c:out value="<%=refEntity.getName() %>" />
<%
				}
%>
</label></li>
<%
			}
		}
%>
</ul>
<%
	} else if (editor.getDisplayType() == ReferenceDisplayType.TREE && updatable && !isMappedby) {
		//ツリー(基本はリンクと同じ、選択ダイアログを変える)
		String ulId = "ul_" + propName;

		if (!editPageDetail) {
			//参照モードなのでカスタムスタイル変更
			if (StringUtil.isNotEmpty(editor.getCustomStyle())) {
				customStyle = EntityViewUtil.getCustomStyle(rootDefName, scriptKey, editor.getOutputCustomStyleScriptKey(), entity, propValue);
			}
		}

		//初期値として設定された際に、NameやVersionが未指定の場合を考慮して詰め直す
		List<Entity> entityList = getLinkTypeItems(propValue, pd, editor);
%>
<ul id="<c:out value="<%=ulId %>"/>" data-deletable="<c:out value="<%=(!hideDeleteButton && updatable) %>"/>" class="mb05">
<%
		for (int i = 0; i < entityList.size(); i++) {
			Entity refEntity = entityList.get(i);
			String liId = "li_" + propName + i;
			String linkId = propName + "_" + refEntity.getOid();
			String key = refEntity.getOid() + "_" + refEntity.getVersion();
%>
<li id="<c:out value="<%=liId %>"/>" class="list-add">
<%
			if (editPageDetail) {
%>
<a href="javascript:void(0)" class="modal-lnk" style="<c:out value="<%=customStyle%>"/>" id="<c:out value="<%=linkId %>"/>" onclick="showReference('<%=StringUtil.escapeJavaScript(viewAction)%>', '<%=StringUtil.escapeJavaScript(refDefName)%>', '<%=StringUtil.escapeJavaScript(refEntity.getOid())%>', '<%=refEntity.getVersion() %>', '<%=StringUtil.escapeJavaScript(linkId)%>', <%=refEdit %>)"><c:out value="<%=refEntity.getName() %>" /></a>
<%
				if (!hideDeleteButton && updatable) {
%>
<input type="button" value="${m:rs('mtp-gem-messages', 'generic.editor.reference.ReferencePropertyEditor_Edit.delete')}" class="gr-btn-02 del-btn" onclick="deleteItem('<%=StringUtil.escapeJavaScript(liId)%>')" />
<%				}
			} else {
%>
<a href="javascript:void(0)" class="modal-lnk" style="<c:out value="<%=customStyle%>"/>" id="<c:out value="<%=linkId %>"/>" onclick="showReference('<%=StringUtil.escapeJavaScript(viewAction)%>', '<%=StringUtil.escapeJavaScript(refDefName)%>', '<%=StringUtil.escapeJavaScript(refEntity.getOid())%>', '<%=refEntity.getVersion() %>', '<%=StringUtil.escapeJavaScript(linkId)%>', false)"><c:out value="<%=refEntity.getName() %>" /></a>
<%
			}
%>
<input type="hidden" name="<c:out value="<%=propName %>"/>" value="<c:out value="<%=key %>"/>" />
</li>
<%
		}
%>
</ul>
<%
		if (editPageDetail) {
			if (!hideSelectButton) {
				String selBtnId = "sel_btn_" + propName;
				String title = getTitle(refDefName, viewName);

				String prefix = "";
				int index = propName.indexOf(pd.getName());
				if (index > 0) {
					//propNameから実際のプロパティ名を除去してプレフィックスを取得
					prefix = propName.substring(0, index);
				}

				String linkPropName = "";
				String upperType = "";
				if (editor.getLinkProperty() != null) {
					linkPropName = editor.getLinkProperty().getLinkFromPropertyName();
					PropertyEditor upperEditor = getLinkUpperPropertyEditor(rootDefName, viewName, editor.getLinkProperty());
					upperType = getLinkUpperType(upperEditor);
				}
%>
<input type="button" value="${m:rs('mtp-gem-messages', 'generic.editor.reference.ReferencePropertyEditor_Edit.select')}" class="gr-btn-02 sel-btn recursiveTreeTrigger" id="<c:out value="<%=selBtnId %>"/>"
 data-defName="<c:out value="<%=rootDefName%>"/>"
 data-viewType="detail"
 data-viewName="<c:out value="<%=viewName%>"/>"
 data-propName="<c:out value="<%=pd.getName()%>"/>"
 data-prefix="<c:out value="<%=prefix%>"/>"
 data-multiplicity="<c:out value="<%=pd.getMultiplicity()%>"/>"
 data-linkPropName="<c:out value="<%=linkPropName%>"/>"
 data-upperType="<c:out value="<%=upperType%>"/>"
 data-webapiName="<%=SearchTreeDataCommand.WEBAPI_NAME %>"
 data-container="<c:out value="<%=ulId %>"/>"
 data-title="<c:out value="<%=title%>"/>"
 data-deletable="<c:out value="<%=(!hideDeleteButton && updatable) %>"/>"
 data-customStyle="<c:out value="<%=customStyle%>"/>"
 data-viewAction="<c:out value="<%=viewAction%>"/>"
 data-refDefName="<c:out value="<%=refDefName%>"/>"
 data-refEdit="<c:out value="<%=refEdit%>"/>"
 />
<%
			}
			if (auth.checkPermission(new EntityPermission(refDefName, EntityPermission.Action.CREATE)) && !hideRegistButton) {
				String insBtnId = "ins_btn_" + propName;
%>
<input type="button" value="${m:rs('mtp-gem-messages', 'generic.editor.reference.ReferencePropertyEditor_Edit.new')}" class="gr-btn-02 modal-btn ins-btn" id="<c:out value="<%=insBtnId %>"/>" />
<script type="text/javascript">
$(function() {
	var callback = function(entity, propName) {
<%
				if (editor.getInsertActionCallbackScript() != null) {
%>
<%-- XSS対応-メタの設定のため対応なし(editor.getInsertActionCallbackScript) --%>
<%=editor.getInsertActionCallbackScript()%>
<%
				}
%>
	};
	var key = "insertActionCallback_" + new Date().getTime();
	scriptContext[key] = callback;
	var params = {
		addAction: "<%=StringUtil.escapeJavaScript(addAction) %>"
		, viewAction: "<%=StringUtil.escapeJavaScript(viewAction) %>"
		, defName: "<%=StringUtil.escapeJavaScript(refDefName) %>"
		, propName: "<%=StringUtil.escapeJavaScript(propName) %>"
		, multiplicity: "<%=pd.getMultiplicity() %>"
		, urlParam: "<%=StringUtil.escapeJavaScript(urlParam) %>"
		, parentOid: "<%=StringUtil.escapeJavaScript(parentOid)%>"
		, parentVersion: "<%=StringUtil.escapeJavaScript(parentVersion)%>"
		, parentDefName: "<%=StringUtil.escapeJavaScript(defName)%>"
		, parentViewName: "<%=StringUtil.escapeJavaScript(viewName)%>"
		, refEdit: <%=refEdit %>
		, callbackKey: key
	}
	var $insBtn = $(":button[id='<%=StringUtil.escapeJavaScript(insBtnId)%>']");
	for (key in params) {
		$insBtn.attr("data-" + key, params[key]);
	}
	$insBtn.on("click", function() {
		insertReference(params.addAction, params.viewAction, params.defName, params.propName, params.multiplicity,
				 params.urlParam, params.parentOid, params.parentVersion, params.parentDefName, params.parentViewName, params.refEdit, callback, this);
	});

});
</script>
<%
			}
		}
	} else if (editor.getDisplayType() == ReferenceDisplayType.REFCOMBO && updatable && !isMappedby) {
		//連動コンボ
		//多重度1限定
%>
<jsp:include page="ReferencePropertyEditor_RefCombo.jsp" />
<%
	} else if (editor.getDisplayType() == ReferenceDisplayType.NESTTABLE) {
		//テーブル
		//include先で利用するためパラメータを詰めなおし
		//updatableかはinclude先で改めて判断しておく
		request.setAttribute(Constants.EDITOR_REF_MAPPEDBY, pd.getMappedBy());
%>
<jsp:include page="ReferencePropertyEditor_Table.jsp" />
<%
	} else {
		//初期値として設定された際に、NameやVersionが未指定の場合を考慮して詰め直す
		List<Entity> entityList = getLinkTypeItems(propValue, pd, editor);
		request.setAttribute(Constants.EDITOR_PROP_VALUE, entityList.toArray(new Entity[0]));
		request.setAttribute(Constants.OUTPUT_HIDDEN, true);
%>
<jsp:include page="ReferencePropertyEditor_View.jsp" />
<%
		request.removeAttribute(Constants.OUTPUT_HIDDEN);
	}
%>

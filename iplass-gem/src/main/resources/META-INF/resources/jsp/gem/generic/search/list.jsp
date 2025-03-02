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

<%@ page import="java.sql.Timestamp" %>
<%@ page import="java.text.ParseException" %>
<%@ page import="java.util.Date"%>
<%@ page import="java.util.List" %>
<%@ page import="org.iplass.mtp.auth.AuthContext" %>
<%@ page import="org.iplass.mtp.entity.permission.EntityPermission" %>
<%@ page import="org.iplass.mtp.entity.definition.*" %>
<%@ page import="org.iplass.mtp.entity.definition.properties.*" %>
<%@ page import="org.iplass.mtp.entity.Entity"%>
<%@ page import="org.iplass.mtp.view.generic.*" %>
<%@ page import="org.iplass.mtp.view.generic.editor.*" %>
<%@ page import="org.iplass.mtp.view.generic.element.property.PropertyColumn"%>
<%@ page import="org.iplass.mtp.view.generic.element.section.SearchResultSection" %>
<%@ page import="org.iplass.mtp.view.generic.element.Element"%>
<%@ page import="org.iplass.mtp.view.generic.element.VirtualPropertyItem"%>
<%@ page import="org.iplass.mtp.view.top.parts.EntityListParts"%>
<%@ page import="org.iplass.mtp.util.StringUtil" %>
<%@ page import="org.iplass.mtp.web.template.TemplateUtil" %>
<%@ page import="org.iplass.mtp.ManagerLocator" %>
<%@ page import="org.iplass.gem.command.generic.detail.DetailViewCommand"%>
<%@ page import="org.iplass.gem.command.generic.search.*" %>
<%@ page import="org.iplass.gem.command.Constants" %>
<%@ page import="org.iplass.gem.command.MenuCommand"%>
<%@ page import="org.iplass.gem.command.ViewUtil"%>

<%!
	boolean isDispProperty(String defName, PropertyDefinition pd, PropertyColumn property) {
		if (!EntityViewUtil.isDisplayElement(defName, property.getElementRuntimeId(), OutputType.SEARCHRESULT)) return false;
		if (property.getEditor() == null) return false;
		return true;
	}
	boolean isDispProperty(PropertyDefinition pd, NestProperty property) {
		if (property.getEditor() == null) return false;
		return true;
	}
%>
<%
	EntityListParts parts = (EntityListParts) request.getAttribute("entityListParts");
	if (parts == null) return;

	SearchFormView form = (SearchFormView) request.getAttribute("searchFormView");

	String topViewListOffsetInfo = request.getParameter(Constants.TOPVIEW_LIST_OFFSET);

	String topViewListOffsetKey = "";
	Integer topViewListOffset = 0;
	if (StringUtil.isNotEmpty(topViewListOffsetInfo)) {
		try {
			String[] info = topViewListOffsetInfo.split("\\.");
			topViewListOffsetKey = info[0];
			topViewListOffset = Integer.parseInt(info[1]);
		} catch (Exception e) {
		}
	}

	String searchType = "normal";
	if (StringUtil.isNotEmpty(parts.getFilterName())) {
		searchType = "fixed";
	}

	EntityDefinitionManager edm = ManagerLocator.getInstance().getManager(EntityDefinitionManager.class);
	EntityDefinition ed = edm.get(parts.getDefName());
	String defName = parts.getDefName();

	SearchResultSection section = form.getResultSection();

	//ビュー名があればアクションの後につける
	String urlPath = ViewUtil.getParamMappingPath(parts.getDefName(), 
			parts.getViewNameForDetail() != null ? parts.getViewNameForDetail() : parts.getViewName());

	//詳細表示アクション
	String view = "";
	if (StringUtil.isNotBlank(form.getViewActionName())) {
		view = form.getViewActionName() +  urlPath;
	} else {
		view = DetailViewCommand.VIEW_ACTION_NAME + urlPath;
	}

	//詳細編集アクション
	String detail = "";
	if (StringUtil.isNotBlank(form.getEditActionName())) {
		detail = form.getEditActionName() +  urlPath;
	} else {
		detail = DetailViewCommand.DETAIL_ACTION_NAME + urlPath;
	}

	//検索結果表示アクション
	urlPath = ViewUtil.getParamMappingPath(parts.getDefName(), parts.getViewNameForLink());
	String action = TemplateUtil.getTenantContextPath() + "/" + SearchViewCommand.SEARCH_ACTION_NAME + urlPath;
	String params = "{";
	params = params + "\"searchType\": \"" + searchType + "\"";
	params = params + ", \"filterName\": \"" + parts.getFilterName() + "\"";
	params = params + "}";

	//Limit
	Integer limit = ViewUtil.getSearchLimit(section);

	//Height
 	Integer gridHeight = parts.getHeight();
	if (gridHeight == null || gridHeight < 0) {
		gridHeight = 160;
	}

	AuthContext auth = AuthContext.getCurrentContext();
	boolean canUpdate = auth.checkPermission(new EntityPermission(ed.getName(), EntityPermission.Action.UPDATE));
	boolean canDelete = auth.checkPermission(new EntityPermission(ed.getName(), EntityPermission.Action.DELETE));
%>
<div class="entity-list topview-parts" id="topview-parts-id_${partsCnt}" style="display:none;">
<h3 class="hgroup-02">
${entityListParts.iconTag}
${m:esc(title)}
</h3>
<%
	String id = ((int)(Math.random() * 1000) + "_" + new Date().getTime());
%>
<form id="form_<c:out value="<%=id%>"/>" class="flat-block-top">
<table id="searchResult_<c:out value="<%=id%>"/>"></table>
</form>
<script type="text/javascript">
$(function() {
	var cellAttrFunc = function (rowId, val, rowObject, colModel, rdata) {
<%
	if (section.isGroupingData()) {
%>
		var rowIndex = parseInt(rowId) - 1;
		var data = grid.getGridParam("_data");
		var row = data[rowIndex];
		var colName = colModel.name;
		if (rowIndex > 0) {
			var beforeRow = data[rowIndex - 1];
			//前の行と値が同じか確認
			var dif = false;
			if (row.orgOid != beforeRow.orgOid || row.orgVersion != beforeRow.orgVersion || row[colName] != beforeRow[colName]) {
				dif = true;
			}
			if (!dif) return " style=\"display:none;\" ";//同じ場合は非表示にする
		}
		//この行から何行分rowspanを設定するか計算
		var count = 0;
		for (var i = rowIndex; i < data.length; i++) {
			if (i >= data.length) break;
			var nextRow = data[i];
			var dif = false;
			if (row.orgOid != nextRow.orgOid || row.orgVersion != nextRow.orgVersion || row[colName] != nextRow[colName]) {
				dif = true;
				break;
			}
			if (!dif) count++;
			else break;
		}
		if (count > 1) return " style=\"vertical-align: center !important;\" rowspan=\"" + count + "\"";
		else return null;
<%
	} else {
%>
		//definitionの設定がfalseなら結合しない
		return null;
<%
	}
%>
	}

	var clearRowHighlight = function(rowIndex) {
		var $rows = $("#searchResult_<%=id%> tr.jqgrow");
		if (rowIndex >= $rows.length) return;
		//選択された行以外にハイライトをクリアします。
		$rows.each(function(index) {
			if (index != rowIndex) $(this).removeClass("ui-state-highlight");
		});
	}

	var colModel = new Array();
	var isloaded = false;
	colModel.push({name:"orgOid", idnex:"orgOid", sortable:false, hidden:true, frozen:true, label:"oid"});
	colModel.push({name:"orgVersion", idnex:"orgVersion", sortable:false, hidden:true, frozen:true, label:"version"});
	colModel.push({name:'_mtpDetailLink', index:'_mtpDetailLink', width:${m:rs("mtp-gem-messages", "generic.search.list.detailLinkWidth")}, sortable:false, align:'center', frozen:true, label:"", classes:"detail-links", cellattr: cellAttrFunc});
<%

	for (Element element : section.getElements()) {
		if (element instanceof PropertyColumn) {
			PropertyColumn property = (PropertyColumn) element;
			String propName = property.getPropertyName();
			PropertyDefinition pd = EntityViewUtil.getPropertyDefinition(propName, ed);
			String displayLabel = TemplateUtil.getMultilingualString(property.getDisplayLabel(), property.getLocalizedDisplayLabelList(), pd.getDisplayName(), pd.getLocalizedDisplayNameList());

			if (isDispProperty(defName, pd, property)) {
				if (!(pd instanceof ReferenceProperty)) {
					String sortPropName = StringUtil.escapeHtml(propName);
					String width = "";
					if (property.getWidth() > 0) {
						width = ", width:" + property.getWidth();
					}
					String align = "";
					if (property.getTextAlign() != null) {
						align = ", align:'" + property.getTextAlign().name().toLowerCase() + "'";
					}
					String style = property.getStyle() != null ? property.getStyle() : "";
					String sortable = "sortable:true";
					if (!ViewUtil.getEntityViewHelper().isSortable(pd)) {
						sortable = "sortable:false";
					}
%>
	colModel.push({name:"<%=sortPropName%>", index:"<%=sortPropName%>", label:"<p class='title'><%=displayLabel%></p>", <%=sortable%><%=width%>, cellattr: cellAttrFunc});
<%
				} else if (property.getEditor() instanceof ReferencePropertyEditor) {
					//参照型のName以外を表示する場合
					List<NestProperty> nest = ((ReferencePropertyEditor) property.getEditor()).getNestProperties();
					if (nest.size() == 0) {
						String sortPropName = StringUtil.escapeHtml(propName);
						String width = "";
						if (property.getWidth() > 0) {
							width = ", width:" + property.getWidth();
						}
						String align = "";
						if (property.getTextAlign() != null) {
							align = ", align:'" + property.getTextAlign().name().toLowerCase() + "'";
						}
						String style = property.getStyle() != null ? property.getStyle() : "";
						String sortable = "sortable:true";
						if (!ViewUtil.getEntityViewHelper().isSortable(pd)) {
							sortable = "sortable:false";
						}
%>
	colModel.push({name:"<%=sortPropName%>", index:"<%=sortPropName%>", label:"<p class='title'><%=displayLabel%></p>", <%=sortable%><%=width%>});
<%
					} else if (nest.size() > 0) {
						String style = property.getStyle() != null ? property.getStyle() : "";
						request.setAttribute("nestPropName", propName);
						request.setAttribute("nestProperty", pd);
						request.setAttribute("nestStyle", style);
						request.setAttribute("nestEditor", property.getEditor());
%>
<jsp:include page="../element/section/SearchResultSection_Nest.jsp" />
<%
						request.removeAttribute("nestPropName");
						request.removeAttribute("nestProperty");
						request.removeAttribute("nestStyle");
						request.removeAttribute("nestEditor");
						request.removeAttribute("frozenColNum");
					}
				}
			}
		} else if (element instanceof VirtualPropertyItem) {
			VirtualPropertyItem property = (VirtualPropertyItem) element;
			String propName = StringUtil.escapeHtml(property.getPropertyName());
			String displayLabel = TemplateUtil.getMultilingualString(property.getDisplayLabel(), property.getLocalizedDisplayLabelList());
			String width = "";
			if (property.getWidth() > 0) {
				width = ", width:" + property.getWidth();
			}
			String align = "";
			if (property.getTextAlign() != null) {
				align = ", align:'" + property.getTextAlign().name().toLowerCase() + "'";
			}
			String style = property.getStyle() != null ? property.getStyle() : "";
%>
<%-- XSS対応-メタの設定のため対応なし(displayLabel,style) --%>
colModel.push({name:"<%=propName%>", index:"<%=propName%>", classes:"<%=style%>", label:"<p class='title'><%=displayLabel%></p>", sortable:false <%=width%><%=align%>});
<%
		}
	}
%>
	var $table = $("#searchResult_<%=id%>");
	var gridHeight = <%=gridHeight%>;
	var grid = $table.jqGrid({
		datatype: "local",
		colModel: colModel,
		headertitles: true,
		height: gridHeight,
		multiselect: false,
		caption: "Manipulating Array Data",
		viewrecords: true,
		altRows: true,
		altclass:'myAltRowClass',
		onSortCol: function(index, iCol, sortorder) {
			var sortKey = index;
			var sortType = sortorder.toUpperCase();

			var curSortKey = $table.attr("data-sortKey");
			var curSortType = $table.attr("data-sortType");

			<%-- アイコンは表示されていない可能性があるので必ずやる --%>
			$("#gview_searchResult_<%=id%> tr.ui-jqgrid-labels th .ui-jqgrid-sortable").removeClass('asc desc');
			$("#gview_searchResult_<%=id%> tr.ui-jqgrid-labels th:eq(" + iCol + ") .ui-jqgrid-sortable").addClass(sortType.toLowerCase());

			<%-- ソート条件に変更がある場合のみ実施
				(結果表示用のsetData関数でsortGrid呼び出しによって発生するため) --%>
			if (sortKey !== curSortKey || sortType !== curSortType) {
				$table.attr("data-sortKey", sortKey);
				$table.attr("data-sortType", sortType);
				search();
			}
			return "stop";
		}
		,onSelectRow: function(rowid, e) {
			var row = grid.getRowData(rowid);
			var id = row.orgOid + "_" + row.orgVersion;
			var rowIndex = parseInt(rowid) - 1;

			clearRowHighlight(rowIndex);

			if (e) {
				$("#searchResult_<%=id%> tr[id]").each(function() {
					var _rowid = $(this).attr("id");
					if (_rowid == rowid) return;
					var _row = grid.getRowData(_rowid);
					var _id = _row.orgOid + "_" + _row.orgVersion;
					if (id == _id) $(this).addClass("ui-state-highlight");
				});
			}
		}
	});

	var offset = 0;
	var limit = <%=limit%>;


	var $parent = $table.parents("div.entity-list");
	var idname = $parent.attr("id");

	var topViewListOffset = <%= topViewListOffset %>;
	var topViewListOffsetKey = "<%= StringUtil.escapeJavaScript(topViewListOffsetKey) %>";
	if (topViewListOffset != 0) {
		if (idname == topViewListOffsetKey) {
			offset = topViewListOffset;
		}
	}


	var $pager = $(".result-nav", $parent).pager({
		limit: limit,
		showPageLink: false,
		showPageJump: false,
		showItemCount: false,
		previewFunc: function(){
			offset -= limit;
			search();
		},
		nextFunc: function() {
			offset += limit;
			search();
		}
	});

	search();

	function search() {
		var sortKey = $table.attr("data-sortKey");
		var sortType = $table.attr("data-sortType");
		searchEntityList("<%=SearchListCommand.WEBAPI_NAME%>", "${m:escJs(entityListParts.defName)}", "${m:escJs(entityListParts.viewName)}", "${m:escJs(entityListParts.filterName)}", offset, sortKey, sortType, function(count, list) {
			$pager.setPage(offset, list.length, count);

			grid.clearGridData(true);
			grid.setGridParam({"_data": list}).trigger("reloadGrid");
			$(list).each(function(index) {

				this["searchResultDataId"] = this.orgOid + "_" + this.orgVersion;
<% if (!section.isHideDetailLink() && (canUpdate || canDelete)) { %>
				this["_mtpDetailLink"] = "<a href='javascript:void(0)' action='<%=StringUtil.escapeJavaScript(view)%>' oid='" + this.orgOid + "' version='" + this.orgVersion + "' class='jqborder detailLink'>${m:rs('mtp-gem-messages', 'generic.element.section.SearchResultSection.detail')}</a><a href='javascript:void(0)' action='<%=StringUtil.escapeJavaScript(detail)%>' oid='" + this.orgOid + "' version='" + this.orgVersion + "' class='detailLink editLink'>${m:rs('mtp-gem-messages', 'generic.element.section.SearchResultSection.edit')}</a>";
<% } else { %>
				this["_mtpDetailLink"] = "<a href='javascript:void(0)' action='<%=StringUtil.escapeJavaScript(view)%>' oid='" + this.orgOid + "' version='" + this.orgVersion + "' class='detailLink'>${m:rs('mtp-gem-messages', 'generic.element.section.SearchResultSection.detail')}</a>";
<% } %>
				grid.addRowData(index + 1, this);
			});

			var option = {
				"<%=Constants.BACK_PATH%>":"<%=MenuCommand.ACTION_NAME%>"
				,"<%=Constants.TOPVIEW_LIST_OFFSET%>":idname + "." + offset
			};
			$(".detailLink", $table).click(function(e) {
				var action = $(this).attr("action");
				var oid = $(this).attr("oid");
				var version = $(this).attr("version");
				var isEdit = $(this).is(".editLink");
				if (e.ctrlKey) {
					showDetail(action, oid, version, isEdit, "_blank", option);
				} else {
					showDetail(action, oid, version, isEdit, null, option);
				}
				return false;
			});

			var isSubModal = $("body.modal-body").length != 0;
			if (isSubModal) {
				var a = $("#searchResult_<%=id%> .modal-lnk");
				a.subModalWindow();
			} else {
				var a = $("#searchResult_<%=id%> .modal-lnk");
				a.modalWindow();
			}

			$("#topview-parts-id_${partsCnt}").show();
			$(".fixHeight").fixHeight();
		});
	}
});
</script>
<ul class="link-list-01 entity-list">
<li class="list-paging">
<div class="result-nav"></div>
</li>
<li class="list-all"><a href="javascript:void(0)" onclick='submitForm("<%=StringUtil.escapeJavaScript(action)%>", <%=params%>)'>${m:rs("mtp-gem-messages", "generic.search.list.showSearch")}</a></li>
</ul>
</div>

/*
 * Copyright (C) 2012 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
 *
 * Unless you have purchased a commercial license,
 * the following license terms apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.iplass.gem.command.generic.detail;

import java.util.List;

import org.iplass.gem.command.Constants;
import org.iplass.gem.command.GemResourceBundleUtil;
import org.iplass.mtp.ApplicationException;
import org.iplass.mtp.auth.AuthContext;
import org.iplass.mtp.auth.User;
import org.iplass.mtp.command.RequestContext;
import org.iplass.mtp.command.annotation.CommandClass;
import org.iplass.mtp.command.annotation.CommandConfig;
import org.iplass.mtp.command.annotation.action.ActionMapping;
import org.iplass.mtp.command.annotation.action.ActionMappings;
import org.iplass.mtp.command.annotation.action.ParamMapping;
import org.iplass.mtp.command.annotation.action.Result;
import org.iplass.mtp.command.annotation.action.Result.Type;
import org.iplass.mtp.command.annotation.template.Template;
import org.iplass.mtp.command.annotation.template.Templates;
import org.iplass.mtp.entity.BinaryReference;
import org.iplass.mtp.entity.Entity;
import org.iplass.mtp.entity.EntityValidationException;
import org.iplass.mtp.entity.GenericEntity;
import org.iplass.mtp.entity.ValidateError;
import org.iplass.mtp.entity.definition.PropertyDefinition;
import org.iplass.mtp.entity.definition.properties.BinaryProperty;
import org.iplass.mtp.entity.definition.properties.ReferenceProperty;
import org.iplass.mtp.entity.definition.properties.ReferenceType;
import org.iplass.mtp.util.StringUtil;
import org.iplass.mtp.view.generic.DetailFormView;
import org.iplass.mtp.view.generic.DetailFormView.CopyTarget;

/**
 * 詳細画面表示用コマンド
 * @author lis3wg
 */
@ActionMappings({
	@ActionMapping(name=DetailViewCommand.VIEW_ACTION_NAME,
		displayName="詳細表示",
		paramMapping={
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${0}", condition="subPath.length==2"),
			@ParamMapping(name=Constants.OID, mapFrom="${1}", condition="subPath.length==2"),
			@ParamMapping(name=Constants.VIEW_NAME, mapFrom="${0}", condition="subPath.length==3"),
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${1}", condition="subPath.length==3"),
			@ParamMapping(name=Constants.OID, mapFrom="${2}", condition="subPath.length==3")
		},
		command=@CommandConfig(commandClass=DetailViewCommand.class, value="cmd.detail=false;"),
		result={
			@Result(status=Constants.CMD_EXEC_SUCCESS, type=Type.TEMPLATE, value=Constants.TEMPLATE_VIEW),
			@Result(status=Constants.CMD_EXEC_ERROR_LOCK, type=Type.TEMPLATE, value=Constants.TEMPLATE_VIEW),
			@Result(status=Constants.CMD_EXEC_ERROR_VIEW, type=Type.TEMPLATE, value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_NORMAL_ACTION),
			@Result(status=Constants.CMD_EXEC_ERROR_NODATA,type=Type.TEMPLATE, value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_NORMAL_ACTION)
		}
	),
	@ActionMapping(name=DetailViewCommand.REF_VIEW_ACTION_NAME,
		displayName="参照詳細表示",
		paramMapping={
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${0}", condition="subPath.length==2"),
			@ParamMapping(name=Constants.OID, mapFrom="${1}", condition="subPath.length==2"),
			@ParamMapping(name=Constants.VIEW_NAME, mapFrom="${0}", condition="subPath.length==3"),
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${1}", condition="subPath.length==3"),
			@ParamMapping(name=Constants.OID, mapFrom="${2}", condition="subPath.length==3")
		},
		command=@CommandConfig(commandClass=DetailViewCommand.class, value="cmd.detail=false;"),
		result={
			@Result(status=Constants.CMD_EXEC_SUCCESS, type=Type.TEMPLATE, value=Constants.TEMPLATE_REF_VIEW),
			@Result(status=Constants.CMD_EXEC_ERROR_LOCK, type=Type.TEMPLATE, value=Constants.TEMPLATE_REF_VIEW),
			@Result(status=Constants.CMD_EXEC_ERROR_VIEW, type=Type.TEMPLATE,
					value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_POPOUT_ACTION),
			@Result(status=Constants.CMD_EXEC_ERROR_NODATA, type=Type.TEMPLATE,
					value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_POPOUT_ACTION)
		}
	),
	@ActionMapping(name=DetailViewCommand.DETAIL_ACTION_NAME,
		displayName="詳細編集",
		paramMapping={
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${0}", condition="subPath.length==1"),
			@ParamMapping(name=Constants.VIEW_NAME, mapFrom="${0}", condition="subPath.length==2"),
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${1}", condition="subPath.length==2")
		},
		command=@CommandConfig(commandClass=DetailViewCommand.class, value="cmd.detail=true;"),
		result={
			@Result(status=Constants.CMD_EXEC_SUCCESS, type=Type.TEMPLATE, value=Constants.TEMPLATE_EDIT),
			@Result(status=Constants.CMD_EXEC_ERROR_LOCK, type=Type.TEMPLATE, value=Constants.TEMPLATE_VIEW),
			@Result(status=Constants.CMD_EXEC_ERROR_VALIDATE, type=Type.TEMPLATE, value=Constants.TEMPLATE_VIEW),
			@Result(status=Constants.CMD_EXEC_ERROR_VIEW, type=Type.TEMPLATE, value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_NORMAL_ACTION),
			@Result(status=Constants.CMD_EXEC_ERROR_NODATA, type=Type.TEMPLATE, value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_NORMAL_ACTION)
		}
	),
	@ActionMapping(name=DetailViewCommand.REF_DETAIL_ACTION_NAME,
		displayName="参照詳細編集",
		paramMapping={
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${0}", condition="subPath.length==1"),
			@ParamMapping(name=Constants.VIEW_NAME, mapFrom="${0}", condition="subPath.length==2"),
			@ParamMapping(name=Constants.DEF_NAME, mapFrom="${1}", condition="subPath.length==2")
		},
		command=@CommandConfig(commandClass=DetailViewCommand.class, value="cmd.detail=true;"),
		result={
			@Result(status=Constants.CMD_EXEC_SUCCESS, type=Type.TEMPLATE, value=Constants.TEMPLATE_REF_EDIT),
			@Result(status=Constants.CMD_EXEC_ERROR_LOCK, type=Type.TEMPLATE, value=Constants.TEMPLATE_REF_VIEW),
			@Result(status=Constants.CMD_EXEC_ERROR_VIEW, type=Type.TEMPLATE,
					value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_POPOUT_ACTION),
			@Result(status=Constants.CMD_EXEC_ERROR_NODATA, type=Type.TEMPLATE,
					value=Constants.TEMPLATE_COMMON_ERROR,
					layoutActionName=Constants.LAYOUT_POPOUT_ACTION)
		}
	)
})
@CommandClass(name="gem/generic/detail/DetailViewCommand", displayName="詳細表示")
@Templates({
	@Template(
			name=Constants.TEMPLATE_VIEW,
			path=Constants.CMD_RSLT_JSP_VIEW,
			layoutActionName=Constants.LAYOUT_NORMAL_ACTION
	),
	@Template(
			name=Constants.TEMPLATE_REF_VIEW,
			path=Constants.CMD_RSLT_JSP_REF_VIEW,
			layoutActionName=Constants.LAYOUT_POPOUT_ACTION
	),
	@Template(
			name=Constants.TEMPLATE_EDIT,
			path=Constants.CMD_RSLT_JSP_EDIT,
			layoutActionName=Constants.LAYOUT_NORMAL_ACTION
	),
	@Template(
			name=Constants.TEMPLATE_REF_EDIT,
			path=Constants.CMD_RSLT_JSP_REF_EDIT,
			layoutActionName=Constants.LAYOUT_POPOUT_ACTION
	),
	@Template(
			name=Constants.TEMPLATE_COMPLETED,
			path=Constants.CMD_RSLT_JSP_COMPLETED,
			contentType="text/html; charset=utf-8"
	)
})
public final class DetailViewCommand extends DetailCommandBase {

	public static final String VIEW_ACTION_NAME = "gem/generic/detail/view";

	public static final String REF_VIEW_ACTION_NAME = "gem/generic/detail/ref/view";

	public static final String DETAIL_ACTION_NAME = "gem/generic/detail/edit";

	public static final String REF_DETAIL_ACTION_NAME = "gem/generic/detail/ref/edit";

	private boolean detail;

	public boolean isDetail() {
		return detail;
	}

	public void setDetail(boolean detail) {
		this.detail = detail;
	}

	/**
	 * コンストラクタ
	 */
	public DetailViewCommand() {
		super();
	}

	@Override
	public String execute(RequestContext request) {
		DetailCommandContext context = getContext(request);

		//必要なパラメータ取得
		String oid = context.getOid();
		Long version = context.getVersion();
		String searchCond = context.getSearchCond();
		if(oid == null || oid.length() == 0) {
			// SearchCommandからのChainの可能性があるので、Attributeから取得する
			oid = (String) request.getAttribute(Constants.OID);
		}

		//各種定義取得
		DetailFormView view = context.getView();
		if (view == null) {
			request.setAttribute(Constants.MESSAGE, resourceString("command.generic.detail.DetailViewCommand.viewErr"));
			return Constants.CMD_EXEC_ERROR_VIEW;
		}

		Entity entity = null;
		if (oid != null && oid.trim().length() > 0) {
			if (view.isLoadDefinedReferenceProperty()) {
				entity = loadViewEntity(context, oid, version, context.getDefinitionName(), context.getReferencePropertyName());
			} else {
				entity = loadViewEntity(context, oid, version, context.getDefinitionName(), (List<String>) null);
			}
			if (entity == null) {
				request.setAttribute(Constants.MESSAGE, resourceString("command.generic.detail.DetailViewCommand.noPermission"));
				return Constants.CMD_EXEC_ERROR_NODATA;
			}
		}

		//画面で利用するデータ設定
		DetailFormViewData data = new DetailFormViewData();
		data.setEntityDefinition(context.getEntityDefinition());
		data.setView(context.getView());
		data.setEntity((GenericEntity) entity);
		if (entity == null) {
			data.setExecType(Constants.EXEC_TYPE_INSERT);
			if (context.isUpdateByParam()) {
				//パラメータの値を使って表示データの値更新
				Entity updateParam = context.createEntity();
				data.setEntity((GenericEntity) updateParam);
			}
			if (StringUtil.isNotBlank(context.getView().getInitScript())) {
				//初期化スクリプトで初期化
				if (data.getEntity() != null) {
					evm.initEntity(context.getDefinitionName(), context.getViewName(), data.getEntity());
				} else {
					Entity initEntity = evm.initEntity(context.getDefinitionName(), context.getViewName(), null);
					data.setEntity(initEntity);
				}
			}
		} else {
			data.setExecType(Constants.EXEC_TYPE_UPDATE);
			if (context.isUpdateByParam()) {
				//パラメータの値を使って表示データの値更新
				Entity updateParam = context.createEntity();
				for (PropertyDefinition pd : context.getEntityDefinition().getPropertyList()) {
					Object updateValue = updateParam.getValue(pd.getName());
					if (updateValue != null) {
						entity.setValue(pd.getName(), updateValue);
					}
				}
			}
		}

		String ret = Constants.CMD_EXEC_SUCCESS;
		if (context.isCopy()) {
			if (context.getCopyTarget() == CopyTarget.SHALLOW) {
				//コピーの場合はoid、作成者とかを消す
				//→参照型はASSOCIATIONで逆参照でないもの以外は消す
				initCopyProperty(context, data.getEntity());
				data.setExecType(Constants.EXEC_TYPE_INSERT);
			} else if (context.getCopyTarget() == CopyTarget.DEEP) {
				try {
					// ディープコピーの場合はデータ登録を行う
					entity = em.deepCopy(oid, context.getDefinitionName());
					data.setEntity((GenericEntity) entity);
					data.setExecType(Constants.EXEC_TYPE_UPDATE);
				} catch (EntityValidationException e) {
					String refPropName = createHierarchyRefPropDispName(e);
					request.setAttribute(Constants.MESSAGE, resourceString("command.generic.detail.DetailViewCommand.failedDeepCopy", refPropName));
					ret = Constants.CMD_EXEC_ERROR_VALIDATE;
				}
			} else if (context.getCopyTarget() == CopyTarget.CUSTOM) {
				Entity _entity = evm.copyEntity(context.getViewName(), entity);
				if (_entity != null) {
					if (_entity.getOid() == null) {
						data.setExecType(Constants.EXEC_TYPE_INSERT);
					} else {
						data.setExecType(Constants.EXEC_TYPE_UPDATE);
					}
					data.setEntity(_entity);
				} else {
					throw new ApplicationException(resourceString("command.generic.detail.DetailViewCommand.failedCopy"));
				}
			} else {
				request.setAttribute(Constants.MESSAGE, resourceString("command.generic.detail.DetailViewCommand.illegalCopy"));
				ret = Constants.CMD_EXEC_ERROR_LOCK;
				data.setExecType(Constants.EXEC_TYPE_INSERT);
			}
		} else {
			//コピーじゃない場合はロック状態確認
			if (entity != null && entity.getLockedBy() != null) {
				String userOid = AuthContext.getCurrentContext().getUser().getOid();
				if (!entity.getLockedBy().equals(userOid)) {
					//他ユーザによるロックあり
					Entity user = loadEntityWithoutReference(entity.getLockedBy(), null, User.DEFINITION_NAME);
					String message = null;
					if (user == null) {
						message = resourceString("command.generic.detail.DetailViewCommand.lockedOtherUser");
					} else {
						message = user.getName() + resourceString("command.generic.detail.DetailViewCommand.lockedSpecificUser");
					}
					request.setAttribute(Constants.MESSAGE, message);
					ret = Constants.CMD_EXEC_ERROR_LOCK;
				}
			}
		}

		//UserPropertyEditor用のマップ作製
		if (data.getEntity() != null) {
			setUserInfoMap(context, data.getEntity(), detail);
		}

		request.setAttribute(Constants.DATA, data);
		request.setAttribute(Constants.SEARCH_COND, searchCond);
		return ret;
	}

	/**
	 * コピー不可項目を初期化します
	 * @param ed Entity定義
	 * @param entity Entity
	 */
	protected void initCopyProperty(DetailCommandContext context, Entity entity) {
		if (entity == null) return;
		for (PropertyDefinition pd : context.getPropertyList()) {
			boolean isReset = false;
			//変更不可項目なら初期化
			if (!pd.isUpdatable()) isReset = true;
			//参照型でCOMPOSITIONか逆参照なら初期化
			if (!isReset && pd instanceof ReferenceProperty) {
				ReferenceProperty rp = (ReferenceProperty) pd;
				if (rp.getReferenceType() == ReferenceType.COMPOSITION || rp.getMappedBy() != null) {
					isReset = true;
				}
			}

			if (isReset) entity.setValue(pd.getName(), null);
			else {
				//バイナリの場合はデータもコピー
				if (pd instanceof BinaryProperty) {
					Object value = null;
					if (pd.getMultiplicity() == 1) {
						BinaryReference br = entity.getValue(pd.getName());
						if (br != null) value = copyBinary(br);
					} else {
						BinaryReference[] br = entity.getValue(pd.getName());
						if (br != null && br.length > 0) {
							BinaryReference[] _br = new BinaryReference[br.length];
							for (int i = 0; i < br.length; i++) {
								_br[i] = copyBinary(br[i]);
							}
							value = _br;
						}
					}
					entity.setValue(pd.getName(), value);
				}
			}
		}
	}
	private BinaryReference copyBinary(BinaryReference br) {
		return em.createBinaryReference(br.getName(), br.getType(), em.getInputStream(br));
	}

	private static String resourceString(String key, Object... arguments) {
		return GemResourceBundleUtil.resourceString(key, arguments);
	}

	private String createHierarchyRefPropDispName(EntityValidationException e) {
		StringBuilder builder = new StringBuilder();
		for (ValidateError err : e.getValidateResults()) {
			builder.append(err.getPropertyDisplayName() + ".");
		}
		builder.deleteCharAt(builder.length() - 1);
		return StringUtil.reverseDelimited(builder.toString(), '.');
	}
}

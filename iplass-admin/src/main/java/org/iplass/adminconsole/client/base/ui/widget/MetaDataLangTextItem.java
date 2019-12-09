/*
 * Copyright (C) 2019 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
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

package org.iplass.adminconsole.client.base.ui.widget;

import java.util.ArrayList;
import java.util.List;

import org.iplass.adminconsole.client.base.i18n.AdminClientMessageUtil;
import org.iplass.adminconsole.client.metadata.ui.common.LocalizedStringSettingDialog;
import org.iplass.mtp.definition.LocalizedStringDefinition;

import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;

public class MetaDataLangTextItem extends TextItem {

	private List<LocalizedStringDefinition> localizedList;

	public MetaDataLangTextItem() {
		this(true);
	}
	public MetaDataLangTextItem(boolean showLang) {
		super();

		if (showLang) {
			FormItemIcon icon = new FormItemIcon();
			icon.setSrc(CommonIconConstants.COMMON_ICON_LANG);
			icon.addFormItemClickHandler(new FormItemClickHandler() {

				@Override
				public void onFormItemClick(FormItemIconClickEvent event) {

					if (localizedList == null) {
						localizedList = new ArrayList<LocalizedStringDefinition>();
					}
					LocalizedStringSettingDialog dialog = new LocalizedStringSettingDialog(localizedList);
					dialog.show();
				}
			});
			icon.setPrompt(AdminClientMessageUtil.getString("ui_metadata_common_MetaCommonAttributeSection_eachLangDspName"));
			icon.setBaseStyle("adminButtonRounded");
			setIcons(icon);
		}
	}

	public List<LocalizedStringDefinition> getLocalizedList() {
		return localizedList != null ? localizedList.isEmpty() ? null : localizedList : null;
	}

	public void setLocalizedList(List<LocalizedStringDefinition> localizedList) {
		this.localizedList = localizedList;
	}

}

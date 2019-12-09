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

package org.iplass.adminconsole.client.metadata.ui.top.item;

import org.iplass.adminconsole.client.base.event.MTPEvent;
import org.iplass.adminconsole.client.base.ui.widget.MetaDataLangTextItem;
import org.iplass.adminconsole.client.base.ui.widget.MtpDialog;
import org.iplass.adminconsole.client.base.ui.widget.form.MtpForm;
import org.iplass.adminconsole.client.base.util.SmartGWTUtil;
import org.iplass.adminconsole.client.metadata.ui.top.PartsOperationHandler;
import org.iplass.mtp.view.top.parts.ApplicationMaintenanceParts;

import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.widgets.HeaderControl;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;

public class ApplicationMaintenanceItem extends PartsItem {

	private PartsOperationHandler controler;

	private ApplicationMaintenanceParts parts;

	/**
	 * コンストラクタ
	 */
	public ApplicationMaintenanceItem(ApplicationMaintenanceParts parts, PartsOperationHandler controler) {
		this.parts = parts;
		this.controler = controler;
		setTitle("Application Maintenance");
		setBackgroundColor("#909090");

		setHeaderControls(HeaderControls.HEADER_LABEL, new HeaderControl(HeaderControl.SETTINGS, new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				ApplicationMaintenanceItemSettingDialog dialog = new ApplicationMaintenanceItemSettingDialog();
				dialog.show();
			}
		}), HeaderControls.CLOSE_BUTTON);
	}

	@Override
	public ApplicationMaintenanceParts getParts() {
		return parts;
	}

	@Override
	protected boolean onPreDestroy() {
		MTPEvent e = new MTPEvent();
		e.setValue("key", dropAreaType + "_" + ApplicationMaintenanceParts.class.getName() + "_");
		controler.remove(e);
		return true;
	}

	private class ApplicationMaintenanceItemSettingDialog extends MtpDialog {

		private MetaDataLangTextItem txtTitle;

		/**
		 * コンストラクタ
		 */
		public ApplicationMaintenanceItemSettingDialog() {

			setTitle("Application Maintenance");
			setHeight(130);
			centerInPage();

			final DynamicForm form = new MtpForm();
			form.setAutoFocus(true);

			txtTitle = new MetaDataLangTextItem();
			txtTitle.setTitle("Title");
			txtTitle.setValue(parts.getTitle());
			txtTitle.setLocalizedList(parts.getLocalizedTitleList());

			form.setItems(txtTitle);

			container.addMember(form);

			IButton save = new IButton("OK");
			save.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					if (form.validate()){
						//入力情報をパーツに
						parts.setTitle(SmartGWTUtil.getStringValue(txtTitle));
						parts.setLocalizedTitleList(txtTitle.getLocalizedList());
						destroy();
					}
				}
			});

			IButton cancel = new IButton("Cancel");
			cancel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					destroy();
				}
			});

			footer.setMembers(save, cancel);
		}

	}
}

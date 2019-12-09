/*
 * Copyright (C) 2011 INFORMATION SERVICES INTERNATIONAL - DENTSU, LTD. All Rights Reserved.
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

package org.iplass.adminconsole.client.metadata.ui.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.iplass.adminconsole.client.base.i18n.AdminClientMessageUtil;
import org.iplass.adminconsole.client.base.ui.widget.AbstractWindow;
import org.iplass.adminconsole.client.base.ui.widget.ScriptEditorDialogCondition;
import org.iplass.adminconsole.client.base.ui.widget.ScriptEditorDialogHandler;
import org.iplass.adminconsole.client.base.ui.widget.ScriptEditorDialogMode;
import org.iplass.adminconsole.client.base.util.SmartGWTUtil;
import org.iplass.adminconsole.client.metadata.data.MetaDataNameDS;
import org.iplass.adminconsole.client.metadata.data.MetaDataNameDS.MetaDataNameDSOption;
import org.iplass.adminconsole.client.metadata.ui.MetaDataUtil;
import org.iplass.mtp.entity.definition.EntityDefinition;
import org.iplass.mtp.entity.definition.EventListenerDefinition;
import org.iplass.mtp.entity.definition.listeners.EventType;
import org.iplass.mtp.entity.definition.listeners.JavaClassEventListenerDefinition;
import org.iplass.mtp.entity.definition.listeners.ScriptingEventListenerDefinition;
import org.iplass.mtp.entity.definition.listeners.SendNotificationEventListenerDefinition;
import org.iplass.mtp.entity.definition.listeners.SendNotificationType;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 *
 * @author lis2s8
 *
 */
public class EventListenerListGrid extends ListGrid {

	private final String SCRIPT = "Script";
	private final String JAVACLASS = "JavaClass";
	private final String SENDNOTIFICATION = "SendNotification";

	/**
	 * コンストラクタ
	 *
	 * @param defName 選択Entity名
	 * @param service Service
	 */
	public EventListenerListGrid() {
		// 横幅を画面の100%に
		setWidth100();
		// 縦幅
		setHeight(1);
		// 列を全て表示
		setShowAllColumns(true);
		// レコードを全て表示
		setShowAllRecords(true);
		// 列幅変更可能
		setCanResizeFields(true);
		setBodyOverflow(Overflow.VISIBLE);
		setOverflow(Overflow.VISIBLE);
		setCanSort(false);

		//grid内でのD&Dでの並べ替えを許可
		setCanDragRecordsOut(true);
		setCanAcceptDroppedRecords(true);
		setCanReorderRecords(true);

		// 各フィールド初期化
		ListGridField elNameField = new ListGridField(EventListenerListGridRecord.ELNAME, "Type");
		elNameField.setWidth(150);
		ListGridField gpField = new ListGridField(EventListenerListGridRecord.GP, "Value");

		// 各フィールドをListGridに設定
		setFields(elNameField, gpField);

		// レコードダブルクリックイベント設定
		addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
			public void onRecordDoubleClick(RecordDoubleClickEvent event) {
				startEventListenerEdit(false, (EventListenerListGridRecord)getRecord(event.getRecordNum()));
			}
		});
	}

	public void setDefinition(EntityDefinition definition) {
		List<EventListenerDefinition> lstElDef = definition.getEventListenerList();

		if (lstElDef == null) { return; }

		int size = lstElDef.size();
		EventListenerListGridRecord[] records = new EventListenerListGridRecord[size];

		for (int i = 0; i < size; i++) {

			EventListenerListGridRecord record = new EventListenerListGridRecord();
			EventListenerDefinition elDef = lstElDef.get(i);

			setElDefData(record, elDef);
			records[i] = record;
		}

		setData(records);
	}

	public EntityDefinition getEditDefinition(EntityDefinition definition) {
		List<EventListenerDefinition> lstResult = new ArrayList<EventListenerDefinition>();

		ListGridRecord[] records = getRecords();

		for (ListGridRecord record : records) {
			EventListenerDefinition elDef = createElDef((EventListenerListGridRecord)record);
			lstResult.add(elDef);
		}
		definition.setEventListenerList(lstResult);
		return definition;
	}

	private void setElDefData(EventListenerListGridRecord record, EventListenerDefinition elDef) {
		if (elDef instanceof ScriptingEventListenerDefinition) {
			record.setElName(SCRIPT);

			ScriptingEventListenerDefinition sDef = (ScriptingEventListenerDefinition)elDef;
			// Script文字列
			record.setScript(sDef.getScript());
			record.setGeneralPurpus(sDef.getScript());

			// 実行タイミングの各チェックボックス
			List<EventType> lstEType = sDef.getListenEvent();
			if (lstEType != null) {
				for (EventType eType : lstEType) {
					switch (eType) {
						case AFTER_DELETE:
							record.setAfterD(true);
							break;
						case AFTER_INSERT:
							record.setAfterI(true);
							break;
						case AFTER_UPDATE:
							record.setAfterU(true);
							break;
						case BEFORE_DELETE:
							record.setBeforeD(true);
							break;
						case BEFORE_INSERT:
							record.setBeforeI(true);
							break;
						case BEFORE_UPDATE:
							record.setBeforeU(true);
							break;
						case AFTER_RESTORE:
							record.setAfterR(true);
							break;
						case AFTER_PURGE:
							record.setAfterP(true);
							break;
						case ON_LOAD:
							record.setOnLoad(true);
							break;
						case BEFORE_VALIDATE:
							record.setBeforeValidate(true);
						default:
							break;
					}
				}
			}
		} else if (elDef instanceof JavaClassEventListenerDefinition) {
			record.setElName(JAVACLASS);

			JavaClassEventListenerDefinition jcDef = (JavaClassEventListenerDefinition)elDef;
			record.setClassName(jcDef.getClassName());
			record.setGeneralPurpus(jcDef.getClassName());
//		} else if (elDef instanceof OutboundEventListenerDefinition) {
//			// TODO OutboundEventListenerの場合
//			record.setElName(OUTBOUND);
//			OutboundEventListenerDefinition oDef = (OutboundEventListenerDefinition)elDef;
		} else if (elDef instanceof SendNotificationEventListenerDefinition) {
			record.setElName(SENDNOTIFICATION);

			SendNotificationEventListenerDefinition snDef = (SendNotificationEventListenerDefinition)elDef;
			record.setNotificationType(snDef.getNotificationType().name());
			record.setGeneralPurpus(snDef.getNotificationType().name());
			record.setTmplDefName(snDef.getTmplDefName());
			record.setNotificationCondScript(snDef.getNotificationCondScript());
			
			List<EventType> lstEType = snDef.getListenEvent();
			if (lstEType != null) {
				for (EventType eType : lstEType) {
					switch(eType) {
						case AFTER_DELETE:
							record.setNotifyAfterD(true);
							break;
						case AFTER_INSERT:
							record.setNotifyAfterI(true);
							break;
						case AFTER_UPDATE:
							record.setNotifyAfterU(true);
							break;
						case BEFORE_DELETE:
							record.setNotifyBeforeD(true);
							break;
						case BEFORE_INSERT:
							record.setNotifyBeforeI(true);
							break;
						case BEFORE_UPDATE:
							record.setNotifyBeforeU(true);
							break;
						case AFTER_RESTORE:
							record.setNotifyAfterR(true);
							break;
						case AFTER_PURGE:
							record.setNotifyAfterP(true);
							break;
						case ON_LOAD:
							record.setNotifyOnLoad(true);
							break;
						case BEFORE_VALIDATE:
							record.setNotifyBeforeValidate(true);
						default:
							break;
					}
				}
			}
			
		} else {
		}
		record.setWithoutMappedByReference(elDef.isWithoutMappedByReference());
	}

	/**
	 *
	 * @param record
	 * @return
	 */
	private EventListenerDefinition createElDef(EventListenerListGridRecord record) {
		EventListenerDefinition result = null;
		String elName = record.getElName();

		if (elName != null && elName.length() > 0) {
			if (SCRIPT.equals(elName)) {
				ScriptingEventListenerDefinition sDef = new ScriptingEventListenerDefinition();
				sDef.setScript(record.getScript());

				List<EventType> lstEType = new ArrayList<EventType>();
				if (record.isAfterD()) { lstEType.add(EventType.AFTER_DELETE); }
				if (record.isAfterI()) { lstEType.add(EventType.AFTER_INSERT); }
				if (record.isAfterU()) { lstEType.add(EventType.AFTER_UPDATE); }
				if (record.isBeforeD()) { lstEType.add(EventType.BEFORE_DELETE); }
				if (record.isBeforeI()) { lstEType.add(EventType.BEFORE_INSERT); }
				if (record.isBeforeU()) { lstEType.add(EventType.BEFORE_UPDATE); }
				if (record.isAfterR()) { lstEType.add(EventType.AFTER_RESTORE); }
				if (record.isAfterP()) { lstEType.add(EventType.AFTER_PURGE); }
				if (record.isOnLoad()) { lstEType.add(EventType.ON_LOAD); }
				if (record.isBeforeValidate()) { lstEType.add(EventType.BEFORE_VALIDATE); }
				sDef.setListenEvent(lstEType);

				result = sDef;
			} else if (JAVACLASS.equals(elName)) {
				JavaClassEventListenerDefinition jDef = new JavaClassEventListenerDefinition();
				jDef.setClassName(record.getClassName());

				result = jDef;
//			} else if (OUTBOUND.equals(elName)) {
//
			} else if (SENDNOTIFICATION.equals(elName)) {
				SendNotificationEventListenerDefinition snDef = new SendNotificationEventListenerDefinition();
				snDef.setNotificationType(SendNotificationType.valueOf(record.getNotificationType()));
				snDef.setTmplDefName(record.getTmplDefName());
				snDef.setNotificationCondScript(record.getNotificationCondScript());

				List<EventType> lstEType = new ArrayList<EventType>();
				if (record.isNotifyAfterD()) { lstEType.add(EventType.AFTER_DELETE); }
				if (record.isNotifyAfterI()) { lstEType.add(EventType.AFTER_INSERT); }
				if (record.isNotifyAfterU()) { lstEType.add(EventType.AFTER_UPDATE); }
				if (record.isNotifyBeforeD()) { lstEType.add(EventType.BEFORE_DELETE); }
				if (record.isNotifyBeforeI()) { lstEType.add(EventType.BEFORE_INSERT); }
				if (record.isNotifyBeforeU()) { lstEType.add(EventType.BEFORE_UPDATE); }
				if (record.isNotifyAfterR()) { lstEType.add(EventType.AFTER_RESTORE); }
				if (record.isNotifyAfterP()) { lstEType.add(EventType.AFTER_PURGE); }
				if (record.isNotifyOnLoad()) { lstEType.add(EventType.ON_LOAD); }
				if (record.isNotifyBeforeValidate()) { lstEType.add(EventType.BEFORE_VALIDATE); }
				snDef.setListenEvent(lstEType);

				result = snDef;
			}
			if (result != null) {
				result.setWithoutMappedByReference(record.isWithoutMappedByReference());
			}
		}

		return result;
	}

	/**
	 *
	 *
	 * @param isNewRow
	 * @param record
	 */
	public void startEventListenerEdit(boolean isNewRow, EventListenerListGridRecord record) {
		EventListenerEditDialog dialog = new EventListenerEditDialog(isNewRow, record);
		dialog.show();
	}


	/**
	 * EventListener編集用ダイアログ
	 *
	 * @author lis2s8
	 *
	 */
	private class EventListenerEditDialog extends AbstractWindow {

		private static final int DEFAULT_WIDTH = 670;

		private boolean isNewRow = false;
		private EventListenerListGridRecord target;

		//Type
		private DynamicForm typeItemForm;
		private SelectItem typeItem;

		//Main
		private VLayout mainLayout = new VLayout();

		//Script
		private DynamicForm scriptItemForm;
		private TextAreaItem scriptItem;

		//ScriptEvent
		private DynamicForm scriptEventItemForm;
		private CheckboxItem beforeIItem;
		private CheckboxItem afterIItem;
		private CheckboxItem beforeUItem;
		private CheckboxItem afterUItem;
		private CheckboxItem beforeDItem;
		private CheckboxItem afterDItem;
		private CheckboxItem afterRItem;
		private CheckboxItem afterPItem;
		private CheckboxItem onLoadItem;
		private CheckboxItem beforeValidateItem;

		//JavaClass
		private DynamicForm javaClassItemForm;
		private TextItem javaClassNameItem;

		//SendNotification
		private DynamicForm sendNotificationForm;
		private SelectItem notificationTypeItem;
		private SelectItem tmplDefNameItem;
		//NotificationCondition
		private DynamicForm notificationCondForm;
		private TextAreaItem notificationCondScriptItem;
		//NotificationEvent
		private DynamicForm notifyEventItemForm;
		private CheckboxItem notifyBeforeIItem;
		private CheckboxItem notifyAfterIItem;
		private CheckboxItem notifyBeforeUItem;
		private CheckboxItem notifyAfterUItem;
		private CheckboxItem notifyBeforeDItem;
		private CheckboxItem notifyAfterDItem;
		private CheckboxItem notifyAfterRItem;
		private CheckboxItem notifyAfterPItem;
		private CheckboxItem notifyOnLoadItem;
		private CheckboxItem notifyBeforeValidateItem;

		//withoutMappedByReference
		private DynamicForm withoutMappedByReferenceItemForm;
		private CheckboxItem withoutMappedByReferenceItem;


		private EventListenerEditDialog(boolean isNewRow, EventListenerListGridRecord target) {
			this.isNewRow = isNewRow;
			this.target = target;

			initialize();
			dataInitialize();
			formVisibleChange();
		}

		private void initialize() {

			// ダイアログ本体のプロパティ設定
			setWidth(DEFAULT_WIDTH);
			setHeight(100);
			setTitle("EventListener");
			setShowMinimizeButton(false);
			setShowMaximizeButton(true);	//最大化は可能に設定（スクリプト編集用）
			setCanDragResize(true);			//リサイズは可能に設定（スクリプト編集用）
			setIsModal(true);
			setShowModalMask(true);
			centerInPage();

			//---------------------------------
			//Type
			//---------------------------------
			typeItem = new SelectItem();
			typeItem.setTitle("Type");
			SmartGWTUtil.setRequired(typeItem);
			typeItem.addChangedHandler(new ChangedHandler() {
				public void onChanged(ChangedEvent event) {
					formVisibleChange();
				}
			});

			typeItemForm = new DynamicForm();
			typeItemForm.setMargin(5);
			typeItemForm.setNumCols(3);
			typeItemForm.setColWidths(100, "*", 100);
			typeItemForm.setAlign(Alignment.LEFT);
			typeItemForm.setItems(typeItem);

			mainLayout.setWidth100();
			mainLayout.setHeight100();
//			mainLayout.setMargin(10);

			//---------------------------------
			//Script
			//---------------------------------
			scriptItem = new TextAreaItem();
			scriptItem.setColSpan(2);
			scriptItem.setTitle("Script");
			scriptItem.setWidth("100%");
			scriptItem.setHeight("100%");
			SmartGWTUtil.setRequired(scriptItem);
			SmartGWTUtil.setReadOnlyTextArea(scriptItem);

			ButtonItem editScript = new ButtonItem("editScript", "Edit");
			editScript.setWidth(100);
			editScript.setStartRow(false);
			editScript.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {

				@Override
				public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
					MetaDataUtil.showScriptEditDialog(ScriptEditorDialogMode.GROOVY_SCRIPT,
							SmartGWTUtil.getStringValue(scriptItem),
							ScriptEditorDialogCondition.ENTITY_EVENT_LISTNER,
							"ui_metadata_entity_EventListenerListGrid_scriptHint",
							null,
							new ScriptEditorDialogHandler() {

								@Override
								public void onSave(String text) {
									scriptItem.setValue(text);
								}
								@Override
								public void onCancel() {
								}
							});
				}
			});

			scriptItemForm = new DynamicForm();
			scriptItemForm.setMargin(5);
			scriptItemForm.setNumCols(3);
			scriptItemForm.setColWidths(100, "*", 100);
			scriptItemForm.setWidth100();
			scriptItemForm.setHeight100();
			scriptItemForm.setAlign(Alignment.LEFT);
			scriptItemForm.setItems(new SpacerItem(), new SpacerItem(), editScript, scriptItem);

			beforeIItem = new CheckboxItem();
			beforeIItem.setTitle("beforeInsert");
			afterIItem = new CheckboxItem();
			afterIItem.setTitle("afterInsert");
			beforeUItem = new CheckboxItem();
			beforeUItem.setTitle("beforeUpdate");
			afterUItem = new CheckboxItem();
			afterUItem.setTitle("afterUpdate");
			beforeDItem = new CheckboxItem();
			beforeDItem.setTitle("beforeDelete");
			afterDItem = new CheckboxItem();
			afterDItem.setTitle("afterDelete");
			afterRItem = new CheckboxItem();
			afterRItem.setTitle("afterRestore");
			afterPItem = new CheckboxItem();
			afterPItem.setTitle("afterPurge");
			onLoadItem = new CheckboxItem();
			onLoadItem.setTitle("onLoad");
			beforeValidateItem = new CheckboxItem();
			beforeValidateItem.setTitle("beforeValidate");

			scriptEventItemForm = new DynamicForm();
			scriptEventItemForm.setMargin(5);
			scriptEventItemForm.setNumCols(9);
			scriptEventItemForm.setHeight(100);
			scriptEventItemForm.setIsGroup(true);
			scriptEventItemForm.setGroupTitle("Events");
			scriptEventItemForm.setItems(beforeIItem, afterIItem, beforeUItem, afterUItem, SmartGWTUtil.createSpacer(),
					beforeDItem, afterDItem, afterRItem, afterPItem,SmartGWTUtil.createSpacer(),
					onLoadItem, beforeValidateItem);

			//---------------------------------
			//Java Class
			//---------------------------------
			javaClassNameItem = new TextItem();
			javaClassNameItem.setTitle("Class Name");
			javaClassNameItem.setWidth("*");
			SmartGWTUtil.setRequired(javaClassNameItem);
			SmartGWTUtil.addHoverToFormItem(javaClassNameItem,
					AdminClientMessageUtil.getString("ui_metadata_entity_EventListenerListGrid_javaClassNameItemComment"));

			javaClassItemForm = new DynamicForm();
			javaClassItemForm.setMargin(5);
			javaClassItemForm.setNumCols(3);
			javaClassItemForm.setColWidths(100, "*", 50);
			javaClassItemForm.setHeight(25);
			javaClassItemForm.setItems(javaClassNameItem, SmartGWTUtil.createSpacer(50));

			//---------------------------------
			//Send Notification
			//---------------------------------
			notificationTypeItem = new SelectItem();
			notificationTypeItem.setTitle("Notification type");
			SmartGWTUtil.setRequired(notificationTypeItem);
			notificationTypeItem.addChangedHandler(new ChangedHandler() {

				@Override
				public void onChanged(ChangedEvent event) {
					MetaTemplateChange();
				}
			});

			tmplDefNameItem = new SelectItem("template", "Template");
			tmplDefNameItem.setWidth(250);
			SmartGWTUtil.setRequired(tmplDefNameItem);

			sendNotificationForm = new DynamicForm();
			sendNotificationForm.setMargin(5);
			sendNotificationForm.setNumCols(3);
			sendNotificationForm.setColWidths(100, "*", 50);
			sendNotificationForm.setHeight(50);
			sendNotificationForm.setItems(notificationTypeItem, SmartGWTUtil.createSpacer(50), tmplDefNameItem, SmartGWTUtil.createSpacer(50));


			//---------------------------------
			//Notification Condition
			//---------------------------------
			notificationCondScriptItem = new TextAreaItem();
			notificationCondScriptItem.setColSpan(2);
			notificationCondScriptItem.setTitle("Notification condition");
			notificationCondScriptItem.setWidth("100%");
			notificationCondScriptItem.setHeight("100%");
			SmartGWTUtil.setReadOnlyTextArea(notificationCondScriptItem);

			ButtonItem editNotificationCond = new ButtonItem("editNotificationCond", "Edit");
			editNotificationCond.setWidth(100);
			editNotificationCond.setStartRow(false);
			editNotificationCond.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {

				@Override
				public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
					MetaDataUtil.showScriptEditDialog(ScriptEditorDialogMode.GROOVY_SCRIPT,
							SmartGWTUtil.getStringValue(notificationCondScriptItem),
							ScriptEditorDialogCondition.ENTITY_EVENT_LISTNER,
							"ui_metadata_entity_EventListenerListGrid_notificationCondScriptHint",
							null,
							new ScriptEditorDialogHandler() {

								@Override
								public void onSave(String text) {
									notificationCondScriptItem.setValue(text);
								}
								@Override
								public void onCancel() {
								}
							});
				}
			});

			notificationCondForm = new DynamicForm();
			notificationCondForm.setMargin(5);
			notificationCondForm.setNumCols(3);
			notificationCondForm.setColWidths(100, "*", 100);
			notificationCondForm.setWidth100();
			notificationCondForm.setHeight100();
			notificationCondForm.setAlign(Alignment.LEFT);
			notificationCondForm.setItems(new SpacerItem(), new SpacerItem(), editNotificationCond, notificationCondScriptItem);

			notifyBeforeIItem = new CheckboxItem();
			notifyBeforeIItem.setTitle("beforeInsert");
			notifyAfterIItem = new CheckboxItem();
			notifyAfterIItem.setTitle("afterInsert");
			notifyBeforeUItem = new CheckboxItem();
			notifyBeforeUItem.setTitle("beforeUpdate");
			notifyAfterUItem = new CheckboxItem();
			notifyAfterUItem.setTitle("afterUpdate");
			notifyBeforeDItem = new CheckboxItem();
			notifyBeforeDItem.setTitle("beforeDelete");
			notifyAfterDItem = new CheckboxItem();
			notifyAfterDItem.setTitle("afterDelete");
			notifyAfterRItem = new CheckboxItem();
			notifyAfterRItem.setTitle("afterRestore");
			notifyAfterPItem = new CheckboxItem();
			notifyAfterPItem.setTitle("afterPurge");
			notifyOnLoadItem = new CheckboxItem();
			notifyOnLoadItem.setTitle("onLoad");
			notifyBeforeValidateItem = new CheckboxItem();
			notifyBeforeValidateItem.setTitle("beforeValidate");
			
			notifyEventItemForm = new DynamicForm();
			notifyEventItemForm.setMargin(5);
			notifyEventItemForm.setNumCols(9);
			notifyEventItemForm.setHeight(100);
			notifyEventItemForm.setIsGroup(true);
			notifyEventItemForm.setGroupTitle("Events");
			notifyEventItemForm.setItems(notifyBeforeIItem, notifyAfterIItem, notifyBeforeUItem, notifyAfterUItem, SmartGWTUtil.createSpacer(),
					notifyBeforeDItem, notifyAfterDItem, notifyAfterRItem, notifyAfterPItem,SmartGWTUtil.createSpacer(),
					notifyOnLoadItem, notifyBeforeValidateItem);

			//---------------------------------
			//withoutMappedByReference
			//---------------------------------
			withoutMappedByReferenceItem = new CheckboxItem();
			withoutMappedByReferenceItem.setTitle("mapped by reference info is unnecessary for listner.");
			withoutMappedByReferenceItem.setShowTitle(false);
			SmartGWTUtil.addHoverToFormItem(withoutMappedByReferenceItem,
					AdminClientMessageUtil.getString("ui_metadata_entity_EventListenerListGrid_withoutMappByRefComment"));

			withoutMappedByReferenceItemForm = new DynamicForm();
			withoutMappedByReferenceItemForm.setMargin(5);
			withoutMappedByReferenceItemForm.setNumCols(3);
			withoutMappedByReferenceItemForm.setColWidths(100, "*", 50);
			withoutMappedByReferenceItemForm.setHeight(25);
			withoutMappedByReferenceItemForm.setItems(withoutMappedByReferenceItem, SmartGWTUtil.createSpacer(50));

			//---------------------------------
			//Footer
			//---------------------------------
			IButton ok = new IButton("OK");
			ok.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					if (!validate()) {
						return;
					}
					if (isNewRow) {
						addData(target);
					}
					updateRecordData();
					destroy();
				}
			});
			IButton cancel = new IButton("Cancel");
			cancel.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					destroy();
				}
			});

			HLayout footer = new HLayout(5);
			footer.setMargin(5);
			footer.setHeight(20);
			footer.setWidth100();
			footer.setAlign(VerticalAlignment.CENTER);
			footer.setMembers(ok, cancel);

			addItem(typeItemForm);
			addItem(mainLayout);
			addItem(SmartGWTUtil.separator());
			addItem(footer);
		}

		private void dataInitialize() {
			LinkedHashMap<String, String> typeMap = new LinkedHashMap<String, String>();
			typeMap.put(SCRIPT, "Script");
			typeMap.put(JAVACLASS, "JavaClass");
			typeMap.put(SENDNOTIFICATION, "SendNotification");

			typeItem.setValueMap(typeMap);

			typeItem.setValue(target.getElName());

			scriptItem.setValue(target.getScript());
			afterDItem.setValue(target.isAfterD());
			afterIItem.setValue(target.isAfterI());
			afterUItem.setValue(target.isAfterU());
			beforeDItem.setValue(target.isBeforeD());
			beforeIItem.setValue(target.isBeforeI());
			beforeUItem.setValue(target.isBeforeU());
			afterRItem.setValue(target.isAfterR());
			afterPItem.setValue(target.isAfterP());
			onLoadItem.setValue(target.isOnLoad());
			beforeValidateItem.setValue(target.isBeforeValidate());

			javaClassNameItem.setValue(target.getClassName());

			LinkedHashMap<String, String> notificationTypeMap = new LinkedHashMap<String, String>();
			notificationTypeMap.put(SendNotificationType.MAIL.name(), SendNotificationType.MAIL.displayName());
			notificationTypeMap.put(SendNotificationType.SMS.name(), SendNotificationType.SMS.displayName());
			notificationTypeMap.put(SendNotificationType.PUSH.name(), SendNotificationType.PUSH.displayName());
			notificationTypeItem.setValueMap(notificationTypeMap);

			notificationTypeItem.setValue(target.getNotificationType());
			notificationCondScriptItem.setValue(target.getNotificationCondScript());
			tmplDefNameItem.setValue(target.getTmplDefName());
			notifyAfterDItem.setValue(target.isNotifyAfterD());
			notifyAfterIItem.setValue(target.isNotifyAfterI());
			notifyAfterUItem.setValue(target.isNotifyAfterU());
			notifyBeforeDItem.setValue(target.isNotifyBeforeD());
			notifyBeforeIItem.setValue(target.isNotifyBeforeI());
			notifyBeforeUItem.setValue(target.isNotifyBeforeU());
			notifyAfterRItem.setValue(target.isNotifyAfterR());
			notifyAfterPItem.setValue(target.isNotifyAfterP());
			notifyOnLoadItem.setValue(target.isNotifyOnLoad());
			notifyBeforeValidateItem.setValue(target.isNotifyBeforeValidate());

			withoutMappedByReferenceItem.setValue(target.isWithoutMappedByReference());
		}

		private void formVisibleChange() {
			typeItemForm.clearErrors(true);

			if (mainLayout.contains(scriptItemForm)) {
				scriptItemForm.clearErrors(true);
				mainLayout.removeMember(scriptItemForm);
			}
			if (mainLayout.contains(scriptEventItemForm)) {
				scriptEventItemForm.clearErrors(true);
				mainLayout.removeMember(scriptEventItemForm);
			}
			if (mainLayout.contains(javaClassItemForm)) {
				javaClassItemForm.clearErrors(true);
				mainLayout.removeMember(javaClassItemForm);
			}
			if (mainLayout.contains(sendNotificationForm)) {
				sendNotificationForm.clearErrors(true);
				mainLayout.removeMember(sendNotificationForm);
			}
			if (mainLayout.contains(notificationCondForm)) {
				notificationCondForm.clearErrors(true);
				mainLayout.removeMember(notificationCondForm);
			}
			if (mainLayout.contains(notifyEventItemForm)) {
				notifyEventItemForm.clearErrors(true);
				mainLayout.removeMember(notifyEventItemForm);
			}
			if (mainLayout.contains(withoutMappedByReferenceItemForm)) {
				withoutMappedByReferenceItemForm.clearErrors(true);
				mainLayout.removeMember(withoutMappedByReferenceItemForm);
			}

			String selectValType = SmartGWTUtil.getStringValue(typeItem);
			if (SCRIPT.equals(selectValType)) {
				mainLayout.addMember(scriptItemForm);
				mainLayout.addMember(scriptEventItemForm);
				mainLayout.addMember(withoutMappedByReferenceItemForm);
				setHeight(470);
				centerInPage();
			} else if (JAVACLASS.equals(selectValType)) {
				mainLayout.addMember(javaClassItemForm);
				mainLayout.addMember(withoutMappedByReferenceItemForm);
				setHeight(190);
			} else if (SENDNOTIFICATION.equals(selectValType)) {
				mainLayout.addMember(sendNotificationForm);
				mainLayout.addMember(notificationCondForm);
				mainLayout.addMember(notifyEventItemForm);
				mainLayout.addMember(withoutMappedByReferenceItemForm);
				setHeight(470);
				centerInPage();
			} else {
				setHeight(120);
			}
		}

		private void MetaTemplateChange() {
			sendNotificationForm.clearErrors(true);
			String selectValType = SmartGWTUtil.getStringValue(notificationTypeItem);
			SendNotificationType type = SendNotificationType.valueOf(selectValType);
			MetaDataNameDS.setDataSource(tmplDefNameItem, type.definitionClass(), new MetaDataNameDSOption(true, false));
		}

		private boolean validate() {
			boolean isValidate = true;
			if (!typeItemForm.validate()) {
				isValidate = false;
			}
			if (mainLayout.contains(scriptItemForm)) {
				if (!scriptItemForm.validate()) {
					isValidate = false;
				}
			}
			if (mainLayout.contains(scriptEventItemForm)) {
				if (!scriptEventItemForm.validate()) {
					isValidate = false;
				}
			}
			if (mainLayout.contains(javaClassItemForm)) {
				if (!javaClassItemForm.validate()) {
					isValidate = false;
				}
			}
			if (mainLayout.contains(sendNotificationForm)) {
				if (!sendNotificationForm.validate()) {
					isValidate = false;
				}
			}
			if (mainLayout.contains((notificationCondForm))) {
				if (!notificationCondForm.validate()) {
					isValidate = false;
				}
			}
			if (mainLayout.contains(notifyEventItemForm)) {
				if (!notifyEventItemForm.validate()) {
					isValidate = false;
				}
			}
			if (mainLayout.contains(withoutMappedByReferenceItemForm)) {
				if (!withoutMappedByReferenceItemForm.validate()) {
					isValidate = false;
				}
			}
			return isValidate;
		}

		private void updateRecordData() {
			String selectValType = SmartGWTUtil.getStringValue(typeItem);
			target.setElName(selectValType);
			if (SCRIPT.equals(selectValType)) {
				target.setScript(SmartGWTUtil.getStringValue(scriptItem));
				target.setGeneralPurpus(SmartGWTUtil.getStringValue(scriptItem));

				target.setAfterD(SmartGWTUtil.getBooleanValue(afterDItem));
				target.setAfterI(SmartGWTUtil.getBooleanValue(afterIItem));
				target.setAfterU(SmartGWTUtil.getBooleanValue(afterUItem));
				target.setBeforeD(SmartGWTUtil.getBooleanValue(beforeDItem));
				target.setBeforeI(SmartGWTUtil.getBooleanValue(beforeIItem));
				target.setBeforeU(SmartGWTUtil.getBooleanValue(beforeUItem));
				target.setAfterR(SmartGWTUtil.getBooleanValue(afterRItem));
				target.setAfterP(SmartGWTUtil.getBooleanValue(afterPItem));
				target.setOnLoad(SmartGWTUtil.getBooleanValue(onLoadItem));
				target.setBeforeValidate(SmartGWTUtil.getBooleanValue(beforeValidateItem));
			} else if (JAVACLASS.equals(selectValType)) {
				target.setClassName(SmartGWTUtil.getStringValue(javaClassNameItem));
				target.setGeneralPurpus(SmartGWTUtil.getStringValue(javaClassNameItem));
			} else if (SENDNOTIFICATION.equals(selectValType)) {
				target.setNotificationType(SmartGWTUtil.getStringValue(notificationTypeItem));
				target.setTmplDefName(SmartGWTUtil.getStringValue(tmplDefNameItem));
				target.setGeneralPurpus(SmartGWTUtil.getStringValue(notificationTypeItem));
				target.setNotificationCondScript(SmartGWTUtil.getStringValue(notificationCondScriptItem));

				target.setNotifyAfterD(SmartGWTUtil.getBooleanValue(notifyAfterDItem));
				target.setNotifyAfterI(SmartGWTUtil.getBooleanValue(notifyAfterIItem));
				target.setNotifyAfterU(SmartGWTUtil.getBooleanValue(notifyAfterUItem));
				target.setNotifyBeforeD(SmartGWTUtil.getBooleanValue(notifyBeforeDItem));
				target.setNotifyBeforeI(SmartGWTUtil.getBooleanValue(notifyBeforeIItem));
				target.setNotifyBeforeU(SmartGWTUtil.getBooleanValue(notifyBeforeUItem));
				target.setNotifyAfterR(SmartGWTUtil.getBooleanValue(notifyAfterRItem));
				target.setNotifyAfterP(SmartGWTUtil.getBooleanValue(notifyAfterPItem));
				target.setNotifyOnLoad(SmartGWTUtil.getBooleanValue(notifyOnLoadItem));
				target.setNotifyBeforeValidate(SmartGWTUtil.getBooleanValue(notifyBeforeValidateItem));
			}
			target.setWithoutMappedByReference(SmartGWTUtil.getBooleanValue(withoutMappedByReferenceItem));
			updateData(target);
			refreshFields();
		}
	}
}

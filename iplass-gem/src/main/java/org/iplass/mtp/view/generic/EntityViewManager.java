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

package org.iplass.mtp.view.generic;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.iplass.mtp.definition.DefinitionModifyResult;
import org.iplass.mtp.definition.TypedDefinitionManager;
import org.iplass.mtp.entity.Entity;
import org.iplass.mtp.entity.query.condition.Condition;
import org.iplass.mtp.view.generic.editor.PropertyEditor;

/**
 * 画面定義を管理するクラスのインターフェース。
 * @author lis3wg
 */
public interface EntityViewManager extends TypedDefinitionManager<EntityView> {

	/**
	 * 画面定義を新規作成します。
	 * @param name Entity定義の名前
	 * @param entityView 画面定義
	 * @deprecated {@link #create(EntityView)} を利用してください。
	 */
	@Deprecated
	public DefinitionModifyResult create(String name, EntityView entityView);

	/**
	 * 指定の画面定義で既存の定義を更新します。
	 * @param name Entity定義の名前
	 * @param entityView 画面定義
	 * @deprecated {@link #update(EntityView)} を利用してください。
	 */
	@Deprecated
	public DefinitionModifyResult update(String name, EntityView entityView);

	/**
	 * 指定の画面定義に設定されているEditorを取得します。
	 *
	 * @param defName Entity定義の名前
	 * @param viewType FormViewの種類
	 * @param viewName View名
	 * @param propName プロパティ名
	 * @return エディター
	 */
	public PropertyEditor getPropertyEditor(String defName, String viewType, String viewName, String propName);

	/**
	 * 指定の画面定義に設定されているEditorを取得します。
	 *
	 * @param defName Entity定義の名前
	 * @param viewType FormViewの種類
	 * @param viewName View名
	 * @param propName プロパティ名
	 * @param refSection 参照セクションインデックス
	 * @return エディター
	 */
	public PropertyEditor getPropertyEditor(String defName, String viewType, String viewName, String propName, Integer refSectionIndex);

	/**
	 * スクリプトテンプレートを実行します。
	 * @param name 定義名
	 * @param templateName テンプレート名
	 * @param req リクエスト
	 * @param res レスポンス
	 * @param application サーブレットコンテキスト
	 * @param page ページコンテキスト
	 */
	public void executeTemplate(String name, String templateName, HttpServletRequest req,
			HttpServletResponse res, ServletContext application, PageContext page);

	/**
	 * 画面定義に設定されたカスタムコピースクリプトを使ってEntityをコピーします。
	 * @param viewName view名
	 * @param entity コピー元Entity
	 * @return コピーしたEntity
	 */
	public Entity copyEntity(String viewName, Entity entity);

	/**
	 * 画面定義に設定された初期化スクリプトを使ってEntityを初期化します。
	 * @param definitionName Entity定義名
	 * @param viewName view名
	 * @param entity 初期化対象Entity
	 * @return 初期化したEntity
	 */
	public Entity initEntity(String definitionName, String viewName, Entity entity);

	/**
	 * 画面定義に設定されたデフォルトプロパティ条件設定スクリプトを使って検索条件を設定します。
	 *
	 * @param definitionName Entity定義名
	 * @param viewName view名
	 * @param defaultCondMap パラメータなどで設定された検索条件
	 * @return デフォルトプロパティ条件が適用された検索条件
	 */
	public Map<String, Object> applyDefaultPropertyCondition(String definitionName, String viewName, Map<String, Object> defaultCondMap);

	/**
	 * 画面定義に設定されたCSVダウンロードファイル名Formatを使ってCSVファイル名を返します。
	 *
	 * @param definitionName Entity定義名
	 * @param viewName view名
	 * @param defaultName デフォルトファイル名
	 * @param csvVariableMap TemplateBind引数
	 * @return CSVファイル名
	 */
	public String getCsvDownloadFileName(String definitionName, String viewName, String defaultName, Map<String, Object> csvVariableMap);

	/**
	 * 大規模参照セクション用の絞り込み条件を取得します。
	 * @param name Entity定義名
	 * @param key 条件名
	 * @return 条件
	 */
	public Condition getMassReferenceSectionCondition(String name, String key);

	/**
	 * Entity定義に対応する標準のSearchFormViewを返します。
	 *
	 * @param definitionName Entity定義名
	 * @return SearchFormView
	 */
	public SearchFormView createDefaultSearchFormView(String definitionName);

	/**
	 * Entity定義に対応する標準のDetailFormViewを返します。
	 *
	 * @param definitionName Entity定義名
	 * @return DetailFormView
	 */
	public DetailFormView createDefaultDetailFormView(String definitionName);

	/**
	 * Entity定義に対応する標準のBulkFormViewを返します。
	 *
	 * @param definitionName Entity定義名
	 * @return BulkFormView
	 */
	public BulkFormView createDefaultBulkFormView(String definitionName);

	/**
	 * カスタムスタイルを取得します。
	 *
	 * @param definitionName Entity定義名
	 * @param scriptKey EntityViewのカスタムスタイルキー
	 * @param editorScriptKey EntityViewに設定されているプロパティ毎のカスタムスタイルキー
	 * @param entity Entityデータ
	 * @param propValue プロパティの値
	 * @return DetailFormView
	 */
	public String getCustomStyle(String definitionName, String scriptKey, String editorScriptKey, Entity entity, Object propValue);

	/**
	 * エレメントの表示可否を判定します。
	 *
	 * @param definitionName Entity定義名
	 * @param elementRuntimeId エレメントのランタイムID
	 * @param outputType 表示タイプ
	 * @param entity 表示対象のエンティティ
	 * @return 表示可否
	 */
	public boolean isDisplayElement(String definitionName, String elementRuntimeId, OutputType outputType, Entity entity);

	/**
	 * ボタンの表示可否を判定します。
	 * @param definitionName Entity定義名
	 * @param buttonKey ボタンのキー
	 * @param outputType 表示タイプ
	 * @param entity 表示対象のエンティティ
	 * @return
	 */
	public boolean isDisplayButton(String definitionName, String buttonKey, OutputType outputType, Entity entity);

	/**
	 * 参照ダイアログ用のURLパラメータを取得します。
	 * @param name 定義名
	 * @param templateName テンプレート名
	 * @param entity Entity
	 * @param req リクエスト
	 * @param res レスポンス
	 * @param application サーブレットコンテキスト
	 * @param page ページコンテキスト
	 */
	public String getUrlParameter(String name, String templateName, Entity entity);

	/**
	 * 自動補完の値を取得します。
	 * @param definitionName Entity定義名
	 * @param viewName view名
	 * @param viewType 表示タイプ
	 * @param propName プロパティ名
	 * @param autocompletionKey 自動補完のキー
	 * @param param 連動元の値
	 * @param currentValue 連動先の値
	 * @return 自動補完の値
	 */
	public Object getAutocompletionValue(String definitionName, String viewName, String viewType, String propName, String autocompletionKey, Integer referenceSectionIndex, Map<String, String[]> param, List<String> currentValue);

	/**
	 * <p>
	 * EntityViewの操作を許可するロールを返します。
	 * View定義が1つもない場合はnullを返します。
	 * </p>
	 *
	 * @param definitionName Entity定義名
	 * @param viewName view名
	 * @return 許可ロール
	 */
	public List<String> getPermitRoles(String definitionName, String viewName);

}

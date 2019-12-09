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

package org.iplass.mtp.impl.tools.entityport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.iplass.mtp.ManagerLocator;
import org.iplass.mtp.auth.User;
import org.iplass.mtp.entity.BinaryReference;
import org.iplass.mtp.entity.DeleteCondition;
import org.iplass.mtp.entity.DeleteOption;
import org.iplass.mtp.entity.Entity;
import org.iplass.mtp.entity.EntityApplicationException;
import org.iplass.mtp.entity.EntityManager;
import org.iplass.mtp.entity.EntityRuntimeException;
import org.iplass.mtp.entity.InsertOption;
import org.iplass.mtp.entity.TargetVersion;
import org.iplass.mtp.entity.UpdateOption;
import org.iplass.mtp.entity.definition.EntityDefinition;
import org.iplass.mtp.entity.definition.EntityDefinitionManager;
import org.iplass.mtp.entity.definition.VersionControlType;
import org.iplass.mtp.entity.query.OrderBy;
import org.iplass.mtp.entity.query.Query;
import org.iplass.mtp.entity.query.SortSpec;
import org.iplass.mtp.entity.query.SortSpec.SortType;
import org.iplass.mtp.entity.query.Where;
import org.iplass.mtp.entity.query.condition.expr.And;
import org.iplass.mtp.entity.query.condition.predicate.Equals;
import org.iplass.mtp.entity.query.condition.predicate.NotEquals;
import org.iplass.mtp.entity.query.hint.FetchSizeHint;
import org.iplass.mtp.impl.core.ExecuteContext;
import org.iplass.mtp.impl.entity.EntityContext;
import org.iplass.mtp.impl.entity.EntityHandler;
import org.iplass.mtp.impl.entity.csv.EntityCsvReader;
import org.iplass.mtp.impl.entity.csv.EntitySearchCsvWriter;
import org.iplass.mtp.impl.entity.csv.EntityWriteOption;
import org.iplass.mtp.impl.entity.property.MetaPrimitiveProperty;
import org.iplass.mtp.impl.entity.property.PrimitivePropertyHandler;
import org.iplass.mtp.impl.entity.property.PropertyHandler;
import org.iplass.mtp.impl.entity.property.ReferencePropertyHandler;
import org.iplass.mtp.impl.metadata.MetaDataEntry;
import org.iplass.mtp.impl.parser.ParseContext;
import org.iplass.mtp.impl.parser.ParseException;
import org.iplass.mtp.impl.parser.SyntaxContext;
import org.iplass.mtp.impl.parser.SyntaxService;
import org.iplass.mtp.impl.query.OrderBySyntax;
import org.iplass.mtp.impl.query.QuerySyntaxRegister;
import org.iplass.mtp.impl.tools.ToolsResourceBundleUtil;
import org.iplass.mtp.impl.tools.metaport.MetaDataTagEntity;
import org.iplass.mtp.impl.tools.pack.PackageEntity;
import org.iplass.mtp.spi.Config;
import org.iplass.mtp.spi.Service;
import org.iplass.mtp.transaction.Transaction;
import org.iplass.mtp.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EntityのExport/Import用Service
 */
public class EntityPortingService implements Service {

	private static Logger logger = LoggerFactory.getLogger(EntityPortingService.class);
	private static Logger auditLogger = LoggerFactory.getLogger("mtp.audit.porting.entity");
	private static Logger toolLogger = LoggerFactory.getLogger("mtp.tools.entity");

	/** LOBデータ格納パス */
	public static final String ENTITY_LOB_DIR = "lobs/";

	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSXXX";
	private static final String TIME_FORMAT = "HH:mm:ss";

	private SyntaxService syntaxService;

	private EntityManager em;
	private EntityDefinitionManager edm;

	@Override
	public void init(Config config) {
		syntaxService = config.getDependentService(SyntaxService.class);

		em = ManagerLocator.getInstance().getManager(EntityManager.class);
		edm = ManagerLocator.getInstance().getManager(EntityDefinitionManager.class);
	}

	@Override
	public void destroy() {
	}

	/**
	 * EntityデータをCSV形式でExportします。
	 *
	 * @param os        出力先CSVファイル(Stream)
	 * @param entry     出力対象Entity
	 * @param condition Export条件
	 * @return 出力件数
	 * @throws IOException
	 */
	public int write(final OutputStream os, final MetaDataEntry entry, final EntityDataExportCondition condition) throws IOException {

		return writeWithBinary(os, entry, condition, null);
	}

	/**
	 * EntityデータをCSV形式でExportします。
	 *
	 * @param os        出力先CSVファイル(Stream)
	 * @param entry     出力対象Entity
	 * @param condition Export条件
	 * @param zos    Lobを追加するZipのOutputStream
	 * @param lobPrefixPath LobをZipに追加する際のPrefixPath
	 * @return 出力件数
	 * @throws IOException
	 */
	public int writeWithBinary(final OutputStream os, final MetaDataEntry entry, final EntityDataExportCondition condition, final ZipOutputStream zos) throws IOException {

		EntityDefinition definition = edm.get(entry.getMetaData().getName());

		Where where = null;
		if (StringUtil.isNotEmpty(condition.getWhereClause())) {
			where = Where.newWhere("where " + condition.getWhereClause());
		}
		OrderBy orderBy = null;
		if (StringUtil.isNotEmpty(condition.getOrderByClause())) {
			try {
				SyntaxContext sc = syntaxService.getSyntaxContext(QuerySyntaxRegister.QUERY_CONTEXT);
				orderBy = sc.getSyntax(OrderBySyntax.class).parse(new ParseContext("order by " + condition.getOrderByClause()));
			} catch(ParseException e) {
				throw new EntityDataPortingRuntimeException(e);
			}
		} else {
			orderBy = new OrderBy();
			orderBy.add(new SortSpec(Entity.OID, SortType.ASC));
			orderBy.add(new SortSpec(Entity.VERSION, SortType.ASC));
		}

		//Writer生成
		EntityWriteOption option = new EntityWriteOption()
				.withReferenceVersion(true)
				.withBinary(true)
				.where(where)
				.orderBy(orderBy)
				.dateFormat(DATE_FORMAT)
				.datetimeSecFormat(DATE_TIME_FORMAT)
				.timeSecFormat(TIME_FORMAT);
		int count = 0;
		try (EntitySearchCsvWriter writer = new EntitySearchCsvWriter(os, definition.getName(), option, zos)) {
			count = writer.write();
		}

		return count;
	}

	/**
	 * EntityデータをImportします。
	 *
	 * @param targetName インポート対象の名前(Package名またはCSVファイル名)
	 * @param is CSVファイル(Stream)
	 * @param entry 対象Entity
	 * @param condition Import条件
	 * @param zipFile LOBファイルが格納されているzipファイル(nullの場合、LOBファイルは取り込みません)
	 * @return Import結果
	 */
	public EntityDataImportResult importEntityData(String targetName, final InputStream is, final MetaDataEntry entry, final EntityDataImportCondition condition, final ZipFile zipFile) {

		toolLogger.info("start entity data import. {target:{}, entity:{}}", targetName, entry.getPath());

		EntityDataImportResult result = new EntityDataImportResult();
		try {
			if (PackageEntity.ENTITY_DEFINITION_NAME.equals(entry.getMetaData().getName())) {
				result.addMessages(rs("entityport.cantImportEntity", PackageEntity.ENTITY_DEFINITION_NAME));
				return result;
			}
			if (MetaDataTagEntity.ENTITY_DEFINITION_NAME.equals(entry.getMetaData().getName())) {
				result.addMessages(rs("entityport.cantImportEntity", MetaDataTagEntity.ENTITY_DEFINITION_NAME));
				return result;
			}

			EntityDefinition definition = edm.get(entry.getMetaData().getName());

			//全件削除
			if (condition.isTruncate()) {
				toolLogger.info("start entity data truncate. {target:{}, entity:{}}", targetName, entry.getPath());
				int delCount = truncateEntity(definition, condition, result);
				toolLogger.info("finish entity data truncate. {target:{}, entity:{}, count:{}}", targetName, entry.getPath(), delCount);
			}

			//データ読み込み
			readCSV(is, definition, condition, zipFile, result);

			return result;
		} finally {
			toolLogger.info("finish entity data import. {target:{}, entity:{}, result:{}}", targetName, entry.getPath(), (result.isError() ? "failed" : "success"));
		}
	}

	private int truncateEntity(final EntityDefinition definition, final EntityDataImportCondition cond, final EntityDataImportResult result) {

		return Transaction.requiresNew(new Function<Transaction, Integer>() {

			private boolean isUserEntity = false;
			private int delCount = 0;

			@Override
			public Integer apply(Transaction transaction) {

				try {
					final ExecuteContext executeContext = ExecuteContext.getCurrentContext();
					final EntityContext entityContext = EntityContext.getCurrentContext();
					final EntityHandler entityHandler = entityContext.getHandlerByName(definition.getName());

					auditLogger.info("deleteAll entity," + definition.getName());

					//ユーザEntity以外の場合、deleteAll可能かチェック(deleteAllではListnerが実行されないため)
					boolean canDeleteAll = false;
					if (User.DEFINITION_NAME.equals(definition.getName())) {
						isUserEntity = true;
					} else {
						canDeleteAll = entityHandler.canDeleteAll();
					}

					if (canDeleteAll) {
						delCount = em.deleteAll(new DeleteCondition(definition.getName()));
					} else {
						//DeleteAll不可の場合、OIDを検索して1件1件削除
						Query query = new Query().select(Entity.OID).from(definition.getName());

						DeleteOption option = new DeleteOption(false);

						//物理削除(固定)
						option.setPurge(true);
						//Lockチェックしない(固定)
						option.setCheckLockedByUser(false);

						if (isUserEntity) {
							//ユーザEntityの場合、実行者を除外する
							query.where(new NotEquals(User.OID, executeContext.getClientId()));

							//ユーザEntityの場合、Listenerを実行してt_account削除
							option.setNotifyListeners(true);
						} else {
							//ユーザEntity以外の場合、Listenerの実行は指定されたもの
							option.setNotifyListeners(cond.isNotifyListeners());
						}

						em.searchEntity(query, new Predicate<Entity>() {

							@Override
							public boolean test(Entity entity) {
								em.delete(entity, option);

								delCount++;
								return true;
							}
						});
					}

					result.addMessages(rs("entityport.truncateData", definition.getName(), delCount));
				} catch (EntityApplicationException e) {
					logger.error("An error occurred in the process of remove the data.", e);
					transaction.setRollbackOnly();
					result.setError(true);
					result.addMessages(e.getMessage());
				} catch (EntityRuntimeException e) {
					logger.error("An error occurred in the process of remove the data.", e);
					transaction.setRollbackOnly();
					result.setError(true);
					result.addMessages(e.getMessage());
				}
				return delCount;
			}

		});
	}

	private int readCSV(final InputStream is, final EntityDefinition definition, final EntityDataImportCondition condition, final ZipFile zipFile, final EntityDataImportResult result) {

		return Transaction.required(new Function<Transaction, Integer>() {

			private int currentCount = 0;
			private int registCount = 0;

			@Override
			public Integer apply(Transaction transaction) {

				try (EntityCsvReader reader = new EntityCsvReader(definition, is, true, condition.getPrefixOid())){

					final Iterator<Entity> iterator = reader.iterator();
					final List<String> properties = reader.properties();
					final boolean useCtrl = reader.isUseCtrl();

					while (iterator.hasNext()) {

						Transaction.requiresNew(new Consumer<Transaction>() {

							private int storeCount = 0;

							@Override
							public void accept(Transaction transaction) {

								try {
									while (iterator.hasNext()) {
										storeCount++;

										//Commit件数チェック
										if (condition.getCommitLimit() > 0
												&& (storeCount % (condition.getCommitLimit() + 1) == 0)) {
											break;
										}

										currentCount++;

										Entity entity = null;
										try {
											entity = iterator.next();

											//バイナリファイルの登録
											registBinaryReference(definition, entity, zipFile);

											//Entityの登録
											if (registEntity(condition, entity, definition, useCtrl, properties, currentCount, result)){
												registCount++;
											}
										} catch (EntityDataPortingRuntimeException e) {
											String message = rs("entityport.updateErrMessage", definition.getName(), currentCount, e.getMessage(), getOidStatus(entity));
											if (condition.isErrorSkip()) {
												result.addMessages(message);
												result.errored();
											} else {
												result.errored();
												throw new EntityDataPortingRuntimeException(message , e);
											}
										} catch (EntityApplicationException e) {
											String message = rs("entityport.updateErrMessage", definition.getName(), currentCount, e.getMessage(), getOidStatus(entity));
											if (condition.isErrorSkip()) {
												result.addMessages(message);
												result.errored();
											} else {
												result.errored();
												throw new EntityDataPortingRuntimeException(message , e);
											}
										} catch (EntityRuntimeException e) {
											String message = rs("entityport.updateErrMessage", definition.getName(), currentCount, e.getMessage(), getOidStatus(entity));
											if (condition.isErrorSkip()) {
												result.addMessages(message);
												result.errored();
											} else {
												result.errored();
												throw new EntityDataPortingRuntimeException(message , e);
											}
										}
									}

								} catch (Throwable e) {
									logger.error("An error occurred in the process of import the entity data", e);
									transaction.setRollbackOnly();
									result.setError(true);
									if (e.getMessage() != null) {
										result.addMessages(e.getMessage());
									} else {
										result.addMessages(rs("entityport.importErrMessage", definition.getName(), e.getClass().getName()));
									}
								}

							}

						});

						//Loop内でエラー終了していた場合は抜ける
						if (result.isError()) {
							break;
						}

						if (logger.isDebugEnabled()) {
							logger.debug("commit " + definition.getName() + " data. currentCount=" + currentCount);
						}
						result.addMessages(rs("entityport.commitData", definition.getName(), currentCount));
					}

					if (result.getErrorCount() != 0) {
						result.setError(true);
					}

					String message = rs("entityport.resultInfo", definition.getName(), result.getInsertCount(), result.getUpdateCount(), result.getDeleteCount(), result.getErrorCount());
					if (condition.isNotifyListeners()) {
						message += "(Listner)";
					}
					if (condition.isWithValidation()) {
						message += "(Validation)";
					}
					result.addMessages(message);

				} catch (EntityDataPortingRuntimeException | IOException e) {
					logger.error("An error occurred in the process of import the entity data", e);
					transaction.setRollbackOnly();
					result.setError(true);
					if (e.getMessage() != null) {
						result.addMessages(e.getMessage());
					} else {
						result.addMessages(rs("entityport.importErrMessage", definition.getName(), e.getClass().getName()));
					}
				}
				return registCount;
			}

		});
	}

	private void registBinaryReference(final EntityDefinition definition, final Entity entity, final ZipFile zipFile) {

		if (zipFile == null) {
			return;
		}

		definition.getPropertyList().forEach(property -> {

			Object value = entity.getValue(property.getName());
			if (value != null) {
				if (value instanceof BinaryReference) {
					BinaryReference br = registBinaryReference(definition, (BinaryReference)value, zipFile);
					entity.setValue(property.getName(), br);
				} else if (value instanceof BinaryReference[]) {
					BinaryReference[] brArray = (BinaryReference[])value;
					for (int i = 0; i < brArray.length; i++) {
						brArray[i] = registBinaryReference(definition, (BinaryReference)brArray[i], zipFile);
					}
					entity.setValue(property.getName(), brArray);
				}
			}
		});
	}

	private BinaryReference registBinaryReference(final EntityDefinition definition, final BinaryReference br, final ZipFile zipFile) {

		if (br != null && zipFile != null) {
			String lobId = Long.toString(br.getLobId());

			String entryPath = ENTITY_LOB_DIR + definition.getName() + "." + lobId;
			ZipEntry zipEntry = zipFile.getEntry(entryPath);

			if (zipEntry == null) {
				logger.warn("Fail to find binary data. path = " + entryPath);
			} else {
				try (InputStream is = zipFile.getInputStream(zipEntry)){

					return em.createBinaryReference(br.getName(), br.getType(), is);
				} catch (IOException e) {
					logger.warn("Fail to create binary data. path = " + entryPath);
				}
			}
		}
		return br;
	}

	private boolean registEntity(final EntityDataImportCondition cond, final Entity entity,
			final EntityDefinition definition, final boolean useCtrl, final List<String> properties,
			int index, final EntityDataImportResult result) {

		ExecuteContext executeContext = ExecuteContext.getCurrentContext();
		EntityContext entityContext = EntityContext.getCurrentContext();
		EntityHandler entityHandler = entityContext.getHandlerByName(definition.getName());

		String uniqueKey = cond.getUniqueKey() != null ? cond.getUniqueKey() : Entity.OID;
		String uniqueValue = entity.getValue(uniqueKey);

		//更新の判断(指定されたUniqueKeyでチェック)
		final List<String> storedOidList = new ArrayList<>();
		TargetVersion updateTargetVersion = null;
		if (StringUtil.isNotEmpty(uniqueValue)) {
			if (definition.getVersionControlType().equals(VersionControlType.VERSIONED)
					&& entity.getVersion() != null) {
				//バージョン管理かつバージョンが指定されている場合はバージョンで検索
				Query query = new Query().select(Entity.OID).from(definition.getName());
				query.where(new And(new Equals(uniqueKey, uniqueValue), new Equals(Entity.VERSION, entity.getVersion())));
				query.versioned(true);
				query.getSelect().addHint(new FetchSizeHint(1));
				entityHandler.search(query, null, new Predicate<Object[]>() {
					@Override
					public boolean test(Object[] dataModel) {
						storedOidList.add((String)dataModel[0]);
						return false;	//1件でいい（OIDは一意）
					}
				});
			}

			if (!storedOidList.isEmpty()) {
				updateTargetVersion = TargetVersion.SPECIFIC;

				//UniqueKeyで検索している可能性があるので登録済のOIDをセット
				entity.setOid(storedOidList.get(0));
			} else {
				//バージョンなしで検索
				Query query = new Query().select(Entity.OID).from(definition.getName());
				query.where(new Equals(uniqueKey, uniqueValue));
				query.getSelect().addHint(new FetchSizeHint(1));
				entityHandler.search(query, null, new Predicate<Object[]>() {
					@Override
					public boolean test(Object[] dataModel) {
						storedOidList.add((String)dataModel[0]);
						return false;	//1件でいい（OIDは一意）
					}
				});

				if (!storedOidList.isEmpty()) {
					if (definition.getVersionControlType().equals(VersionControlType.VERSIONED)) {
						updateTargetVersion = TargetVersion.NEW;
					} else {
						updateTargetVersion = TargetVersion.CURRENT_VALID;
					}

					//UniqueKeyで検索している可能性があるので登録済のOIDをセット
					entity.setOid(storedOidList.get(0));
				}
			}
		}

		//Userエンティティの場合の実行ユーザチェック
		if (entity.getOid() != null && User.DEFINITION_NAME.equals(definition.getName())) {
			if (entity.getOid().equals(executeContext.getClientId())) {
				//UserEntityの場合、実行ユーザは更新不可
				result.addMessages(rs("entityport.importUserSkipMessage", definition.getName(), index, entity.getOid(), entity.getValue(User.ACCOUNT_ID)));
				return false;
			}
		}

		//Controlフラグに対する整合性チェック
		if (useCtrl) {
			String ctrlCode = entity.getValue(EntityCsvReader.CTRL_CODE_KEY);
			if (StringUtil.isNotEmpty(ctrlCode)) {
				if (EntityCsvReader.CTRL_INSERT.equals(ctrlCode)) {
					if (updateTargetVersion != null) {
						//既に登録済
						throw new EntityDataPortingRuntimeException(rs("entityport.alreadyExists", definition.getName(), index, entity.getOid(), uniqueKey + "(" + uniqueValue + ")", ctrlCode));
					}
				} else if (EntityCsvReader.CTRL_UPDATE.equals(ctrlCode)) {
					if (updateTargetVersion == null) {
						//更新対象がない
						throw new EntityDataPortingRuntimeException(rs("entityport.notExistsForUpdate", definition.getName(), index, entity.getOid(), uniqueKey + "(" + uniqueValue + ")", ctrlCode));
					}
				} else if (EntityCsvReader.CTRL_DELETE.equals(ctrlCode)) {
					if (updateTargetVersion == null) {
						//削除対象がない
						throw new EntityDataPortingRuntimeException(rs("entityport.notExistsForDelete", definition.getName(), index, entity.getOid(), uniqueKey + "(" + uniqueValue + ")", ctrlCode));
					}

					//削除処理
					DeleteOption option = new DeleteOption(false);
					option.setNotifyListeners(cond.isNotifyListeners());

					em.delete(entity, option);
					auditLogger.info("delete entity," + definition.getName() + ",oid:" + entity.getOid());

					if (logger.isDebugEnabled()) {
						logger.debug("delete " + definition.getName() + " data. oid=" + entity.getOid() + ", csv line no=" + index);
					}

					result.deleted();

					return true;
				}
			}
		}

		//登録、更新処理
		if (updateTargetVersion != null) {
			UpdateOption option = getUpdateOption(cond, properties, updateTargetVersion, entityContext, entityHandler);
			em.update(entity, option);
			auditLogger.info("update entity," + definition.getName() + ",oid:" + entity.getOid() + " " + option);

			if (logger.isDebugEnabled()) {
				logger.debug("update " + definition.getName() + " data. oid=" + entity.getOid() + ", csv line no=" + index);
			}

			result.updated();
		} else {
			InsertOption option = new InsertOption();
			//OID,AutoNumberは指定されていればそれを利用するためfalse
			option.setRegenerateOid(false);
			option.setRegenerateAutoNumber(false);

			option.setNotifyListeners(cond.isNotifyListeners());
			option.setWithValidation(cond.isWithValidation());

			String oid = em.insert(entity, option);
			auditLogger.info("insert entity," + definition.getName() + ",oid:" + oid + " " + option);

			if (logger.isDebugEnabled()) {
				logger.debug("insert " + definition.getName() + " data. oid=" + oid + ", csv line no=" + index);
			}

			result.inserted();
		}

		return true;
	}

	private UpdateOption getUpdateOption(final EntityDataImportCondition cond, final List<String> properties, TargetVersion updateTargetVersion, EntityContext entityContext, EntityHandler entityHandler) {

		UpdateOption option = new UpdateOption(false);
		option.setNotifyListeners(cond.isNotifyListeners());
		if (cond.isUpdateDisupdatableProperty()) {
			//更新不可項目も対象にする場合はValidationを実行しない
			option.setWithValidation(false);
		} else {
			//更新不可項目を対象にしない場合はValidationの実行は指定されたもの
			option.setWithValidation(cond.isWithValidation());
		}

		//除外対象のプロパティチェック
		List<String> execOptionProperties = new ArrayList<String>();
		//headerはmultipleの場合、同じものが含まれるためdistinct(set化で対応)
		for (String propName : properties.stream().collect(Collectors.toSet())) {
			PropertyHandler ph = entityHandler.getProperty(propName, entityContext);
			if (ph != null) {
				if (cond.isUpdateDisupdatableProperty()) {
					//更新不可項目も含む場合

					//キー項目は除外
					if (propName.equals(Entity.OID) || propName.equals(Entity.VERSION)) {
						continue;
					}
					//被参照項目は除外
					if (ph instanceof ReferencePropertyHandler
							&& ((ReferencePropertyHandler) ph).getMetaData().getMappedByPropertyMetaDataId() != null) {
						continue;
					}
					//仮想項目は除外
					if (ph instanceof PrimitivePropertyHandler
							&& ((MetaPrimitiveProperty) ph.getMetaData()).getType().isVirtual()) {
						continue;
					}
					execOptionProperties.add(propName);
				} else {
					//更新不可項目を含まない場合
					if (ph.getMetaData().isUpdatable()) {
						execOptionProperties.add(propName);
					}
				}
			}
		}

		option.setUpdateProperties(execOptionProperties);
		option.setTargetVersion(updateTargetVersion);
		option.setForceUpdate(cond.isFourceUpdate());

		return option;
	}

	private String getOidStatus(Entity entity) {
		return (entity == null ? "UnRead" : entity.getOid() != null ? entity.getOid() : "New");
	}

	private String rs(String key, Object... args) {
		return ToolsResourceBundleUtil.resourceString(key, args);
	}

}

package jp.gr.java_conf.mu.csb.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class DynamoDBUtil {

	private LambdaLogger logger;
	private AmazonDynamoDB dynamoDBClient;
	private final static int RETRYCOUNT_PUTITEM = 3;
	private final static int RETRYINTERVAL_PUTITEM = 30000;
	private final static int RETRYCOUNT_UPDATEITEM = 3;
	private final static int RETRYINTERVAL_UPDATEITEM = 30000;
	
	
	// コンストラクタ
	public DynamoDBUtil(LambdaLogger logger) {
		this.logger = logger;
		this.dynamoDBClient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_NORTHEAST_1).build();
	}

	// LSIを利用したクエリ検索
	public List<Map<String, AttributeValue>> query(String tableName, String hashName, String hashValue,
			String lsiKeyName, ComparisonOperator lsiOpe, String lsiValue, String indexName) {
		logger.log("--------------query start--------------");
		logger.log("テーブル名: " + tableName);
		logger.log("条件1: " + hashName + " = " + hashValue);
		logger.log("条件2: " + lsiKeyName + " " + lsiOpe + " " + lsiValue);

		// ハッシュキーの条件
		Map<String, Condition> keyConditions = new HashMap<String, Condition>();
		keyConditions.put(hashName, new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
				.withAttributeValueList(new AttributeValue().withS(hashValue)));

		// インデックスの条件
		Condition lsiKeyCondition = new Condition().withComparisonOperator(lsiOpe)
				.withAttributeValueList(new AttributeValue().withS(lsiValue));
		keyConditions.put(lsiKeyName, lsiKeyCondition);

		// リクエストを組み立て
		QueryRequest queryRequest = new QueryRequest().withTableName(tableName).withKeyConditions(keyConditions);
		queryRequest.setIndexName(indexName);

		// クエリ実施
		QueryResult result = dynamoDBClient.query(queryRequest);
		for (Map<String, AttributeValue> item : result.getItems()) {
			printItem(item);
		}
		logger.log("--------------query end--------------");

		return result.getItems();
	}

	// 条件を指定してスキャン
	public List<Map<String, AttributeValue>> scan(String tableName, String condKey, ComparisonOperator ope,
			int condValue) {
		logger.log("--------------scan start--------------");
		logger.log("テーブル名: " + tableName);
		logger.log("条件: " + condKey + " " + ope + " " + condValue);

		Condition scanFilterCondition = new Condition().withComparisonOperator(ope)
				.withAttributeValueList(new AttributeValue().withN(condValue + ""));
		Map<String, Condition> conditions = new HashMap<String, Condition>();
		conditions.put(condKey, scanFilterCondition);
		ScanRequest scanRequest = new ScanRequest().withTableName(tableName).withScanFilter(conditions);
		ScanResult result = dynamoDBClient.scan(scanRequest);
		for (Map<String, AttributeValue> item : result.getItems()) {
			printItem(item);
		}
		logger.log("--------------scan end--------------");

		return result.getItems();
	}

	// アイテムを1件取得(hashName = hashValueの条件で検索)
	public Map<String, AttributeValue> getItem(String tableName, String hashName, String hashValue) {
		logger.log("--------------getItem start--------------");
		logger.log("テーブル名: " + tableName);
		logger.log("条件: " + hashName + "=" + hashValue);

		// 検索条件
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashName, new AttributeValue().withS(hashValue));
		GetItemRequest getItemRequest = new GetItemRequest().withTableName(tableName).withKey(key);

		// 検索実施
		GetItemResult result = dynamoDBClient.getItem(getItemRequest);
		printItem(result.getItem(), "get");
		logger.log("--------------getItem end----------------");

		return result.getItem();
	}

	// アイテムを1件取得(hashName = hashValue、rangeName = rangeValueの条件で検索)
	public Map<String, AttributeValue> getItem(String tableName, String hashName, String hashValue, String rangeName,
			String rangeValue) {
		logger.log("--------------getItem start--------------");
		logger.log("テーブル名: " + tableName);
		logger.log("条件: " + hashName + "=" + hashValue + "," + rangeName + "=" + rangeValue);

		// 検索条件
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashName, new AttributeValue().withS(hashValue));
		key.put(rangeName, new AttributeValue().withS(rangeValue));

		// 検索実施
		GetItemRequest getItemRequest = new GetItemRequest().withTableName(tableName).withKey(key);
		GetItemResult result = dynamoDBClient.getItem(getItemRequest);
		printItem(result.getItem(), "get");
		logger.log("--------------getItem end----------------");

		return result.getItem();
	}

	// アイテム1件を登録
	public void putItem(String tableName, Map<String, AttributeValue> item) {
		// 登録日列を追加
		String updateDate = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());
		item.put("update_date", new AttributeValue().withS(updateDate));
		// 準備
		PutItemRequest putItemRequest = new PutItemRequest().withTableName(tableName).withItem(item);
		int count = 0;
		boolean success = false;
		// 登録実施
		do {
			try {
				dynamoDBClient.putItem(putItemRequest);
				success = true;
				printItem(item, "put");
			} catch (Exception e1) {
				logger.log("putItem失敗 : " + e1.getMessage());
				// 失敗した場合は待機後に再実行
				if (count < RETRYCOUNT_PUTITEM) {
					try {
						Thread.sleep(RETRYINTERVAL_PUTITEM);
					} catch (InterruptedException e2) {
					}
				}
			}
		} while (!success && count < RETRYCOUNT_PUTITEM);
	}

	// アイテム1件を更新
	public void updateItem(String tableName, Map<String, AttributeValue> key, Map<String, AttributeValueUpdate> item) {
		// 登録日列を追加
		String updateDate = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());
		item.put("update_date", new AttributeValueUpdate().withAction(AttributeAction.PUT)
				.withValue(new AttributeValue().withS(updateDate)));
		// 準備
		UpdateItemRequest updateItemRequest = new UpdateItemRequest().withTableName(tableName).withKey(key)
				.withAttributeUpdates(item);

		int count = 0;
		boolean success = false;
		// 登録実施
		do {
			try {
				dynamoDBClient.updateItem(updateItemRequest);
				success = true;
				printItem(key, item, "update");
			} catch (Exception e1) {
				logger.log("putItem失敗 : " + e1.getMessage());
				// 失敗した場合は待機後に再実行
				if (count < RETRYCOUNT_UPDATEITEM) {
					try {
						Thread.sleep(RETRYINTERVAL_UPDATEITEM);
					} catch (InterruptedException e2) {
					}
				}
			}
		} while (!success && count < RETRYCOUNT_UPDATEITEM);
	}

	// アイテムの情報を表示
	public void printItem(Map<String, AttributeValue> updateKey, Map<String, AttributeValueUpdate> updateItem,
			String message) {
		logger.log("-------DynamoDBItem(" + message + ")-------");
		logger.log("--updateKey--");

		if (updateKey == null) {
			logger.log("空のキー");
		} else {
			for (Map.Entry<String, AttributeValue> item : updateKey.entrySet()) {
				String attributeName = item.getKey();
				AttributeValue value = item.getValue();
				logger.log(attributeName + " " + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
						+ (value.getN() == null ? "" : "N=[" + value.getN() + "]")
						+ (value.getB() == null ? "" : "B=[" + value.getB() + "]")
						+ (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
						+ (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]")
						+ (value.getBS() == null ? "" : "BS=[" + value.getBS() + "] n"));
			}
		}

		logger.log("--updateValue--");
		if (updateItem == null) {
			logger.log("空のアイテム");
		} else {
			for (Entry<String, AttributeValueUpdate> item : updateItem.entrySet()) {
				String attributeName = item.getKey();
				AttributeValue value = item.getValue().getValue();
				logger.log(attributeName + " " + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
						+ (value.getN() == null ? "" : "N=[" + value.getN() + "]")
						+ (value.getB() == null ? "" : "B=[" + value.getB() + "]")
						+ (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
						+ (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]")
						+ (value.getBS() == null ? "" : "BS=[" + value.getBS() + "] n"));
			}
		}
		logger.log("--------------------------");
	}

	// アイテムの情報を表示
	public void printItem(Map<String, AttributeValue> attributeList, String message) {
		logger.log("-------DynamoDBItem(" + message + ")-------");

		if (attributeList == null) {
			logger.log("空アイテム");
		} else {
			for (Map.Entry<String, AttributeValue> item : attributeList.entrySet()) {
				String attributeName = item.getKey();
				AttributeValue value = item.getValue();
				logger.log(attributeName + " " + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
						+ (value.getN() == null ? "" : "N=[" + value.getN() + "]")
						+ (value.getB() == null ? "" : "B=[" + value.getB() + "]")
						+ (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
						+ (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]")
						+ (value.getBS() == null ? "" : "BS=[" + value.getBS() + "] n"));
			}
		}
		logger.log("--------------------------");
	}

	// アイテムの情報を表示
	public void printItem(Map<String, AttributeValue> attributeList) {
		printItem(attributeList, "");
	}
}

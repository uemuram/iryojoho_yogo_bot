package jp.gr.java_conf.mu.csb.util;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class DynamoDBUtil {

	private LambdaLogger logger;
	private AmazonDynamoDB dynamoDBClient;

	// コンストラクタ
	public DynamoDBUtil(LambdaLogger logger) {
		this.logger = logger;
		this.dynamoDBClient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_NORTHEAST_1).build();
	}

	// public void getItem(String tableName, String hashName, int id) {
	// System.out.println("--- " + tableName + " ---");
	// Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
	// key.put(hashName, new AttributeValue().withN(Integer.toString(id)));
	// GetItemRequest getItemRequest = new
	// GetItemRequest().withTableName(tableName).withKey(key);
	// GetItemResult result = dynamoDBClient.getItem(getItemRequest);
	// printItem(result.getItem());
	// }

	// アイテムを1件取得
	public GetItemResult getItem(String tableName, String hashName, String id) {
		logger.log("-------getItem(" + tableName + ", " + hashName + "=" + id + ")-------");
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashName, new AttributeValue().withS(id));
		GetItemRequest getItemRequest = new GetItemRequest().withTableName(tableName).withKey(key);
		GetItemResult result = dynamoDBClient.getItem(getItemRequest);
		printItem(result.getItem());
		return result;
	}

	// アイテムの情報を表示
	public void printItem(Map<String, AttributeValue> attributeList) {
		logger.log("-------DynamoDBItem-------");
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
		logger.log("--------------------------");
	}
}

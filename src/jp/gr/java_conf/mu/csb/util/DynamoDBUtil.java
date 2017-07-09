package jp.gr.java_conf.mu.csb.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class DynamoDBUtil {

	private LambdaLogger logger;
	private AmazonDynamoDB dynamoDBClient;
	private final static int RETRYCOUNT_PUTITEM = 3;
	private final static int RETRYINTERVAL_PUTITEM = 30000;

	// RXgN^
	public DynamoDBUtil(LambdaLogger logger) {
		this.logger = logger;
		this.dynamoDBClient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_NORTHEAST_1).build();
	}

	// ACe๐1ๆพ(hashName = hashValueฬ๐ล๕)
	public Map<String, AttributeValue> getItem(String tableName, String hashName, String hashValue) {
		logger.log("--------------getItem start--------------");
		logger.log("e[uผ: " + tableName);
		logger.log("๐: " + hashName + "=" + hashValue);

		// ๕๐
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashName, new AttributeValue().withS(hashValue));
		GetItemRequest getItemRequest = new GetItemRequest().withTableName(tableName).withKey(key);

		// ๕ภ{
		GetItemResult result = dynamoDBClient.getItem(getItemRequest);
		printItem(result.getItem(), "get");
		logger.log("--------------getItem end----------------");

		return result.getItem();
	}

	// ACe๐1ๆพ(hashName = hashValueArangeName = rangeValueฬ๐ล๕)
	public Map<String, AttributeValue> getItem(String tableName, String hashName, String hashValue, String rangeName,
			String rangeValue) {
		logger.log("--------------getItem start--------------");
		logger.log("e[uผ: " + tableName);
		logger.log("๐: " + hashName + "=" + hashValue + "," + rangeName + "=" + rangeValue);

		// ๕๐
		Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put(hashName, new AttributeValue().withS(hashValue));
		key.put(rangeName, new AttributeValue().withS(rangeValue));

		// ๕ภ{
		GetItemRequest getItemRequest = new GetItemRequest().withTableName(tableName).withKey(key);
		GetItemResult result = dynamoDBClient.getItem(getItemRequest);
		printItem(result.getItem(), "get");
		logger.log("--------------getItem end----------------");

		return result.getItem();
	}

	// ACe1๐o^
	public void putItem(String tableName, Map<String, AttributeValue> item) {
		// o^๚๑๐วม
		String updateDate = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now());
		item.put("update_date", new AttributeValue().withS(updateDate));
		// ๕
		PutItemRequest putItemRequest = new PutItemRequest().withTableName(tableName).withItem(item);
		int count = 0;
		boolean success = false;
		// o^ภ{
		do {
			try {
				dynamoDBClient.putItem(putItemRequest);
				success = true;
				printItem(item, "put");
			} catch (Exception e1) {
				logger.log("putItemธs : " + e1.getMessage());
				// ธsตฝ๊อา@ใษฤภs
				if (count < RETRYCOUNT_PUTITEM) {
					try {
						Thread.sleep(RETRYINTERVAL_PUTITEM);
					} catch (InterruptedException e2) {
					}
				}
			}
		} while (!success && count < RETRYCOUNT_PUTITEM);
	}

	// ACeฬ๎๑๐\ฆ
	public void printItem(Map<String, AttributeValue> attributeList, String message) {
		logger.log("-------DynamoDBItem(" + message + ")-------");

		if (attributeList == null) {
			logger.log("๓ACe");
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

	// ACeฬ๎๑๐\ฆ
	public void printItem(Map<String, AttributeValue> attributeList) {
		printItem(attributeList, "");
	}
}

package jp.gr.java_conf.mu.iyb.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jp.gr.java_conf.mu.iyb.util.DynamoDBUtil;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class GetRecord implements RequestHandler<Object, Object> {
	private LambdaLogger logger;

	private static final String TABLE_NAME_SCRAMBLE = System.getenv("table_name_scramble");
	private static final String TABLE_NAME_STATUS = System.getenv("table_name_status");
	private static final String TABLE_NAME_RECORD = System.getenv("table_name_record");
	private static final String TABLE_NAME_USER = System.getenv("table_name_user");

	public Object handleRequest(Object input, Context context) {
		logger = context.getLogger();
		logger.log("Input: " + input);

		// DynamoDB���p����
		DynamoDBUtil dynamoDBUtil = new DynamoDBUtil(logger);
		Map<String, AttributeValue> item;

		// Twitter���p����
		// ���ϐ�����e��L�[��ݒ�
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(System.getenv("twitter4j_oauth_consumerKey"))
				.setOAuthConsumerSecret(System.getenv("twitter4j_oauth_consumerSecret"))
				.setOAuthAccessToken(System.getenv("twitter4j_oauth_accessToken"))
				.setOAuthAccessTokenSecret(System.getenv("twitter4j_oauth_accessTokenSecret"));
		Configuration configuration = cb.build();
		TwitterFactory tf = new TwitterFactory(configuration);
		Twitter twitter = tf.getInstance();

		// �O��܂łŎ�荞��ID���擾
		item = dynamoDBUtil.getItem(TABLE_NAME_STATUS, "key", "get_record_since_id");
		long sinceId = Long.parseLong(item.get("value").getS());
		logger.log("ID: " + sinceId + " ����̕ԐM���擾");

		// -----------------------------�ԐM�擾���� ��������-----------------------------
		// ������
		ResponseList<Status> responseList;
		ResponseList<Status> tmpResponseList;

		// ��x�Ɏ擾����ԐM��
		int count = 20;
		long maxId = 0L;
		Paging paging;

		// �܂�1�y�[�W�ڂ��擾
		paging = new Paging(1, count);
		paging.setSinceId(sinceId);
		try {
			responseList = twitter.getMentionsTimeline(paging);
			logger.log("�ԐM�擾 " + responseList.size() + " ��");
		} catch (TwitterException e1) {
			logger.log("�ԐM�擾���s : " + e1.getErrorMessage());
			throw new RuntimeException(e1);
		}
		boolean endFlag = false;
		if (responseList.size() < count) {
			// �擾���ʂ��擾������菭�Ȃ������ꍇ�́A����ȏ�Ȃ��̂ŏI��
			endFlag = true;
		} else {
			// �擾�ł����ꍇ�͍Ō��ID���擾���ď������s
			maxId = responseList.get(responseList.size() - 1).getId() - 1;
		}

		// 1�y�[�W�ڂ��擾�ł����ꍇ��2�y�[�W�ڈȍ~���擾
		while (!endFlag) {
			paging = new Paging();
			paging.setCount(count);
			paging.setMaxId(maxId);
			paging.setSinceId(sinceId);
			try {
				tmpResponseList = twitter.getMentionsTimeline(paging);
				logger.log("�ԐM�擾 " + tmpResponseList.size() + " �� (maxId= " + maxId + " )");
			} catch (TwitterException e1) {
				logger.log("�ԐM�擾���s : " + e1.getErrorMessage());
				throw new RuntimeException(e1);
			}
			// �擾���ʂ�ǉ�
			responseList.addAll(tmpResponseList);
			if (tmpResponseList.size() < count) {
				// �擾���ʂ��擾������菭�Ȃ������ꍇ�́A����ȏ�Ȃ��̂ŏI��
				endFlag = true;
			} else {
				// �擾�ł����ꍇ�͍Ō��ID���擾���ď������s
				maxId = tmpResponseList.get(tmpResponseList.size() - 1).getId() - 1;
			}
		}
		logger.log("�v " + responseList.size() + " ���̕ԐM���擾");
		// -----------------------------�ԐM�擾���� �����܂�-----------------------------

		// �L���b�V��
		// �c�C�[�g�ς݂̃X�N�����u���̃c�C�[�gID
		Map<String, Boolean> scrambleIdCache = new HashMap<String, Boolean>();
		// �o�^����L�^
		Map<String, Status> recordCache = new HashMap<String, Status>();
		// DB�o�^�ς݃��[�U�̍��܂ł̃c�C�[�g��
		Map<String, Integer> userTweetCountCache = new HashMap<String, Integer>();

		// �Â����Ƀ��[�v
		int lastIndex = responseList.size() - 1;
		int loopCount = 0;
		for (int i = lastIndex; i >= 0; i--) {
			loopCount++;

			// 10���1��A�L���b�V�����ꂽ���e��DB�ɔ��f
			if (loopCount % 10 == 0) {
				updateDb(recordCache, userTweetCountCache, dynamoDBUtil);
				recordCache.clear();
			}

			Status status = responseList.get(i);
			logger.log(loopCount
					+ " ---------------------------------------------------------------------------------------------------------");
			logger.log("���[�U��:" + status.getUser().getName());
			logger.log("���[�U��(�\����):" + status.getUser().getScreenName());
			logger.log("�ԐM���e:" + status.getText());
			logger.log("�ԐMID:" + status.getId() + "");
			logger.log("�ԐM������:" + status.getCreatedAt() + "");
			logger.log("�ԐM�惆�[�U��(�\����):" + status.getInReplyToScreenName());
			logger.log("�ԐM��c�C�[�gID:" + status.getInReplyToStatusId() + "");

			// �ԐM��c�C�[�gID���Ƃ�Ȃ��ꍇ(����̃c�C�[�g�ɑ΂���ԐM�ł͂Ȃ��ꍇ)�̓X�L�b�v
			if (status.getInReplyToStatusId() == -1) {
				logger.log("�X�L�b�v(�ԐM�ȊO)");
				continue;
			}
			// �ԐM��ID
			String replyToId = status.getInReplyToStatusId() + "";
			if (scrambleIdCache.get(replyToId) == null) {
				// DB����A�ߋ��ɐ��������X�N�����u�����擾���ăL���b�V��
				item = dynamoDBUtil.getItem(TABLE_NAME_SCRAMBLE, "id", replyToId);
				if (item == null) {
					scrambleIdCache.put(replyToId, false);
				} else {
					scrambleIdCache.put(replyToId, true);
				}
			}
			// �ߋ��ɐ��������X�N�����u���ɑ΂���ԐM�ł͂Ȃ��ꍇ�̓X�L�b�v
			if (!scrambleIdCache.get(replyToId)) {
				logger.log("�X�L�b�v(�Ώۃc�C�[�g�ȊO�ւ̕ԐM)");
				continue;
			}
			// �c�C�[�g����A�ŏ��ɏo�Ă��鐔�������𒊏o�ł��Ȃ���΃X�L�b�v
			String recordStr = getRecordStr(status.getText());
			if (recordStr == null) {
				continue;
			}

			// �����܂œ��B������ADB�o�^�ΏۂƂ��ăL���b�V���ɓ����(�ԐM��ID���d�����Ă����ꍇ�͐V���������D�悳���)
			String screenName = status.getUser().getScreenName();
			String key = screenName + "_" + replyToId;
			recordCache.put(key, status);
			// �Ώۃ��[�U�̖��W�v�c�C�[�g����DB����擾
			if (userTweetCountCache.get(screenName) == null) {
				item = dynamoDBUtil.getItem(TABLE_NAME_USER, "user_name", screenName);
				if (item == null) {
					userTweetCountCache.put(screenName, Integer.valueOf(0));
					// DB���o�^�̏ꍇ�͂����œo�^���Ă���
					Map<String, AttributeValue> putItem = new HashMap<String, AttributeValue>();
					putItem.put("user_name", new AttributeValue().withS(screenName));
					putItem.put("not_summarized_tweet_count", new AttributeValue().withN("0"));
					putItem.put("oldest_summarized_tweet_id", new AttributeValue().withS("0"));
					dynamoDBUtil.putItem(TABLE_NAME_USER, putItem);

				} else {
					userTweetCountCache.put(screenName, Integer.valueOf(item.get("not_summarized_tweet_count").getN()));
				}
			}
		}

		// �ŏI�I�ɂ�����xDB�X�V
		updateDb(recordCache, userTweetCountCache, dynamoDBUtil);

		logger.log(
				"---------------------------------------------------------------------------------------------------------");
		return null;
	}

	// �L�^��DB�ɔ��f
	private void updateDb(Map<String, Status> recordCache, Map<String, Integer> userTweetCountCache,
			DynamoDBUtil dynamoDBUtil) {
		logger.log("*******DB���f�J�n*******");

		long maxTweetId = 0L;

		// �L�^���f
		for (Map.Entry<String, Status> entry : recordCache.entrySet()) {
			String key = entry.getKey();
			Status status = entry.getValue();
			logger.log("****" + key + "****(�L�^)");

			// �c�C�[�g�����𒊏o
			LocalDateTime localDateTime = LocalDateTime.parse(status.getCreatedAt().toString(),
					DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"));
			String createdAt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(localDateTime);
			// �L�^�𒊏o
			String recordStr = getRecordStr(status.getText());
			// �ő�̃c�C�[�gID�𒊏o
			if (status.getId() > maxTweetId) {
				maxTweetId = status.getId();
			}

			// �L�^��DB�ɓo�^
			String screenName = status.getUser().getScreenName();
			Map<String, AttributeValue> putItem = new HashMap<String, AttributeValue>();
			putItem.put("user_name", new AttributeValue().withS(screenName));
			putItem.put("reply_to_id", new AttributeValue().withS(status.getInReplyToStatusId() + ""));
			putItem.put("reply_id", new AttributeValue().withS(status.getId() + ""));
			putItem.put("record", new AttributeValue().withN(recordStr));
			putItem.put("reply_date", new AttributeValue().withS(createdAt));
			dynamoDBUtil.putItem(TABLE_NAME_RECORD, putItem);

			// �Ώۃ��[�U�̖��W�v�c�C�[�g�����C���N�������g���ăL���b�V��
			int userTweetCount = userTweetCountCache.get(screenName) + 1;
			userTweetCountCache.put(screenName, Integer.valueOf(userTweetCount));

			logger.log("****" + screenName + "****(���W�v�c�C�[�g��)");
			// ���W�v�c�C�[�g����DB�ɓo�^
			Map<String, AttributeValue> updateKey = new HashMap<String, AttributeValue>();
			updateKey.put("user_name", new AttributeValue().withS(screenName));

			Map<String, AttributeValueUpdate> updateItem = new HashMap<String, AttributeValueUpdate>();
			updateItem.put("not_summarized_tweet_count", new AttributeValueUpdate().withAction(AttributeAction.PUT)
					.withValue(new AttributeValue().withN(userTweetCount + "")));
			dynamoDBUtil.updateItem(TABLE_NAME_USER, updateKey, updateItem);
		}

		// sinceID(�ǂ��܂�DB�ɔ��f������)���X�V
		if (maxTweetId > 0L) {
			Map<String, AttributeValue> putItem = new HashMap<String, AttributeValue>();
			putItem.put("key", new AttributeValue().withS("get_record_since_id"));
			putItem.put("value", new AttributeValue().withS(maxTweetId + ""));
			dynamoDBUtil.putItem(TABLE_NAME_STATUS, putItem);
		}
		logger.log("*******DB���f�I��*******");
	}

	// �c�C�[�g�̕����񂩂�L�^�𒊏o����B
	// �L�^�ɂȂ镶���񂪂Ȃ����null��Ԃ�
	private String getRecordStr(String tweetText) {
		// �c�C�[�g����A�ŏ��ɏo�Ă��鐔�������𒊏o
		Matcher matcher = Pattern.compile("^@[^ ]+? \\D*(\\d+\\.\\d+|\\d+).*$").matcher(tweetText);
		String recordStr;
		if (matcher.find()) {
			recordStr = matcher.group(1);
			logger.log("�L�^�𒊏o1: " + recordStr);
		} else {
			// �����������擾�ł��Ȃ������ꍇ�͏I��
			logger.log("�X�L�b�v(�L�^�Ȃ�)");
			return null;
		}
		try {
			// ������3�ʂŎl�̌ܓ�
			recordStr = String.format("%.2f", Double.parseDouble(recordStr));
			logger.log("�L�^�𒊏o2: " + recordStr);
		} catch (Exception e) {
			// �t�H�[�}�b�g�ϊ��Ɏ��s�����ꍇ�͏I��
			logger.log("�X�L�b�v(�t�H�[�}�b�g�������s)");
			return null;
		}
		return recordStr;
	}

}

package jp.gr.java_conf.mu.csb.handler;

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

import jp.gr.java_conf.mu.csb.util.DynamoDBUtil;
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

		// DynamoDB利用準備
		DynamoDBUtil dynamoDBUtil = new DynamoDBUtil(logger);
		Map<String, AttributeValue> item;

		// Twitter利用準備
		// 環境変数から各種キーを設定
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(System.getenv("twitter4j_oauth_consumerKey"))
				.setOAuthConsumerSecret(System.getenv("twitter4j_oauth_consumerSecret"))
				.setOAuthAccessToken(System.getenv("twitter4j_oauth_accessToken"))
				.setOAuthAccessTokenSecret(System.getenv("twitter4j_oauth_accessTokenSecret"));
		Configuration configuration = cb.build();
		TwitterFactory tf = new TwitterFactory(configuration);
		Twitter twitter = tf.getInstance();

		// 前回までで取り込んだIDを取得
		item = dynamoDBUtil.getItem(TABLE_NAME_STATUS, "key", "get_record_since_id");
		long sinceId = Long.parseLong(item.get("value").getS());
		logger.log("ID: " + sinceId + " より後の返信を取得");

		// -----------------------------返信取得処理 ここから-----------------------------
		// 初期化
		ResponseList<Status> responseList;
		ResponseList<Status> tmpResponseList;

		// 一度に取得する返信数
		int count = 20;
		long maxId = 0L;
		Paging paging;

		// まず1ページ目を取得
		paging = new Paging(1, count);
		paging.setSinceId(sinceId);
		try {
			responseList = twitter.getMentionsTimeline(paging);
			logger.log("返信取得 " + responseList.size() + " 件");
		} catch (TwitterException e1) {
			logger.log("返信取得失敗 : " + e1.getErrorMessage());
			throw new RuntimeException(e1);
		}
		boolean endFlag = false;
		if (responseList.size() < count) {
			// 取得結果が取得件数より少なかった場合は、それ以上ないので終了
			endFlag = true;
		} else {
			// 取得できた場合は最後のIDを取得して処理続行
			maxId = responseList.get(responseList.size() - 1).getId() - 1;
		}

		// 1ページ目が取得できた場合は2ページ目以降を取得
		while (!endFlag) {
			paging = new Paging();
			paging.setCount(count);
			paging.setMaxId(maxId);
			paging.setSinceId(sinceId);
			try {
				tmpResponseList = twitter.getMentionsTimeline(paging);
				logger.log("返信取得 " + tmpResponseList.size() + " 件 (maxId= " + maxId + " )");
			} catch (TwitterException e1) {
				logger.log("返信取得失敗 : " + e1.getErrorMessage());
				throw new RuntimeException(e1);
			}
			// 取得結果を追加
			responseList.addAll(tmpResponseList);
			if (tmpResponseList.size() < count) {
				// 取得結果が取得件数より少なかった場合は、それ以上ないので終了
				endFlag = true;
			} else {
				// 取得できた場合は最後のIDを取得して処理続行
				maxId = tmpResponseList.get(tmpResponseList.size() - 1).getId() - 1;
			}
		}
		logger.log("計 " + responseList.size() + " 件の返信を取得");
		// -----------------------------返信取得処理 ここまで-----------------------------

		// キャッシュ
		// ツイート済みのスクランブルのツイートID
		Map<String, Boolean> scrambleIdCache = new HashMap<String, Boolean>();
		// 登録する記録
		Map<String, Status> recordCache = new HashMap<String, Status>();
		// DB登録済みユーザの今までのツイート数
		Map<String, Integer> userTweetCountCache = new HashMap<String, Integer>();

		// 古い順にループ
		int lastIndex = responseList.size() - 1;
		int loopCount = 0;
		for (int i = lastIndex; i >= 0; i--) {
			loopCount++;

			// 10回に1回、キャッシュされた内容をDBに反映
			if (loopCount % 10 == 0) {
				updateDb(recordCache, userTweetCountCache, dynamoDBUtil);
				recordCache.clear();
			}

			Status status = responseList.get(i);
			logger.log(loopCount
					+ " ---------------------------------------------------------------------------------------------------------");
			logger.log("ユーザ名:" + status.getUser().getName());
			logger.log("ユーザ名(表示名):" + status.getUser().getScreenName());
			logger.log("返信内容:" + status.getText());
			logger.log("返信ID:" + status.getId() + "");
			logger.log("返信生成日:" + status.getCreatedAt() + "");
			logger.log("返信先ユーザ名(表示名):" + status.getInReplyToScreenName());
			logger.log("返信先ツイートID:" + status.getInReplyToStatusId() + "");

			// 返信先ツイートIDがとれない場合(特定のツイートに対する返信ではない場合)はスキップ
			if (status.getInReplyToStatusId() == -1) {
				logger.log("スキップ(返信以外)");
				continue;
			}
			// 返信先ID
			String replyToId = status.getInReplyToStatusId() + "";
			if (scrambleIdCache.get(replyToId) == null) {
				// DBから、過去に生成したスクランブルを取得してキャッシュ
				item = dynamoDBUtil.getItem(TABLE_NAME_SCRAMBLE, "id", replyToId);
				if (item == null) {
					scrambleIdCache.put(replyToId, false);
				} else {
					scrambleIdCache.put(replyToId, true);
				}
			}
			// 過去に生成したスクランブルに対する返信ではない場合はスキップ
			if (!scrambleIdCache.get(replyToId)) {
				logger.log("スキップ(対象ツイート以外への返信)");
				continue;
			}
			// ツイートから、最初に出てくる数字部分を抽出できなければスキップ
			String recordStr = getRecordStr(status.getText());
			if (recordStr == null) {
				continue;
			}

			// ここまで到達したら、DB登録対象としてキャッシュに入れる(返信先IDが重複していた場合は新しい方が優先される)
			String screenName = status.getUser().getScreenName();
			String key = screenName + "_" + replyToId;
			recordCache.put(key, status);
			// 対象ユーザの未集計ツイート数をDBから取得
			if (userTweetCountCache.get(screenName) == null) {
				item = dynamoDBUtil.getItem(TABLE_NAME_USER, "user_name", screenName);
				if (item == null) {
					userTweetCountCache.put(screenName, Integer.valueOf(0));
					// DB未登録の場合はここで登録しておく
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

		// 最終的にもう一度DB更新
		updateDb(recordCache, userTweetCountCache, dynamoDBUtil);

		logger.log(
				"---------------------------------------------------------------------------------------------------------");
		return null;
	}

	// 記録をDBに反映
	private void updateDb(Map<String, Status> recordCache, Map<String, Integer> userTweetCountCache,
			DynamoDBUtil dynamoDBUtil) {
		logger.log("*******DB反映開始*******");

		long maxTweetId = 0L;

		// 記録反映
		for (Map.Entry<String, Status> entry : recordCache.entrySet()) {
			String key = entry.getKey();
			Status status = entry.getValue();
			logger.log("****" + key + "****(記録)");

			// ツイート日時を抽出
			LocalDateTime localDateTime = LocalDateTime.parse(status.getCreatedAt().toString(),
					DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"));
			String createdAt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(localDateTime);
			// 記録を抽出
			String recordStr = getRecordStr(status.getText());
			// 最大のツイートIDを抽出
			if (status.getId() > maxTweetId) {
				maxTweetId = status.getId();
			}

			// 記録をDBに登録
			String screenName = status.getUser().getScreenName();
			Map<String, AttributeValue> putItem = new HashMap<String, AttributeValue>();
			putItem.put("user_name", new AttributeValue().withS(screenName));
			putItem.put("reply_to_id", new AttributeValue().withS(status.getInReplyToStatusId() + ""));
			putItem.put("reply_id", new AttributeValue().withS(status.getId() + ""));
			putItem.put("record", new AttributeValue().withN(recordStr));
			putItem.put("reply_date", new AttributeValue().withS(createdAt));
			dynamoDBUtil.putItem(TABLE_NAME_RECORD, putItem);

			// 対象ユーザの未集計ツイート数をインクリメントしてキャッシュ
			int userTweetCount = userTweetCountCache.get(screenName) + 1;
			userTweetCountCache.put(screenName, Integer.valueOf(userTweetCount));

			logger.log("****" + screenName + "****(未集計ツイート数)");
			// 未集計ツイート数をDBに登録
			Map<String, AttributeValue> updateKey = new HashMap<String, AttributeValue>();
			updateKey.put("user_name", new AttributeValue().withS(screenName));

			Map<String, AttributeValueUpdate> updateItem = new HashMap<String, AttributeValueUpdate>();
			updateItem.put("not_summarized_tweet_count", new AttributeValueUpdate().withAction(AttributeAction.PUT)
					.withValue(new AttributeValue().withN(userTweetCount + "")));
			dynamoDBUtil.updateItem(TABLE_NAME_USER, updateKey, updateItem);
		}

		// sinceID(どこまでDBに反映したか)を更新
		if (maxTweetId > 0L) {
			Map<String, AttributeValue> putItem = new HashMap<String, AttributeValue>();
			putItem.put("key", new AttributeValue().withS("get_record_since_id"));
			putItem.put("value", new AttributeValue().withS(maxTweetId + ""));
			dynamoDBUtil.putItem(TABLE_NAME_STATUS, putItem);
		}
		logger.log("*******DB反映終了*******");
	}

	// ツイートの文字列から記録を抽出する。
	// 記録になる文字列がなければnullを返す
	private String getRecordStr(String tweetText) {
		// ツイートから、最初に出てくる数字部分を抽出
		Matcher matcher = Pattern.compile("^@[^ ]+? \\D*(\\d+\\.\\d+|\\d+).*$").matcher(tweetText);
		String recordStr;
		if (matcher.find()) {
			recordStr = matcher.group(1);
			logger.log("記録を抽出1: " + recordStr);
		} else {
			// 数字部分が取得できなかった場合は終了
			logger.log("スキップ(記録なし)");
			return null;
		}
		try {
			// 小数第3位で四捨五入
			recordStr = String.format("%.2f", Double.parseDouble(recordStr));
			logger.log("記録を抽出2: " + recordStr);
		} catch (Exception e) {
			// フォーマット変換に失敗した場合は終了
			logger.log("スキップ(フォーマット整備失敗)");
			return null;
		}
		return recordStr;
	}

}

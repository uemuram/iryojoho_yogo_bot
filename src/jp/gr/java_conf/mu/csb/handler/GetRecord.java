package jp.gr.java_conf.mu.csb.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.dynamodbv2.model.GetItemResult;
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

	public Object handleRequest(Object input, Context context) {
		logger = context.getLogger();
		logger.log("Input: " + input);

		// DynamoDB利用準備
		DynamoDBUtil dynamoDBUtil = new DynamoDBUtil(logger);

		// Twitter利用準備
		// 環境変数から各種キーを設定
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(System.getenv("twitter4j_oauth_consumerKey"))
				.setOAuthConsumerSecret(System.getenv("twitter4j_oauth_consumerSecret"))
				.setOAuthAccessToken(System.getenv("twitter4j_oauth_accessToken"))
				.setOAuthAccessTokenSecret(System.getenv("twitter4j_oauth_accessTokenSecret"));
		Configuration configuration = cb.build();

		// 前回までで取り込んだIDを取得
		GetItemResult result = dynamoDBUtil.getItem("csb_status", "key", "get_record_since_id");
		long sinceId = Long.parseLong(result.getItem().get("value").getS());
		logger.log("ID: " + sinceId + " より後の返信を取得");

		// -----------------------------返信取得処理 ここから-----------------------------
		// 初期化
		TwitterFactory tf = new TwitterFactory(configuration);
		Twitter twitter = tf.getInstance();
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
		// -----------------------------返信取得処理 ここまで-----------------------------

		logger.log(responseList.size() + " 件の返信を取得");
		int lastIndex = responseList.size() - 1;
		// 古い順にループ
		for (int i = lastIndex; i >= 0; i--) {
			Status status = responseList.get(i);
			logger.log("---------------------------------------------------------------------");
			logger.log("ユーザ名:" + status.getUser().getName());
			logger.log("ユーザ名(表示名):" + status.getUser().getScreenName());
			logger.log("返信内容:" + status.getText());
			logger.log("返信ID:" + status.getId() + "");
			logger.log("返信生成日:" + status.getCreatedAt() + "");
			logger.log("返信先ユーザ名(表示名):" + status.getInReplyToScreenName());
			logger.log("返信先ツイートID:" + status.getInReplyToStatusId() + "");

			// 返信先ツイートIDがとれない場合(特定のツイートに対する返信ではない場合)はスキップ
			if (status.getInReplyToStatusId() == -1) {
				logger.log("スキップ(特定ツイートに対する返信以外)");
				continue;
			}

			// ツイートから、最初に出てくる数字部分を抽出
			Matcher matcher = Pattern.compile("^@[^ ]+? \\D*(\\d+\\.\\d+|\\d+).*$").matcher(status.getText());
			String recordStr;
			if (matcher.find()) {
				recordStr = matcher.group(1);
				logger.log("記録を抽出1: " + recordStr);
			} else {
				// 数字部分が取得できなかった場合はスキップ
				logger.log("スキップ(記録なし)");
				continue;
			}

			// 数値型に変換。変換できなかった場合はスキップ
			double record;
			try {
				// 小数第3位で四捨五入した上でdouble型に変換
				record = Double.parseDouble(String.format("%.2f", Double.parseDouble(recordStr)));
				logger.log("記録を抽出2: " + record);
			} catch (Exception e) {
				continue;
			}

			// 取得されたデータを登録

		}

		return null;
	}

}

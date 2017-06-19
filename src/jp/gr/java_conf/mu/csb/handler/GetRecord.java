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
		Paging paging = new Paging(sinceId);

		int count = 0;
		boolean success = false;
		TwitterFactory tf;
		ResponseList<Status> responseList = null;
		// リトライ回数
		int retryCount = 3;
		do {
			tf = new TwitterFactory(configuration);
			Twitter twitter = tf.getInstance();
			// 返信の一覧を取得
			try {
				count++;
				responseList = twitter.getMentionsTimeline(paging);
				success = true;
			} catch (TwitterException e1) {
				logger.log("返信取得失敗 : " + e1.getErrorMessage());
				// 失敗した場合は待機後に再実行
				if (count < retryCount) {
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e2) {
					}
				}
			}
		} while (!success && count < retryCount);

		// 取得に失敗した場合は終了
		if (!success) {
			return null;
		}

		logger.log(responseList.size() + " 件の返信を取得");
		for (Status status : responseList) {
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

package jp.gr.java_conf.mu.iyb.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jp.gr.java_conf.mu.iyb.util.DynamoDBUtil;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class Tweet implements RequestHandler<Object, Object> {
	private LambdaLogger logger;

	private static final String TABLE_NAME_SCRAMBLE = System.getenv("table_name_scramble");

	public Object handleRequest(Object input, Context context) {
		logger = context.getLogger();
		logger.log("Input: " + input);

		// // DynamoDB利用準備
		// DynamoDBUtil dynamoDBUtil = new DynamoDBUtil(logger);
		//
		// // Twitter利用準備
		// // 環境変数から各種キーを設定
		// ConfigurationBuilder cb = new ConfigurationBuilder();
		// cb.setDebugEnabled(true).setOAuthConsumerKey(System.getenv("twitter4j_oauth_consumerKey"))
		// .setOAuthConsumerSecret(System.getenv("twitter4j_oauth_consumerSecret"))
		// .setOAuthAccessToken(System.getenv("twitter4j_oauth_accessToken"))
		// .setOAuthAccessTokenSecret(System.getenv("twitter4j_oauth_accessTokenSecret"));
		// Configuration configuration = cb.build();
		// TwitterFactory tf = new TwitterFactory(configuration);
		// Twitter twitter = tf.getInstance();
		//
		// // ツイート文言を生成
		// String scramble = generateScramble(20);
		// logger.log("スクランブル: " + scramble);
		// String tweetText = scramble + "\n\n※タイム(秒)を何回か返信すると、集計して通知します。";
		//
		// // ツイート
		// Status status;
		// try {
		// status = twitter.updateStatus(tweetText);
		// logger.log("---------------------------------------------------------------------");
		// logger.log("ツイート内容:" + status.getText());
		// logger.log("ツイートID:" + status.getId() + "");
		// logger.log("ツイート生成日時:" + status.getCreatedAt() + "");
		// } catch (TwitterException e1) {
		// logger.log("ツイート失敗 : " + e1.getErrorMessage());
		// throw new RuntimeException(e1);
		// }
		//
		// // ツイート時刻を取得
		// LocalDateTime localDateTime =
		// LocalDateTime.parse(status.getCreatedAt().toString(),
		// DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"));
		// String createdAt = DateTimeFormatter.ofPattern("yyyy/MM/dd
		// HH:mm:ss").format(localDateTime);
		//
		// // ツイート内容をDBに登録
		// Map<String, AttributeValue> item = new HashMap<String,
		// AttributeValue>();
		// item.put("id", new AttributeValue().withS(status.getId() + ""));
		// item.put("scramble", new AttributeValue().withS(scramble));
		// item.put("tweet_date", new AttributeValue().withS(createdAt));
		// dynamoDBUtil.putItem(TABLE_NAME_SCRAMBLE, item);

		Output output = new Output();
		output.yogoNo = 3;
		output.index = 10;

		int a = 3 / 0;

		return output;
	}

	// 出力生成クラス
	public static class Output {
		public Integer yogoNo;
		public Integer index;
	}

	// スクランブルを生成
	private String generateScramble(int l) {
		String faces[] = { "U", "D", "R", "L", "F", "B" };
		String options[] = { "", "'", "2" };
		String scramble = "";
		int beforeFace = -1;
		int before2Face = -1;
		int currentFace;
		for (int i = 0; i < l; i++) {
			// 回す面を決める。
			do {
				currentFace = randomN(6);
			} while (!faceCheck(currentFace, beforeFace, before2Face));
			scramble += (faces[currentFace] + options[randomN(3)]);
			if (i < l - 1) {
				scramble += " ";
			}
			// 2つ前、1つ前の手順を記録
			before2Face = beforeFace;
			beforeFace = currentFace;
		}
		return scramble;
	}

	// n種類(0～n-1)の乱数を生成
	private int randomN(int n) {
		return (int) (Math.random() * n);
	}

	// 回す面のチェック
	private boolean faceCheck(int current, int before, int before2) {
		// 1つ前と同じ面は回さない
		if (current == before) {
			return false;
		}
		// 同じ面 -> 対面 -> 同じ面 の順の回転はNG(例: U2, D, U')
		if (current == before2
				&& ((current % 2 == 0 && current - before == -1) || (current % 2 == 1 && current - before == 1))) {
			return false;
		}
		return true;
	}
}

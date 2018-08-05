package jp.gr.java_conf.mu.iyb.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Tweet implements RequestHandler<YogoDao, Object> {
	private LambdaLogger logger;

	public Object handleRequest(YogoDao input, Context context) {
		logger = context.getLogger();
		logger.log("Input: " + input);

		int offset = input.getOffset();
		String keyword = input.getKeyword();
		String description = input.getDescription();

		logger.log("オフセット : " + offset);
		logger.log("キーワード : " + keyword);
		logger.log("説明 : " + description);

		// TODO ツイートIDをDAOに入れる
		// TODO 判断用部品を入れる(オフセットが-1なら、とか)
		// TODO ツイートの余地があればツイート
		// TODO オフセットが0なら、キーワード + つぶやけるとこまでツイート
		// TODO オフセットが0以上なら返信
		// TODO 最後まで行ったらオフセット-1を返す、そうでなければオフセット値を返す

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

		YogoDao output = new YogoDao();
		output.setOffset(2);
		output.setKeyword("aaa");
		output.setDescription("bbb");

		return output;
	}

}

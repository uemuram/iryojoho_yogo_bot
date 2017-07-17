package jp.gr.java_conf.mu.csb.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jp.gr.java_conf.mu.csb.util.DynamoDBUtil;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class SummarizeRecord implements RequestHandler<Object, Object> {
	private LambdaLogger logger;

	private static final String TABLE_NAME_RECORD = System.getenv("table_name_record");
	private static final String TABLE_NAME_USER = System.getenv("table_name_user");

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
		TwitterFactory tf = new TwitterFactory(configuration);
		Twitter twitter = tf.getInstance();

		return null;
	}

}

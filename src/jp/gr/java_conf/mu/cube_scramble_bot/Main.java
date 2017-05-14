package jp.gr.java_conf.mu.cube_scramble_bot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Main implements RequestHandler<Object, Object> {
	private LambdaLogger logger;

	public Object handleRequest(Object input, Context context) {
		logger = context.getLogger();
		logger.log("Input: " + input);

		// 環境変数から各種キーを設定
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(System.getenv("twitter4j_oauth_consumerKey"))
				.setOAuthConsumerSecret(System.getenv("twitter4j_oauth_consumerSecret"))
				.setOAuthAccessToken(System.getenv("twitter4j_oauth_accessToken"))
				.setOAuthAccessTokenSecret(System.getenv("twitter4j_oauth_accessTokenSecret"));
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();

		try {
			Status status = twitter.updateStatus("テスト");
			logger.log("Successfully updated the status to [" + status.getText() + "].");
		} catch (TwitterException e) {
			logger.log(e.getErrorMessage());
			e.printStackTrace();
			return null;
		}
		return null;
	}
}

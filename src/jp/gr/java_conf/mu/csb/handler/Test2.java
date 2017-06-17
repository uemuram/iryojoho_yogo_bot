package jp.gr.java_conf.mu.csb.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import twitter4j.HttpClient;
import twitter4j.HttpClientFactory;
import twitter4j.HttpParameter;
import twitter4j.HttpResponse;
import twitter4j.JSONArray;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

// 生でAPI呼んでみるテスト
public class Test2 implements RequestHandler<Object, Object> {
	private LambdaLogger logger;

	public Object handleRequest(Object input, Context context) {
		logger = context.getLogger();
		logger.log("Input: " + input);

		Configuration conf = ConfigurationContext.getInstance();
		OAuthAuthorization oauth = new OAuthAuthorization(conf);
		AccessToken accessToken = new AccessToken(System.getenv("twitter4j_oauth_accessToken"),
				System.getenv("twitter4j_oauth_accessTokenSecret"));
		oauth.setOAuthAccessToken(accessToken);
//		oauth.setOAuthConsumer(System.getenv("twitter4j_oauth_consumerKey"),
//				System.getenv("twitter4j_oauth_consumerSecret"));

		 oauth.setOAuthConsumer("3nVuSoBZnx6U4vzUxf5w",
		 "Bcs59EFbbsdF6Sl9Ng71smgStWEGwXXKSjYvPVt7qys");

		// oauth.generateOAuthSignatureHttpParams(method, url)

		HttpClient http = HttpClientFactory.getInstance();

		// http.get

		String url = "https://api.twitter.com/1.1/activity/about_me.json";
		HttpParameter[] parameters = new HttpParameter[] { new HttpParameter("key1", "value1"),
				new HttpParameter("key2", "value2") };

		try {
			HttpResponse res = http.get(url, parameters, oauth, null);
			// HttpResponse res = http.post(url, parameters, oauth);
			// HttpResponse res = http.delete(url, parameters, oauth);
			String result = res.asString();
			if (res.getStatusCode() != 200) {
				logger.log("result: " + result);
				return false;
			}

			JSONArray jsonArr = res.asJSONArray(); // for json array
			logger.log(jsonArr.toString());

			// Document document = res.asDocument(); // for xml
			// JSONObject jsonObj = res.asJSONObject(); // for json object
			// JSONArray jsonArr = res.asJSONArray(); // for json array
		} catch (TwitterException e) {
			e.printStackTrace();
			
			
			return false;
		}
		return true;

		// int count = 0;
		// boolean success = false;
		// // リトライ回数
		// int retryCount = 3;
		// do {
		// Twitter twitter = tf.getInstance();
		//
		// // テスト
		// Paging arg0 = new Paging(1, 40);
		// ResponseList<Status> responseList = null;
		// try {
		// count++;
		// // responseList = twitter.getRetweetsOfMe(arg0);
		// logger.log("a");
		// // responseList = twitter.getRetweetsOfMe();
		// // responseList = twitter.getHomeTimeline();
		// responseList = twitter.getUserTimeline("masaru_uemura", new Paging(3,
		// 20));
		//
		// success = true;
		// logger.log("c");
		// } catch (TwitterException e1) {
		// logger.log("ツイート失敗 : " + e1.getErrorMessage());
		//
		// // 失敗した場合は待機後に再実行
		// if (count < retryCount) {
		// try {
		// Thread.sleep(30000);
		// } catch (InterruptedException e2) {
		// }
		// }
		// }
		// logger.log("size:" + responseList.size());
		// for (Status b : responseList) {
		// logger.log("10:" + b.getUser().getName());
		// logger.log("20:" + b.getUser().getScreenName());
		// logger.log("30:" + b.getUser().getId() + "");
		// logger.log("40:" + b.getText());
		// logger.log("50:" + b.getId() + "");
		// logger.log("60:" + b.getCreatedAt() + "");
		// logger.log("70:" + b.getRetweetCount() + "");
		// }
		//
		// } while (!success && count < retryCount);
		//
		// return null;
	}

}

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

		// スクランブル生成
		String scramble = generateScramble(20);
		logger.log("スクランブル: " + scramble);

		int count = 0;
		boolean success = false;
		// リトライ回数
		int retryCount = 3;
		do {
			// ツイート
			try {
				count++;
				Status status = twitter.updateStatus(scramble);
				logger.log("Successfully updated the status to [" + status.getText() + "].");
				success = true;
			} catch (TwitterException e) {
				logger.log("ツイート失敗");
				logger.log(e.getErrorMessage());

				// 失敗した場合は待機後に再実行
				if (count < retryCount) {
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e2) {
					}
				}
			}
			// 最大で3回リトライする
		} while (!success && count < retryCount);

		return null;
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
			// 回す面を決める。1つ前と同じ面を選ばないようにする
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

package jp.gr.java_conf.mu.iyb.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jp.gr.java_conf.mu.iyb.util.CommonUtil;
import jp.gr.java_conf.mu.iyb.util.TwitterUtil;
import twitter4j.Status;

public class Tweet implements RequestHandler<YogoDao, Object> {
	private LambdaLogger logger;
	private final int TWEET_LIMIT = 140;

	public Object handleRequest(YogoDao input, Context context) {
		CommonUtil util = new CommonUtil();
		logger = context.getLogger();
		logger.log("Input: " + input);

		int offset = input.getOffset();
		String keyword = input.getKeyword();
		String description = input.getDescription();
		long beforeTweetId = input.getBeforeTweetId();

		logger.log("オフセット : " + offset);
		logger.log("キーワード : " + keyword);
		logger.log("説明 : " + description);
		logger.log("前回のツイートID: " + beforeTweetId);

		// ツイートする文言
		String tweetText = "";
		// 初回実行時
		if (offset == 0) {
			tweetText += "【" + keyword + "】\n";
		}
		tweetText += description.substring(offset);
		tweetText = tweetText.substring(0, util.min(TWEET_LIMIT, tweetText.length()));
		logger.log("生成された文言 : " + tweetText);

		// 次回用のオフセット値を計算
		int newOffset = offset + TWEET_LIMIT;
		if (offset == 0) {
			newOffset -= (keyword.length() + 3);
		}
		// 終了判定
		if (newOffset > description.length()) {
			newOffset = -1;
		}

		// Twitter利用準備
		TwitterUtil twitterUtil = new TwitterUtil();

		Status status;
		// ツイート実行
		if (beforeTweetId == -1) {
			status = twitterUtil.tweet(tweetText);
		} else {
			status = twitterUtil.reply(tweetText, beforeTweetId);
		}

		YogoDao output = new YogoDao();
		output.setOffset(newOffset);
		output.setKeyword(keyword);
		output.setDescription(description);
		output.setBeforeTweetId(status.getId());

		return output;
	}

}

package jp.gr.java_conf.mu.iyb.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterUtil {

	private Twitter twitter;
	private CommonUtil util;

	// コンストラクタ
	public TwitterUtil() {

		// util利用準備
		util = new CommonUtil();

		// Twitter利用準備
		// 環境変数から各種キーを読み込む
		String consumerKey = System.getenv("twitter4j_oauth_consumerKey");
		String consumerSecret = System.getenv("twitter4j_oauth_consumerSecret");
		String accessToken = System.getenv("twitter4j_oauth_accessToken");
		String accessTokenSecret = System.getenv("twitter4j_oauth_accessTokenSecret");

		System.out.println("consumerKey:\t" + consumerKey);
		System.out.println("consumerSecret:\t" + consumerSecret);
		System.out.println("accessToken:\t" + accessToken);
		System.out.println("accessTokenSecret:\t" + accessTokenSecret);

		// Twitter接続用オブジェクト
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret)
				.setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessTokenSecret);
		Configuration configuration = cb.build();
		TwitterFactory tf = new TwitterFactory(configuration);
		this.twitter = tf.getInstance();
	}

	// 指定された文言をツイートする
	public void tweet(String msg) {
		Status status;
		try {
			status = twitter.updateStatus(msg);
			System.out.println("---------------------------------------------------------------------");
			System.out.println("ツイート内容:\n" + status.getText());
			System.out.println("ツイートID:" + status.getId() + "");
			System.out.println("ツイート生成日時:" + status.getCreatedAt() + "");
		} catch (TwitterException e1) {
			System.out.println("ツイート失敗 : " + e1.getErrorMessage());
			throw new RuntimeException(e1);
		}
	}

	// 指定されたキーワードで、指定された件数分Twitterを検索し、そのテキスト要素を返す
	public ArrayList<String> searchTweetText(String word, int count) {
		List<Status> searchResultList = searchTweet(word, count);
		if (searchResultList == null) {
			return null;
		}
		ArrayList<String> tweetTextList = new ArrayList<String>();
		for (Status status : searchResultList) {
			tweetTextList.add(status.getText());
		}
		return tweetTextList;
	}

	// 指定されたキーワードで、指定された件数分Twitterを検索する
	private List<Status> searchTweet(String word, int count) {
		// 初期化
		List<Status> searchResultList;
		List<Status> tmpSearchResultList;
		long maxId = 0L;
		// 終了フラグ
		boolean endFlag = false;
		// 検索条件。指定された文言で検索、ただしリツイートを除く
		Query query = new Query();
		query.setQuery(word + " exclude:retweets");
		QueryResult result;
		// 検索回数と上限
		int searchCount = 0;
		int searchCountLimit = 10;

		// まず1ページ目を取得
		try {
			result = twitter.search(query);
			searchResultList = result.getTweets();
			System.out.println("検索結果取得 " + searchResultList.size() + " 件");
		} catch (TwitterException e1) {
			System.out.println("検索結果取得失敗 : " + e1.getErrorMessage());
			return null;
		}
		// 1件も取得できなかった場合は終了
		if (searchResultList.size() == 0) {
			endFlag = true;
		} else {
			// 最後のIDを取得しておく
			maxId = searchResultList.get(searchResultList.size() - 1).getId() - 1;
			// 結果チェック
			checkSearchResult(searchResultList);
			// 目的の件数以上に取得できた場合も終了
			if (searchResultList.size() >= count) {
				endFlag = true;
			}
		}
		// 1ページ目が取得できた場合は2ページ目以降を取得
		while (!endFlag) {
			// 検索しつづけることを防止
			searchCount++;
			if (searchCount > searchCountLimit) {
				return null;
			}

			query.setMaxId(maxId);
			// 連続してリクエストを投げないようにするために少し待つ
			util.sleep(1000);
			try {
				result = twitter.search(query);
				tmpSearchResultList = result.getTweets();
				System.out.println("検索結果取得 " + tmpSearchResultList.size() + " 件 (maxId= " + maxId + " )");
			} catch (TwitterException e1) {
				System.out.println("検索結果取得失敗 : " + e1.getErrorMessage());
				return null;
			}
			// 1件も取得できなかった場合は終了
			if (tmpSearchResultList.size() == 0) {
				endFlag = true;
			} else {
				// 最後のIDを取得しておく
				maxId = tmpSearchResultList.get(tmpSearchResultList.size() - 1).getId() - 1;
				// 結果チェック
				checkSearchResult(tmpSearchResultList);
				// 取得結果を追加
				searchResultList.addAll(tmpSearchResultList);
				// 目的の件数以上に取得できた場合も終了
				if (searchResultList.size() >= count) {
					endFlag = true;
				}
			}
		}
		// 余計に取得された分を切り捨てる
		if (searchResultList.size() > count) {
			int lastIndex = searchResultList.size() - 1;
			for (int i = lastIndex; i >= count; i--) {
				searchResultList.remove(i);
			}
		}
		return searchResultList;
	}

	// 検索結果をチェック、利用できないものがあれば除外する
	private void checkSearchResult(List<Status> list) {
		HashMap<String, Boolean> tmpHash = new HashMap<String, Boolean>();
		int lastIndex = list.size() - 1;
		for (int i = lastIndex; i >= 0; i--) {
			String logStr = "";
			Status status = list.get(i);
			String text = status.getText();
			if (text.startsWith("RT @")) {
				// 非公式リツイートを除外
				list.remove(i);
				logStr += "【削除】";
			} else if (text.contains("[定期") || text.contains("(定期") || text.contains("【定期") || text.contains("[自動")
					|| text.contains("(自動") || text.contains("【自動")) {
				// 定期ツイート、自動ツイートと思われるものを除外
				list.remove(i);
				logStr += "【削除】";
			} else if (text.contains("http://") || text.contains("https://")) {
				// URLが含まれているツイートを除外
				list.remove(i);
				logStr += "【削除】";
			} else if (text.contains("FF外から") || text.contains("拡散希望") || text.contains("相互希望")) {
				// 余計な決まり文句(FF外から失礼します 等)が入っているツイートを除外
				list.remove(i);
				logStr += "【削除】";
			} else if (!util.isIncludedHiragana(text)) {
				// ひらがなが1文字も入っていないツイートを除外
				list.remove(i);
				logStr += "【削除】";
			} else {
				// 同じ内容のツイートを2回利用しないように、ハッシュを使ってチェック
				Boolean check = tmpHash.get(text);
				if (check != null) {
					list.remove(i);
					logStr += "【削除】";
				} else {
					tmpHash.put(text, true);
				}
			}
			logStr += (i + ":" + status.getCreatedAt() + ":" + status.getId() + ":" + text + "\n");
			System.out.print(logStr);
		}
	}

}

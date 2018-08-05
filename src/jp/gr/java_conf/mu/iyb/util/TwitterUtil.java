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

	// 繧ｳ繝ｳ繧ｹ繝医Λ繧ｯ繧ｿ
	public TwitterUtil() {
		// Twitter蛻ｩ逕ｨ貅門ｙ
		// 迺ｰ蠅�螟画焚縺九ｉ蜷�遞ｮ繧ｭ繝ｼ繧定ｪｭ縺ｿ霎ｼ繧�
		String consumerKey = System.getenv("twitter4j_oauth_consumerKey");
		String consumerSecret = System.getenv("twitter4j_oauth_consumerSecret");
		String accessToken = System.getenv("twitter4j_oauth_accessToken");
		String accessTokenSecret = System.getenv("twitter4j_oauth_accessTokenSecret");

		System.out.println("consumerKey:\t" + consumerKey);
		System.out.println("consumerSecret:\t" + consumerSecret);
		System.out.println("accessToken:\t" + accessToken);
		System.out.println("accessTokenSecret:\t" + accessTokenSecret);

		// Twitter謗･邯夂畑繧ｪ繝悶ず繧ｧ繧ｯ繝�
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret)
				.setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessTokenSecret);
		Configuration configuration = cb.build();
		TwitterFactory tf = new TwitterFactory(configuration);
		this.twitter = tf.getInstance();
	}

	// 謖�螳壹＆繧後◆譁�險�繧偵ヤ繧､繝ｼ繝医☆繧�
	public void tweet(String msg) {
		Status status;
		try {
			status = twitter.updateStatus(msg);
			System.out.println("---------------------------------------------------------------------");
			System.out.println("繝�繧､繝ｼ繝亥��螳ｹ:\n" + status.getText());
			System.out.println("繝�繧､繝ｼ繝�ID:" + status.getId() + "");
			System.out.println("繝�繧､繝ｼ繝育函謌先律譎�:" + status.getCreatedAt() + "");
		} catch (TwitterException e1) {
			System.out.println("繝�繧､繝ｼ繝亥､ｱ謨� : " + e1.getErrorMessage());
			throw new RuntimeException(e1);
		}
	}

	// 謖�螳壹＆繧後◆繧ｭ繝ｼ繝ｯ繝ｼ繝峨〒縲∵欠螳壹＆繧後◆莉ｶ謨ｰ蛻�Twitter繧呈､懃ｴ｢縺励�√◎縺ｮ繝�繧ｭ繧ｹ繝郁ｦ∫ｴ�繧定ｿ斐☆
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

	// 謖�螳壹＆繧後◆繧ｭ繝ｼ繝ｯ繝ｼ繝峨〒縲∵欠螳壹＆繧後◆莉ｶ謨ｰ蛻�Twitter繧呈､懃ｴ｢縺吶ｋ
	private List<Status> searchTweet(String word, int count) {
		// 蛻晄悄蛹�
		List<Status> searchResultList;
		List<Status> tmpSearchResultList;
		long maxId = 0L;
		// 邨ゆｺ�繝輔Λ繧ｰ
		boolean endFlag = false;
		// 讀懃ｴ｢譚｡莉ｶ縲よ欠螳壹＆繧後◆譁�險�縺ｧ讀懃ｴ｢縲√◆縺�縺励Μ繝�繧､繝ｼ繝医ｒ髯､縺�
		Query query = new Query();
		query.setQuery(word + " exclude:retweets");
		QueryResult result;
		// 讀懃ｴ｢蝗樊焚縺ｨ荳企剞
		int searchCount = 0;
		int searchCountLimit = 10;

		// 縺ｾ縺�1繝壹�ｼ繧ｸ逶ｮ繧貞叙蠕�
		try {
			result = twitter.search(query);
			searchResultList = result.getTweets();
			System.out.println("讀懃ｴ｢邨先棡蜿門ｾ� " + searchResultList.size() + " 莉ｶ");
		} catch (TwitterException e1) {
			System.out.println("讀懃ｴ｢邨先棡蜿門ｾ怜､ｱ謨� : " + e1.getErrorMessage());
			return null;
		}
		// 1莉ｶ繧ょ叙蠕励〒縺阪↑縺九▲縺溷�ｴ蜷医�ｯ邨ゆｺ�
		if (searchResultList.size() == 0) {
			endFlag = true;
		} else {
			// 譛�蠕後�ｮID繧貞叙蠕励＠縺ｦ縺翫￥
			maxId = searchResultList.get(searchResultList.size() - 1).getId() - 1;
			// 邨先棡繝√ぉ繝�繧ｯ
			checkSearchResult(searchResultList);
			// 逶ｮ逧�縺ｮ莉ｶ謨ｰ莉･荳翫↓蜿門ｾ励〒縺阪◆蝣ｴ蜷医ｂ邨ゆｺ�
			if (searchResultList.size() >= count) {
				endFlag = true;
			}
		}
		// 1繝壹�ｼ繧ｸ逶ｮ縺悟叙蠕励〒縺阪◆蝣ｴ蜷医�ｯ2繝壹�ｼ繧ｸ逶ｮ莉･髯阪ｒ蜿門ｾ�
		while (!endFlag) {
			// 讀懃ｴ｢縺励▽縺･縺代ｋ縺薙→繧帝亟豁｢
			searchCount++;
			if (searchCount > searchCountLimit) {
				return null;
			}

			query.setMaxId(maxId);
			// 騾｣邯壹＠縺ｦ繝ｪ繧ｯ繧ｨ繧ｹ繝医ｒ謚輔￡縺ｪ縺�繧医≧縺ｫ縺吶ｋ縺溘ａ縺ｫ蟆代＠蠕�縺､
			CommonUtil.sleep(1000);
			try {
				result = twitter.search(query);
				tmpSearchResultList = result.getTweets();
				System.out.println("讀懃ｴ｢邨先棡蜿門ｾ� " + tmpSearchResultList.size() + " 莉ｶ (maxId= " + maxId + " )");
			} catch (TwitterException e1) {
				System.out.println("讀懃ｴ｢邨先棡蜿門ｾ怜､ｱ謨� : " + e1.getErrorMessage());
				return null;
			}
			// 1莉ｶ繧ょ叙蠕励〒縺阪↑縺九▲縺溷�ｴ蜷医�ｯ邨ゆｺ�
			if (tmpSearchResultList.size() == 0) {
				endFlag = true;
			} else {
				// 譛�蠕後�ｮID繧貞叙蠕励＠縺ｦ縺翫￥
				maxId = tmpSearchResultList.get(tmpSearchResultList.size() - 1).getId() - 1;
				// 邨先棡繝√ぉ繝�繧ｯ
				checkSearchResult(tmpSearchResultList);
				// 蜿門ｾ礼ｵ先棡繧定ｿｽ蜉�
				searchResultList.addAll(tmpSearchResultList);
				// 逶ｮ逧�縺ｮ莉ｶ謨ｰ莉･荳翫↓蜿門ｾ励〒縺阪◆蝣ｴ蜷医ｂ邨ゆｺ�
				if (searchResultList.size() >= count) {
					endFlag = true;
				}
			}
		}
		// 菴呵ｨ医↓蜿門ｾ励＆繧後◆蛻�繧貞��繧頑昏縺ｦ繧�
		if (searchResultList.size() > count) {
			int lastIndex = searchResultList.size() - 1;
			for (int i = lastIndex; i >= count; i--) {
				searchResultList.remove(i);
			}
		}
		return searchResultList;
	}

	// 讀懃ｴ｢邨先棡繧偵メ繧ｧ繝�繧ｯ縲∝茜逕ｨ縺ｧ縺阪↑縺�繧ゅ�ｮ縺後≠繧後�ｰ髯､螟悶☆繧�
	private void checkSearchResult(List<Status> list) {
		HashMap<String, Boolean> tmpHash = new HashMap<String, Boolean>();
		int lastIndex = list.size() - 1;
		for (int i = lastIndex; i >= 0; i--) {
			String logStr = "";
			Status status = list.get(i);
			String text = status.getText();
			if (text.startsWith("RT @")) {
				// 髱槫�ｬ蠑上Μ繝�繧､繝ｼ繝医ｒ髯､螟�
				list.remove(i);
				logStr += "縲仙炎髯､縲�";
			} else if (text.contains("[螳壽悄") || text.contains("(螳壽悄") || text.contains("縲仙ｮ壽悄") || text.contains("[閾ｪ蜍�")
					|| text.contains("(閾ｪ蜍�") || text.contains("縲占�ｪ蜍�")) {
				// 螳壽悄繝�繧､繝ｼ繝医�∬�ｪ蜍輔ヤ繧､繝ｼ繝医→諤昴ｏ繧後ｋ繧ゅ�ｮ繧帝勁螟�
				list.remove(i);
				logStr += "縲仙炎髯､縲�";
			} else if (text.contains("http://") || text.contains("https://")) {
				// URL縺悟性縺ｾ繧後※縺�繧九ヤ繧､繝ｼ繝医ｒ髯､螟�
				list.remove(i);
				logStr += "縲仙炎髯､縲�";
			} else if (text.contains("FF螟悶°繧�") || text.contains("諡｡謨｣蟶梧悍") || text.contains("逶ｸ莠貞ｸ梧悍")) {
				// 菴呵ｨ医↑豎ｺ縺ｾ繧頑枚蜿･(FF螟悶°繧牙､ｱ遉ｼ縺励∪縺� 遲�)縺悟�･縺｣縺ｦ縺�繧九ヤ繧､繝ｼ繝医ｒ髯､螟�
				list.remove(i);
				logStr += "縲仙炎髯､縲�";
			} else if (!CommonUtil.isIncludedHiragana(text)) {
				// 縺ｲ繧峨′縺ｪ縺�1譁�蟄励ｂ蜈･縺｣縺ｦ縺�縺ｪ縺�繝�繧､繝ｼ繝医ｒ髯､螟�
				list.remove(i);
				logStr += "縲仙炎髯､縲�";
			} else {
				// 蜷後§蜀�螳ｹ縺ｮ繝�繧､繝ｼ繝医ｒ2蝗槫茜逕ｨ縺励↑縺�繧医≧縺ｫ縲√ワ繝�繧ｷ繝･繧剃ｽｿ縺｣縺ｦ繝√ぉ繝�繧ｯ
				Boolean check = tmpHash.get(text);
				if (check != null) {
					list.remove(i);
					logStr += "縲仙炎髯､縲�";
				} else {
					tmpHash.put(text, true);
				}
			}
			logStr += (i + ":" + status.getCreatedAt() + ":" + status.getId() + ":" + text + "\n");
			System.out.print(logStr);
		}
	}

}

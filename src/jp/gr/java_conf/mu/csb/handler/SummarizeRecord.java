package jp.gr.java_conf.mu.csb.handler;

import java.awt.BasicStroke;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jp.gr.java_conf.mu.csb.util.DynamoDBUtil;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class SummarizeRecord implements RequestHandler<Object, Object> {
	private LambdaLogger logger;

	private static final String TABLE_NAME_RECORD = System.getenv("table_name_record");
	private static final String TABLE_NAME_USER = System.getenv("table_name_user");

	private static final int SUMMARIZE_TRIGGER_TWEET_COUNT = 5;
	private static final int RECORD_COUNT_FOR_GRAPH = 20;

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

		// DBからユーザ一覧を取得(未処理のツイート数が一定数より多いユーザのみ抽出
		List<Map<String, AttributeValue>> users = dynamoDBUtil.scan(TABLE_NAME_USER, "not_summarized_tweet_count",
				ComparisonOperator.GE, SUMMARIZE_TRIGGER_TWEET_COUNT);

		// 対象ユーザに対してループ
		int count = 0;
		for (Map<String, AttributeValue> item : users) {
			count++;
			logger.log(count + "----------------------------------------------------------");

			// DB取得結果を整理
			String userName = item.get("user_name").getS();
			String oldestSummarizedTweetId = item.get("oldest_summarized_tweet_id").getS();

			logger.log("ユーザ名: " + userName);
			logger.log("最古の分析済みツイートID: " + oldestSummarizedTweetId);

			// 対象ユーザの記録を検索
			List<Map<String, AttributeValue>> records = dynamoDBUtil.query(TABLE_NAME_RECORD, "user_name", userName,
					"reply_id", ComparisonOperator.GE, oldestSummarizedTweetId, "user_name-reply_id-index");

			// グラフ画像を生成
			File graphFile = createGraph(records, userName);

			// ツイートと画像添付
			Status status;
			String tweetText = "@" + userName + " " + "最近の記録です。" + calcAvarage(records);

			// 上限を超えないように140文字で切り取る
			logger.log("ツイートテキスト: " + tweetText);
			if (tweetText.length() > 140) {
				tweetText = tweetText.substring(0, 140);
				logger.log("ツイートテキスト(切り取り後): " + tweetText);
			}

			try {
				status = twitter.updateStatus(new StatusUpdate(tweetText).media(graphFile));
				logger.log("---------------------------------------------------------------------");
				logger.log("ツイート内容:" + status.getText());
				logger.log("ツイートID:" + status.getId() + "");
				logger.log("ツイート生成日時:" + status.getCreatedAt() + "");
			} catch (TwitterException e1) {
				logger.log("ツイート失敗 : " + e1.getErrorMessage());
				throw new RuntimeException(e1);
			}

			// ユーザ情報を更新
			// 次回の起点となるツイートID
			int size = item.size();
			int idx = size >= RECORD_COUNT_FOR_GRAPH ? size - RECORD_COUNT_FOR_GRAPH : 0;
			String oldestSummarizedTweetIdUpd = records.get(idx).get("reply_id").getS();

			// 更新実施
			Map<String, AttributeValue> putItem = new HashMap<String, AttributeValue>();
			putItem.put("user_name", new AttributeValue().withS(userName));
			putItem.put("not_summarized_tweet_count", new AttributeValue().withN("0"));
			putItem.put("oldest_summarized_tweet_id", new AttributeValue().withS(oldestSummarizedTweetIdUpd));
			dynamoDBUtil.putItem(TABLE_NAME_USER, putItem);
		}

		return null;
	}

	// 平均を計算し、文字列として返す
	private String calcAvarage(List<Map<String, AttributeValue>> records) {
		int size = records.size();
		// データ数が5に満たない場合は計算しない
		if (size < 5) {
			return "";
		}
		// 最大と最小をチェック
		int start = size - 5;
		int maxIdx = start, minIdx = start;
		double max = 0, min = Double.POSITIVE_INFINITY;
		for (int i = start; i < size; i++) {
			Map<String, AttributeValue> item = records.get(i);
			double record = Double.parseDouble(item.get("record").getN());
			if (record > max) {
				maxIdx = i;
				max = record;
			}
			if (record < min) {
				minIdx = i;
				min = record;
			}
		}

		// 全て同じ値だったときに最大値と最小値を別扱いにするための処理
		if (maxIdx == minIdx && maxIdx == start) {
			minIdx = start + 1;
		}
		// 平均を計算
		double recordSum = 0;
		String resultStr = "\n";
		for (int i = start; i < size; i++) {
			Map<String, AttributeValue> item = records.get(i);
			double record = Double.parseDouble(item.get("record").getN());
			String recordStr;
			if (i == maxIdx || i == minIdx) {
				// 最大or最小のためスキップ
				recordStr = "(" + String.format("%.2f", record) + ")";
			} else {
				// スキップせず計算に利用
				recordSum += record;
				recordStr = String.format("%.2f", record);
			}
			resultStr += recordStr;
			if (i < size - 1) {
				resultStr += " ";
			}
		}
		double avarage = recordSum / 3;
		resultStr += "\n平均:" + String.format("%.2f", avarage);

		return resultStr;
	}

	// グラフ画像を作成
	private File createGraph(List<Map<String, AttributeValue>> records, String userName) {
		// 取り出すデータ範囲の決定
		int size = records.size();
		int start = size >= RECORD_COUNT_FOR_GRAPH ? size - RECORD_COUNT_FOR_GRAPH : 0;
		int end = size - 1;

		// グラフ用データ準備
		String series1 = "record";
		double recordMax = 0;
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		// 記録でループ
		for (int i = start; i <= end; i++) {
			Map<String, AttributeValue> item = records.get(i);

			// データを取り出す
			double record = Double.parseDouble(item.get("record").getN());
			String replyDate = item.get("reply_date").getS();
			logger.log(replyDate + " : " + record);
			dataset.addValue(record, series1, replyDate);
			// 最大値をとっておく
			if (record > recordMax) {
				recordMax = record;
			}
		}

		// JFreeChartオブジェクトの生成
		JFreeChart chart = ChartFactory.createLineChart("Record(@" + userName + ")", "date", "sec", dataset,
				PlotOrientation.VERTICAL, false, true, false);

		CategoryPlot plot = chart.getCategoryPlot();
		LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
		// 線の太さを設定
		renderer.setSeriesStroke(0, new BasicStroke(2));
		// 線の図形を表示
		renderer.setSeriesShapesVisible(0, true);
		// 線の付近に値を表示
		renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setBaseItemLabelsVisible(true);
		// ラベルの向きを変える
		CategoryAxis axis = plot.getDomainAxis();
		axis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		// 文字が枠をはみ出すのを防ぐため、縦軸の最大値を、記録の最大値より少し大きくする
		NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
		numberAxis.setUpperBound(recordMax * 1.1);

		// グラフ出力
		String fileName = "record_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
				+ ".png";
		String filePath = "/tmp/" + fileName;
		File outputFile = new File(filePath);
		try {
			ChartUtilities.saveChartAsPNG(outputFile, chart, 1000, 500);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.log("ファイル出力 : " + filePath);

		return outputFile;
	}

}

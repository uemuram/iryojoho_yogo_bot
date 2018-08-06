package jp.gr.java_conf.mu.iyb.handler;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.opencsv.CSVReader;

public class GetYogo implements RequestHandler<Object, Object> {
	private LambdaLogger logger;

	public Object handleRequest(Object input, Context context) {
		logger = context.getLogger();
		logger.log("Input: " + input);

		// 用語が記載されたCSVのパスを取得
		URL url = getClass().getClassLoader().getResource("yogo.csv");
		String filePath = url.getPath();
		logger.log("入力ファイルパス : " + filePath);

		// CSV読み込み実施
		FileReader fileReader = null;
		CSVReader csvReader = null;
		List<String[]> yogoList = null;
		try {
			fileReader = new FileReader(filePath);
			csvReader = new CSVReader(fileReader);
			yogoList = csvReader.readAll();
			logger.log(yogoList.size() + " 件読み込み");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			// リーダーをクローズ
			try {
				csvReader.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		// 読み込んだ内容から1つ選択
		int index = randomN(yogoList.size());
		YogoDao output = new YogoDao();
		output.setOffset(0);
		output.setKeyword(yogoList.get(index)[0]);
		output.setDescription(yogoList.get(index)[1]);
		output.setBeforeTweetId(-1);

		logger.log("インデックス  : " + index);
		logger.log("キーワード : " + output.getKeyword());
		logger.log("説明: " + output.getDescription());

		return output;
	}

	// n種類(0～n-1)の乱数を生成
	private int randomN(int n) {
		return (int) (Math.random() * n);
	}
}

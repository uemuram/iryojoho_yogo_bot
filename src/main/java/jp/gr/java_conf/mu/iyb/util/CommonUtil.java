package jp.gr.java_conf.mu.iyb.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class CommonUtil {

	// // テスト
	// public static void test() {
	// System.out.println("test");
	// }
	//
	// // 大きい方を返す
	// public static int max(int a, int b) {
	// return a > b ? a : b;
	// }
	//
	// // 小さい方を返す
	// public static int min(int a, int b) {
	// return a < b ? a : b;
	// }
	//
	// // 0～n-1までの乱数を返す(n=3なら0,1,2のどれかを返す)
	// public static int random(int n) {
	// return (int) (Math.random() * n);
	// }
	//
	// // from～toまでの乱数を返す(from=2,to=5なら、2,3,4,5のどれかを返す)
	// public static int random(int from, int to) {
	// return (int) (from + Math.random() * (to - from + 1));
	// }

	// // 指定されたファイルを読み込んで配列に格納して返す
	// // ErrorBehavior == trueであればエラー時に終了 falseであればエラー時にnullを返す
	// public static ArrayList<String> readFileWithFullPath(String filePath,
	// boolean errorBehavior) {
	//
	// ArrayList<String> list = new ArrayList<String>();
	//
	// BufferedReader br = null;
	// try {
	// FileReader fr = new FileReader(filePath);
	// br = new BufferedReader(fr);
	// String l = null;
	//
	// while ((l = br.readLine()) != null) {
	// // コメント行と空行をスキップ
	// if (l.length() == 0 || l.startsWith("#")) {
	// continue;
	// }
	// // System.out.println(l);
	// list.add(l);
	// }
	// br.close();
	// } catch (IOException | NullPointerException e) {
	// if (errorBehavior) {
	// e.printStackTrace();
	// } else {
	// return null;
	// }
	// }
	// return list;
	// }

	// 指定されたファイルを読み込んで配列に格納して返す
	// ErrorBehavior == trueであればエラー時に終了 falseであればエラー時にnullを返す
	public ArrayList<String> readFile(String filename, boolean errorBehavior, LambdaLogger logger) {

		ArrayList<String> list = new ArrayList<String>();

		URL url = getClass().getClassLoader().getResource(filename);
		String filePath = url.getPath();
		logger.log("Path : " + filePath);

		// ファイルの存在を確認する
		File file = new File(filePath);
		if (file.exists()) {
			logger.log("ファイルが存在します");
		} else {
			logger.log("ファイルが存在しません");
		}

		logger.log("xxx");
		// "src/main/resources"からファイルを読み込む．
		try {
			InputStream is = ClassLoader.getSystemResourceAsStream(filePath);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String l = null;

			while ((l = br.readLine()) != null) {
				logger.log("aaa");
				// コメント行と空行をスキップ
				if (l.length() == 0 || l.startsWith("#")) {
					continue;
				}
				// System.out.println(l);
				list.add(l);
			}
			// } catch (IOException | NullPointerException e) {
		} catch (Exception e) {
			if (errorBehavior) {
				logger.log("bbb");
				e.printStackTrace();
			} else {
				logger.log("ccc");
				return null;
			}
		}
		logger.log("ddd");
		return list;
	}

	// スリープする
	public void sleep(int millisec) {
		try {
			Thread.sleep(millisec);
		} catch (InterruptedException e) {
		}
	}

	//
	// // 文字列に絵文字が含まれているか判定する
	// public static boolean isSurrogate(String text) {
	// for (int i = 0; i < text.length(); i++) {
	// char c = text.charAt(i);
	// if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// 文字列にひらがなが含まれているか判定する
	public boolean isIncludedHiragana(String text) {
		for (int i = 0; i < text.length(); i++) {
			String x = String.valueOf(text.charAt(i));
			if (x.matches("^[\\u3040-\\u309F]+$")) {
				return true;
			}
		}
		return false;
	}
	//
	// // 文字列が半角のみかどうか判定する
	// public static boolean isHankakuOnly(String source) {
	// if (source == null || source.equals("")) {
	// return true;
	// }
	// String regText = "[ -~｡-ﾟ]+";
	// Pattern pattern = Pattern.compile(regText);
	// return pattern.matcher(source).matches();
	// }
}

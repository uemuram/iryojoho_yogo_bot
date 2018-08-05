package jp.gr.java_conf.mu.iyb.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class CommonUtil {

	// 繝�繧ｹ繝�
	public static void test() {
		System.out.println("test");
	}

	// 螟ｧ縺阪＞譁ｹ繧定ｿ斐☆
	public static int max(int a, int b) {
		return a > b ? a : b;
	}

	// 蟆上＆縺�譁ｹ繧定ｿ斐☆
	public static int min(int a, int b) {
		return a < b ? a : b;
	}

	// 0�ｽ柤-1縺ｾ縺ｧ縺ｮ荵ｱ謨ｰ繧定ｿ斐☆(n=3縺ｪ繧�0,1,2縺ｮ縺ｩ繧後°繧定ｿ斐☆)
	public static int random(int n) {
		return (int) (Math.random() * n);
	}

	// from�ｽ柎o縺ｾ縺ｧ縺ｮ荵ｱ謨ｰ繧定ｿ斐☆(from=2,to=5縺ｪ繧峨��2,3,4,5縺ｮ縺ｩ繧後°繧定ｿ斐☆)
	public static int random(int from, int to) {
		return (int) (from + Math.random() * (to - from + 1));
	}

	// 謖�螳壹＆繧後◆繝輔ぃ繧､繝ｫ繧定ｪｭ縺ｿ霎ｼ繧薙〒驟榊�励↓譬ｼ邏阪＠縺ｦ霑斐☆
	// ErrorBehavior == true縺ｧ縺ゅｌ縺ｰ繧ｨ繝ｩ繝ｼ譎ゅ↓邨ゆｺ� false縺ｧ縺ゅｌ縺ｰ繧ｨ繝ｩ繝ｼ譎ゅ↓null繧定ｿ斐☆
	public static ArrayList<String> readFileWithFullPath(String filePath, boolean errorBehavior) {

		ArrayList<String> list = new ArrayList<String>();

		BufferedReader br = null;
		try {
			FileReader fr = new FileReader(filePath);
			br = new BufferedReader(fr);
			String l = null;

			while ((l = br.readLine()) != null) {
				// 繧ｳ繝｡繝ｳ繝郁｡後→遨ｺ陦後ｒ繧ｹ繧ｭ繝�繝�
				if (l.length() == 0 || l.startsWith("#")) {
					continue;
				}
				// System.out.println(l);
				list.add(l);
			}
			br.close();
		} catch (IOException | NullPointerException e) {
			if (errorBehavior) {
				e.printStackTrace();
			} else {
				return null;
			}
		}
		return list;
	}

	// 謖�螳壹＆繧後◆繝輔ぃ繧､繝ｫ繧定ｪｭ縺ｿ霎ｼ繧薙〒驟榊�励↓譬ｼ邏阪＠縺ｦ霑斐☆
	// ErrorBehavior == true縺ｧ縺ゅｌ縺ｰ繧ｨ繝ｩ繝ｼ譎ゅ↓邨ゆｺ� false縺ｧ縺ゅｌ縺ｰ繧ｨ繝ｩ繝ｼ譎ゅ↓null繧定ｿ斐☆
	public static ArrayList<String> readFile(String filename, boolean errorBehavior) {

		ArrayList<String> list = new ArrayList<String>();

		// "src/main/resources"縺九ｉ繝輔ぃ繧､繝ｫ繧定ｪｭ縺ｿ霎ｼ繧��ｼ�
		try {
			InputStream is = ClassLoader.getSystemResourceAsStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String l = null;

			while ((l = br.readLine()) != null) {
				// 繧ｳ繝｡繝ｳ繝郁｡後→遨ｺ陦後ｒ繧ｹ繧ｭ繝�繝�
				if (l.length() == 0 || l.startsWith("#")) {
					continue;
				}
				// System.out.println(l);
				list.add(l);
			}
		} catch (IOException | NullPointerException e) {
			if (errorBehavior) {
				e.printStackTrace();
			} else {
				return null;
			}
		}
		return list;
	}

	// 繧ｹ繝ｪ繝ｼ繝励☆繧�
	// 繧ｹ繝ｪ繝ｼ繝�
	public static void sleep(int millisec) {
		try {
			Thread.sleep(millisec);
		} catch (InterruptedException e) {
		}
	}

	// 譁�蟄怜�励↓邨ｵ譁�蟄励′蜷ｫ縺ｾ繧後※縺�繧九°蛻､螳壹☆繧�
	public static boolean isSurrogate(String text) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
				return true;
			}
		}
		return false;
	}

	// 譁�蟄怜�励↓縺ｲ繧峨′縺ｪ縺悟性縺ｾ繧後※縺�繧九°蛻､螳壹☆繧�
	public static boolean isIncludedHiragana(String text) {
		for (int i = 0; i < text.length(); i++) {
			String x = String.valueOf(text.charAt(i));
			if (x.matches("^[\\u3040-\\u309F]+$")) {
				return true;
			}
		}
		return false;
	}

	// 譁�蟄怜�励′蜊願ｧ偵�ｮ縺ｿ縺九←縺�縺句愛螳壹☆繧�
	public static boolean isHankakuOnly(String source) {
		if (source == null || source.equals("")) {
			return true;
		}
		String regText = "[ -~�ｽ｡-�ｾ歉+";
		Pattern pattern = Pattern.compile(regText);
		return pattern.matcher(source).matches();
	}
}

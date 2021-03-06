package unsupervised;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import pos.PosGram;
import util.FileUtil;
import util.XmlUtil;

public class Boosting {

	HashMap<String, Double> posExpert = new HashMap<String, Double>();
	HashMap<String, Double> negExpert = new HashMap<String, Double>();

	public void initialExperts(String filepath) {
		String str = FileUtil.readFile(filepath);
		String[] str2 = str.split("\n");
		for (String s : str2) {
			String[] tmp = s.split("\t");
			double weight = Double.parseDouble(tmp[1]);
			if (weight > 0)
				posExpert.put(tmp[0], 1.0);
			else
				negExpert.put(tmp[0], 1.0);
		}

		int posSize = posExpert.size();
		int negSize = negExpert.size();
		for (String key : posExpert.keySet()) {
			posExpert.put(key, 1 / (double) posSize);
		}
		for (String key : negExpert.keySet()) {
			negExpert.put(key, 1 / (double) negSize);
		}

	}

	public static double BETA = 0.4;

	public double predict(String context, double sentiment) {
		ArrayList<String> grams = new PosGram().generatePosGram(context);

		HashSet<String> pos = new HashSet<String>();
		HashSet<String> neg = new HashSet<String>();
		for (String s : grams) {
			if (posExpert.containsKey(s)) {
				pos.add(s);
			} 
			if (negExpert.containsKey(s)) {
				neg.add(s);
			}
		}

		double posSentiment = 0.0;
		double negSentiment = 0.0;
		for (String s : pos) {
			posSentiment += posExpert.get(s);
			// System.out.println(posExpert.get(s));
		}
		for (String s : neg) {
			negSentiment += negExpert.get(s);
		}
		double pSent = posSentiment - negSentiment;

		int predict = (pSent > 0.0 ? 1 : -1);
		int label = (sentiment > 0.0 ? 1 : -1);

		if (predict != label) {
			// adjust weight:
			if (sentiment > 0) {
				// positive actually, then penalize neg
				for (String s : neg) {
					negExpert.put(s, negExpert.get(s) * BETA);
				}
			} else {
				// negative actually , penalize pos
				for (String s : pos) {
					posExpert.put(s, posExpert.get(s) * BETA);
				}
			}
		}
		return pSent;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int cycle = 1;
		int counter = 0;
		int right = 0;
		int wrong = 0;
		File file = new File("corpus/opfine_corpus/total/");
		File[] files = file.listFiles();
		Boosting boosting = new Boosting();
		boosting.initialExperts("query_so");
		for (int i = 0; i < files.length; i++) {

			File f = files[i];
			if (f.isHidden())
				continue;
			String text = FileUtil.readFile(f.getAbsolutePath());
			double sentiment = XmlUtil.extractSentiment(text);
			String content = XmlUtil.extractContent(text);
			double predict = boosting.predict(content, sentiment);
			// System.out.println("predit: " + predict + ", sentiment: " +
			// sentiment);
			if ((predict > 0 && sentiment > 0)
					|| (predict < 0 && sentiment < 0)) {
				right++;
			} else {
				wrong++;
			}
			counter++;
			DecimalFormat df = new DecimalFormat("###.####");
			if (counter == 50) {
				System.out.println(cycle + "\t" + right + "/" + counter + "\t"
						+ df.format(((float) (right * 100) / (float) counter))
						+ "%");
				counter = 0;
				right = 0;
				wrong = 0;
				cycle++;
			}
		}

	}
}

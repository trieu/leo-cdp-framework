package leotech.system.util.media;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TextSummarizer {

	public String summarizeText(String input, int maxSentences) {

		ArrayList<String> words = new ArrayList<>();
		words.addAll(Arrays.asList(input.split(" ")));

		HashSet<String> keywords = getKeywords(words);

		ArrayList<String> sentenceList = new ArrayList<>();
		sentenceList.addAll(Arrays.asList(input.split(" *[\\.\\?!][\\'\"\\)\\]]* *")));

		List<Map.Entry<Double, String>> entryList = new ArrayList<>();

		for (String sentence : sentenceList) {

			Map<Double, String> map = new HashMap<>(1);
			map.put(computeSentenceWeight(sentence, keywords), sentence);
			entryList.add(map.entrySet().iterator().next());
		}

		entryList.sort(Comparator.comparing(Map.Entry::getKey));

		StringBuilder ret = new StringBuilder();
		int sentenceCount = maxSentences < sentenceList.size() ? maxSentences : sentenceList.size();

		for (int i = 0; i < sentenceCount; i++)
			ret.append(' ').append(entryList.get(i).getValue()).append('.');

		return ret.toString();

	}

	private HashSet<String> getKeywords(ArrayList<String> words) {
		HashSet<String> ret = new HashSet<>();

		HashMap<String, Integer> countMap = new HashMap<>();

		for (String s : words) {
			if (countMap.get(s) != null)
				countMap.compute(s, (k, v) -> v++);
			else
				countMap.put(s, 0);
		}

		countMap.forEach((word, count) -> {
			double wordPercentage = countMap.get(word) * 1.0 / words.size();
			if (wordPercentage <= 0.5 && wordPercentage >= 0.05)
				ret.add(word);
		});

		return ret;
	}

	@SuppressWarnings("ConstantConditions")
	private double computeSentenceWeight(String sentence, Set<String> keywords) {

		int windowEnd = 0, windowStart = 0;

		String[] sentenceArray = sentence.split(" ");

		// compute window end
		for (int i = 0; i < sentenceArray.length; i++) {
			if (keywords.contains(sentenceArray[i])) {
				windowEnd = i;
				break;
			}
		}

		// compute window start
		for (int i = sentenceArray.length - 1; i > 0; i--) {
			if (keywords.contains(sentenceArray[i])) {
				windowEnd = i;
				break;
			}
		}

		if (windowStart > windowEnd)
			return 0;
		int windowSize = windowEnd - windowStart + 1;

		// number of keywords
		int keywordsCount = 0;
		for (String word : sentenceArray) {
			if (keywords.contains(word))
				keywordsCount++;
		}
		return keywordsCount * keywordsCount * 1.0 / windowSize;
	}

}
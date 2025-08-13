package leotech.cdp.domain.scoring;

import leotech.cdp.model.analytics.ScoreCX;
import rfx.core.util.RandomUtil;

/**
 * the processor for CX scoring
 * 
 * @author tantrieuf31
 *
 */
public final class ProcessorScoreCX {

	/**
	 * compute ScoreCX at scale of 5
	 * 
	 * @param feedbackScore
	 * @param cxScore
	 * @return
	 */
	public static final ScoreCX computeScale5double(double newFeedbackScore, ScoreCX currentScore) {
		return computeScale5double(newFeedbackScore, currentScore.getPositive(), currentScore.getNeutral(),currentScore.getNegative());
	}

	/**
	 * compute Score for Customer Experience CX at scale of 5
	 * 
	 * @param ratingScore
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @return ScoreCX
	 */
	public static final ScoreCX computeScale5double(double ratingScore, int curPositive, int curNeutral, int curNegative) {
		if (ratingScore < 0 || ratingScore > 5) {
			System.err.println("Invalid feedbackScore at computeScale5double " + ratingScore);
			return null;
		}
		// init
		ScoreCX score = new ScoreCX();
		// positive 100
		if (ratingScore >= 4.3 && ratingScore <= 5) {
			positive100Percentage(curPositive, curNeutral, curNegative, score);
		}
		// positive 50%
		else if (ratingScore >= 3.3 && ratingScore < 4.3) {
			positive50Percentage(curPositive, curNeutral, curNegative, score);
		}
		// neutral 100
		else if (ratingScore >= 2.3 && ratingScore < 3.3) {
			neutral100Percentage(curPositive, curNeutral, curNegative, score);
		}
		// negative 50%
		else if (ratingScore >= 1.3 && ratingScore < 2.3) {
			negative50Percentage(curPositive, curNeutral, curNegative, score);
		}
		// negative 100
		else if (ratingScore > 0 && ratingScore < 1.3) {
			negative100Percentage(curPositive, curNeutral, curNegative, score);
		}
		else {
			neutral100Percentage(curPositive, curNeutral, curNegative, score);
		}
		score.computeSentimentScoreAndPercentage();
		return score;
	}

	/**
	 * compute ScoreCX at scale of 5
	 * 
	 * @param feedbackScore
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @return ScoreCX
	 */
	public static final ScoreCX computeScale5integer(int feedbackScore, int curPositive, int curNeutral,
			int curNegative) {
		//
		System.out.println("computeScale5integer feedbackScore " + feedbackScore);
		System.out.println("computeScale5integer curPositive " + curPositive);
		System.out.println("computeScale5integer curNeutral " + curNeutral);
		System.out.println("computeScale5integer curNegative " + curNegative);

		if (feedbackScore <= 0 || feedbackScore > 5) {
			System.err.println("Invalid feedbackScore at computeScale5integer " + feedbackScore);
			return null;
		}

		ScoreCX score = new ScoreCX();

		if (feedbackScore == 5) {
			positive100Percentage(curPositive, curNeutral, curNegative, score);
		} else if (feedbackScore == 1) {
			negative100Percentage(curPositive, curNeutral, curNegative, score);
		} else if (feedbackScore == 3) {
			neutral100Percentage(curPositive, curNeutral, curNegative, score);
		} else if (feedbackScore == 4) {
			positive50Percentage(curPositive, curNeutral, curNegative, score);
		} else if (feedbackScore == 2) {
			negative50Percentage(curPositive, curNeutral, curNegative, score);
		}
		score.computeSentimentScoreAndPercentage();
		System.out.println("Result score " + score);
		return score;
	}

	/**
	 * compute ScoreCX at scale of 10 (NPS)
	 * 
	 * @param feedbackEventScore
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @return ScoreCX
	 */
	public static final ScoreCX computeScale10(int feedbackEventScore, int curPositive, int curNeutral,
			int curNegative) {
		if (feedbackEventScore < 0 || feedbackEventScore > 10) {
			System.err.println("Invalid feedbackScore at computeScale10 " + feedbackEventScore);
			return null;
		}

		int percentScore = feedbackEventScore * 10;
		ScoreCX score = null;
		if (percentScore >= 0 && percentScore <= 60) {
			int impact = 100 - percentScore;
			if (curNegative == 0 && curNeutral == 0 && curPositive == 0) {
				curNegative = 100;
			} else {
				curNegative = (int) Math.floor((curNegative + impact) / 2);
			}
			int neutral = (100 - curNegative) / 2;
			int positive = 100 - curNegative - neutral;
			score = new ScoreCX(positive, neutral, curNegative);
		} else if (percentScore > 60 && percentScore <= 80) {
			if (curNeutral == 0 && curNegative == 0 && curPositive == 0) {
				curNeutral = 100;
			} else {
				curNeutral = (int) Math.ceil((curNeutral + percentScore) / 2);
			}
			int negative = (100 - curNeutral) / 2;
			int positive = 100 - curNeutral - negative;
			score = new ScoreCX(positive, curNeutral, negative);
		} else {
			if (curPositive == 0 && curNegative == 0 && curNeutral == 0) {
				curPositive = 100;
			} else {
				curPositive = (int) Math.ceil((curPositive + percentScore) / 2);
			}
			int negative = (100 - curPositive) / 2;
			int neutral = 100 - curPositive - negative;
			score = new ScoreCX(curPositive, neutral, negative);
		}
		score.computeSentimentScoreAndPercentage();
		return score;
	}

	// ------------------ --------- --------- --------- local functions ---------
	// --------- --------- --------- --------- --------- ---------

	/**
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @param score
	 */
	static void negative50Percentage(int curPositive, int curNeutral, int curNegative, ScoreCX score) {
		int neutral = curNeutral;
		if (neutral == 0 && curPositive == 0 && curNegative == 0) {
			if (curPositive >= curNegative) {
				neutral = RandomUtil.getRandomInteger(40, 30);
			} else {
				neutral = RandomUtil.getRandomInteger(30, 20);
			}
		} else {
			neutral = (int) Math.ceil((neutral + 30) / 2);
		}
		score.setNeutral(neutral);

		int positive = curPositive;
		if (curNegative == 0) {
			positive = 100 - neutral;
		} else {
			int adiff = curPositive - curNegative;
			if (adiff >= 0) {
				positive = (int) Math.ceil((100 - neutral) / 2F);
			} else {
				positive = (int) Math.ceil((100 - neutral) / 3F);
			}
		}
		score.setPositive(positive);

		int remain = 100 - positive - neutral;
		score.setNegative(remain);
	}

	/**
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @param score
	 */
	static void positive50Percentage(int curPositive, int curNeutral, int curNegative, ScoreCX score) {
		int neutral = curNeutral;
		if (neutral == 0 && curPositive == 0 && curNegative == 0) {
			if (curPositive >= curNegative) {
				neutral = RandomUtil.getRandomInteger(80, 70);
			} else {
				neutral = RandomUtil.getRandomInteger(70, 60);
			}
		} else {
			neutral = (int) Math.ceil((neutral + 70) / 2);
		}
		score.setNeutral(neutral);
		
		int positive = curPositive;
		if (curPositive == 0 ) {
			positive = 100 - neutral;
		} else {
			int adiff = curNegative - curPositive;
			if (adiff >= 0) {
				positive = (int) Math.ceil((100 - neutral) / 2F);
			} else {
				positive = (int) Math.ceil((100 - neutral) / 3F);
			}
		}
		score.setPositive(positive);

		int remain = 100 - positive - neutral;
		score.setNegative(remain);
	}

	/**
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @param score
	 */
	static void neutral100Percentage(int curPositive, int curNeutral, int curNegative, ScoreCX score) {
		int neutral = curNeutral;
		if (neutral == 0 && curPositive == 0 && curNegative == 0) {
			neutral = 100;
		} else {
			neutral = (int) Math.ceil((neutral + 100) / 2);
		}
		score.setNeutral(neutral);

		int positive = curPositive;
		int adiff = curPositive - curNegative;
		if (adiff >= 0) {
			if (curNegative == 0) {
				positive = 100 - neutral;
			} else {
				positive = (int) Math.ceil((100 - neutral) / 2F);
			}
		} else {
			positive = (int) Math.ceil((100 - neutral) / 3F);
		}
		score.setPositive(positive);

		int remain = 100 - positive - neutral;
		score.setNegative(remain);
	}

	/**
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @param score
	 */
	static void negative100Percentage(int curPositive, int curNeutral, int curNegative, ScoreCX score) {
		int negative = curNegative;
		if (negative == 0 && curNeutral == 0 && curPositive == 0) {
			negative = 100;
		} else {
			negative = (int) Math.ceil((negative + 100) / 2F);
		}
		score.setNegative(negative);

		//
		int neutral = curNeutral;
		if (neutral == 0 || curPositive == 0) {
			neutral = 100 - negative;
		} else {
			neutral = (int) Math.ceil((100 - negative) / 2F);
		}
		score.setNeutral(neutral);

		//
		int positive = 100 - negative - neutral;
		score.setPositive(positive);
	}

	/**
	 * @param curPositive
	 * @param curNeutral
	 * @param curNegative
	 * @param score
	 */
	static void positive100Percentage(int curPositive, int curNeutral, int curNegative, ScoreCX score) {
		int positive = curPositive;
		if (positive == 0 && curNeutral == 0 && curNegative == 0) {
			positive = 100;
		} else {
			positive = (int) Math.ceil((positive + 100) / 2);
		}
		score.setPositive(positive);

		//
		int neutral = curNeutral;
		if (neutral == 0 || curNegative == 0) {
			neutral = 100 - positive;
		} else {
			neutral = (int) Math.ceil((100 - positive) / 2F);
		}
		score.setNeutral(neutral);

		//
		int negative = 100 - positive - neutral;
		score.setNegative(negative);
	}
}

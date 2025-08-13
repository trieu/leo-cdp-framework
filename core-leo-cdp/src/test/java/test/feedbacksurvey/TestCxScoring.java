package test.feedbacksurvey;

import leotech.cdp.domain.scoring.ProcessorScoreCX;
import leotech.cdp.model.analytics.ScoreCX;
import rfx.core.util.RandomUtil;

public class TestCxScoring {

	static int NUMBER_TEST = 5000;
	public static void main(String[] args) {

		
		runTestInteger();
		runTestDouble();
		
		//runTestCxScoringInt(4, NUMBER_TEST);
	}

	 static void runTestDouble() {
		runTestCxScoringDouble(1, NUMBER_TEST);
		runTestCxScoringDouble(0.5, NUMBER_TEST);
		runTestCxScoringDouble(1.5, NUMBER_TEST);
		runTestCxScoringDouble(1.7, NUMBER_TEST);
		runTestCxScoringDouble(1.3, NUMBER_TEST);
		
		runTestCxScoringDouble(2, NUMBER_TEST);
		runTestCxScoringDouble(2.2, NUMBER_TEST);
		runTestCxScoringDouble(2.5, NUMBER_TEST);
		runTestCxScoringDouble(2.7, NUMBER_TEST);
		
		runTestCxScoringDouble(3, NUMBER_TEST);
		runTestCxScoringDouble(3.1, NUMBER_TEST);
		runTestCxScoringDouble(3.5, NUMBER_TEST);
		runTestCxScoringDouble(3.7, NUMBER_TEST);
		
		runTestCxScoringDouble(4, NUMBER_TEST);
		runTestCxScoringDouble(4.1, NUMBER_TEST);
		runTestCxScoringDouble(4.5, NUMBER_TEST);
		runTestCxScoringDouble(4.7, NUMBER_TEST);
		
		runTestCxScoringDouble(5, NUMBER_TEST);
		
		//runTestCxScoringDouble(5.1);
		//runTestCxScoringDouble(6);
		//runTestCxScoringDouble(-1);
	}
	
	 static void runTestInteger() {
		runTestCxScoringInt(1, NUMBER_TEST);
		runTestCxScoringInt(2, NUMBER_TEST);
		runTestCxScoringInt(3, NUMBER_TEST);
		runTestCxScoringInt(4, NUMBER_TEST);
		runTestCxScoringInt(5, NUMBER_TEST);
		//runTestCxScoringInt(6);
		//runTestCxScoringInt(-1);
	}

	static void runTestCxScoringInt(int feedbackScoreInit, int loopCount) {
		//
		ScoreCX score = null;
		
		for (int i = 0; i < loopCount; i++) {
			int feedbackScore = RandomUtil.getRandomInteger(5, 1);
			if(score == null) {
				feedbackScore = feedbackScoreInit;
				System.out.println("\n ===> feedbackScore " + feedbackScore);
				
				score = new ScoreCX();
				int currentPositive = score.getPositive();
				int currentNeutral = score.getNeutral();
				int currentNegative = score.getNegative();
				System.out.println("currentPositive " + currentPositive);
				System.out.println("currentNeutral " + currentNeutral);
				System.out.println("currentNegative " + currentNegative);
				score = ProcessorScoreCX.computeScale5integer(feedbackScore, currentPositive, currentNeutral, currentNegative);
			} else {
				System.out.println("\n ===> feedbackScore " + feedbackScore);
				
				int currentPositive = score.getPositive();
				int currentNeutral = score.getNeutral();
				int currentNegative = score.getNegative();
				System.out.println("currentPositive " + currentPositive);
				System.out.println("currentNeutral " + currentNeutral);
				System.out.println("currentNegative " + currentNegative);
				score = ProcessorScoreCX.computeScale5integer(feedbackScore, currentPositive, currentNeutral, currentNegative);
			}
			
			int sum = score.getPositive() + score.getNeutral() + score.getNegative();
			if(score.getPositive() < 0 || score.getNeutral() < 0 || score.getNegative() < 0 || sum > 100) {
				System.err.println(score);
				throw new IllegalArgumentException(score.toString());
			} else {
				System.out.println(score);
			}
			
		}
	}
	
	static void runTestCxScoringDouble(double feedbackScoreInit, int loopCount) {
		//
		ScoreCX score = null;
		
		for (int i = 0; i < loopCount; i++) {
			double feedbackScore = (double)RandomUtil.getRandomInteger(5, 1) + ( (double) RandomUtil.getRandomInteger(100, 1) / 100F);
			feedbackScore = Math.round(feedbackScore * 100.00) / 100.0;
			if(score == null) {
				feedbackScore = feedbackScoreInit;
				System.out.println("\n ===> feedbackScore " + feedbackScore);
				
				score = new ScoreCX();
				int currentPositive = score.getPositive();
				int currentNeutral = score.getNeutral();
				int currentNegative = score.getNegative();
				System.out.println("currentPositive " + currentPositive);
				System.out.println("currentNeutral " + currentNeutral);
				System.out.println("currentNegative " + currentNegative);
				score = ProcessorScoreCX.computeScale5double(feedbackScore, currentPositive, currentNeutral, currentNegative);
			} else {
				System.out.println("\n ===> feedbackScore " + feedbackScore);
				
				int currentPositive = score.getPositive();
				int currentNeutral = score.getNeutral();
				int currentNegative = score.getNegative();
				System.out.println("currentPositive " + currentPositive);
				System.out.println("currentNeutral " + currentNeutral);
				System.out.println("currentNegative " + currentNegative);
				score = ProcessorScoreCX.computeScale5double(feedbackScore, currentPositive, currentNeutral, currentNegative);
			}
			
			int sum = score.getPositive() + score.getNeutral() + score.getNegative();
			if(score.getPositive() < 0 || score.getNeutral() < 0 || score.getNegative() < 0 || sum > 100) {
				System.err.println(score);
				throw new IllegalArgumentException(score.toString());
			} else {
				System.out.println(score);
			}
			
		}
	}
}

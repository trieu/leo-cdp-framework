package leotech.crawler.model;

import com.google.gson.Gson;

public class SocialStatistics {

	long likeCount;
	long bookmarkCount;
	long commentCount;
	long viewCount;
	long totalScore = 0;

	public SocialStatistics() {

	}

	public SocialStatistics(long likeCount, long bookmarkCount, long commentCount, long view) {
		super();
		this.likeCount = likeCount;
		this.bookmarkCount = bookmarkCount;
		this.commentCount = commentCount;
		this.viewCount = view;
		this.totalScore = 0;
	}

	public long getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(long likeCount) {
		this.likeCount = likeCount;
	}

	public long getBookmarkCount() {
		return bookmarkCount;
	}

	public void setBookmarkCount(long bookmarkCount) {
		this.bookmarkCount = bookmarkCount;
	}

	public long getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(long commentCount) {
		this.commentCount = commentCount;
	}

	public long getViewCount() {
		return viewCount;
	}

	public void setViewCount(long viewCount) {
		this.viewCount = viewCount;
	}

	public void computeDefaultScore() {
		if (totalScore == 0 && viewCount > 0) {
			totalScore = viewCount + commentCount * 3 + bookmarkCount * 4 + likeCount * 2;
		}
	}

	public long getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(long totalScore) {
		this.totalScore = totalScore;
	}

	public long totalScore() {
		computeDefaultScore();
		return totalScore;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}

package jp.gr.java_conf.mu.iyb.handler;

public class YogoDao {
	private Integer offset;
	private String keyword;
	private String description;
	private long beforeTweetId;

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getBeforeTweetId() {
		return beforeTweetId;
	}

	public void setBeforeTweetId(long beforeTweetId) {
		this.beforeTweetId = beforeTweetId;
	}

}

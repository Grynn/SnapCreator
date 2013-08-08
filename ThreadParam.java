public class ThreadParam { 
	private int min;
	private int max;
	private int threadId;
	private String status;
	private String zimbraHost;
	public ThreadParam(int threadId, int min, int max, String status, String zimbraHost) {
		this.threadId = threadId;
		this.min = min;
		this.max = max;
		this.status = status;
		this.zimbraHost = zimbraHost;
	}
	public String getZimbraHost() {
		return zimbraHost;
	}

	public int getThreadId() {
		return threadId;
	}
	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}
	
	public String getStatus() {
		return status;
	}
	
}

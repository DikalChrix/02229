package objectClasses;

public class EDPTuple {
	private boolean result;
	private int responseTime;
	
	public EDPTuple(boolean Result, int ResponseTime) {
		this.setResult(Result);
		this.setResponseTime(ResponseTime);
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}

	public int getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(int responseTime) {
		this.responseTime = responseTime;
	}

}

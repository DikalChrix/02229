
public class testFormat {
	private String name;
	private int duration;
	private int priority;
	private String type;
	private int period;
	private int deadline;
	
	public testFormat(String Name, int Duration, int Period, String Type, int Priority, int Deadline) {
		this.setName(Name);
		this.setDuration(Duration);
		this.setPriority(Priority);
		this.setType(Type);
		this.setPeriod(Period);
		this.setDeadline(Deadline);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}

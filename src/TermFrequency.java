
public class TermFrequency implements Comparable<TermFrequency> {
	
	private String term;
	private int frequency;
	
	public TermFrequency(String term, int frequency) {
		this.term = term;
		this.frequency = frequency;
	}
	
	public String getTerm() {
		return term;
	}
	
	public int getFrequency() {
		return frequency;
	}
	
	public int compareTo(TermFrequency arg0) {
		return term.compareTo(arg0.getTerm());
	}

}

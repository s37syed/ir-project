public class EvalQuery {

	private int id;
	private String source;
	private String query;

	public EvalQuery() {
		this.query =
		this.source = "";
		this.id = -1;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @param query the query to set
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	public void addQuery(String query) {
		this.query += (this.query.isEmpty() ? "" : " ") + query;
	}

	public void addSource(String source) {
		this.source += (this.source.isEmpty() ? "" : " ") + source;
	}

	@Override
	public String toString() {
		StringBuilder strbuf = new StringBuilder();
		strbuf.append("QueryID: ").append(id).
			append("\nQuery: ").append(query).
			append("\nSource: ").append(source);
		return strbuf.toString();
	}
}
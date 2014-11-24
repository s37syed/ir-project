import java.util.*;


public class Search {

	private static String ZZEND = "ZZEND";
	private static String YES = "y";
	private static String NO = "n";
	private static Map<Integer, Document> documents;
	private static Map<String,DocumentFrequency> documentFrequencies;
	private static DocumentParser docParser;
	private static boolean stopwordsOn = true;
	private static boolean stemmerOn = true;
	private static Set<String> stopwords;
	private static int topK = 15;
	private static double w1 = 0;
	private static double w2 = 0;
	private static final int MAX_ITERATIONS = 30;
	private static final double ALPHA = 0.15;

	public static void setTFIDFWeights() {
		for (Integer docID : documents.keySet()) {
			Document doc = documents.get(docID);
			for (String term : doc.getTerms()) {
				double tf = doc.getFrequency(term);		
				tf = 1 + Math.log10(tf);
				DocumentFrequency docFreq = documentFrequencies.get(term);
				int df = 0;
				if (docFreq != null) {
					df = docFreq.getDocumentFrequency();
				}
				double idf = Math.log10(documents.size() / df);
				if (Double.isNaN(idf)) {
					idf = 0.0;
				}
				doc.setWeight(term, tf * idf);
			}
		}
	}

	public static Document queryVector(String query) {
		Document queryDoc = new Document();	
		queryDoc.setAbstract(query);
		DocumentParser queryParser = new DocumentParser(stemmerOn);
		queryParser.setDocument(queryDoc);
		for (String term : queryParser.findTerms()) {
			if (!stopwordsOn || !stopwords.contains(term)) {
				int tf = queryParser.calculateFrequency(term);
				queryDoc.setFrequency(term, tf);
				/*
				DocumentFrequency docFreq = documentFrequencies.get(term);
				double df = 0;
				if (docFreq != null) {
					df = docFreq.getDocumentFrequency();
				}				
				double idf = Math.log10(documents.size()/df);	
				if (Double.isNaN(idf)) {
		            idf = 0.0;
		        }
				if (idf == 0) {
					System.out.println("\nIDF IS ZERO\n");
				}
				 */
				double weight = (1 + Math.log10(tf));
				queryDoc.setWeight(term, weight);
			}
		}
		return queryDoc;
	}

	public static double sim(Document doc, Document queryDoc) {
		double product = 0;
		double docMagnitude = 0;
		double queryMagnitude = 0;
		Set<String> terms = new HashSet<String>();
		terms.addAll(doc.getTerms());
		terms.addAll(queryDoc.getTerms());
		for(String term : terms) {			
			double docWeight = doc.getWeight(term);
			double queryWeight = queryDoc.getWeight(term);
			product += (docWeight*queryWeight);
			docMagnitude += Math.pow(docWeight, 2);
			queryMagnitude += Math.pow(queryWeight,2);
		}
		docMagnitude = Math.sqrt(docMagnitude);
		queryMagnitude = Math.sqrt(queryMagnitude);
		if (docMagnitude > 0 && queryMagnitude > 0) {
			return product / (docMagnitude * queryMagnitude);
		}
		else {
			return 0;
		}
	}

	public static void calculatePageRank() {
		Set<Integer> documentSet = documents.keySet();
		int n = documentSet.size();
		double p[][] = new double[n][n];
		double x[] = new double[n];
		for (Integer docID : documentSet) {
			x[docID-1] = 0.0;
			Document doc = documents.get(docID);
			Set<Integer> citationSet = doc.getCitations();
			double probability = 0;
			if (citationSet != null || citationSet.isEmpty()) {
				probability = (1.0/citationSet.size()) * (1.0-ALPHA) + ALPHA/n;
			}
			else {
				probability = (1-ALPHA)/n + ALPHA/n;
			}
			for (int i = 0; i < n; i++) {
				if (citationSet != null || citationSet.isEmpty()) {
					if (citationSet.contains(i+1)) {
						p[docID-1][i] = probability;
					}
					else {
						p[docID-1][i] = ALPHA/n;
					}
				}
				else {
					p[docID-1][i] = probability;
				}
			}
		}
		x[0] = 1.0;
		for (int i = 1; i <= MAX_ITERATIONS; i++) {
			double sum = 0;
			double xP[] = new double[x.length];
			for (int a = 0; a < n; a++) {
				for (int b = 0; b < n; b++) {
					sum += x[b]*p[b][a];
				}
				xP[a] = sum;
				sum = 0;
			}
			x = xP;
		}
		double sum = 0;
		for (Integer docID : documents.keySet()) {
			Document doc = documents.get(docID);
			doc.setPageRank(x[docID-1]);
			sum += x[docID-1];
		}
		System.out.println("Sum of all PageRanks: " + sum);
	}

	public static double score(Document doc, Document query) {
		return w1 * sim(doc,query) + w2 * doc.getPageRank();
	}

	public static void main(String[] args) {		
		Scanner user = new Scanner(System.in);
		System.out.println("Would you like to use stop words? y/n");
		String input = user.nextLine();

		while (!(input.equalsIgnoreCase(YES) || input.equalsIgnoreCase(NO))) {
			System.out.println("Invalid entry");
			input = user.nextLine();
		}
		if (input.equalsIgnoreCase(YES)) {
			stopwordsOn = true;
		}
		else if (input.equalsIgnoreCase(NO)) {
			stopwordsOn = false;
		}

		System.out.println("Would you like to use stemming? y/n");
		input = user.nextLine();
		while (!(input.equalsIgnoreCase(YES) || input.equalsIgnoreCase(NO))) {
			System.out.println("Invalid entry");
			input = user.nextLine();
		}		
		if (input.equalsIgnoreCase(YES)) {
			stemmerOn = true;
		}
		else if (input.equalsIgnoreCase(NO)) {
			stemmerOn = false;
		}

		System.out.println("Creating tf-idf vectors and calculating PageRanks, please wait...");

		long startTime = System.currentTimeMillis();

		Invert invertedIndex = new Invert(stopwordsOn,stemmerOn);		
		invertedIndex.invert();

		stopwords = invertedIndex.getStopWords();
		documents = invertedIndex.getDocuments();
		docParser = new DocumentParser();
		documentFrequencies = invertedIndex.getDocumentFrequencies();

		setTFIDFWeights();
		calculatePageRank();

		long totalTime = System.currentTimeMillis() - startTime;

		System.out.println("Total time to create tf-idf vectors and calculate PageRanks: " + totalTime + "ms"); 

		System.out.print("\nPlease enter a query: ");
		for (input = user.nextLine(); !input.equalsIgnoreCase(ZZEND); input = user.nextLine()) {
			System.out.println("Please enter values for w1 and w2, where w1 + w2 = 1");
			System.out.print("w1: ");
			w1 = Double.parseDouble(user.nextLine());
			System.out.print("w2: ");
			w2 = Double.parseDouble(user.nextLine());
			while (w1 + w2 != 1) {
				System.out.println("You have entered invalid w1 and w2 values, please re-enter: ");
				System.out.print("w1: ");
				w1 = Double.parseDouble(user.nextLine());
				System.out.print("w2: ");
				w2 = Double.parseDouble(user.nextLine());
			}
			Set<RelevanceScore> relevanceScores = new TreeSet<RelevanceScore>(Collections.reverseOrder());
			long queryStart = System.currentTimeMillis();
			for (Integer docID : documents.keySet()) {
				RelevanceScore rs = new RelevanceScore(docID, score(documents.get(docID),queryVector(input)));	
				if (rs.getScore() > 0) {
					relevanceScores.add(rs);
				}
			}

			int j = 0;
			if (!relevanceScores.isEmpty()) {
				for (RelevanceScore rs : relevanceScores) {
					if (j >= topK) {
						break;
					}
					int docID = rs.getID();
					Document doc = documents.get(docID);
					docParser.setDocument(doc);
					System.out.println((j+1) + ") " + doc.getTitle().replaceAll("\n", " "));
					System.out.println("\tDocument ID:\t\t" + doc.getID());
					System.out.println("\tAuthor:\t\t\t" + doc.getAuthor().replaceAll("\n",", "));
					System.out.println("\tRelevance score:\t" + rs.getScore().doubleValue());
					System.out.println("\tPageRank:\t\t" + doc.getPageRank());
					j += 1;
				}
				if (j < topK) {
					System.out.println("\nNo more relevant results");
				}
			}
			else {
				System.out.println("No results for query: " + input);
			}
			long queryEnd = System.currentTimeMillis() - queryStart;
			System.out.println("\nTime required to create query vector and relevance scores: " + (queryEnd) + " ms");
			System.out.print("\nPlease enter a query: ");
		}
		user.close();
		System.out.println("ZZEND");
	}
}

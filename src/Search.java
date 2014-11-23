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

		System.out.println("Creating tf-idf vectors, please wait...");

		long startTime = System.currentTimeMillis();

		Invert invertedIndex = new Invert(stopwordsOn,stemmerOn);		
		invertedIndex.invert();
		
		stopwords = invertedIndex.getStopWords();
		documents = invertedIndex.getDocuments();
		docParser = new DocumentParser();
		documentFrequencies = invertedIndex.getDocumentFrequencies();
		
		setTFIDFWeights();
		
		long totalTime = System.currentTimeMillis() - startTime;

		System.out.println("Total time to create tf-idf vectors: " + totalTime + "ms"); 

		System.out.print("\nPlease enter a query: ");
		for (input = user.nextLine(); !input.equalsIgnoreCase(ZZEND); input = user.nextLine()) {
			Set<RelevanceScore> relevanceScores = new TreeSet<RelevanceScore>(Collections.reverseOrder());
			long queryStart = System.currentTimeMillis();
			for (Integer docID : documents.keySet()) {
				RelevanceScore rs = new RelevanceScore(docID, sim(documents.get(docID),queryVector(input)));	
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

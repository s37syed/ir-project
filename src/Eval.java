import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class Eval {

	private static String QUERY_FILE = "query.text";
	private static String QRELS = "qrels.text";

	private static String YES = "y";
	private static String NO = "n";
	private static Map<Integer, Document> documents;
	private static Map<String,DocumentFrequency> documentFrequencies;
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

	public static Map<Integer, Set<Integer>> parseQRels(String fileName) throws IOException,FileNotFoundException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));

		Map<Integer, Set<Integer>> qrelsList = new TreeMap<Integer, Set<Integer>>();
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			if ((line = line.trim()).isEmpty()) {
				continue;
			}
			String[] tokens = line.split("\\s+");
			int queryNum = Integer.parseInt(tokens[0]);
			int docs = Integer.parseInt(tokens[1]);

			Set<Integer> relevantDocs = qrelsList.get(queryNum);
			if (relevantDocs == null) {
				relevantDocs = new HashSet<Integer>();
			}
			relevantDocs.add(docs);
			qrelsList.put(queryNum, relevantDocs);
		}
		reader.close();
		return qrelsList;
	}

	public static void main(String[] args) throws Exception {		
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

		//parsing query.text
		BufferedReader reader = new BufferedReader(new FileReader(new File(QUERY_FILE)));
		LinkedList<EvalQuery> queries = new LinkedList<EvalQuery>();
		EvalQuery query = null;
		String line = "";
		char state = 0;
		while ((line = reader.readLine()) != null) {
			if ((line = line.trim()).isEmpty()) {
				continue;
			}
			if (line.charAt(0) == '.') {
				state = line.charAt(1);
				if (state == 'I') {
					if (query != null) {
						queries.add(query);
					}
					query = new EvalQuery();
					query.setId(Integer.parseInt(line.substring(2).trim()));
				}
			} else {
				switch (state) {
				case 'A':
				case 'W':
					query.addQuery(line);
					break;
				case 'N':
					query.addSource(line);
					break;
					/* I and no state should never happen */
				case 'I':
				default:
				}
			}
		}
		reader.close();

		Map<Integer,Set<Integer>> relevantDocs = parseQRels(QRELS);

		long startTime = System.currentTimeMillis();

		Invert invertedIndex = new Invert(stopwordsOn,stemmerOn);		
		invertedIndex.invert();

		stopwords = invertedIndex.getStopWords();
		documents = invertedIndex.getDocuments();
		documentFrequencies = invertedIndex.getDocumentFrequencies();

		setTFIDFWeights();

		long totalTime = System.currentTimeMillis() - startTime;

		System.out.println("Total time to create tf-idf vectors: " + totalTime + "ms"); 

		System.out.print("\nCreating MAP and R-Precision Values\n");
		Set<Double> mapSet = new HashSet<Double>();
		Set<Double> rPrecisionSet = new HashSet<Double>();

		long queryStart = System.currentTimeMillis();
		double averageRPrecision = 0;
		double averageMAPValue = 0;
		for (Integer queryNum : relevantDocs.keySet()) {
			Set<RelevanceScore> relevanceScores = new TreeSet<RelevanceScore>(Collections.reverseOrder());
			String currentQuery = queries.get(queryNum-1).getQuery();
			for (Integer docID : documents.keySet()) {
				RelevanceScore rs = new RelevanceScore(docID, sim(documents.get(docID),queryVector(currentQuery)));	
				if (rs.getScore() > 0) {
					relevanceScores.add(rs);
				}
			}

			Set<Integer> relDocs = relevantDocs.get(queryNum);
			int r = relDocs.size();
			double j = 1;
			double relevantDocNum = 0;
			double mapValue = 0;
			if (!relevanceScores.isEmpty()) {
				for (RelevanceScore rs : relevanceScores) {
					if (j > topK) {
						break;
					}
					int docID = rs.getID();
					if (relDocs.contains(docID)) {
						if (j <= r) {
							relevantDocNum++;
						}
						mapValue += relevantDocNum / j;
					}
					j += 1;
				}
				double rPrecision = relevantDocNum / r;
				rPrecisionSet.add(rPrecision);
				averageRPrecision += rPrecision;
				System.out.println("\nQuery " + queryNum + "\n\tRelevant documents retrieved: " + relevantDocNum +
						" / Total relevant documents in query: " + r);
				System.out.println("\tR-Precision = " + rPrecision);
				double totalMAP = mapValue / r;
				mapSet.add(totalMAP);
				averageMAPValue += totalMAP;
				System.out.println("\tMAP Value = " + totalMAP);
			}
			else {
				System.out.println("No match for query " + queryNum + ": " + currentQuery);
			}
		}
		user.close();
		System.out.println("Average R-Precision over " + queries.size() + " queries = " + (averageRPrecision/rPrecisionSet.size()));
		System.out.println("Average MAP Value over "+ queries.size() + " queries = " + (averageMAPValue/mapSet.size()));
		long queryEnd = System.currentTimeMillis() - queryStart;
		System.out.println("\nTime required to create MAP and R-Precision values: " + (queryEnd) + " ms");
	}
}

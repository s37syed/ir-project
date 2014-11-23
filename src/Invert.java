import java.text.ParseException;
import java.util.*;
import java.io.*;

public class Invert {

	private final String CACM_FILE = "cacm.all";
	private final String STOPWORD_FILE = "stopwords.txt";

	private Set<String> dictionary;
	private Map<Integer, Document> documents;
	private Map<String, DocumentFrequency> documentFrequencies;
	private Set<String> stopWords = new TreeSet<String>();
	private FileParser fileParser;
	private DocumentParser docParser;
	private boolean stopwordsOn;

	public Invert (boolean stopwordsOn, boolean stemmerOn) {
		fileParser = new FileParser();
		docParser = new DocumentParser(stemmerOn);
		dictionary = new TreeSet<String>();
		documentFrequencies = new TreeMap<String, DocumentFrequency>();
		try {
			documents = fileParser.extractDocuments(CACM_FILE);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		this.stopwordsOn = stopwordsOn;
		if (stopwordsOn) {
			stopWords = createStopWords(STOPWORD_FILE);
		}
	}

	public Set<String> getDictionary() {
		return dictionary;
	}

	public Map<Integer,Document> getDocuments() {
		return documents;
	}

	public Map<String, DocumentFrequency> getDocumentFrequencies() {
		return documentFrequencies;
	}

	public Set<String> createStopWords(String fileName) {
		Set<String> words = new TreeSet<String>();
		try {
			File stopwordFile = new File(fileName);
			BufferedReader reader = new BufferedReader(new FileReader(stopwordFile));			
			for (String word = reader.readLine(); word != null; word = reader.readLine()) {
				words.add(word);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return words;
	}

	public Set<String> getStopWords() {
		return stopWords;
	}

	public void invert() {
		for (Integer docID : documents.keySet()) {
			Document doc = documents.get(docID);
			docParser.setDocument(doc);
			for (String term : docParser.findTerms()) {
				if (!stopwordsOn || !stopWords.contains(term)) {	
					dictionary.add(term);
					int tf = docParser.calculateFrequency(term);
					doc.setFrequency(term, tf);
					DocumentFrequency df = documentFrequencies.get(term);
					if (df == null) {
						df = new DocumentFrequency();
						if (tf > 0) {
							df.updateFrequency();
						}
						documentFrequencies.put(term, df);
					}
					else if (tf > 0) {
						df.updateFrequency();
					}
				}
			}
		}
		System.out.println("Number of terms:\t" + dictionary.size());
		System.out.println("Number of documents:\t" + documents.size());
	}
}

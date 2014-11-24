//CPS842 Assignment 1
//By: Harold Leung 500383211

//This class parses a file to convert into documents

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public class FileParser {

	private final String DOC_ID = ".I";
	private final String TITLE = ".T";
	private final String ABSTRACT = ".W";
	private final String PUBLICATION_DATE = ".B";
	private final String AUTHOR = ".A";
	private final String CITATION = ".X";
	private final String K = ".K";
	private final String C = ".C";
	private final String N = ".N";
	private final String SPLIT = "\\s+";

	public FileParser() {
	}

	public Map<Integer, Document> extractDocuments(String fileName) throws ParseException {
		Map<Integer, Document> documents = new TreeMap<Integer, Document>();
		try {
			File file = new File(fileName);		
			BufferedReader reader = new BufferedReader(new FileReader(file));
			int currentID = 0;
			String line = reader.readLine();
			while (line != null) {
				if (line.startsWith(DOC_ID)) {
					currentID = parseID(line);
					Document doc = new Document(currentID);
					documents.put(currentID, doc);
					line = reader.readLine();
				}
				else if (line.startsWith(TITLE)) {
					line = reader.readLine();
					StringBuilder builder = new StringBuilder();
					while (line != null && !(line.startsWith(DOC_ID) || line.startsWith(TITLE) || line.startsWith(ABSTRACT) ||
							line.startsWith(PUBLICATION_DATE) || line.startsWith(AUTHOR) || 
							line.startsWith(CITATION) || line.startsWith(N) || line.startsWith(C) || line.startsWith(K))) {
						builder.append(line + "\n");
						line = reader.readLine();
					}
					Document doc = documents.get(currentID);
					doc.setTitle(builder.toString().trim());
				}	
				else if (line.startsWith(ABSTRACT)) {
					line = reader.readLine();
					StringBuilder builder = new StringBuilder();
					while (line != null && !(line.startsWith(DOC_ID) || line.startsWith(TITLE) || line.startsWith(ABSTRACT) ||
							line.startsWith(PUBLICATION_DATE) || line.startsWith(AUTHOR) || 
							line.startsWith(CITATION) || line.startsWith(N) || line.startsWith(C) || line.startsWith(K))) {
						builder.append(line + "\n");
						line = reader.readLine();
					}
					Document doc = documents.get(currentID);
					doc.setAbstract(builder.toString());
				}	
				else if (line.startsWith(PUBLICATION_DATE)) {
					line = reader.readLine();
					StringBuilder builder = new StringBuilder();
					while (line != null && !(line.startsWith(DOC_ID) || line.startsWith(TITLE) || line.startsWith(ABSTRACT) ||
							line.startsWith(PUBLICATION_DATE) || line.startsWith(AUTHOR) || 
							line.startsWith(CITATION) || line.startsWith(N) || line.startsWith(C) || line.startsWith(K))) {
						builder.append(line + "\n");
						line = reader.readLine();
					}
					Document doc = documents.get(currentID);
					String dateString = builder.toString().substring(5).trim();
					dateString = dateString.replaceAll(" ", "");
					dateString = dateString.replaceAll(",", "");
					try {
						SimpleDateFormat sdf = new SimpleDateFormat("MMMMy");
						Date d = sdf.parse(dateString);
						doc.setPublicationDate(d);
					}
					catch (ParseException e) {
						SimpleDateFormat sdf = new SimpleDateFormat("y");
						Date d = sdf.parse(dateString);
						doc.setPublicationDate(d);
					}
					/*else if (dateString.matches("\\w+,\\d+")) {
						System.out.println("Date format is valid: " + dateString);
						SimpleDateFormat sdf = new SimpleDateFormat("MMMM,y");
						Date d = sdf.parse(dateString);
						doc.setPublicationDate(d);
					}
					else if (dateString.matches("\\w+\\s+\\d+")) {
						System.out.println("Date format is valid: " + dateString);
						SimpleDateFormat sdf = new SimpleDateFormat("MMMM y");
						Date d = sdf.parse(dateString);
						doc.setPublicationDate(d);
					}
					else {
						System.out.println("Date format is invalid: " + dateString);
					}*/
				}
				else if (line.startsWith(AUTHOR)) {
					line = reader.readLine();
					StringBuilder builder = new StringBuilder();
					while (line != null && !(line.startsWith(DOC_ID) || line.startsWith(TITLE) || line.startsWith(ABSTRACT) ||
							line.startsWith(PUBLICATION_DATE) || line.startsWith(AUTHOR) || 
							line.startsWith(CITATION) || line.startsWith(N) || line.startsWith(C) || line.startsWith(K))) {
						builder.append(line + "\n");
						line = reader.readLine();
					}
					Document doc = documents.get(currentID);
					doc.setAuthor(builder.toString().trim());
				}	
				else if (line.startsWith(CITATION)) {
					Document doc = documents.get(currentID);
					Set<Integer> citationSet = new TreeSet<Integer>();
					line = reader.readLine();					
					while (line != null && !(line.startsWith(DOC_ID) || line.startsWith(TITLE) || line.startsWith(ABSTRACT) ||
							line.startsWith(PUBLICATION_DATE) || line.startsWith(AUTHOR) || 
							line.startsWith(CITATION) || line.startsWith(N) || line.startsWith(C) || line.startsWith(K))) {
						int citation = parseCitation(line);
						if (citation != -1) {
							citationSet.add(citation);
						}
						line = reader.readLine();
					}
					doc.setCitationSet(citationSet);
				}
				else {
					line = reader.readLine();
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Could not find file: " + fileName);
		} catch (IOException e) {
			System.out.println("Could not read file: " + fileName);
		}		

		return documents;
	}

	public int parseID(String line) {
		String[] split = line.split(SPLIT);
		String id = split[1];
		return Integer.parseInt(id);
	}

	public int parseCitation(String line) {
		String[] split = line.split(SPLIT);
		if (split[1].equals("5")) {
			return Integer.parseInt(split[0]);
		}
		else {
			return -1;
		}
	}
}

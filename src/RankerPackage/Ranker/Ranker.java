//package RankerPackage.Ranker;
//import IndexerPackages.Indexer.*;
//import DataBasePackages.DataBase.DataBase;
////import QueryProcessingPackages.Query.*;
//import HelpersPackages.Helpers.*;
//import java.lang.reflect.Array;
//import java.util.*;
//import org.json.*;
//import java.io.*;
//import java.lang.Math.*;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import java.io.IOException;
//import java.sql.*;
//import java.sql.ResultSet;
//
//public class Ranker
//{
//    private DataBase connect = new DataBase();
//    JSONArray dividedQuery = new JSONArray();
//    Map<String,Long> wordsCount;
//    String[] completedLinks;
//    Map<String, Double> popularityResult;
//    int completeCount;
//
//    public Ranker()
//    {
//        System.out.printf("From ranker constructor");
//        wordsCount = connect.getWordsCountAsMap();
//        completedLinks = connect.getAllUrls();
//        completeCount=connect.getCompleteCount();
//        popularityResult=calculatePopularity(completeCount);
//    }
//
//
//    public Map<String, Double> calculatePopularity(int totalNodes)             //Popularity
//    {
//
//        //pagesRank1 ==> store the final results of popularity ( each page and its popularity )
//        Map<String, Double> pagesRank1 = new HashMap<String, Double>();
//
//        //TempPageRank is used to store values of pagesRank1 in it temporarily
//        Map<String, Double> TempPageRank = new HashMap<String, Double>();
//
//        //calculate the initial value of popularity for all pages
//        double InitialPageRank = 1.0 / totalNodes;
//
//        // initialize the rank of each page //
//        for (int k = 1; k <= totalNodes; k++)
//            pagesRank1.put(completedLinks[k], InitialPageRank);
//
//        //ITERATION_STEP is used to iterate twice following PageRank Algorithm steps
//        int ITERATION_STEP = 1;
//        while (ITERATION_STEP <= 2) {
//
//            // Store the PageRank for All Nodes in Temporary Map
//            for (int k = 1; k <= totalNodes; k++) {
//                TempPageRank.put(completedLinks[k], pagesRank1.get(completedLinks[k]));
//                pagesRank1.put(completedLinks[k], 0.0);
//            }
//
//            double tempSum = 0;
//            int counter12=0;
//            for (int currentPage = 0; currentPage < totalNodes; currentPage++)
//            {
//                //I will send child link and I must get Number of OutgoingLinks of the parent
//                double OutgoingLinks = connect.getParentLinksNum(completedLinks[currentPage]);         //Get it from From ==> (Reda) to recieve the number of outgoing links from parent link
//
//                //if the link does not have a parent link
//                if ( connect.getParentLink(completedLinks[currentPage]) == "-1" )
//                {
//                    pagesRank1.put(completedLinks[currentPage], -1.0);
//                    counter12++;
//                    continue;
//                }
//                //I will send child link and get parent link ==> it will be changed later
//                double temp = TempPageRank.get(connect.getParentLink(completedLinks[currentPage])) * (1.0 / OutgoingLinks) ;
//                pagesRank1.put(completedLinks[currentPage], temp);
//                tempSum += pagesRank1.get(completedLinks[currentPage]);
//            }
//
//            //Special handling for the first page only as there is no outgoing links to it
//            double temp = 1 - tempSum;
//            double slice = temp / counter12;
//            for ( int i=0 ; i< counter12 ; i++ )
//            {
//                pagesRank1.put(completedLinks[i],slice );
//            }
//
//            ITERATION_STEP++;
//        }
//
//        // Add the Damping Factor to PageRank
//        double DampingFactor = 0.75;
//        double temp = 0;
//        for (int k = 1; k <= totalNodes; k++) {
//            temp = (1 - DampingFactor) + DampingFactor * pagesRank1.get(completedLinks[k]);
//            pagesRank1.put(completedLinks[k], temp);
//        }
//
//        return pagesRank1;
//    }
//
//
//
//    public String calculateRelevance(ArrayList<String> tempLines , HashMap<String, Integer> allLinks , boolean isPhraseSearching) throws FileNotFoundException, JSONException {
//
//        JSONArray finalJsonFile = new JSONArray();   //For final results
//        Map<String, Double> pagesRanks = new HashMap<String, Double>();
//        double tf = 0.0,
//                idf = 0.0,
//                tf_idf = 0.0,
//                numOfOccerrencesInCurrentDocument = 0.0, // this value will be weighted
//                numOfOccerrencesInAllDocuments = 0.0;
//        int counterForWords = 0;
//        //tempMap is used to store each link and its tf-idf value
//        Map<String, Double> allLinksTf_Idf = new HashMap<String, Double>();
//        ArrayList<String> uniqueLinks = new ArrayList<String>();
//
//
//        //
//
//        for (int i = 0; i < tempLines.size(); i++) {
//            Map<String, Double> Links_numOfOccurrences = new HashMap<String, Double>();
//            //to make priority between title,header,paragraph
//            double coeff = 0.0;
//
//            int startIndex = tempLines.get(i).indexOf('|');
//            String lineWithoutTheWord = tempLines.get(i).substring(startIndex + 1);
//            String[] linksWithWordPosition = lineWithoutTheWord.split(";");
//
//            //array to store all links of the current query
//            ArrayList<String> arr = new ArrayList<String>();
//            int counter =0;
//            //iterate over the links of each word in the query
//            for (int j = 0; j < linksWithWordPosition.length; j++) {
//
//
//                //to get id of current page
//                int bracePosition = linksWithWordPosition[j].indexOf('[');
//                String linkOfCurrentPage = linksWithWordPosition[j].substring(bracePosition + 1, linksWithWordPosition[j].indexOf(','));
//
//                if(isPhraseSearching && allLinks.containsKey(linkOfCurrentPage) || !isPhraseSearching)
//                {
//                    //get the length of the page
//                    Long lengthOfPage = wordsCount.get(linkOfCurrentPage);
//
//                    //to get the type of the word ==> paragraph or title or strong or header
//                    int separetorPosition = linksWithWordPosition[j].indexOf(',');
//                    char wordType = linksWithWordPosition[j].charAt(separetorPosition + 1);
//
//                    if (wordType == 't')                                   //title
//                        coeff = 1.0 / 2.0;
//                    else if (wordType == 'h' || wordType == 's')         //header or strong
//                        coeff = 1.0 / 4.0;
//                    else                                                    //paragraph
//                        coeff = 1.0 / 8.0;
//
//                    //to get number of occurrences of each word
//                    int countSeperator = linksWithWordPosition[j].indexOf("]::");
//                    String wordCount = linksWithWordPosition[j].substring(countSeperator + 3);
//                    numOfOccerrencesInCurrentDocument = coeff * Integer.parseInt(wordCount);
//                    numOfOccerrencesInAllDocuments += coeff * Integer.parseInt(wordCount);
//
//                    if (lengthOfPage != null && lengthOfPage != 0) {
//                        tf = Double.valueOf(numOfOccerrencesInCurrentDocument) / lengthOfPage;
//                        if (Links_numOfOccurrences.containsKey(linkOfCurrentPage)) {
//                            double tempTf = Links_numOfOccurrences.get(linkOfCurrentPage).doubleValue();
//                            tf += tempTf;
//                        } else {
//                            arr.add(linkOfCurrentPage);
//                        }
//                        Links_numOfOccurrences.put(linkOfCurrentPage, tf);
//                    }
//                    tf = 0;
//                }
//                //
//            }
//
//            counterForWords++;
//
//            //calculate the idf value of the page
//            idf = completeCount / Double.valueOf(numOfOccerrencesInAllDocuments);                                      // 5100 ==> number of indexed web pages
//
//            // the map will contain the link with its tf_idf
//            for (int h = 0; h < arr.size(); h++) {
//                tf_idf=0;
//                if(allLinksTf_Idf.containsKey(arr.get(h)))
//                {
//                    tf_idf+=allLinksTf_Idf.get(arr.get(h));
//                }
//                else
//                {
//                    uniqueLinks.add(arr.get(h));
//                }
//
//                tf_idf += idf * Links_numOfOccurrences.get(arr.get(h));
//                allLinksTf_Idf.put(arr.get(h), tf_idf);
//            }
//            // to the new word
//            numOfOccerrencesInAllDocuments=0;
//
//        }
//        System.out.println("the map "+ allLinksTf_Idf+"\n \n \n");
//
//
//        for(int i=0;i<uniqueLinks.size();i++)
//        {
//            allLinksTf_Idf.put(uniqueLinks.get(i),(0.7*allLinksTf_Idf.get(uniqueLinks.get(i))+0.3*popularityResult.get(uniqueLinks.get(i))));
//        }
//        // will need to use the popularty here
//        allLinksTf_Idf.entrySet()
//                .stream()
//                .sorted(Map.Entry.comparingByValue())
//                .forEachOrdered(x -> allLinksTf_Idf.put(x.getKey(), x.getValue()));
//        System.out.println("the map "+ allLinksTf_Idf+"\n \n \n");
//
//
//        for (Iterator<Map.Entry<String, Double>> iter = allLinksTf_Idf.entrySet().iterator(); iter.hasNext(); ) {
//
//
//            Map.Entry<String, Double> LinkEntry = iter.next();
//
//
//            String link = LinkEntry.getKey();
//
//            // Get description and populate Json Array
//            StringBuffer description = new StringBuffer("");
//            JSONObject Jo = new JSONObject();
//            connect.getDescription(link, description);
//            Jo.put("Link", link);
//            Jo.put("Description", description);
//            finalJsonFile.put(Jo);
//        }
//
//
//        return finalJsonFile.toString();
//    }
//
//
//
//}
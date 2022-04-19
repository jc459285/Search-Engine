
package ServletsPackages.ServletPackage;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.crypto.Data;

import DataBasePackages.DataBase.DataBase;
import PhraseSearchingPackages.PhraseSearching.*;
import HelpersPackages.Helpers.*;
import QueryProcessingPackages.Query.*;
import com.mysql.cj.xdevapi.JsonArray;
import org.json.JSONException;

import java.io.*;
import java.sql.*;
import java.util.*;

import org.json.*;
import org.tartarus.snowball.ext.PorterStemmer;

public class QuerySearch extends HttpServlet {
    public String searchingQuery;
    public ArrayList<String> rankerArray;
    public JSONArray dividedQuery;
    public QueryDivide SendQuery= new QueryDivide();

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        res.addHeader("Access-Control-Allow-Origin","*");
        String searchingQuery=req.getParameter("query");
        SendQuery.setDivided(searchingQuery);
        JSONArray results=null;
        DataBase dataBaseObj = new DataBase();
        WorkingFiles workingFilesObj = new WorkingFiles(dataBaseObj.getCompleteCount());
        if(searchingQuery.startsWith("\"") && searchingQuery.endsWith("\""))
        {
            //call the function of the phrase searching

            PhraseSearching obj = new PhraseSearching(workingFilesObj);

            try {
                 results  =obj.run(searchingQuery,rankerArray,dividedQuery);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
        {
            //call function of query processing
            QueryProcessing obj = new QueryProcessing(workingFilesObj);
            try {
                 results  =obj.run(searchingQuery,rankerArray,dividedQuery);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
//        //Ranker
        res.setContentType("application/json");
        res.getWriter().write(results.toString());
;
    }


    static class QueryProcessing{

        DataBase dataBaseObject = new DataBase();
        WorkingFiles working;
        private Map<String, File> invertedFiles;
        PorterStemmer stemObject = new PorterStemmer();
        String[] stopWords;


        public QueryProcessing(WorkingFiles files)
        {
            working = files;
            stopWords = files.getStopWordsAsArr();
        }

        private String[] SplitQuery(String searchQuery)
        {
            String[] subStrings = searchQuery.trim().split("\\s+");
            return subStrings;
        }



        //Utility Function for removeStopWords()
        private static String[] removeElement(String[] arr, int[] index) {
            List<String> list = new ArrayList<>(Arrays.asList(arr));
            for (int i=0; i<index.length;i++)
            {
                list.remove(new String(arr[index[i]]));
            }
            return list.toArray(String[]::new);
        }


        private String[] removeStopWords(String[] searchQuery)
        {
            int length =searchQuery.length;
            ArrayList<Integer> indeces = new ArrayList<Integer>();
            for(int i = 0; i< length; i++)
            {
                System.out.println(searchQuery[i].toLowerCase());
                if (Arrays.asList(this.stopWords).contains(searchQuery[i].toLowerCase()))
                {
                    indeces.add(i);
                }
            }
            searchQuery = removeElement(searchQuery, indeces.stream().mapToInt(Integer::intValue).toArray());
            return searchQuery;
        }

        //What remains: Search for word in file and create array for each word in the search query:
        //First element is the actual word if present
        //The rest are the words with same root in that file


        public static void searchInInvertedFiles(String word, File myFile, ArrayList<String> results, boolean stemmingFlag) throws FileNotFoundException {
            Scanner read = new Scanner(myFile);
            String tempInput,
                    stemmedVersion = " ";

            // stemming the word
            if (stemmingFlag)
                stemmedVersion = HelperClass.stemTheWord(word);

            boolean wordIsFound = false;

            int stopIndex, counter;

            results.add(0, "");     // if the targeted word is not found, replace empty in its index
            while(read.hasNextLine())
            {
                tempInput = read.nextLine();
                if (tempInput.equals(""))
                    continue;

                // check if this line is for a word or just an extension for the previous line
                if (tempInput.charAt(0) == '/')
                // compare to check if this tempWord = ourWord ?
                {
                    // extract the word from the line that read by the scanner
                    stopIndex = tempInput.indexOf('|');
                    String theWord = tempInput.substring(1, stopIndex);

                    // this condition for the targeted word
                    if(!wordIsFound && theWord.equals(word))
                    {
                        results.set(0, tempInput);     // target word will have the highest priority
                        wordIsFound = true;
                        continue;
                    }

                    counter = 1;
                    // comparing the stemmed version of the target word by the stemmed version of the word in the inverted file
                    if (stemmingFlag)
                    {
                        if (stemmedVersion.equals(HelperClass.stemTheWord(theWord)))
                            results.add(counter++, tempInput);
                    }
                }
            }
        }

        public static HashMap<Integer, Double> sortByValue(HashMap<Integer, Double> hm)
        {
            // Create a list from elements of HashMap
            List<Map.Entry<Integer, Double> > list =
                    new LinkedList<Map.Entry<Integer, Double> >(hm.entrySet());

            // Sort the list
            Collections.sort(list, new Comparator<Map.Entry<Integer, Double> >() {
                public int compare(Map.Entry<Integer, Double> o1,
                                   Map.Entry<Integer, Double> o2)
                {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            // put data from sorted list to hashmap
            HashMap<Integer, Double> temp = new LinkedHashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> aa : list) {
                temp.put(aa.getKey(), aa.getValue());
            }
            return temp;
        }

        public static HashMap<String, Double> replaceIDByLink(HashMap<Integer, Double> hm)
        {
            StringBuffer link = new StringBuffer("");
            DataBase dataBaseObject = new DataBase();
            StringBuffer description = new StringBuffer("");
            HashMap<String, Double> temp = new HashMap<String, Double>();
            for (Iterator<Map.Entry<Integer, Double>> it = hm.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry<Integer, Double> IDEntry = it.next();
                dataBaseObject.getLinkByID(IDEntry.getKey(), link, description);
                temp.put(link.toString(), IDEntry.getValue());
            }

            return temp;
        }

        public JSONArray run(String message, ArrayList<String> queryLinesResult, JSONArray dividedQuery)
                throws FileNotFoundException, JSONException {
            invertedFiles = working.getInvertedFiles();
            boolean[] indexProcessed;
            Map<Integer, Integer> allIDs = new HashMap<Integer, Integer>();
            ArrayList<String> words = new ArrayList<String>();
            words.add(message);
            JSONObject divide = new JSONObject();
            ArrayList<String> allWordsResult = new ArrayList<String>();


            String[] result = SplitQuery(message);
            result  = removeStopWords(result);
            indexProcessed = new boolean[result.length];
            String json = "{ [";
            StringBuffer jsonFile = new StringBuffer(json);
            JSONArray finalJsonFile = new JSONArray();
            int length = result.length;
            for(int i=0; i<length;i++)
            {

                // Loop over words
                words.add(result[i]);
                ArrayList<String> oneWordResult = new ArrayList<String>();

                String fileName = "";
                if (HelperClass.isProbablyArabic(result[i]))
                    fileName = "arabic";
                else if(result[i].length() == 2)
                    fileName = "two";

                else
                    fileName = "_" + result[i].substring(0,3);

                searchInInvertedFiles(result[i], invertedFiles.get(fileName),oneWordResult, true);

                int length_2 = oneWordResult.size();
                for(int j = 0; j<length_2; j++)
                {
                    queryLinesResult.add(oneWordResult.get(j));
                    // Loop over versions of Words


                    String[] splitLine= oneWordResult.get(j).split("\\[");
                    int length_3 = splitLine.length;
                    for (int k=1; k<length_3; k+=2)
                    {

                        // Loop over links of the same version of each Word

                        int End = splitLine[k].indexOf(']');
                        String temp = splitLine[k].substring(0, End);

                        String[] finalID = temp.split(",");
                        int ID = Integer.parseInt(finalID[0]);

                        if (i == 0 && !indexProcessed[i]) {
                            allIDs.put(ID, 1);
                            indexProcessed[0] = true;
                        }
                        else if (!indexProcessed[i] && allIDs.containsKey(ID)) {
                            allIDs.put(ID, 1 + allIDs.get(ID));
                            indexProcessed[i] = true;
                        }

                    }
                }

            }
            for (Iterator<Map.Entry<Integer, Integer>> it = allIDs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, Integer> entry = it.next();
                if (entry.getValue() < length) {
                    it.remove();
                }

                for (Iterator<Map.Entry<Integer, Integer>> iter = allIDs.entrySet().iterator(); it.hasNext(); ) {

                    Map.Entry<Integer, Integer> IDEntry = iter.next();

                    StringBuffer link = new StringBuffer("");
                    StringBuffer description = new StringBuffer("");
                    JSONObject Jo = new JSONObject();
                    dataBaseObject.getLinkByID(IDEntry.getKey(), link, description);
                    Jo.put("Link", link);
                    Jo.put("Description", description);
                    finalJsonFile.put(Jo);
                }

            }

            divide.put("Result", words);
            dividedQuery.put(divide);
            return finalJsonFile;

        }
    }


    static class PhraseSearching {
        DataBase dataBaseObject = new DataBase();
        WorkingFiles working;
        private Map<String, File> invertedFiles;
        PorterStemmer stemObject = new PorterStemmer();
        String[] stopWords;


        public PhraseSearching(WorkingFiles files) {
            working = files;
            stopWords = files.getStopWordsAsArr();
        }


        private String[] SplitQuery(String searchQuery) {
            String[] subStrings = searchQuery.trim().split("\\s+");
            return subStrings;
        }

        private static String[] removeElement(String[] arr, int[] index) {
            List<String> list = new ArrayList<>(Arrays.asList(arr));
            for (int i = 0; i < index.length; i++) {
                list.remove(new String(arr[index[i]]));
            }
            return list.toArray(String[]::new);
        }


        private String[] removeStopWords(String[] searchQuery) {
            int length = searchQuery.length;
            ArrayList<Integer> indeces = new ArrayList<Integer>();
            for (int i = 0; i < length; i++) {
                System.out.println(searchQuery[i].toLowerCase());
                if (Arrays.asList(this.stopWords).contains(searchQuery[i].toLowerCase())) {
                    indeces.add(i);
                }
            }
            searchQuery = removeElement(searchQuery, indeces.stream().mapToInt(Integer::intValue).toArray());
            return searchQuery;
        }


        public JSONArray run(String message, ArrayList<String> queryLinesResult, JSONArray dividedQuery) throws FileNotFoundException, JSONException {
            invertedFiles = working.getInvertedFiles();
            boolean[] indexProcessed;
            Map<Integer, Integer> allIDs = new HashMap<Integer, Integer>();
            JSONObject divide = new JSONObject();
            divide.put("Results", message);
            dividedQuery.put(divide);


            ArrayList<String> allWordsResult = new ArrayList<String>();


            String[] result = SplitQuery(message);
            result = removeStopWords(result);
            indexProcessed = new boolean[result.length];
            String json = "{ [";
            StringBuffer jsonFile = new StringBuffer(json);
            JSONArray finalJsonFile = new JSONArray();
            int length = result.length;
            for (int i = 0; i < length; i++) {
                // Loop over words
                ArrayList<String> oneWordResult = new ArrayList<String>();


                String fileName = "";
                if (HelperClass.isProbablyArabic(result[i]))
                    fileName = "arabic";
                else if(result[i].length() == 2)
                    fileName = "two";

                else
                    fileName = "_" + result[i].substring(0,3);

                QueryProcessingPackages.Query.QueryProcessing.searchInInvertedFiles(result[i], invertedFiles.get(fileName),
                        oneWordResult, false);

                int length_2 = oneWordResult.size();
                for (int j = 0; j < length_2; j++) {
                    queryLinesResult.add(oneWordResult.get(j));
                    // Loop over versions of Words


                    String[] splitLine = oneWordResult.get(j).split("\\[");
                    int length_3 = splitLine.length;
                    for (int k = 1; k < length_3; k += 2) {

                        // Loop over links of the same version of each Word

                        int End = splitLine[k].indexOf(']');
                        String temp = splitLine[k].substring(0, End);

                        String[] finalID = temp.split(",");
                        int ID = Integer.parseInt(finalID[0]);
                        if (i == 0 && !indexProcessed[i]) {
                            allIDs.put(ID, 1);
                            indexProcessed[0] = true;
                        }
                        else if (!indexProcessed[i] && allIDs.containsKey(ID)) {
                            allIDs.put(ID, 1 + allIDs.get(ID));
                            indexProcessed[i] = true;
                        }
                    }
                }

            }

            for (Iterator<Map.Entry<Integer, Integer>> it = allIDs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, Integer> entry = it.next();
                if (entry.getValue() < length) {
                    it.remove();
                }

                for (Iterator<Map.Entry<Integer, Integer>> iter = allIDs.entrySet().iterator(); it.hasNext(); ) {

                    Map.Entry<Integer, Integer> IDEntry = iter.next();

                    StringBuffer link = new StringBuffer("");
                    StringBuffer description = new StringBuffer("");
                    JSONObject Jo = new JSONObject();
                    dataBaseObject.getLinkByID(IDEntry.getKey(), link, description);
                    Jo.put("Link", link);
                    Jo.put("Description", description);
                    finalJsonFile.put(Jo);
                }



            }
            return finalJsonFile;
        }
    }

    static class DataBase {
        private Connection connect;
        private Statement stmt;

        public DataBase() {
            try {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (Exception e) {

                }
                connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/search-engine", "root", "");
                this.stmt = connect.createStatement();
                if (connect != null) {
                    System.out.println("Connected to database");
                } else {
                    System.out.println("Cannot connect to database");
                }

            } catch (SQLException e) {

            }
        }

        //--------------------------------------Create Link --------------------------------------------------------------------//
        public synchronized void createLink(String Link, int Layer, String ThreadName, int ParentId) {
            try {
                this.stmt.executeUpdate("INSERT INTO links (Link, Layer, ThreadName, LinkParent,Completed) VALUES ('" + Link + "', '" + Layer + "', '" + ThreadName + "', " + ParentId + ",'" + 0 + "');");
            } catch (SQLException e) {
            }
        }
//----------------------------------------------------------------------------------------------------------------------//

// --------------------------------------Update Link to Complete -------------------------------------------------------//

        public synchronized void urlCompleted(String Link) {
            try {
                this.stmt.executeUpdate("UPDATE links SET Completed=1 WHERE link='" + Link + "'");
            } catch (SQLException e) {
            }
        }
//----------------------------------------------------------------------------------------------------------------------//

        // --------------------------------------Update Link to Complete -------------------------------------------------------//

// --------------------------------------Set and Get Thread Position -------------------------------------------------------//

        public synchronized void setThreadPosition(String ThreadName, int Layer, int Index) {
            try {
                if (Layer == 1) {
                    this.stmt.executeUpdate("UPDATE threads SET Layer=" + Layer + " WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET UrlIndex=" + Index + " WHERE ThreadName='" + ThreadName + "';");

                } else if (Layer == 2) {

                    this.stmt.executeUpdate("UPDATE threads SET Layer=" + Layer + " WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET UrlIndex1=" + Index + " WHERE ThreadName='" + ThreadName + "';");

                } else if (Layer == 3) {


                    this.stmt.executeUpdate("UPDATE threads SET Layer=" + Layer + " WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET UrlIndex2=" + Index + " WHERE ThreadName='" + ThreadName + "';");
                } else if (Layer == 4) {

                    this.stmt.executeUpdate("UPDATE threads SET Layer=" + Layer + " WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET UrlIndex3=" + Index + " WHERE ThreadName='" + ThreadName + "';");
                } else {
                    this.stmt.executeUpdate("UPDATE threads SET Layer=1 WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET  UrlIndex=0 WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET  UrlIndex1=0  WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET  UrlIndex2=0 WHERE ThreadName='" + ThreadName + "';");
                    this.stmt.executeUpdate("UPDATE threads SET   UrlIndex3=0 WHERE ThreadName='" + ThreadName + "';");


                }
            } catch (SQLException e) {
            }
        }

        public synchronized ResultSet getThreadPosition(String ThreadName) {
            try {
                ResultSet resultSet = this.stmt.executeQuery("SELECT * FROM threads WHERE ThreadName='" + ThreadName + "'");
                return resultSet;
            } catch (SQLException e) {
                return null;
            }
        }
//----------------------------------------------------------------------------------------------------------------------//

        public synchronized ResultSet getUrls(String Url) {
            try {
                return this.stmt.executeQuery("SELECT * FROM links WHERE Link='" + Url + "' AND Completed = 1");
            } catch (SQLException e) {
                return null;
            }
        }

        //---------------------------------------------get the url similar to the url-------------------------------------------//
        public synchronized ResultSet getUrls2(String Url) {
            try {
                return this.stmt.executeQuery("SELECT * FROM links WHERE Link='" + Url + "';");
            } catch (SQLException e) {
                return null;
            }
        }
// ---------------------------------------------------------------------------------------------------------------------//


        //---------------------------------------get link by ID  -------------------------------------------------------------//
        public synchronized Boolean getLinkByID(Integer ID, StringBuffer linkUrl, StringBuffer description) {
            try {
                //String query = "Select Link FROM links WHERE Id= " + ID +" ";
                String query = "Select * FROM links";
                ResultSet resultSet = this.stmt.executeQuery("Select Link, Descripation FROM links WHERE Id= " + ID + ";");
                resultSet.next();
                String linkResult = resultSet.getString("Link");
                linkUrl.append(linkResult);
                String descriptionResult = resultSet.getString("Descripation");
                description.append(descriptionResult);
                return true;

            } catch (SQLException e) {
                return false;
            }

        }


// ---------------------------------------------------------------------------------------------------------------------//


// --------------------------------------get the id of the link  -------------------------------------------------------//

        public synchronized int getId(String Url, String ThreadName) {
            try {
                ResultSet resultSet = this.stmt.executeQuery("SELECT * FROM links WHERE Link='" + Url + "' AND ThreadName='" + ThreadName + "' AND Completed=0 ;");
                while (resultSet.next()) {
                    int Id = -1;
                    Id = resultSet.getInt("Id");
                    return Id;
                }
            } catch (SQLException e) {

            }
            return -1;
        }
//----------------------------------------------------------------------------------------------------------------------//

        //-----------------------------------------get the family of the link --------------------------------------------------//
        public synchronized ResultSet getParentUrl(String ThreadName, StringBuffer parentLink, StringBuffer grandLink, String link, int Layer) {
            try {
                if (Layer == 1) {
                    ResultSet resultSet = this.stmt.executeQuery("SELECT * FROM links WHERE  ThreadName='" + ThreadName + "' AND Layer=" + Layer + ";");
                    while (resultSet.next()) {
                        grandLink.append(resultSet.getString("Link"));
                    }
                    return this.stmt.executeQuery("SELECT * FROM links WHERE  ThreadName='" + ThreadName + "' AND Layer=" + Layer + ";");
                } else if (Layer == 2) {
                    ResultSet resultSet = this.stmt.executeQuery("SELECT * FROM links WHERE  ThreadName='" + ThreadName + "' AND Layer=" + Layer + " AND Completed=0;");
                    while (resultSet.next()) {
                        resultSet = this.stmt.executeQuery("SELECT  k.Link  , k.LinkParent , k.Layer FROM links as e , links as k WHERE e.Layer= " + Layer + " AND e.ThreadName='" + ThreadName + "' AND k.Id=e.LinkParent;");
                        while (resultSet.next()) {
                            parentLink.append(resultSet.getString("Link"));
                            return this.stmt.executeQuery("SELECT  k.Link  , k.LinkParent , k.Layer FROM links as e , links as k WHERE e.Layer= " + Layer + " AND e.ThreadName='" + ThreadName + "' AND k.Id=e.LinkParent;");
                        }

                    }
                } else if (Layer == 3) {
                    ResultSet resultSet = this.stmt.executeQuery("SELECT * FROM links WHERE  ThreadName='" + ThreadName + "' AND Layer=" + Layer + " AND Completed=0;");
                    while (resultSet.next()) {
                        resultSet = this.stmt.executeQuery("SELECT  k.Link  , k.LinkParent , k.Layer FROM links as e , links as k WHERE e.Layer= " + Layer + " AND e.ThreadName='" + ThreadName + "' AND k.Id=e.LinkParent;");
                        while (resultSet.next()) {
                            parentLink.append(resultSet.getString("Link"));
                            Layer = resultSet.getInt("Layer");
                            resultSet = this.stmt.executeQuery("SELECT  k.Link  , k.LinkParent , k.Layer FROM links as e , links as k WHERE e.Layer= " + Layer + " AND e.ThreadName='" + ThreadName + "' AND k.Id=e.LinkParent;");
                            while (resultSet.next()) {
                                grandLink.append(resultSet.getString("Link"));
                                return this.stmt.executeQuery("SELECT  k.Link  , k.LinkParent , k.Layer FROM links as e , links as k WHERE e.Layer= " + Layer + " AND e.ThreadName='" + ThreadName + "' AND k.Id=e.LinkParent;");
                            }

                        }


                    }
                }
            } catch (SQLException e) {
                return null;

            }
            return null;
        }
//----------------------------------------------------------------------------------------------------------------------//


        //------------------------------------------get the completed urls------------------------------------------------------//
        public synchronized int getCompleteCount() {
            try {
                ResultSet result = this.stmt.executeQuery("SELECT count(Link) as Number FROM links WHERE  Completed=1 ;");
                int count = 0;
                while (result.next()) {
                    count = result.getInt("Number");
                }
                return count;
            } catch (SQLException e) {
            }
            return 0;
        }
//----------------------------------------------------------------------------------------------------------------------//

        public java.sql.Date getMaxDate() {
            try {
                ResultSet result = this.stmt.executeQuery("SELECT max(LastTime) as Time FROM links;");
                java.sql.Date count = null;
                while (result.next()) {
                    count = result.getDate("columnName");
                }
                return count;
            } catch (SQLException e) {
            }
            return null;
        }

        //---------------------------------------------get url and its related ID-------------------------------------------//
        public ResultSet getAllUrls() {
            try {
                return this.stmt.executeQuery("SELECT Link, Id FROM links where Completed=1;");
            } catch (SQLException e) {
                return null;
            }
        }

        // ---------------------------------------------------------------------------------------------------------------------//
        //-----------------------------------------------get the number of links out from the parent link-----------------------//
        public int getParentLinksNum(int childId) {

            try {
                ResultSet resultSet = this.stmt.executeQuery("SELECT LinkParent FROM links  where Id=" + childId + " ;");
                while (resultSet.next()) {
                    int parentId = resultSet.getInt("LinkParent");
                    return this.stmt.executeQuery("SELECT count(Id) as Number FROM links  where LinkParent=" + parentId + " ;").getInt("Number");
                }
            } catch (SQLException e) {
                return -1;
            }
            return -1;
        }

        // ---------------------------------------------------------------------------------------------------------------------//
        //-----------------------------------------------Add Link descripation--------------------------------------------------//
        public void addDesc(int id, String desc) {
            try {
                this.stmt.executeUpdate("UPDATE links SET Descripation='" + desc + "' WHERE Id=" + id + ";");
            } catch (SQLException e) {

            }
        }
    }

    //-------------------------------------------------------------------------------------------------///////////

    static class WorkingFiles {
        private Map<String, File> invertedFiles;
        private String[] stopWords;
        private Map<String, File> pageContentFiles;
        public WorkingFiles(int countOfPageContentFiles)
        {
            // create the files of pages content
            String path = "";
            for (int i = 1; i <= countOfPageContentFiles; i++)
            {
                path = HelperClass.pageContentFilesPath(String.valueOf(i));
                File myObj = new File(path);
                try {
                    myObj.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to create the file");
                }
            }
            // page content files
            createPagesContentFiles(countOfPageContentFiles);
            // inverted files
            initializeFiles();

            // stop words
            try {
                readStopWords();
            } catch (FileNotFoundException e) {
                System.out.println("Failed to open Stop words file");
                e.printStackTrace();
            }


        }

        // initialization of inverted files
        private void initializeFiles()
        {
            invertedFiles = new HashMap<String, File>();
            String letters = "qwertyuiopasdfghjklzxcvbnm";
            String currentFileName = "";

            for (int i = 0; i < 26; i++){
                for (int j = 0; j < 26; j++)
                {
                    for(int k = 0; k < 26; k++)
                    {
                        currentFileName = "_";
                        currentFileName += letters.charAt(i);
                        currentFileName += letters.charAt(j);
                        currentFileName += letters.charAt(k);

                        String path = HelperClass.invertedFilePath_V3(currentFileName);
                        File myObj = new File(path);
                        try {
                            myObj.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Failed to create the file");
                        }

                        invertedFiles.put(currentFileName, new File(HelperClass.invertedFilePath_V3(currentFileName)));
                        currentFileName = "";
                    }

                }
            }

            // create a file for two-letter words
            currentFileName = "two";
            String path = HelperClass.invertedFilePath_V3(currentFileName);
            File myObj = new File(path);
            try {
                myObj.createNewFile();
                invertedFiles.put(currentFileName, new File(HelperClass.invertedFilePath_V3(currentFileName)));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to create the file");
            }

            // create a file for Arabic words
            currentFileName = "arabic";
            path = HelperClass.invertedFilePath_V3(currentFileName);
            File myObj_2 = new File(path);
            try {
                myObj_2.createNewFile();
                invertedFiles.put(currentFileName, new File(HelperClass.invertedFilePath_V3(currentFileName)));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to create the file");
            }

            // print
            System.out.println("Content Files Created Successfully");
        }

        // initialization of page content files
        private void createPagesContentFiles(int count)
        {
            // initialization map of the files
            pageContentFiles = new HashMap<String,File>();

            for (int i = 1; i <= count; i++)
            {
                pageContentFiles.put(String.valueOf(i), new File(HelperClass.pageContentFilesPath(String.valueOf(i))));
            }
        }

        // read the stop words
        private void readStopWords() throws FileNotFoundException {
            // open the file that contains stop words
            String filePath = System.getProperty("user.dir");   // get the directory of the project
            filePath += File.separator + "helpers" + File.separator + "stop_words.txt";
            File myFile = new File(filePath);

            stopWords = new String[851];

            // read from the file
            Scanner read = new Scanner(myFile);
            String tempInput;
            int counter = 0;
            while(read.hasNextLine())
            {
                tempInput = read.nextLine();
                stopWords[counter++] = tempInput;
            }
            read.close();

        }

        // get stop words
        public String[] getStopWordsAsArr()
        {
            return stopWords;
        }

        // get stop words
        public Map<Character, Vector<String>> getStopWordsAsMap()
        {
            // hold stop words in arr
            String[] myStopWords = this.getStopWordsAsArr();

            // creating Map
            Map<Character, Vector<String>> wordsMap = new HashMap<>();
            String letters = "qwertyuiopasdfghjklzxcvbnm'";
            // initialize map
            for (int i = 0; i < 27; i++){

                wordsMap.put(letters.charAt(i), new Vector<String>());
            }

            // fill the map
            int x = 0;
            for (String word : myStopWords)
            {
                if (wordsMap.get(word.charAt(0)) != null)
                    wordsMap.get(word.charAt(0)).add(word);
            }

            return wordsMap;
        }

        // get inverted files
        public Map<String, File> getInvertedFiles()
        {
            return invertedFiles;
        }

        // add to page content file
        public void addToPageContentFile(String fileName, String content)
        {
            FileWriter myWriter = null;

            try {
                myWriter = new FileWriter(HelperClass.pageContentFilesPath(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                myWriter.write(content);
                System.out.println("Successfully added the content to the file " + fileName +".txt");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to add the content to the file " + fileName +".txt");
            }

            try {
                myWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


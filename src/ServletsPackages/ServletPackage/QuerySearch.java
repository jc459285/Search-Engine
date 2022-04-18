package ServletsPackages.ServletPackage;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import PhraseSearchingPackages.PhraseSearching.*;
import QueryProcessingPackages.Query.*;
import com.mysql.cj.xdevapi.JsonArray;
import org.json.JSONException;

import java.io.*;
import java.util.ArrayList;
import org.json.*;

public class QuerySearch extends HttpServlet {
    public String searchingQuery;
    public ArrayList<String> rankerArray;
    public JSONArray dividedQuery;
    public QueryDivide SendQuery= new QueryDivide();
    public void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException
    {
        res.addHeader("Access-Control-Allow-Origin","http://localhost:3000");
        searchingQuery=req.getParameter("query");
        JSONArray results=null;
        if(searchingQuery.startsWith("\"") && searchingQuery.endsWith("\""))
        {
            //call the function of the phrase searching
            PhraseSearching obj=new PhraseSearching();

            try {
                 results  =obj.run(searchingQuery,rankerArray,dividedQuery);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else
        {
            //call function of query processing
            QueryProcessing obj = new QueryProcessing();
            try {
                 results  =obj.run(searchingQuery,rankerArray,dividedQuery);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //Ranker
        res.setContentType("application/json");
        res.getWriter().write(results.toString());

    }
}

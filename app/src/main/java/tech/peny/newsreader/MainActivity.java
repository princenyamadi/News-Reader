package tech.peny.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {


    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDB;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//      DB config
        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)" );


//        Config listView
        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),article_activity.class);

                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });

//        Config
        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }
        updateListView();

    }

//    update list view
    public void updateListView(){
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex = c.getColumnIndex("content");

        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
            titles.clear();
            content.clear();

            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }

//    Async

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpsURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while(data != -1){
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }



                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems = 20;

                Log.i(" @@@@@@@@@@ ","OUTSIDE IF");
                if(jsonArray.length() < 20){
                    numberOfItems = jsonArray.length();
                    Log.i(" @@@@@@ ","INSIDE IF");
                }
//                articlesDB.execSQL("DELETE FROM articles");

                    for(int i = 0; i< numberOfItems; i++){
                        String articleId = jsonArray.getString(i);
                        url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");


                        urlConnection = (HttpsURLConnection) url.openConnection();

                         inputStream = urlConnection.getInputStream();
                         inputStreamReader = new InputStreamReader(inputStream);

                         data = inputStreamReader.read();
                         String articleInfo = "";
                        while(data != -1){
                            char current = (char) data;
                            articleInfo += current;
                            data = inputStreamReader.read();
                        }
                        Log.i("ARTICLE INFO",articleInfo);
                        JSONObject jsonObject = new JSONObject(articleInfo);

                        if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                            String articleTitle = jsonObject.getString("title");
                            String articleUrl = jsonObject.getString("url");

                            Log.i("TITLE & URL",articleTitle+articleUrl);
                            url = new URL(articleUrl);
                            urlConnection = (HttpsURLConnection) url.openConnection();

                            inputStream = urlConnection.getInputStream();
                            inputStreamReader = new InputStreamReader(inputStream);

                            data = inputStreamReader.read();

                            String articleContent = "";

                            while(data != -1){
                                char current = (char) data;
                                articleContent += current;
                                data = inputStreamReader.read();
                            }

                            Log.i("HTML", articleContent);

                            String sql = "INSERT INTO articles (articleId, title, content) VALUES (?,?,?)";
                            SQLiteStatement statement = articlesDB.compileStatement(sql);
                            statement.bindString(1,articleId);
                            statement.bindString(2,articleTitle);
                            statement.bindString(3, articleContent);

                            statement.execute();

                        }
                    }



                Log.i("URL CONTENT", result);

                return result;

            }catch (Exception e){

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}
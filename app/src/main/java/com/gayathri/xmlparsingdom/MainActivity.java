package com.gayathri.xmlparsingdom;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity implements ResultsCallback{

    PlaceHolderFragment taskFragment;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) findViewById(R.id.listview);

        if (savedInstanceState == null) {
            taskFragment = new PlaceHolderFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(taskFragment, "MyFragment").commit();
        } else {
            taskFragment = (PlaceHolderFragment) getSupportFragmentManager()
                    .findFragmentByTag("MyFragment");
        }
        taskFragment.startTask();
    }

    public static class PlaceHolderFragment extends Fragment {

        TechCrunchTask downloadTask;
        ResultsCallback callback;

        public PlaceHolderFragment() {

        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            callback = (ResultsCallback) activity;
            if(downloadTask!=null){
                downloadTask.onAttach(callback);
            }
        }

        public void startTask() {
            if (downloadTask != null) {
                downloadTask.cancel(true);
            } else {
                downloadTask = new TechCrunchTask(callback);
                downloadTask.execute();
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            callback = null;
            if(downloadTask!=null){
                downloadTask.onDetach();
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setRetainInstance(true);
        }
    }

    public static class TechCrunchTask extends AsyncTask<Void, Void, ArrayList<HashMap<String ,String>>> {

        ResultsCallback callback = null;

        public TechCrunchTask(ResultsCallback callback) {
            this.callback = callback;
        }

        public void onAttach(ResultsCallback callback){
            this.callback = callback;
        }

        public void onDetach(){
            callback = null;
        }

        @Override
        protected void onPreExecute() {
            if(callback!=null){
                callback.onPreExcute();
            }
        }

        @Override
        protected ArrayList<HashMap<String,String>> doInBackground(Void... voids) {
            String downloadURL = "http://feeds.feedburner.com/Techcrunch/android?format=xml";
            ArrayList<HashMap<String, String >> results = new ArrayList<>();
            try {
                URL url = new URL(downloadURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                results = processXML(inputStream);
            } catch (Exception e) {
                Log.d("", String.valueOf(e));
            }
            return results;
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> results) {
            //Log.d("Logcat", String.valueOf(results));
            if(callback!=null){
                callback.onPostExcute(results);
            }
        }

        public ArrayList<HashMap<String,String>> processXML(InputStream inputStream) throws Exception {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xmldocument = documentBuilder.parse(inputStream);
            Element rootElement = xmldocument.getDocumentElement();//root element
            //Log.d("Log", rootElement.getTagName());
            NodeList itemList = rootElement.getElementsByTagName("item");
            NodeList itemChildren = null;
            Node currentItem = null;
            Node currentChild = null;
            HashMap<String, String> currentMap = null;
            ArrayList<HashMap<String,String>> results = new ArrayList<>();
            for (int i = 0; i < itemList.getLength(); i++) {
                currentItem = itemList.item(i);
                itemChildren = currentItem.getChildNodes();

                currentMap = new HashMap<>();
                for (int j = 0; j < itemChildren.getLength(); j++) {
                    currentChild = itemChildren.item(j);
                    //Log.d("Logcat", currentChild.getNodeName());
                    if (currentChild.getNodeName().equalsIgnoreCase("title")) {
                        currentMap.put("title", currentChild.getTextContent());
                        // Log.d("Log", currentChild.getTextContent());
                    } else if (currentChild.getNodeName().equalsIgnoreCase("pubDate")) {
                        currentMap.put("pubDate", currentChild.getTextContent());
                        //  Log.d("Log", currentChild.getTextContent());
                    } else if (currentChild.getNodeName().equalsIgnoreCase("description")) {
                        currentMap.put("description", currentChild.getTextContent());
                        //Log.d("Log", currentChild.getTextContent());
                    }
                }
                if(currentMap != null && !currentMap.isEmpty()){
                    results.add(currentMap);
                }
            }
           // Log.d("Log", String.valueOf(currentMap));
            return results;
        }
    }

    @Override
    public void onPreExcute() {

    }

    @Override
    public void onPostExcute(ArrayList<HashMap<String, String>> results) {
        //Log.d("Logcat", String.valueOf(results));
        listView.setAdapter(new myAdapter(this, results));
    }
}

interface ResultsCallback{
   public void  onPreExcute();
    public void  onPostExcute(ArrayList<HashMap<String , String >> results);

}

class myAdapter extends BaseAdapter{

    ArrayList<HashMap<String ,String>> datasourse = new ArrayList<>();
    Context context;
    LayoutInflater inflater;

    public myAdapter(Context context, ArrayList<HashMap<String, String>> datasourse) {
        this.datasourse = datasourse;
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return datasourse.size();
    }

    @Override
    public Object getItem(int position) {
        return datasourse.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        MyHolder holder = null;
        if(view == null){
            view = inflater.inflate(R.layout.custom_row,parent,false);
            holder = new MyHolder(view);
            view.setTag(holder);
        }else{
            holder = (MyHolder) view.getTag();
        }
        HashMap <String ,String> currentmap = datasourse.get(position);
        holder.tvtitle.setText(currentmap.get("title"));
        holder.tvpubDate.setText(currentmap.get("pubDate"));
        holder.tvdescription.setText(currentmap.get("description"));
        return view;
    }
}

class MyHolder{
    TextView tvtitle, tvpubDate, tvdescription;

    public MyHolder(View view) {
        tvtitle = (TextView) view.findViewById(R.id.tvtitle);
        tvpubDate = (TextView) view.findViewById(R.id.tvpubdate);
        tvdescription = (TextView)  view.findViewById(R.id.tvdescription);
    }
}

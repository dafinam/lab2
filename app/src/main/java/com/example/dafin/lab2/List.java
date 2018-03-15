package com.example.dafin.lab2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class List extends AppCompatActivity
{
    // Activity items
    Button temp;
    ListView list;
    URL url;

    // Default values
    String urlString;
    int updateTime;
    int amoutItem;

    //Preferances
    SharedPreferences sharedpreferences;
    private static final String MyPREFERENCES = "MyPrefs" ;

    // Lists
    ArrayList<String> titles;
    ArrayList<String> links;

    // Handler
    private Handler fetch = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        // Get values from Preferences
        getPrefValues();

        // Add buttons actions
        addButtonsActions();

        // Create Intent with default values
        Intent receive = getIntent();
        urlString =receive.getStringExtra("url");
        Toast.makeText(getApplicationContext(),urlString,Toast.LENGTH_LONG).show();
        amoutItem = receive.getIntExtra("itemN",10);
        updateTime = receive.getIntExtra("rate",10000);

        // if string is empty fill it
        if(urlString==null)
        {
            urlString = load();
        }

        // List function
        list = (ListView) findViewById(R.id.list);
        titles = new ArrayList<String>();
        links  = new ArrayList<String>();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String link = links.get(position);
                Intent intent = new Intent(getApplicationContext(),ChosenContent.class);
                intent.putExtra("url",link);
                startActivity(intent);
            }
        });

        // Run
        Feed.run();
    }

    /**
     * Function to make sure that there is always an URL to read from
     *
     * @return either url from preferences or default one
     */
    private String load()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String saved = prefs.getString("url","https://www.vg.no/rss/feed/");
        return saved;
    }

    /**
     * Function to add butons function
     */
    private void addButtonsActions()
    {
        // Go to preferences activity
        temp = (Button)findViewById(R.id.listPrefBtn);
        temp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent senderIntent = new Intent(List.this, UserPref.class);
                startActivity(senderIntent); // Start Activity 3
            }
        });

        // Update the links
        temp = (Button)findViewById(R.id.listFetchBtn);
        temp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                forceFetch();
            }
        });
    }

    /**
     * on Back button pressed finish the task
     */
    @Override
    public void onBackPressed()
    {
        finish();
        moveTaskToBack(true);
    }

    /**
     * Loads the preferences from file
     */
    private void getPrefValues()
    {
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        //String urlString = sharedpreferences.getString("url", "http://rss.cnn.com/rss/edition_football.rss");
        String urlString = sharedpreferences.getString("url", "https://www.vg.no/rss/feed/");

        int type = sharedpreferences.getInt("type",0 );
        int updateTime = sharedpreferences.getInt("updateTime", 0);
        int amoutItem = sharedpreferences.getInt("amount", 0);

        switch (amoutItem)
        {
            case (1): amoutItem = 20; break;
            case (2): amoutItem = 50; break;
            case (3): amoutItem = 100; break;
            default: amoutItem = 10; break;
        }

        switch (updateTime)
        {
            case (1): updateTime = 60000; break;
            case (2): updateTime = 10000000; break;
            default: updateTime = 10000; break;
        }
    }


    private Runnable Feed = new Runnable()
    {
        @Override
        public void run()
        {
            new ProcessInBackground().execute();
            fetch.postDelayed(this,updateTime);
        }
    };

    public InputStream getInputStream(URL url)
    {
        try
        {
            return url.openConnection().getInputStream();
        } catch (IOException e)
        {
            return null;
        }
    }

    public class ProcessInBackground extends AsyncTask<Integer, Integer, Exception>
    {
        ProgressDialog progressDialog = new ProgressDialog(List.this);
        Exception exc = null;
        @Override

        protected void onPreExecute()
        {
            super.onPreExecute();
            progressDialog.setMessage("Loading...");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Exception s)
        {
            super.onPostExecute(s);

            ArrayAdapter<String> adapter= new ArrayAdapter<String>(List.this, android.R.layout.simple_list_item_1, titles);
            list.setAdapter(adapter);

            progressDialog.dismiss();
        }

        @Override
        protected Exception doInBackground(Integer... integers)
        {
            try
            {
                if(urlString==null)
                {
                    url = new URL("https://www.vg.no/rss/feed/");
                }
                else
                {
                    url = new URL(urlString);
                }

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

                factory.setNamespaceAware(false);

                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(getInputStream(url), "UTF_8");

                boolean insideItem = false;

                int eventType = xpp.getEventType();

                while(eventType != XmlPullParser.END_DOCUMENT && links.size()<amoutItem)
                {
                    if(eventType == XmlPullParser.START_TAG)
                    {
                        if(xpp.getName().equalsIgnoreCase("item"))
                        {
                            insideItem = true;
                        }
                        else if(xpp.getName().equalsIgnoreCase("title"))
                        {
                            if(insideItem)
                            {
                                titles.add(xpp.nextText());
                            }
                        }
                        else if ( xpp.getName().equalsIgnoreCase("link"))
                        {
                            if(insideItem)
                            {
                                links.add(xpp.nextText());
                            }
                        }
                    }
                    else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item"))
                    {
                        insideItem= false;
                    }
                    eventType = xpp.next();
                }
            }catch (MalformedURLException e)
            {
                exc = e;
            }catch (XmlPullParserException e)
            {
                exc = e;
            }catch(IOException e)
            {
                exc = e;
            }
            return exc;
        }
    }

    /**
     * Force an update
     */

    public void forceFetch()
    {
        Feed.run();
    }
}
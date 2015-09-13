package com.apps.quantum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.content.Context;
import android.util.Log;

public class OutcomeSerializer {
	private static final String TAG = "OutcomeSerializer";
	
	private Context mContext;
	private String mFilename;
	private String mJSONFilename;
	
	private static final String UTF8 = "utf8";
	private static final int BUFFER_SIZE = 8192;
	
	
	public OutcomeSerializer(Context c, String f, String j){
			mContext = c;
			mFilename = f;
			mJSONFilename = j;
	}
	



    public void saveJSONActions(ArrayList<Action> actions) throws JSONException, IOException {
        // build an array in JSON
        JSONArray array = new JSONArray();
        for (Action a : actions)
            array.put(a.toJSON());

        // write the file to disk
        Writer writer = null;
        try {
            OutputStream out = mContext.openFileOutput(mJSONFilename, Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(out);
            writer.write(array.toString());
        } finally {
            if (writer != null)
                writer.close();
        }
    }
	
	public boolean saveActions(ArrayList<String> actions){
			try{
				OutputStream out = mContext.openFileOutput(mFilename, Context.MODE_PRIVATE);
				writeActionsToStream(out, actions);
				Log.d(TAG, "Actions saved to file");
				return true;
			}catch (Exception e){
				Log.e(TAG, "Error saving files", e);
				return false;
			}
	}

	//changed DBXFile -> File
	public void writeActionsToFile(ArrayList<String> actions, File file) throws IOException{
//		OutputStream out = file.getWriteStream();
		FileOutputStream out = new FileOutputStream(file);
		writeActionsToStream(out, actions);
		return;
	}

	public void writeActionsToStream(OutputStream out, ArrayList<String> actions) throws IOException{
		
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new OutputStreamWriter(out, UTF8), BUFFER_SIZE);	
			for(String s: actions){
				writer.write(s);
			}
		}finally {
			if(writer != null)
			writer.close();
		}
	}
	
    public ArrayList<Action> loadFromJSON() throws IOException, JSONException {
        ArrayList<Action> actions = new ArrayList<Action>();
        BufferedReader reader = null;
        try {
            // open and read the file into a StringBuilder
            InputStream in = mContext.openFileInput(mJSONFilename);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder jsonString = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                // line breaks are omitted and irrelevant
                jsonString.append(line);
            }
            // parse the JSON using JSONTokener
            JSONArray array = (JSONArray) new JSONTokener(jsonString.toString()).nextValue();
            // build the array of crimes from JSONObjects
            for (int i = 0; i < array.length(); i++) {
                actions.add(new Action(array.getJSONObject(i)));
            }
        } catch (FileNotFoundException e) {
            // we will ignore this one, since it happens when we start fresh
        } finally {
            if (reader != null)
                reader.close();
        }
        return actions;
    }

//	TODO
//	public ArrayList<Action> loadActionsFromExcel(DbxFile file) throws IOException{
//
//		try {
//			//Open and read the file into a StringBuilder
//			InputStream in = file.getReadStream();
//			return readActionsFromStream(in);
//
//		} catch (Exception e){
//			Log.e(TAG, "Unable to open file from local datastore (from text)", e);
//			return new ArrayList<Action>();
//			//do nothing, happens when file doesn't exist;
//		} finally {
//			file.close();
//		}
//
//	}
	
	public ArrayList<Action> loadActions() throws IOException{
		try {
			//Open and read the file into a StringBuilder
			InputStream in = mContext.openFileInput(mFilename);
			ArrayList<Action> output = readActionsFromStream(in);
			Log.d(TAG, "File opened from datastore.");
			return output;
			
		} catch (Exception e) {
			Log.e(TAG, "Unable to open file from local datastore.", e);
			//do nothing, happens when file doesn't exist;
			return new ArrayList<Action>();
		}
	}
	
	private ArrayList<Action> readActionsFromStream(InputStream in) throws IOException{
		ArrayList<Action> actionsList = new ArrayList<Action>();
		
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new InputStreamReader(in, UTF8), BUFFER_SIZE);
			String line = null;
			while((line = reader.readLine()) != null) {
				//Line breaks are omitted and irrelevant
				Action a = new Action(line);
				actionsList.add(a);
			}
		}catch (Exception e) {
				//Ignore this one, it happens when starting fresh
				throw new IOException();
		} finally {
			if (reader != null)
				reader.close();
		}
		
		return actionsList;
	}
}
package com.example.criminalintent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.dropbox.sync.android.DbxFile;

public class OutcomeSerializer {
	private static final String TAG = "OutcomeSerializer";
	
	private Context mContext;
	private String mFilename;
	
	private static final String UTF8 = "utf8";
	private static final int BUFFER_SIZE = 8192;
	
	
	public OutcomeSerializer(Context c, String f){
			mContext = c;
			mFilename = f;
	}
	
	public void saveActions(ArrayList<String> actions) throws IOException {
			try{
				OutputStream out = mContext.openFileOutput(mFilename, Context.MODE_PRIVATE);
				writeActionsToStream(out, actions);
			}catch (Exception e){
				Log.e(TAG, "Error saving files", e);
			}
			return;
	}
	
	public void writeActionsToFile(ArrayList<String> actions, DbxFile file) throws IOException{
		OutputStream out = file.getWriteStream();
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
	
	public ArrayList<Action> loadActionsFromExcel(DbxFile file) throws IOException{
		
		try {
			//Open and read the file into a StringBuilder
			InputStream in = file.getReadStream();
			return readActionsFromStream(in);
			
		} catch (Exception e){
			Log.e(TAG, "Unable to open file from local datastore (from text)", e);
			return new ArrayList<Action>();
			//do nothing, happens when file doesn't exist;
		} finally {
			file.close();
		}
		
	}
	
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
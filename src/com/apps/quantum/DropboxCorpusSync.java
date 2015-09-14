package com.apps.quantum;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by user on 9/12/15.
 */
//TODO: Test performance on large corpora
public class DropboxCorpusSync {

        final static private String APP_KEY = "nd699tj9zebn0ch";
        final static private String APP_SECRET = "wvhmmttd8163scy";
        final static private int LOCAL_REMOTE_DELTA = 1;
        final static private int UPDATE_INTERVAL = 1; //TODO: Change to ~1 hour
        final static private String CORPUS_FILENAME = "corpus.txt";
        final static private String TEMP_FILENAME = "tmp.txt";
        private static DropboxCorpusSync sdbSync;
        private DropboxAPI<AndroidAuthSession> mDBApi;
        private Activity activity;

        private DropboxCorpusSync(Activity a) {
            activity = a;
            authDropbox();
        }

        //Singleton
        public static DropboxCorpusSync get(Activity a){
            sdbSync = (sdbSync == null) ? new DropboxCorpusSync(a) : sdbSync;
            return sdbSync;
        }

        public DropboxAPI<AndroidAuthSession> getDropboxAPI() {
            if (mDBApi == null) Log.d("DbLog", "Tried to get null session from CorpusSync; is user connected?");
            return mDBApi;
        }

        public void authDropbox() {
            SharedPreferences dbPrefs = activity.getApplicationContext().getSharedPreferences("accessToken", activity.MODE_PRIVATE);
            AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
            AndroidAuthSession session = new AndroidAuthSession(appKeys);
            mDBApi = new DropboxAPI<AndroidAuthSession>(session);
            if (!dbPrefs.contains("accessToken")) {
                mDBApi.getSession().startOAuth2Authentication(activity);
            } else {
                //start connection
                String accessToken = dbPrefs.getString("accessToken", "");
                mDBApi.getSession().setOAuth2AccessToken(accessToken);
            }
        }

        public void authDropboxFinish() {
            // Required to complete auth, sets the access token on the session
            mDBApi.getSession().finishAuthentication();
            String accessToken = mDBApi.getSession().getOAuth2AccessToken();

            //Add access token to sharedpreferences
            SharedPreferences dbPrefs = activity.getApplicationContext().getSharedPreferences("accessToken", activity.MODE_PRIVATE);
            SharedPreferences.Editor edit = dbPrefs.edit();
            edit.clear();
            edit.putString("accessToken", accessToken);
            edit.commit();
        }

        //call in onResume after returning to activity from entering DB credentials
        public void authDropboxResume() {
            if (mDBApi != null && mDBApi.getSession().authenticationSuccessful()) {
                try {
                    authDropboxFinish();
                } catch (IllegalStateException e) {
                    Log.i("DbAuthLog", "Error authenticating", e);
                }
            }
        }

        //Merge two corpora without duplicating entries
        public ArrayList<String> mergeCorpusWords(ArrayList<String> corp1, ArrayList<String> corp2) {
            HashSet<String> c1 = new HashSet<>(corp1);
            HashSet<String> c2 = new HashSet<>(corp2);
            ArrayList<String> out = new ArrayList<>();
            for (String st : c1) out.add(st);
            for (String st : c2) {
                if (!c1.contains(st)) out.add(st);
            }
            return out;
        }

        public void launchCorpusSync() {
            new Thread(new Runnable() {
                public void run() {
                    //Download external corpus
//                getCorpus();
                    while (true) {
                        try {
                            //Add new task names to local corpus
                            saveToCorpus(ActionLab.get(activity).getTaskNames());
                            //Check time diff
                            File localCorpus =  new File(activity.getApplicationContext().getFilesDir(), CORPUS_FILENAME);
                            Date localDate = new Date(localCorpus.lastModified());
                            Date remoteDate = getDateModified(CORPUS_FILENAME);
                            System.out.println("Loc " + localDate.getTime());
                            System.out.println("Rem " + remoteDate.getTime());
                            //merge files if edit times > LOCAL_REMOTE_DELTA minutes apart
                            if (Math.abs(localDate.getTime() - remoteDate.getTime()) > LOCAL_REMOTE_DELTA*60*1000) {
                                System.out.println("Merging Corpora");
                                getRemoteCorpusTemp(CORPUS_FILENAME);
                                File remoteCorpus = new File(activity.getApplicationContext().getFilesDir(), TEMP_FILENAME);
                                ArrayList<String> localWords = readFileAsList(localCorpus);
                                ArrayList<String> remoteWords = readFileAsList(remoteCorpus);
                                ArrayList<String> mergedWords = mergeCorpusWords(localWords, remoteWords);
                                writeListToFile(localCorpus, mergedWords);
                                //push to remote
                                postFileOverwrite(CORPUS_FILENAME);
                                //delete tmp file
                                remoteCorpus.delete();
                            }
                            System.out.println("Not/Done Merging Corpora");

                            //Wait some time
                            Thread.sleep(UPDATE_INTERVAL*60*1000); //todo: change to N minutes
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();
        }


        //for testing
        public String randomString() {
            SecureRandom random = new SecureRandom();
            return (new BigInteger(130, random).toString(32));
        }

        //post a text file to dropbox
        public void postFileOverwrite(String fileName) {
            File file = new File(activity.getApplicationContext().getFilesDir(), fileName);
            if (file.exists()) {
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    DropboxAPI.Entry response = mDBApi.putFileOverwrite("/" + fileName, inputStream,
                            file.length(), null);
                    Log.i("DbLog", "The uploaded file's rev is: " + response.rev);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.d("DbLog", "Tried to post file to Dropbox that doesn't exist on local");
            }
        }

        public void launchPostFileOverwrite(final String fileName) {
            new Thread(new Runnable() {
                public void run() {
                    postFileOverwrite(fileName);
                }
            }).start();
        }

        public void launchGetRemoteFile(final String fileName) {
            new Thread(new Runnable() {
                public void run() {
                    getRemoteFile(fileName);
                }
            }).start();
        }

//      TODO: Check if remote exists first
        public void getRemoteCorpusTemp(String fileName) {
            getRemoteFile(TEMP_FILENAME, CORPUS_FILENAME);
        }

        public void getRemoteFile(String fileName) {
            getRemoteFile(fileName, fileName);
        }

        public void getRemoteFile(String localName, String remoteName) {
            try {
                //write contents of Dropbox version of remoteName to localName
                File file = new File(activity.getApplicationContext().getFilesDir(), localName);
                if (!file.exists()) file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file);
                DropboxAPI.DropboxFileInfo info = mDBApi.getFile("/" + remoteName, null, outputStream, null);
                Log.i("DbLog", "The downloaded file's rev is: " + info.getMetadata().rev);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Date getDateModified(String remoteName) {
            try {
                DropboxAPI.Entry md = mDBApi.metadata("/" + remoteName, 1, null, false, null);
                String timeChanged = md.modified;
                Date d = RESTUtility.parseDate(timeChanged);
                return d;
            } catch (com.dropbox.client2.exception.DropboxException e) {
                Log.d("DbLog", "File did not exist, returning date 0");
                return new Date(0);
            }

        }
    
        public ArrayList<String> readCorpusAsList() {
            try {
                File file = new File(activity.getApplicationContext().getFilesDir(), CORPUS_FILENAME);
                return readFileAsList(file);
            } catch (Exception e) {
                return new ArrayList<String>();
            }
        }

        public ArrayList<String> readFileAsList(File file) throws java.io.IOException, java.io.FileNotFoundException {
            ArrayList<String> words = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line);
                System.out.println("Current item in corpus: " + line);
            }
            reader.close();
            return words;
        }

        public void writeListToFile(File file, ArrayList<String> list) throws java.io.IOException {
            //set append flag on file writer to false
            PrintWriter writeOut = new PrintWriter(new BufferedWriter(new FileWriter(file, false) ));
            for (String st : list) {
                writeOut.println(st);
            }
            writeOut.close();
        }

        public void createCorpus() {
            File dir = activity.getFilesDir();
            File file = new File(activity.getApplicationContext().getFilesDir(), CORPUS_FILENAME);
            System.out.println(Arrays.toString(dir.list()));
            if (!file.exists()) {
                System.out.println("The file corpus.txt does not exist");
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //add new tasks to corpus.txt
        public void saveToCorpus(ArrayList<String> taskStrings) {
            //open corpus
            File corpus = new File(activity.getApplicationContext().getFilesDir(), CORPUS_FILENAME);
            HashSet<String> corpusWords = new HashSet<String>();

            if (!corpus.exists()) {
                System.out.println("corpus did not exist in saveToCorpus");
                createCorpus();
            }

            try {
                //read file in
                BufferedReader reader = new BufferedReader(new FileReader(corpus));
                String line;
                while ((line = reader.readLine()) != null) {
                    corpusWords.add(line);
                    System.out.println("Current item in corpus: " + line);
                }
                reader.close();

                //write unique new tasks to file
                //set append flag on file writer to true
                PrintWriter writeOut = new PrintWriter(new BufferedWriter(new FileWriter(corpus, true) ));
                for (String newString : taskStrings) {
                    if (!corpusWords.contains(newString)) {
                        System.out.println("Item added to corpus: " + newString);
                        writeOut.println(newString);
                    }
                }
                writeOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

}


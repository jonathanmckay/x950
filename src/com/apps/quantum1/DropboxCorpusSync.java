package com.apps.quantum1;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
public class DropboxCorpusSync {

        private static final String TAG = "DbLog";
        final static private String APP_KEY = "588rm6vl0oom62h";
        final static private String APP_SECRET = "3m69jjskzcfcssn";
//    Add new tasks to local corpus every UPDATE_INTERVAL minutes
// Merge with remote every SYNC_INTERVAL minutes if timestamps differ by LOCAL_REMOTE_DELTA minutes
        final static private int LOCAL_REMOTE_DELTA = 1;
        final static private int UPDATE_INTERVAL = 5;
        final static private int SYNC_INTERVAL = 60;
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
            if (mDBApi == null) Log.d(TAG, "Tried to get null session from CorpusSync; is user connected?");
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
            //a thread for merging the local with the remote
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            if (ActionLab.get(activity).getAutosyncOn())
                                mergeCorpusWithDropbox();
                            //Wait some time
                            Thread.sleep(SYNC_INTERVAL*60*1000);
                        } catch (DropboxException e){
                            //In the event of Dropbox exception (i.e. no Internet), admit failure and wait
                            e.printStackTrace();
                            try {
                                Thread.sleep(SYNC_INTERVAL * 60 * 1000);
                            } catch (InterruptedException e0) {
                                final String toastText = "Failed merging corpora";
                                activity.runOnUiThread(new Runnable () {
                                    public void run() {
                                        Toast.makeText(activity.getApplicationContext(), toastText, Toast.LENGTH_LONG).show();
                                    }
                                });
                                e0.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();

                        }

                    }
                }
            }).start();

            //a thread for adding new tasks to the local corpus
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            //Add new task names to local corpus
                            saveToCorpus(ActionLab.get(activity).getTaskNames());
                            //Wait some time
                            Thread.sleep(UPDATE_INTERVAL*60*1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();
        }

        public void mergeCorpusWithDropbox() throws Exception {
            //Check time diff
            File localCorpus =  new File(activity.getApplicationContext().getFilesDir(), CORPUS_FILENAME);
            Date localDate = new Date(localCorpus.lastModified());
            Date remoteDate = getDateModified(CORPUS_FILENAME);
            Log.i(TAG, "Local Corpus Modified: " + localDate.getTime());
            Log.i(TAG, "Remote Corpus Modified: " + remoteDate.getTime());
            //merge files if edit times > LOCAL_REMOTE_DELTA minutes apart
            if (Math.abs(localDate.getTime() - remoteDate.getTime()) > LOCAL_REMOTE_DELTA*60*1000) {
                Log.i(TAG, "Merging Corpora");
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
                //Inform the user of succesful outcome (note UI needs to be done on UI thread)
                final String toastText = "Succesfully merged corpora";
                activity.runOnUiThread(new Runnable () {
                    public void run() {
                        Toast.makeText(activity.getApplicationContext(), toastText, Toast.LENGTH_LONG).show();
                    }
                });
            }
            Log.i(TAG, "Done Merging Corpora");
        }


        //for testing
        public String randomString() {
            SecureRandom random = new SecureRandom();
            return (new BigInteger(130, random).toString(32));
        }

        //post a text file to dropbox
        public void postFileOverwrite(String fileName) throws DropboxException {
            File file = new File(activity.getApplicationContext().getFilesDir(), fileName);
            if (file.exists()) {
                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    DropboxAPI.Entry response = mDBApi.putFileOverwrite("/" + fileName, inputStream,
                            file.length(), null);
                    Log.i(TAG, "The uploaded file's rev is: " + response.rev);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Tried to post file to Dropbox that doesn't exist on local");
            }
        }

        public void launchPostFileOverwrite(final String fileName) {
            new Thread(new Runnable() {
                public void run() {
                    final String success = "Succesful Dropbox Upload";
                    final String fail = "Failed Dropbox Upload";
                    try {
                        postFileOverwrite(fileName);
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity.getApplicationContext(), success, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (DropboxException e) {
                        Log.d(TAG, "Dropbox post failure: " + e);
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity.getApplicationContext(), fail, Toast.LENGTH_LONG).show();
                            }
                        });
                    }


                }
            }).start();
        }

//      Typically won't use this; can lead to race conditions if you download on one thread and open file in another
        public void launchGetRemoteFile(final String fileName) {
            new Thread(new Runnable() {
                public void run() {
                    getRemoteFile(fileName);
                }
            }).start();
        }

        public void getRemoteCorpusTemp(String fileName) {
            //dropbox server exception if remote does not exist
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
                Log.i(TAG, "The downloaded file's rev is: " + info.getMetadata().rev);
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
                Log.d(TAG, "File did not exist, returning date 0");
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
//                Log.i(TAG, "Current item in corpus: " + line);
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
            Log.i(TAG, (Arrays.toString(dir.list())) );
            if (!file.exists()) {
                Log.i(TAG, "The local file corpus.txt does not exist");
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
            if (!corpus.exists())
                createCorpus();

            try {
                //read file in
                BufferedReader reader = new BufferedReader(new FileReader(corpus));
                String line;
                while ((line = reader.readLine()) != null) {
                    corpusWords.add(line);
//                  Log.i(TAG, "Current item in corpus: " + line);
                }
                reader.close();

                //write unique new tasks to file
                //set append flag on file writer to true
                PrintWriter writeOut = new PrintWriter(new BufferedWriter(new FileWriter(corpus, true) ));
                for (String newString : taskStrings) {
                    if (!corpusWords.contains(newString)) {
                        Log.i(TAG, "Item added to corpus: " + newString);
                        writeOut.println(newString);
                    }
                }
                writeOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

}

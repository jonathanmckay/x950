package com.apps.quantum;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class DropboxFragment extends DialogFragment {
	private ActionLab mActionLab;
	private String mFilename;
	private Activity mActivity;
	private boolean mInfoUpdated;
	
	public static final int SAVE = 0;
	public static final int EXPORT = 1;
	public static final int IMPORT = 2;
	private static final int REQUEST_EXPORT_FILENAME = 0;
	private static final int REQUEST_IMPORT_FILENAME = 1;
	private static String LIST_UPDATED = "list_updated";
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }
	
	private void sendResult(int resultCode) {
		if(getTargetFragment() == null) return;
		
		Intent i = new Intent();
		i.putExtra(LIST_UPDATED, mInfoUpdated);
		getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
	}
		
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mActionLab = ActionLab.get(getActivity());
		mInfoUpdated = false;
		return new AlertDialog.Builder(mActivity).setTitle(R.string.dropbox_dialog_title)
				.setItems(R.array.dropbox_options, new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int which) {
		             	   switch (which){
		             	   case SAVE:
		             		   ActionLab.get(getActivity()).saveToDropbox(ActionLab.AUTOSAVE_FILENAME);
		             		   String toastText = "Saved Dropbox";
		             		   Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
		             		   break;
		             	   case EXPORT:
		             		   callFileInputDialog(REQUEST_EXPORT_FILENAME);
		             		   break;
		             	   case IMPORT:
		             		   callFileInputDialog(REQUEST_IMPORT_FILENAME);
		             		   break;
		             	   }
		               }
				}).create();
	}
	
	private void callFileInputDialog(int infoRequested){
		final int infoRequestedLocal = infoRequested;
		final EditText input = new EditText(getActivity());
		input.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				mFilename = c.toString();
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				//This also left blank
			}
		});
		input.setHeight(100);
	
		
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
		        LinearLayout.LayoutParams.WRAP_CONTENT,
		        LinearLayout.LayoutParams.WRAP_CONTENT);
		input.setLayoutParams(lp);
		
		
		
		AlertDialog filename = new AlertDialog.Builder(getActivity())
		.setView(input)
		.setTitle(R.string.select_filename_title)
		.setPositiveButton(android.R.string.ok, 
				new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(mFilename.equals("") || mFilename == null){
						mFilename = "default"; 
					}
					
					String toastText = "";
					
					switch(infoRequestedLocal){
					case REQUEST_IMPORT_FILENAME:
						try{
							mActionLab.importDbxFile(mFilename);
							toastText = "Items imported";
							mInfoUpdated = true;
						} catch (IllegalArgumentException e){
							toastText = "Filename blank";
						} catch (Exception e) {
							toastText = "Could not import file";
						}
						break;
						
					case REQUEST_EXPORT_FILENAME:
						if(!mFilename.endsWith(".txt")){
							mFilename = mFilename + ".txt";
						}
						mActionLab.saveToDropbox(mFilename);
						toastText = "Exported to Dropbox";
						
						break;
					default:
						toastText = "Did not reach any of the predicted branches.";
						break;
					}
					if(toastText != null)
						Toast.makeText(mActivity.getApplicationContext(), toastText, Toast.LENGTH_LONG).show();
					sendResult(Activity.RESULT_OK);
				}	
				
		})
		.setNegativeButton(android.R.string.cancel, 
				new DialogInterface.OnClickListener() {
					
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendResult(Activity.RESULT_CANCELED);
				}
		}).create();
		
		filename.show();
	
	
	}
	

	
}
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;

public class DropboxFragment extends DialogFragment {
	private ActionLab mActionLab;
	private String mFilename;
	private Activity mActivity;
	private boolean mInfoUpdated;
	
	public static final int SAVE = 0;
	public static final int EXPORT = 1;
	public static final int IMPORT = 2;
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
						switch (which) {
							case SAVE:
								String toastText;
								if (mActionLab.saveToDropbox(ActionLab.AUTOSAVE_FILENAME)) {
									toastText = "Saved Dropbox";
								} else {
									toastText = "Save failed - check connection?";
								}
								Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
								break;
							case EXPORT:
								callFileExportDialog();
								break;
							case IMPORT:
								callFileImportDialog();
								break;
						}
					}
				}).create();
	}

	private void callFileImportDialog() {
		final StringBuffer nameout = new StringBuffer();
		ArrayList<String> filenames = mActionLab.readDropboxFiles();
		final RadioButton[] rb = new RadioButton[filenames.size()];

		//TODO: Check network for connectivity
		//TODO: Enable user to delete saved lists
		//Create radiogroup and add buttons to rg
		RadioGroup rg = new RadioGroup(getActivity());
		rg.setOrientation(RadioGroup.VERTICAL);
		for(int i=0; i<filenames.size(); i++) {
			rb[i] = new RadioButton(getActivity());
			rg.addView(rb[i]);
			rb[i].setText(filenames.get(i));
			final String fname = filenames.get(i);
			rb[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (b) {
//						Can only use final variables in inner class, so use mutable StringBuffer
// 						instead of string to update the filename's value
						nameout.delete(0, nameout.length());
						nameout.append(fname);
					}

				}
			});
		}

		//Add radiogroup to scrollview so one may scroll through buttons
		ScrollView scroll = new ScrollView(getActivity());
		scroll.addView(rg);

		AlertDialog filename = new AlertDialog.Builder(getActivity())
				.setView(scroll)
				.setTitle(R.string.select_filename_title)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								String toastText = "";

								if (nameout.length() > 0) {
									try {
										mFilename = nameout.toString();
										if (mActionLab.importDbxFile(mFilename)) {
											toastText = "Items imported";
										} else {
											toastText = "Import failed - check connection?";
										}
										mInfoUpdated = true;
									} catch (IllegalArgumentException e) {
										toastText = "Filename blank";
									} catch (Exception e) {
										toastText = "Could not import file";
									}
								} else {
									toastText = "No file was selected!";
								}

								if (toastText != null)
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
	
	private void callFileExportDialog(){
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
					if (mFilename == null || mFilename.equals("")) {
						mFilename = "default";
					}

					String toastText = "";

					if (!mFilename.endsWith(".txt")) {
						mFilename = mFilename + ".txt";
					}
					if (mFilename.equals("corpus.txt") || mFilename.equals("corpus")) {
						toastText = "No export; corpus.txt is reserved";
					} else if (mFilename.equals("")) {
						toastText = "No filename input";
					} else {
						if (mActionLab.saveToDropbox(mFilename)) {
							toastText = "Exported to Dropbox";
						} else {
							toastText = "Export failed - check connection?";
						}
					}

					if (toastText != null)
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
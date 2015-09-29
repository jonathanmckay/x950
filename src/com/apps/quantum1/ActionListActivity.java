package com.apps.quantum1;


import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class ActionListActivity extends SingleFragmentActivity implements ActionListFragmentDSLV.Callbacks, ActionFragment.Callbacks{
	public static final String TAG = "ActionListActivity";
	public static final String PREFS_NAME = "MyPrefsFile";
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
	private DropboxCorpusSync dbSync;

    private String[] mMenuTitles;
	boolean mAtRoot;
	
	@Override
	 protected void onCreate(Bundle savedInstanceState) {
			//Since XML sets showing the actionbar as false, must request it for
			//getActionBar to return a value
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
			super.onCreate(savedInstanceState);
			//Hide the action bar until app is finished loading. 
			getActionBar().hide();
			

	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
	        	SystemBarTintManager tintManager = new SystemBarTintManager(this);
	        	tintManager.setStatusBarTintEnabled(true);
	        	// Holo light action bar color is #DDDDDD
	        	int actionBarColor = Color.parseColor("#DDDDDD");
	        	tintManager.setStatusBarTintColor(actionBarColor);
	        	}   
	        setContentView(R.layout.activity_threepane);
	        getActionBar().show();
	        //getActionBar().setTitle(R.string.default_title);
	        
	        
	        //getActionBar().show();
	        
	        mMenuTitles = getResources().getStringArray(R.array.menu_titles);
	        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
	        mDrawerList = (ListView) findViewById(R.id.left_drawer);

	        // set a custom shadow that overlays the main content when the drawer opens
	        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
	        // set up the drawer's list view with items and click listener
	        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
	                R.layout.drawer_list_item, mMenuTitles));
	        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

	        // enable ActionBar app icon to behave as action to toggle nav drawer
	        getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActionBar().setHomeButtonEnabled(true);

	        // ActionBarDrawerToggle ties together the the proper interactions
	        // between the sliding drawer and the action bar app icon
	        mDrawerToggle = new ActionBarDrawerToggle(
	                this,                  /* host Activity */
	                mDrawerLayout,         /* DrawerLayout object */
	                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */ 
	                R.string.drawer_open,  /* "open drawer" description for accessibility */
	                R.string.drawer_close  /* "close drawer" description for accessibility */
	                ) {
	            public void onDrawerClosed(View view) {
	                //getActionBar().setTitle(mTitle);
	                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	            }

	            public void onDrawerOpened(View drawerView) {
	                //getActionBar().setTitle(mDrawerTitle);
	                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
	            }
	        };
	        
	        mDrawerLayout.setDrawerListener(mDrawerToggle);
        	onActionSelected(ActionLab.get(this).getRoot());

			dbSync = DropboxCorpusSync.get(this);
	    }

	        protected void onResume() {
            super.onResume();
			dbSync.authDropboxResume();
        }





	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            selectItem(position);
	        }
	    }

	    private void selectItem(int menuPosition) {
	        mDrawerList.setItemChecked(menuPosition, true);
	        FragmentManager fm = getSupportFragmentManager();
	    	ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV)fm.findFragmentById(R.id.listFragment);
	    	
	    	listFragment.selectActionView(menuPosition);
	    	
	        mDrawerLayout.closeDrawer(mDrawerList);
	    }
	 
	    
	 public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
		if(mAtRoot){
			 if (mDrawerToggle.onOptionsItemSelected(item)) {
				 return true;
			 }
		}
     // Handle action buttons
        switch(item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	 
	 /**
	     * When using the ActionBarDrawerToggle, you must call it during
	     * onPostCreate() and onConfigurationChanged()...
	     */
	 
	    @Override
	    protected void onPostCreate(Bundle savedInstanceState) {
	        super.onPostCreate(savedInstanceState);
	        // Sync the toggle state after onRestoreInstanceState has occurred.
	        if(mAtRoot) mDrawerToggle.syncState();
	    }

	    @Override
	    public void onConfigurationChanged(Configuration newConfig) {
	        super.onConfigurationChanged(newConfig);
	        // Pass any configuration change to the drawer toggls
	        if(mAtRoot) mDrawerToggle.onConfigurationChanged(newConfig);
	    }
	
	    /**
	     * Fragment that appears in the "content_frame", shows a planet
	     */
	
	
	@Override
	protected Fragment createFragment() {
		mAtRoot = true;
		return new ActionListFragmentDSLV();
	}
	protected int getLayoutResId(){
		return R.layout.activity_threepane;
	}
	
	 public void onActionSelected(Action a){
		 		//Only show the detail view for individual tasks. This may be modified later.
	    		if(a == ActionLab.get(this).getRoot()){
	    			FragmentManager fm = getSupportFragmentManager();
		    		removeDetailFragment(fm);
		    		mAtRoot = true;
	    		} else if(a.hasChildren()) {
	    			mAtRoot = false;
	    			FragmentManager fm = getSupportFragmentManager();
		    		removeDetailFragment(fm);
	    		}else{
			 		mAtRoot = false;

	    			FragmentManager fm = getSupportFragmentManager();
			 		FragmentTransaction ft = fm.beginTransaction();
		    		ActionFragment oldDetail = (ActionFragment) fm.findFragmentById(R.id.detailFragment);
		    		
		    		if(oldDetail != null){
		    			//Simply update the view, don't create a new fragment
		    			oldDetail.displayActionDetails(a);
		    		} else {
		    			Fragment newDetail = ActionFragment.newInstance(a.getId());	
			    		ft.add(R.id.detailFragment, newDetail);
			    		ft.commit();
		    		}
	    		}
	    		updateDrawerButton();
	    	
	 }
	 public void onDetailViewToggled(Action a){
		 FragmentManager fm = getSupportFragmentManager();
		 if(fm.findFragmentById(R.id.detailFragment)== null){
			 createNewDetailFragment(fm, a);
		 } else {
			 removeDetailFragment(fm);
		 }
	 }
	 
	 private void createNewDetailFragment(FragmentManager fm, Action a){
 		FragmentTransaction ft = fm.beginTransaction();
 		Fragment newDetail = ActionFragment.newInstance(a.getId());
 		ft.add(R.id.detailFragment, newDetail);
 		ft.commit();
	 }
	 
	 private void removeDetailFragment(FragmentManager fm){
		FragmentTransaction ft = fm.beginTransaction();
 		Fragment oldDetail = fm.findFragmentById(R.id.detailFragment);
 		if(oldDetail != null) ft.remove(oldDetail);
 		ft.commit();
 		fm.popBackStack();
	 }
	
	 
    public void onActionUpdated(){
    	FragmentManager fm = getSupportFragmentManager();
    	ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV)fm.findFragmentById(R.id.listFragment);
    	listFragment.updateUI();
    }
    
    private void updateDrawerButton(){
    	mDrawerToggle.setDrawerIndicatorEnabled((mAtRoot) ? true : false);
    }
    
    public void onBackPressed() {
    	 
    	FragmentManager fm = getSupportFragmentManager();
    	ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV)fm.findFragmentById(R.id.listFragment);
    	int backPressResult = listFragment.navigateUp();
    	
    	switch (backPressResult){
	    	case ActionListFragmentDSLV.ALREADY_AT_ROOT:
	    		super.onBackPressed();
	    		break;
	    	case ActionListFragmentDSLV.ARRIVED_AT_ROOT:
	    		mAtRoot = true;
	    		break;
	    	default:
	    		mAtRoot = false;
	    		break;
    	}
    	
    	updateDrawerButton();
    }
    
    public void navigateUp(){
    	onBackPressed();
    }
    
    public void updateOnScreenKeyboard(View v, int visibility){
		InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		
		Log.d(TAG, "updateOnScreenKeyboard Called");
		
		if(imm != null && getCurrentFocus() != null ){
			if(visibility == View.VISIBLE){
				Log.d(TAG, "Keyboard show called");
				imm.showSoftInput(v, 0);
			} else if(visibility == View.INVISIBLE){		
				imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
			
		}
    }
}

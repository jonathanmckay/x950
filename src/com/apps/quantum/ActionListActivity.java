package com.apps.quantum;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class ActionListActivity extends SingleFragmentActivity implements ActionListFragmentDSLV.Callbacks, ActionFragment.Callbacks{
	@Override
	protected Fragment createFragment() {
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
	    		} else if(a.hasChildren()) {
	    			FragmentManager fm = getSupportFragmentManager();
		    		removeDetailFragment(fm);
	    		}else{
			 		FragmentManager fm = getSupportFragmentManager();
		    		
		    		ActionFragment oldDetail = (ActionFragment) fm.findFragmentById(R.id.detailFragment);
		    		
		    		if(oldDetail != null){
		    			//Simply update the view, don't create a new fragment
		    			oldDetail.displayActionDetails(a);
		    		} else {
		    			FragmentTransaction ft = fm.beginTransaction();
		    			Fragment newDetail = ActionFragment.newInstance(a.getId());	
			    		ft.add(R.id.detailFragment, newDetail);
			    		ft.commit();
		    		}
	    		}
	    	
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
    
    public void onBackPressed() {
    	FragmentManager fm = getSupportFragmentManager();
    	ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV)fm.findFragmentById(R.id.listFragment);
    	
    	//The fragment was already at root, can't navigate up
    	if(listFragment.onBackPressed() == -1) super.onBackPressed();
    	
        return;
    }
    
    public void navigateUp(){
    	onBackPressed();
    }
    
    
}

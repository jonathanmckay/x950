package com.example.criminalintent;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class ActionListActivity extends SingleFragmentActivity implements ActionListFragment.Callbacks, ActionFragment.Callbacks{

	@Override
	protected Fragment createFragment() {
		return new ActionListFragment();
	}
	
	@Override
	protected int getLayoutResId(){
		return R.layout.activity_threepane;
	}
	
	public void onActionSelected(Action a){
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		
		Fragment oldDetail = fm.findFragmentById(R.id.detailFragmentContainer);
		Fragment newDetail = ActionFragment.newInstance(a.getId());
		
		if(oldDetail != null){
			ft.remove(oldDetail);
		}
		
		ft.add(R.id.detailFragmentContainer, newDetail);
		ft.commit();
	}
	
	 
    public void onActionUpdated(Action crime){
    	FragmentManager fm = getSupportFragmentManager();
    	ActionListFragment listFragment = (ActionListFragment)fm.findFragmentById(R.id.fragmentContainer);
    	listFragment.updateUI();
    }
    
    public void onBackPressed() {
    	FragmentManager fm = getSupportFragmentManager();
    	ActionListFragment listFragment = (ActionListFragment)fm.findFragmentById(R.id.fragmentContainer);
    	
    	//The fragment was already at root, can't navigate up
    	if(listFragment.onBackPressed() == -1) super.onBackPressed();
    	
        return;
    }  
    
    
}

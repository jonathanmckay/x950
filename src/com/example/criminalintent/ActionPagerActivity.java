package com.example.criminalintent;

import java.util.UUID;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;

public class ActionPagerActivity extends FragmentActivity {
	private ViewPager mViewPager;
	private Action mAction;
	private Action mParent;
	private int list;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		mViewPager = new ViewPager(this);
		mViewPager.setId(R.id.viewPager);
		setContentView(mViewPager);
		
		int index = 0;
		
		UUID ActionId = (UUID)getIntent().getSerializableExtra(ActionFragment.EXTRA_Action_ID);
		mAction = ActionLab.get(this).findAction(ActionId);
		
		//mAction is returning null.
		
		mParent = mAction.getParent();
		
		
		FragmentManager fm = getSupportFragmentManager();
		
		/* This should really be changed to a HashMap, but I'll finish the chapter first
		 * 
		 */
		list = mAction.getActionStatus();
		
		for(int i = 0; i < mParent.getChildren().get(list).size(); i++){
			if(mParent.getChildren().get(list).get(i).getId().equals(mAction.getId())){
				index = i;
			}
		}
		
		mViewPager.setAdapter(new FragmentStatePagerAdapter(fm){
			@Override
			public int getCount(){
				return mParent.getChildren().get(list).size();
			}
			
			@Override
			public Fragment getItem(int pos){
				Action a =  mParent.getChildren().get(list).get(pos);
				return ActionFragment.newInstance(a.getId());
			}
		});	
					
		mViewPager.setCurrentItem(index);
		
		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int pos) {
				Action Action = mParent.getChildren().get(list).get(pos);
				if(Action.getTitle() != null){
					setTitle(Action.getTitle());
				}
			}
			
			@Override
			public void onPageScrolled(int pos, float posOffset, 
					int posOffsetPixels) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
	}
}


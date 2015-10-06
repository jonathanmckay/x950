package com.apps.quantum1;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SearchFragment extends Fragment {
    private ActionLab mActionLab;
    private ActionLab.TitleMap mTitleHash;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
    }

    @Override
    public void onDetach(){
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionLab = ActionLab.get(getActivity());
        mTitleHash = mActionLab.getTitleHash();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View v = (inflater.inflate(R.layout.fragment_search, parent, false));
        //Get activities for adapter
        setSearchAdapter(v);
        return v;
    }

    //TODO: Add context search
    //TODO: Modify search adapter on action list change (and change contents of mTitleHash
    private void setSearchAdapter(View v) {
        ArrayList<String> tasknames = mActionLab.getTaskNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                tasknames.toArray(new String[tasknames.size()]));
        final AutoCompleteTextView searchTextView = (AutoCompleteTextView) v
                .findViewById(R.id.searchTextView);
        searchTextView.setAdapter(adapter);

        //TODO: If same name applies to multiple actions, will only go to first one in list
        final HashMap<String, Action> titleActionHash = new HashMap();
        for (HashMap.Entry<String, List<Action>> entry : mTitleHash.entrySet()) {
            titleActionHash.put(entry.getKey(), entry.getValue().get(0));
        }

        //go to action on click
        searchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchTextView.setSelection(searchTextView.getText().length());
                String actionName = searchTextView.getText().toString();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV) fm.findFragmentById(R.id.listFragment);
                listFragment.goToAction(titleActionHash.get(actionName));
//                TODO: Clear out textview, handle errors
            }
        });

        //TODO: Add listener to search on return

    }



    @Override
    public void onPause() {
        super.onPause();
    }

//    public void onActivityResult(int requestCode, int resultCode, Intent data){
//
//    }

}

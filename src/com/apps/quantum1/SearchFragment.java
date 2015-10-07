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
import java.util.HashSet;
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
//        setSearchAdapter(v);
        setSearchContextAdapter(v);
        return v;
    }

    //TODO: Don't let textview capture keyboard
    //TODO: Modify search adapter on action list change (and change contents of mTitleHash
    private void setSearchTaskAdapter(View v) {
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

    private void setSearchContextAdapter(View v) {
        HashSet<String> contextNames = new HashSet<>();
//        final ArrayList<Action> testActions = new ArrayList<>();

        final HashMap<String, Action> titleActionHash = new HashMap();
        for (HashMap.Entry<String, List<Action>> entry : mTitleHash.entrySet()) {
            titleActionHash.put(entry.getKey(), entry.getValue().get(0));
//            System.out.println("Context = " + entry.getValue().get(0).getContextName());
            String contextName = entry.getValue().get(0).getContextName();
            if (contextName.length() > 0) {
//                System.out.println("Context = " + contextName);
                contextNames.add(contextName);
            }

        }

        FragmentManager fm = getActivity().getSupportFragmentManager();
        ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV) fm.findFragmentById(R.id.listFragment);
//        listFragment.displayTheseActions(testActions);

        //todo: adapter not consistently loading
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                contextNames.toArray(new String[contextNames.size()]));
        final AutoCompleteTextView searchTextView = (AutoCompleteTextView) v
                .findViewById(R.id.searchTextView);
        searchTextView.setAdapter(adapter);

        searchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchTextView.setSelection(searchTextView.getText().length());
                String contextName = searchTextView.getText().toString();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV) fm.findFragmentById(R.id.listFragment);
//                listFragment.goToAction(titleActionHash.get(actionName));
                final ArrayList<Action> actions = new ArrayList<>();
                //TODO: Include all actions
                for (HashMap.Entry<String, List<Action>> entry : mTitleHash.entrySet()) {
                    Action a = entry.getValue().get(0);
                    if (a.getContextName().equals(contextName)) actions.add(a);
                }
                listFragment.displayTheseActions(actions);
//                TODO: Clear out textview, handle errors
            }
        });

    }



    @Override
    public void onPause() {
        super.onPause();
    }

//    public void onActivityResult(int requestCode, int resultCode, Intent data){
//
//    }

}

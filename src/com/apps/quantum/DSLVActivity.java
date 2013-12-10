package com.apps.quantum;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

public class DSLVActivity extends Activity
{ 

DragSortListView listView;
ArrayAdapter<String> adapter;

private DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
{
    @Override
    public void drop(int from, int to)
    {
        if (from != to)
        {
            String item = adapter.getItem(from);
            adapter.remove(item);
            adapter.insert(item, to);
        }
    }
};

private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener()
{
    @Override
    public void remove(int which)
    {
        adapter.remove(adapter.getItem(which));
    }
};


@Override
protected void onCreate(Bundle savedInstanceState)
{
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dslv);

    listView = (DragSortListView) findViewById(R.id.listview);
    
    String[] names = getResources().getStringArray(R.array.jazz_artist_names);
    ArrayList<String> list = new ArrayList<String>(Arrays.asList(names));
    adapter = new ArrayAdapter<String>(this,
            R.layout.jazz_artist_list_item, R.id.text, list);
    listView.setAdapter(adapter);
    listView.setDropListener(onDrop);
    listView.setRemoveListener(onRemove);

    DragSortController controller = new DragSortController(listView);
    controller.setDragHandleId(R.id.drag_handle);
            //controller.setClickRemoveId(R.id.);
    controller.setRemoveEnabled(true);
    controller.setSortEnabled(true);
    controller.setDragInitMode(1);
            //controller.setRemoveMode(removeMode);

    listView.setFloatViewManager(controller);
    listView.setOnTouchListener(controller);
    listView.setDragEnabled(true);
}

}
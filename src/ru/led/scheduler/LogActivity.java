package ru.led.scheduler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

@SuppressWarnings("unused")
public class LogActivity extends ExpandableListActivity{

	@SuppressWarnings("resource")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    String name = getIntent().getExtras().getString("name");
	    
	    // Parse log file
	    List<Map<String,String>> groups = new ArrayList<Map<String,String>>();
	    List<List<Map<String,String>>> childs = new ArrayList<List<Map<String,String>>>();
	    Map<String, String> currGroup = null;
	    List<Map<String,String>> currChilds = null;
	    
	    Pattern r = Pattern.compile("^(\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}):");
	    
	    try {
			BufferedReader reader = new BufferedReader( new FileReader( ExecService.getLogFile(name)) );
			String line = null;
			while(  (line = reader.readLine())!=null ){
				Log.d("log", line);
				Matcher m = r.matcher(line);
				if( m.find() ){
					if( currChilds!=null ) childs.add(currChilds);

					// add new group
					Map<String,String> gr = new HashMap<String, String>();
					gr.put("date", m.group(1) );
					
					groups.add(gr);
					
					currChilds = new ArrayList<Map<String,String>>();
				}else{
					if( currChilds!=null ){
						Map<String,String> ch = new HashMap<String,String>();
						ch.put("line", line);
						currChilds.add(ch);
					}
				}
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		if( currChilds!=null ) childs.add(currChilds);
	    
	    
		ExpandableListAdapter adapter = new SimpleExpandableListAdapter(
	    		this, 
	    		groups, R.layout.item_view_line,
	    		new String[]{"date"}, new int[]{R.id.list_row1},
	    		
	    		childs, R.layout.item_view_line,
	    		new String[]{"line"}, new int[]{R.id.list_row1}
	    );
	    this.setListAdapter(adapter);
	    
	    ExpandableListView listView = getExpandableListView();
	    //listView.setGroupIndicator(groupIndicator);
	    
	    listView.setGroupIndicator(null);
	    int count = adapter.getGroupCount();
	    for (int position = 1; position <= count; position++)
	        listView.expandGroup(position - 1);
	}
	
	
	

}

package ru.led.scheduler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class MultilinePromptDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener 
{  
	 //private final EditText input;
	private final Map<String,EditText> inputs;

	 public MultilinePromptDialog(Context context, String title, Map<String,String> data){  
		  super(context);  
		  setTitle(title);  
		  inputs = new HashMap<String,EditText>();
		  
		  LinearLayout parent = new LinearLayout(context);
		  parent.setOrientation( LinearLayout.VERTICAL );

		  for( Iterator<String> it = data.keySet().iterator(); it.hasNext(); ){
			  String key = it.next();
			  String def_val = data.get(key);
			  
			  TextView text = new TextView(context);
			  text.setText(key);
			  parent.addView(text);
			  
			  EditText edit = new EditText(context);
			  edit.setText(def_val);
			  parent.addView(edit);
			  
			  inputs.put(key, edit);
		  }
		  
		  setView(parent);
		  
		  setPositiveButton(android.R.string.ok, this);  
		  setNegativeButton(android.R.string.cancel, this);  
	 }  

	 public void onCancelClicked(DialogInterface dialog) {  
		  dialog.dismiss();  
	 }
	  
	 public void onClick(DialogInterface dialog, int which) {  
		  
		 if (which == DialogInterface.BUTTON_POSITIVE) {
			   Map<String,String> values = new HashMap<String,String>();
			   for(Iterator<Entry<String,EditText>> it=inputs.entrySet().iterator(); it.hasNext(); ){
				   Entry<String,EditText> entry = it.next();
				   values.put( entry.getKey(), entry.getValue().getText().toString() );
			   }
			   if (onOkClicked(values)) {  
				   dialog.dismiss();  
			   }  
		  } else {  
			  onCancelClicked(dialog);  
		  }
		 dialog.dismiss();
	 }
	 
	 public abstract boolean onOkClicked(Map<String,String> values);
}  

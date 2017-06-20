package ru.led.scheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public abstract class PromptDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener 
{  
	 private final EditText input;  

	 public PromptDialog(Context context, String title, String message, String defValue) {  
		  super(context);  
		  setTitle(title);  
		  setMessage(message);  
		  
		  input = new EditText(context);
		  input.setText(defValue);
		  setView(input);
		  
		  setPositiveButton(android.R.string.ok, this);  
		  setNegativeButton(android.R.string.cancel, this);  
	 }  

	 public void onCancelClicked(DialogInterface dialog) {  
		  dialog.dismiss();  
	 }
	  
	 public void onClick(DialogInterface dialog, int which) {  
		  if (which == DialogInterface.BUTTON_POSITIVE) {  
			   if (onOkClicked(input.getText().toString())) {  
				   dialog.dismiss();  
			   }  
		  } else {  
			  onCancelClicked(dialog);  
		  }  
	 }

	 abstract public boolean onOkClicked(String input);  
}  
package ru.led.scheduler;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import java.net.URI;

/**
 * Created by Alexey.Ponimash on 21.04.2015.
 */


public class TaskEditActivity extends Activity implements View.OnClickListener {
    protected Task mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String taskId = getIntent().getStringExtra("task_id");
        mTask = ((App)getApplication()).getSchedule().getTaskByID(taskId);

        setContentView(R.layout.task_activity);

        EditText edit = (EditText) findViewById(R.id.editTaskName);
        edit.setText( mTask.getName() );

        edit = (EditText) findViewById(R.id.editTaskCommand);
        edit.setText( mTask.getCommand() );

        CheckBox chk = (CheckBox) findViewById(R.id.checkTaskType);
        chk.setChecked( mTask.getType()==Task.TASK_TYPE_WIDGET );

        ImageButton btn = (ImageButton) findViewById(R.id.btnTaskImage);

        if( mTask.getIcon()!=null )
            btn.setImageURI( Uri.parse(mTask.getIcon()) );

        btn.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( resultCode == RESULT_OK ) {
            ImageButton btn = (ImageButton) findViewById(R.id.btnTaskImage);


            btn.setImageURI(data.getData());
            mTask.setIcon( data.getData().toString() );
        }
    }

    @Override
    public void onClick(View view) {
        switch( view.getId() ) {
            case R.id.btnTaskImage:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, 1);
                break;
        }
    }
}


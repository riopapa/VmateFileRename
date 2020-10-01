package com.urrecliner.vmatefilerename;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import static com.urrecliner.vmatefilerename.MainActivity.deleteFlag;
import static com.urrecliner.vmatefilerename.MainActivity.editor;
import static com.urrecliner.vmatefilerename.MainActivity.renameFlag;
import static com.urrecliner.vmatefilerename.MainActivity.timeZone;

public class SetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);
        final EditText et = findViewById(R.id.timeZone);
        et.setText("" + timeZone);
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                timeZone = Float.parseFloat(et.getText().toString());
                editor.putFloat("timeZone", timeZone).apply();
            }
        });
        Switch swRename = findViewById(R.id.rename);
        swRename.setChecked(renameFlag);
        swRename.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                renameFlag = isChecked;
                editor.putBoolean("rename", renameFlag).apply();
                LinearLayout layout = findViewById(R.id.deleteLayout);
                layout.setVisibility((renameFlag) ? View.INVISIBLE : View.VISIBLE);
            }
        });
        Switch swDelete = findViewById(R.id.delete);
        swDelete.setChecked(deleteFlag);
        swDelete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                deleteFlag = isChecked;
                editor.putBoolean("delete", deleteFlag).apply();
            }
        });
        LinearLayout layout = findViewById(R.id.deleteLayout);
        layout.setVisibility((renameFlag) ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        EditText et = findViewById(R.id.timeZone);
        timeZone = Float.parseFloat(et.getText().toString());
        editor.putFloat("timeZone", timeZone).apply();
    }
}



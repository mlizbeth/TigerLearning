package io.valhala.tigerlearningcommons;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class SelectionActivity extends AppCompatActivity {

    private CheckBox[] options;
    private String reason = "";
    private Student student;
    private String barcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkbox_t);
        Bundle extras = getIntent().getExtras();
        barcode = extras.getString("barcode");
        init();
    }

    private void init() {
        options = new CheckBox[]{findViewById(R.id.optionBox1), findViewById(R.id.optionBox2),
                findViewById(R.id.optionBox3), findViewById(R.id.optionBox4), findViewById(R.id.optionBox5),
                findViewById(R.id.optionBox6), findViewById(R.id.optionBox7), findViewById(R.id.optionBox8),
                findViewById(R.id.optionBox10)};
        final Button b = findViewById(R.id.submitBtn);
        final EditText e = findViewById(R.id.editText);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (int x = 0; x < options.length; x++) {
                    if (options[x].isChecked()) {
                        reason += options[x].getText() + "\n";
                    } else {

                    }
                }
                if (!(e.getText().equals(""))) {
                    reason += e.getText();
                }

                student = new Student(barcode + "\n", reason);
                Toast.makeText(getApplicationContext(), student.toString(), Toast.LENGTH_LONG).show();
            }

        });
        //student = new Student(barcode, reason);
        //Toast.makeText(getApplicationContext(), student.toString(), Toast.LENGTH_LONG).show();
    }
}


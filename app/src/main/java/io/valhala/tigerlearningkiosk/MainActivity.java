package io.valhala.tigerlearningkiosk;

import android.Manifest;
import android.accounts.Account;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private final String spreadsheetId = "";
    private Account account;
    private String barcode;
    private NetHttpTransport HTTP_TRANSPORT;
    private GoogleAccountCredential credential;
    private GoogleSignInAccount signInAccount;
    private AsyncTask<String, Void, Boolean> writeTask;
    private static final int REQUEST_CODE_ASK_PERM = 1;
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final String[] REQUIRED_PERMISSION = new String[] {Manifest.permission.INTERNET, Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String APPLICATION_NAME = "Tiger Learning Commons Kiosk";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private Sheets service;
    private Student student;
    private CheckBox[] options;
    private TextView[] titles;
    private EditText otherOpt, idField;
    private Button submitBtn;
    private String reason;
    private boolean valid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        checkPermissions();


        if(account == null) {new Login().execute();}
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        student = new Student();
        idField = findViewById(R.id.editText);
        idField.setInputType(InputType.TYPE_NULL);
        idField.requestFocus();
        idField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(idField.getText().length() == 11) {
                    verifyID(idField.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
        options = new CheckBox[] {findViewById(R.id.option1), findViewById(R.id.option2),
                findViewById(R.id.option3), findViewById(R.id.option4), findViewById(R.id.option5),
                findViewById(R.id.option6), findViewById(R.id.option7), findViewById(R.id.option8),
                findViewById(R.id.option9)};
        titles = new TextView[] {findViewById(R.id.welcomeText), findViewById(R.id.titleText)};
        otherOpt = findViewById(R.id.option10);
        submitBtn = findViewById(R.id.submitBtn);
        for(int x = 0; x < options.length; x++) {
            options[x].setVisibility(View.GONE);
        }
        titles[0].setVisibility(View.VISIBLE);
        titles[1].setVisibility(View.GONE);
        otherOpt.setVisibility(View.GONE);
        submitBtn.setVisibility(View.GONE);
    }

    private void onValidId() {
        valid = true;
        reason = "";
        for (int x = 0; x < options.length; x++) {
            options[x].setVisibility(View.VISIBLE);
        }
        for (int x = 0; x < titles.length; x++) {
            if (x == 0) {
                titles[x].setVisibility(View.GONE);
            } else {
                titles[x].setVisibility(View.VISIBLE);
            }
        }
        otherOpt.setVisibility(View.VISIBLE);
        submitBtn.setVisibility(View.VISIBLE);
        submitBtn.setEnabled(true);
        idField.setVisibility(View.GONE);
        submitBtn.setOnClickListener(e -> {
            submitBtn.setEnabled(false);
            for(int x = 0; x < options.length; x++) {
                if(options[x].isChecked()) {
                    reason += options[x].getText() + "\n";
                }
            }
            if(!(otherOpt.getText().equals(""))) {
                reason += otherOpt.getText() +"\n";
            }
            student.setReason(reason);
            student.setTimeStamp();
            writeTask = new Write().execute();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        idField.requestFocus();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        for(final String permission : REQUIRED_PERMISSION) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if(result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if(!missingPermissions.isEmpty()) {
            final String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERM);
        }
        else {
            final int[] grantResults = new int[REQUIRED_PERMISSION.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERM, REQUIRED_PERMISSION, grantResults);
        }
    }
    @Override
    public void onRequestPermissionsResult(int request, @NonNull String permission[], @NonNull int[] grantResults) {
        switch(request) {
            case REQUEST_CODE_ASK_PERM:
                for(int index = permission.length - 1; index >= 0; --index) {
                    if(grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Required permission '" + permission[index] + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
        }
    }

    private void verifyID(String data) {
        barcode = "";
        String[] delimiter = {";", "?"};
        barcode = data.substring((data.indexOf(delimiter[0]) + 1), data.indexOf(delimiter[1]) - 2);
        student.setId(barcode);
        onValidId();
    }

    private void restart() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        intent.putExtra("Account", account);
        startActivity(intent);
    }

    private class Write extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String[] args) {
            try {
                service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                ValueRange append = new ValueRange().setValues(Arrays.asList(Arrays.asList(student.getId(), student.getReason(), student.getTimeStamp())));
                AppendValuesResponse aResult = service.spreadsheets().values().append(spreadsheetId, "A1", append)
                        .setValueInputOption("USER_ENTERED").setInsertDataOption("INSERT_ROWS").setIncludeValuesInResponse(true).execute();
            } catch (IOException e) {
            }
            return true;
        }

        protected void onPostExecute(Boolean status) {
            if(status == true) {
                restart();
            }
        }
    }

    private class Login extends AsyncTask<String, Void, String> {

        private Account signIn() {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope(SheetsScopes.DRIVE))
                    .requestScopes(new Scope(SheetsScopes.DRIVE_FILE))
                    .requestScopes(new Scope(SheetsScopes.SPREADSHEETS))
                    .build();
            GoogleSignInClient signInClient = GoogleSignIn.getClient(MainActivity.this, options);
            startActivityForResult(signInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
            return GoogleSignIn.getLastSignedInAccount(MainActivity.this).getAccount();
        }

        @Override
        protected String doInBackground(String[] params) {

            try {
                HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
                signInAccount = GoogleSignIn.getLastSignedInAccount(MainActivity.this);

                if (signInAccount != null)
                    account = signInAccount.getAccount();

                else
                    account = signIn();

                credential = GoogleAccountCredential.usingOAuth2(
                        MainActivity.this,
                        Arrays.asList("https://www.googleapis.com/auth/drive",
                                "https://www.googleapis.com/auth/drive.file",
                                "https://www.googleapis.com/auth/spreadsheets") //give it everything
                );

                credential.setSelectedAccount(account);
                credential.setSelectedAccountName("");

                Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                ValueRange append = new ValueRange().setValues(Arrays.asList(Arrays.asList(student.getId(), student.getReason(), student.getTimeStamp())));
                AppendValuesResponse aResult = service.spreadsheets().values().append(spreadsheetId, "A1", append)
                        .setValueInputOption("USER_ENTERED").setInsertDataOption("INSERT_ROWS").setIncludeValuesInResponse(true).execute();
            } catch (Exception e) {
            }
            return "true";
        }
    }

}

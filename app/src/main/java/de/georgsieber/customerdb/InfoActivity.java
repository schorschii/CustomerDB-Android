package de.georgsieber.customerdb;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.georgsieber.customerdb.model.Customer;
import de.georgsieber.customerdb.model.Voucher;
import de.georgsieber.customerdb.tools.ColorControl;
import de.georgsieber.customerdb.tools.CommonDialog;


public class InfoActivity extends AppCompatActivity {

    InfoActivity me = this;

    FeatureCheck mFc;

    String mRegisteredUsername = "";
    String mRegisteredPassword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // init settings
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        // init activity view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init colors
        ColorControl.updateActionBarColor(this, settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onMoreInfoClick(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://georg-sieber.de/?page=app-customerdb")));
    }

    public void onOpenAboutClick(View v) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    Dialog mRegistrationDialog = null;
    public void onCreateAccountClick(View v) {
        mRegistrationDialog = new Dialog(this);
        mRegistrationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mRegistrationDialog.setContentView(R.layout.dialog_register);
        final Button buttonRegister = mRegistrationDialog.findViewById(R.id.buttonRegisterNow);
        final EditText editTextEmail = mRegistrationDialog.findViewById(R.id.editTextEmailAddress);
        final EditText editTextChoosePassword = mRegistrationDialog.findViewById(R.id.editTextChoosePassword);
        final EditText editTextConfirmPassword = mRegistrationDialog.findViewById(R.id.editTextConfirmPassword);
        final CheckBox checkBoxAcceptTerms = mRegistrationDialog.findViewById(R.id.checkBoxAcceptCloudTerms);
        final Button buttonCloudTerms = mRegistrationDialog.findViewById(R.id.buttonCloudTerms);
        buttonCloudTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://georg-sieber.de/?page=app-customerdb-terms"));
                startActivity(browserIntent);
            }
        });
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkBoxAcceptTerms.isChecked()) {
                    CommonDialog.show(me,
                            "", getString(R.string.please_accept_privacy_policy), CommonDialog.TYPE.WARN, false
                    );
                    return;
                }

                if(!editTextChoosePassword.getText().toString().equals(editTextConfirmPassword.getText().toString())) {
                    CommonDialog.show(me,
                            getString(R.string.passwords_not_matching),
                            "", CommonDialog.TYPE.WARN, false
                    );
                    return;
                }

                mRegisteredUsername = editTextEmail.getText().toString();
                mRegisteredPassword = editTextChoosePassword.getText().toString();

                SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                if(settings.getInt("webapi-type", 0) == 2) {
                    AccountApi aa = new AccountApi(me, settings.getString("webapi-url", ""), "account.register");
                    aa.execute(editTextEmail.getText().toString(), editTextChoosePassword.getText().toString());
                } else {
                    AccountApi aa = new AccountApi(me, "account.register");
                    aa.execute(editTextEmail.getText().toString(), editTextChoosePassword.getText().toString());
                }

                buttonRegister.setEnabled(false);
                buttonRegister.setText(getString(R.string.please_wait));
            }
        });
        mRegistrationDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mRegistrationDialog.dismiss();
            }
        });
        mRegistrationDialog.show();
    }

    public void onResetPasswordClick(View v) {
        mRegistrationDialog = new Dialog(this);
        mRegistrationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mRegistrationDialog.setContentView(R.layout.dialog_reset_password);
        final Button buttonOK = mRegistrationDialog.findViewById(R.id.buttonOK);
        final EditText editTextEmail = mRegistrationDialog.findViewById(R.id.editTextEmailAddress);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                if(settings.getInt("webapi-type", 0) == 2) {
                    AccountApi aa = new AccountApi(me, settings.getString("webapi-url", ""), "account.resetpwd");
                    aa.execute(editTextEmail.getText().toString());
                } else {
                    AccountApi aa = new AccountApi(me, "account.resetpwd");
                    aa.execute(editTextEmail.getText().toString());
                }
                buttonOK.setEnabled(false);
                buttonOK.setText(getString(R.string.please_wait));
            }
        });
        mRegistrationDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mRegistrationDialog.dismiss();
            }
        });
        mRegistrationDialog.show();
    }

    public void onDeleteAccountClick(View v) {
        mRegistrationDialog = new Dialog(this);
        mRegistrationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mRegistrationDialog.setContentView(R.layout.dialog_delete_account);
        final Button buttonOK = mRegistrationDialog.findViewById(R.id.buttonOK);
        final EditText editTextEmail = mRegistrationDialog.findViewById(R.id.editTextEmailAddress);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                if(settings.getInt("webapi-type", 0) == 2) {
                    AccountApi aa = new AccountApi(me, settings.getString("webapi-url", ""), "account.delete");
                    aa.execute(editTextEmail.getText().toString());
                } else {
                    AccountApi aa = new AccountApi(me, "account.delete");
                    aa.execute(editTextEmail.getText().toString());
                }
                buttonOK.setEnabled(false);
                buttonOK.setText(getString(R.string.please_wait));
            }
        });
        mRegistrationDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mRegistrationDialog.dismiss();
            }
        });
        mRegistrationDialog.show();
    }

    public void onServerGithubClick(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/schorschii/customerdb-server"));
        startActivity(browserIntent);
    }

    void handleAccountCreationSuccess() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("webapi-type", settings.getInt("webapi-type", 0)==2 ? 2 : 1);
        editor.putString("webapi-username", mRegisteredUsername);
        editor.putString("webapi-password", mRegisteredPassword);
        editor.apply();
        CommonDialog.show(this,
                getString(R.string.registration_succeeded),
                getString(R.string.registration_succeeded_text),
                CommonDialog.TYPE.OK, false
        );
        if(mRegistrationDialog != null)
            mRegistrationDialog.dismiss();

    }
    void handleAccountCreationError(String message) {
        CommonDialog.show(this,
                getString(R.string.registration_failed),
                message,
                CommonDialog.TYPE.FAIL, false
        );
        if(mRegistrationDialog != null) {
            Button buttonRegister = mRegistrationDialog.findViewById(R.id.buttonRegisterNow);
            buttonRegister.setEnabled(true);
            buttonRegister.setText(getString(R.string.register_now2));
        }
    }

    void handleAccountSuccess() {
        CommonDialog.show(this,
                getString(R.string.success),
                getString(R.string.please_check_inbox),
                CommonDialog.TYPE.OK, false
        );
        if(mRegistrationDialog != null)
            mRegistrationDialog.dismiss();

    }
    void handleAccountError(String message) {
        CommonDialog.show(this,
                getString(R.string.error),
                message,
                CommonDialog.TYPE.FAIL, false
        );
        if(mRegistrationDialog != null)
            mRegistrationDialog.dismiss();
    }


    public class AccountApi extends AsyncTask<String, Void, String> {

        private WeakReference<InfoActivity> mInfoActivityReference;

        private String mApiUrl;
        private String mMethod;

        AccountApi(InfoActivity context, String method) {
            mInfoActivityReference = new WeakReference<>(context);
            mApiUrl = CustomerDatabaseApi.MANAGED_API;
            mMethod = method;
        }
        AccountApi(InfoActivity context, String url, String method) {
            mInfoActivityReference = new WeakReference<>(context);
            mApiUrl = url;
            mMethod = method;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                if(mMethod.equals("account.register") && params.length == 2) {
                    register(params[0], params[1]);
                    return null;
                } else if(mMethod.equals("account.resetpwd") && params.length == 1) {
                    resetPassword(params[0]);
                    return null;
                } else if(mMethod.equals("account.delete") && params.length == 1) {
                    delete(params[0]);
                    return null;
                } else {
                    throw new Error("Invalid Method or Missing Parameter");
                }
            } catch(Exception e) {
                return e.getMessage();
            }
        }

        private void register(String email, String password) throws Exception {
            try {
                JSONObject jaccparams = new JSONObject();
                jaccparams.put("email", email);
                jaccparams.put("password", password);

                JSONObject jroot = new JSONObject();
                jroot.put("jsonrpc", "2.0");
                jroot.put("id", 1);
                jroot.put("method", "account.register");
                jroot.put("params", jaccparams);

                //Log.e("API", jroot.toString());
                String result = openConnection(jroot.toString());
                //Log.e("API", result);

                try {
                    JSONObject jresult = new JSONObject(result);
                    if(jresult.isNull("result") || !jresult.getBoolean("result")) {
                        throw new Exception(jresult.getString("error"));
                    }
                } catch(JSONException e) {
                    throw new Exception(result);
                }
            } catch(JSONException ex) {
                throw new Exception(ex.getMessage());
            }
        }

        private void resetPassword(String email) throws Exception {
            try {
                JSONObject jaccparams = new JSONObject();
                jaccparams.put("email", email);

                JSONObject jroot = new JSONObject();
                jroot.put("jsonrpc", "2.0");
                jroot.put("id", 1);
                jroot.put("method", "account.resetpwd");
                jroot.put("params", jaccparams);

                //Log.e("API", jroot.toString());
                String result = openConnection(jroot.toString());
                //Log.e("API", result);

                try {
                    JSONObject jresult = new JSONObject(result);
                    if(jresult.isNull("result") || !jresult.getBoolean("result")) {
                        throw new Exception(jresult.getString("error"));
                    }
                } catch(JSONException e) {
                    throw new Exception(result);
                }
            } catch(JSONException ex) {
                throw new Exception(ex.getMessage());
            }
        }

        private void delete(String email) throws Exception {
            try {
                JSONObject jaccparams = new JSONObject();
                jaccparams.put("email", email);

                JSONObject jroot = new JSONObject();
                jroot.put("jsonrpc", "2.0");
                jroot.put("id", 1);
                jroot.put("method", "account.delete");
                jroot.put("params", jaccparams);

                //Log.e("API", jroot.toString());
                String result = openConnection(jroot.toString());
                //Log.e("API", result);

                try {
                    JSONObject jresult = new JSONObject(result);
                    if(jresult.isNull("result") || !jresult.getBoolean("result")) {
                        throw new Exception(jresult.getString("error"));
                    }
                } catch(JSONException e) {
                    throw new Exception(result);
                }
            } catch(JSONException ex) {
                throw new Exception(ex.getMessage());
            }
        }

        private String openConnection(String send) throws Exception {
            String text = "";
            BufferedReader reader = null;

            try {

                URL url = new URL(mApiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(send);
                wr.flush();

                int statusCode = conn.getResponseCode();
                if(statusCode == 200)
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                else
                    reader = new BufferedReader(new InputStreamReader((conn).getErrorStream()));
                StringBuilder sb2 = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb2.append(line).append("\n");
                }
                text = sb2.toString();

                if(text.equals("")) text = statusCode + " " + conn.getResponseMessage();

            } catch (Exception ex) {

                throw new Exception(ex.getMessage());

            } finally {

                if(reader != null) try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            return text;
        }

        @Override
        protected void onPostExecute(String result) {
            // get a reference to the activity if it is still there
            InfoActivity activity = mInfoActivityReference.get();
            if(activity == null) return;
            if(result == null) {
                if(mMethod.equals("account.register")) {
                    activity.handleAccountCreationSuccess();
                } else {
                    activity.handleAccountSuccess();
                }
            } else {
                if(mMethod.equals("account.register")) {
                    activity.handleAccountCreationError(result);
                } else {
                    activity.handleAccountError(result);
                }
            }
        }

    }

}

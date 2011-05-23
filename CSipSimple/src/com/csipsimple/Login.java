package com.csipsimple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.LoginResponse;

public class Login extends Activity {

    private String loginUrl = "https://ssl7.net/oss/auth?";

    private ProgressDialog progressDialog;
    private Context context;
    private String errorMessage;
    private LoginResponse loginResponse;
    private EditText emailEditText;
    private EditText passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_home_main);

        emailEditText = (EditText) findViewById(R.id.login_email);
        passwordEditText = (EditText) findViewById(R.id.login_password);

        TextView newAccountText = (TextView) findViewById(R.id.register_label);
        String text = "<a href=\"http://www.level7systems.com\">Register now</a>";// getString(R.string.login_register_label);
        newAccountText.setText(Html.fromHtml(text));
        newAccountText.setMovementMethod(LinkMovementMethod.getInstance());

        TextView forgotPasswordText = (TextView) findViewById(R.id.login_forgot_password);
        text = "<a href=\"http://www.level7systems.com\">Click here</a>";// getString(R.string.login_forgot_password_label);
        forgotPasswordText.setText(Html.fromHtml(text));
        forgotPasswordText.setMovementMethod(LinkMovementMethod.getInstance());

        context = this;

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (emailEditText.getText().toString().equalsIgnoreCase("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setPositiveButton(R.string.ok, null);
                    builder.setTitle(R.string.login_error);
                    builder.setMessage(R.string.empty_email_message);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return;
                }
                if (passwordEditText.getText().toString().equalsIgnoreCase("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setPositiveButton(R.string.ok, null);
                    builder.setTitle(R.string.login_error);
                    builder.setMessage(R.string.empty_pas_message);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return;
                }
                if (!isConnected()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setPositiveButton(R.string.ok, null);
                    builder.setTitle(R.string.no_network_connection_title);
                    builder.setMessage(R.string.no_network_connection_msg);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return;
                }
                new LoginAsyncTask().execute(new String[] { emailEditText.getText().toString(), passwordEditText.getText().toString()});
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getInt("accountDbId", -1) != -1) {
            Intent i = new Intent(this, SipHome.class);
            startActivity(i);
            return;
        }
    }

    private void doLogin(String email, String password) {
        String url = loginUrl + "login=" + email + "&password=" + password;

        InputStream is = null;
        try {

            URL myUrl = new URL(url);
            System.out.println(url);
            is = myUrl.openStream();
//            System.out.println(inputStreamToString(is));

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            Document doc = docBuilder.parse(is);
            doc.getDocumentElement().normalize();

            loginResponse = new LoginResponse();

            NodeList nodeList = doc.getElementsByTagName("sso");
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element ssoElement = (Element) node;
                    NodeList ssoElementChilds = ssoElement.getElementsByTagName("d");
                    if (ssoElementChilds.getLength() == 0) {
                        errorMessage = getString(R.string.invalid_username_password);
                        return;
                    }
                    for (int i = 0; i < ssoElementChilds.getLength(); i++) {
                        Element e = (Element) ssoElementChilds.item(i);
                        if (e.getAttribute("k").equals("sip.auth.userid")) {
                            loginResponse.setUserId(e.getAttribute("v"));
                        } else if (e.getAttribute("k").equals("sip.auth.password")) {
                            loginResponse.setPassword(e.getAttribute("v"));
                        } else if (e.getAttribute("k").equals("sip.auth.realm")) {
                            loginResponse.setRealm(e.getAttribute("v"));
                        } else if (e.getAttribute("k").equals("sip.address.name")) {
                            loginResponse.setName(e.getAttribute("v"));
                        } else if (e.getAttribute("k").equals("sip.address.displayname")) {
                            loginResponse.setDisplayName(e.getAttribute("v"));
                        } else if (e.getAttribute("k").equals("sip.address.server.host")) {
                            loginResponse.setHost(e.getAttribute("v"));
                        } else if (e.getAttribute("k").equals("address.server.port")) {
                            loginResponse.setPort(e.getAttribute("v"));
                        }
                    }
                }
            }

            if (loginResponse.getUserId() != null) {
                saveAccount(loginResponse);
            }

        } catch (MalformedURLException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
        } catch (IOException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
        } catch (SAXException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private void saveAccount(LoginResponse loginResponse) {
        DBAdapter database = new DBAdapter(this);

        database.open();

        SipProfile account = database.getAccount(-1);
        account.display_name = getString(R.string.app_name);
        account.acc_id = loginResponse.getDisplayName() + " <sip:" + loginResponse.getUserId() + "@" + loginResponse.getHost() + ">";
        account.reg_uri = "sip:" + loginResponse.getHost() + ":" + loginResponse.getPort();
        account.realm = loginResponse.getRealm();
        account.username = loginResponse.getName();
        account.data = loginResponse.getPassword();
        account.transport = SipProfile.TRANSPORT_UDP;
        account.publish_enabled = 1;
        account.active = true;

        if (account.id == SipProfile.INVALID_ID) {
            account.id = (int) database.insertAccount(account);
        } else {
            database.updateAccount(account);
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("accountDbId", account.id);
        editor.putString("accountId", account.acc_id);
        editor.putString("userId", account.username);
        editor.commit();

        database.close();
    }

    public boolean isConnected() {
        ConnectivityManager con = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = con.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private class LoginAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, "", getString(R.string.login_wait_message), true);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            if (errorMessage != null && errorMessage.length() > 0) {
                Toast toast = Toast.makeText(context, errorMessage, Toast.LENGTH_LONG);
                toast.show();
            } else {
                Intent i = new Intent(context, SipHome.class);
                Bundle b = new Bundle();
                b.putSerializable("loginResponse", loginResponse);
                startActivity(i);
                finish();
            }

        }

        @Override
        protected Void doInBackground(String... params) {
            errorMessage = "";
            doLogin(params[0], params[1]);
            return null;
        }

    }
    
    // Fast Implementation
    private StringBuilder inputStreamToString(InputStream is) throws IOException {
        String line = "";
        StringBuilder total = new StringBuilder();

        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        // Read response until the end
        while ((line = rd.readLine()) != null) {
            total.append(line);
        }

        rd.close();

        // Return full string
        return total;
    }

}

package com.gotobus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class SignupActivity extends AppCompatActivity {

    Spinner busType;
    String[] busTypes;
    ArrayAdapter<String> adapter;

    EditText phoneInput, nameInput, emailInput, busNumberInput;

    Button signupButton;

    String baseUrl;

    String phone, name, email, busNumber, type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        baseUrl = getResources().getString(R.string.base_url);

        busTypes = new String[]{"Bus Type", "Sleeper", "AC", "Volvo"};
        adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, busTypes);

        busType = findViewById(R.id.bus_type);
        busType.setAdapter(adapter);

        phoneInput = findViewById(R.id.phone);
        nameInput = findViewById(R.id.name);
        emailInput = findViewById(R.id.email);
        busNumberInput = findViewById(R.id.bus_number);
        signupButton = findViewById(R.id.signup);


        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                phone = phoneInput.getText().toString();
                name = nameInput.getText().toString();
                email = emailInput.getText().toString();
                busNumber = busNumberInput.getText().toString();
                type = busType.getSelectedItem().toString();

                if (!type.equals("Bus Type")) {

                    AndroidNetworking.post(baseUrl + "/signup.php")
                            .setOkHttpClient(NetworkCookies.okHttpClient)
                            .addBodyParameter("phone", phone)
                            .addBodyParameter("name", name)
                            .addBodyParameter("email", email)
                            .addBodyParameter("bus_number", busNumber)
                            .addBodyParameter("bus_type", type)
                            .setPriority(Priority.MEDIUM)
                            .build()
                            .getAsJSONObject(new JSONObjectRequestListener() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        JSONObject result = response.getJSONObject("result");
                                        boolean success = Boolean.parseBoolean(result.get("success").toString());
                                        if (success) {
                                            Intent intent = new Intent(getApplicationContext(), OTPActivity.class);
                                            intent.putExtra("phone", phone);
                                            startActivity(intent);
                                        } else {
                                            String message = result.get("message").toString();
                                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onError(ANError error) {
                                    Toast.makeText(getApplicationContext(), error.getErrorBody(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        });

    }
}

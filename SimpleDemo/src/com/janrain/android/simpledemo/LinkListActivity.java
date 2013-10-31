package com.janrain.android.simpledemo;

/*
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
* Copyright (c) 2011, Janrain, Inc.
*
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification,
* are permitted provided that the following conditions are met:
*
* * Redistributions of source code must retain the above copyright notice, this
* list of conditions and the following disclaimer.
* * Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation and/or
* other materials provided with the distribution.
* * Neither the name of the Janrain, Inc. nor the names of its
* contributors may be used to endorse or promote products derived from this
* software without specific prior written permission.
*
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.janrain.android.Jump;
import com.janrain.android.capture.Capture;
import com.janrain.android.capture.CaptureApiConnection;
import com.janrain.android.capture.CaptureApiError;
import com.janrain.android.capture.CaptureRecord;
import com.janrain.android.engage.JREngageDelegate;
import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRActivityObject;
import com.janrain.android.engage.types.JRDictionary;
import com.janrain.android.utils.LogUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class LinkListActivity extends ListActivity {
    private static final String TAG = ListActivity.class.getSimpleName();
    private static LinkAccountsAdapter mAdapter;
    private final MyCaptureApiResultHandler captureApiResultHandler = new MyCaptureApiResultHandler();
    ListView link_account;
    TextView mIdentifier;
    ImageView unlinkAccount;
    private Button mLinkAccount;
    private int position;
    private boolean link_unlink;
    private JREngageDelegate mJREngageDelegate = new JREngageDelegate() {
        public void jrEngageDialogDidFailToShowWithError(JREngageError error) {
            String message = "Simpledemo:\nJREngage dialog failed to show.\nError: " +
                    ((error == null) ? "unknown" : error.getMessage());
            Toast.makeText(LinkListActivity.this, message, Toast.LENGTH_LONG).show();
        }

        public void jrAuthenticationDidSucceedForUser(JRDictionary authInfo, String provider) {
            String deviceToken = authInfo.getAsString("device_token");
            JRDictionary profile = (authInfo == null) ? null : authInfo.getAsDictionary("profile");
            String identifier = profile.getAsString("identifier");
            String displayName = (profile == null) ? null : profile.getAsString("displayName");
            String message = "Authentication successful" + ((TextUtils.isEmpty(displayName))
                    ? "" : (" for user: " + displayName));
            showResultDialog(message);
        }

        @Override
        public void jrAuthenticationDidSucceedForLinkAccount(JRDictionary auth_info, String provider) {
            //To change body of implemented methods use File | Settings | File Templates.
            String token = auth_info.getAsString("token");
            link_unlink = true;
            Jump.performLinkAccount(token, captureApiResultHandler);
        }

        public void jrAuthenticationDidReachTokenUrl(String tokenUrl,
                                                     HttpResponseHeaders response,
                                                     String tokenUrlPayload,
                                                     String provider) {
            org.apache.http.Header[] headers = response.getHeaders();
            org.apache.http.cookie.Cookie[] cookies = response.getCookies();
            String firstCookieValue = response.getHeaderField("set-cookie");
            showResultDialog("Token URL response", tokenUrlPayload);
        }

        private void showResultDialog(String title, String message) {
            // This shouldn't be done here because LinkListActivity isn't displayed (resumed?) when this is
            // called but it works most of the time.
            (new AlertDialog.Builder(LinkListActivity.this)).setTitle(title)
                    .setMessage(message)
                    .setNeutralButton("OK", null)
                    .show();
        }

        private void showResultDialog(String title) {
            showResultDialog(title, null);
        }

        public void jrAuthenticationDidNotComplete() {
            showResultDialog("Authentication did not complete");
        }

        public void jrAuthenticationDidFailWithError(JREngageError error, String provider) {
            String message = ((error == null) ? "unknown" : error.getMessage());

            showResultDialog("Authentication Failed.", message);
        }

        public void jrAuthenticationCallToTokenUrlDidFail(String tokenUrl,
                                                          JREngageError error,
                                                          String provider) {
            showResultDialog("Failed to reach token URL");
        }

        public void jrSocialDidNotCompletePublishing() {
            showResultDialog("Sharing did not complete");
        }

        public void jrSocialDidCompletePublishing() {
            showResultDialog("Sharing did complete");
        }

        public void jrSocialDidPublishJRActivity(JRActivityObject activity, String provider) {
            Toast.makeText(LinkListActivity.this, "Activity shared", Toast.LENGTH_LONG).show();
        }

        public void jrSocialPublishJRActivityDidFail(JRActivityObject activity,
                                                     JREngageError error,
                                                     String provider) {
            Toast.makeText(LinkListActivity.this, "Activity failed to share", Toast.LENGTH_LONG).show();
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[onCreate]");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linked_account_listview);
        mLinkAccount = (Button) findViewById(R.id.btn_link_account);
        mLinkAccount.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Jump.getSignedInUser() != null) {
                    Jump.showSocialSignInDialog(LinkListActivity.this, null, true, mJREngageDelegate);

                } else {
                    LinkListActivity.this.startActivity(new Intent(LinkListActivity.this,
                            MainActivity.class));
                }
            }
        });
        link_account = (ListView) findViewById(android.R.id.list);
        validateSignedInUser();
        link_account.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                setPosition(position);
                mIdentifier = (TextView) v.findViewById(R.id.row_profile_linkaccount_label);
                unlinkAccount = (ImageView) v.findViewById(R.id.row_unlink_btn);
                unlinkAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder b = new AlertDialog.Builder(LinkListActivity.this);
                        b.setTitle("Unlink Account");
                        b.setMessage("Do you want to unlink the account?");
                        b.setPositiveButton("Unlink", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                link_unlink = false;
                                if (Jump.getSignedInUser().hasPassword()) {
                                    if (link_account.getChildCount() > 1) {
                                        Jump.performUnlinkAccount(String.valueOf(mIdentifier.getText()),
                                                captureApiResultHandler);
                                    } else {
                                        Toast.makeText(LinkListActivity.this,
                                                "Cannot unlink this account",
                                                Toast.LENGTH_LONG)
                                                .show();
                                    }
                                } else {
                                    Jump.performUnlinkAccount(String.valueOf(mIdentifier.getText()),
                                            captureApiResultHandler);
                                }
                                dialog.dismiss();
                            }
                        });
                        b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        });
                        b.show();
                    }
                });
            }
        });
    }

    public void loadLinkedUnlinkedAccounts() throws JSONException {
        Jump.performFetchCaptureData(new Jump.CaptureApiResultHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                //To change body of implemented methods use File | Settings | File Templates.
                ArrayList<LinkData> linkUnlinkResults = new ArrayList<LinkData>();
                JSONObject json = response;
                try {
                    JSONArray profiles = json.getJSONObject("result").getJSONArray("profiles");
                    for (int i = 0; i < profiles.length(); i++) {
                        JSONObject profileData = profiles.getJSONObject(i);
                        LogUtils.loge(profileData.getString("domain"));
                        LinkData linkedRecords = new LinkData(profileData.getString("identifier"),
                                profileData.getString("domain"));
                        linkUnlinkResults.add(linkedRecords);
                        LogUtils.loge(profileData.getString("identifier"));
                    }
                    mAdapter = new LinkAccountsAdapter(LinkListActivity.this, linkUnlinkResults);
                    link_account.setAdapter(mAdapter);
                } catch (JSONException e) {
                    LogUtils.loge("Error parsing data " + e.toString());
                }
            }

            @Override
            public void onFailure(CaptureAPIError error) {
                Toast.makeText(LinkListActivity.this,
                        "Account LinkUnlink Failed.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void validateSignedInUser() {
        if (Jump.getSignedInUser() != null && Jump.getAccessToken() != null){
            try {
                loadLinkedUnlinkedAccounts();
            } catch (JSONException e) {
                Toast.makeText(LinkListActivity.this,
                        "Account LinkUnlink Failed.",
                        Toast.LENGTH_LONG).show(); //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private class MyCaptureApiResultHandler implements Jump.CaptureApiResultHandler {
        public void onSuccess(JSONObject response) {
            try {
                loadLinkedUnlinkedAccounts();
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public void onFailure(CaptureAPIError error) {
            Toast.makeText(LinkListActivity.this, "Account LinkUnlink Failed.", Toast.LENGTH_LONG).show();
        }
    }

}
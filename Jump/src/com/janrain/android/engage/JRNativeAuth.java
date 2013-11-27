/*
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Copyright (c) 2011, Janrain, Inc.
 *
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *  * Neither the name of the Janrain, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

package com.janrain.android.engage;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import com.janrain.android.engage.session.JRProvider;
import com.janrain.android.engage.session.JRSession;
import com.janrain.android.engage.types.JRDictionary;
import com.janrain.android.utils.ApiConnection;
import com.janrain.android.utils.UiUtils;
import org.json.JSONObject;

public enum JRNativeAuth {
    INSTANCE;

    private NativeProvider currentProvider = null;

    public static boolean canHandleProvider(JRProvider provider) {
        if (provider.getName().equals("facebook") && NativeFacebook.canHandleAuthentication()) return true;

        return false;
    }

    public static void startAuthOnProvider(JRProvider provider, Activity fromActivity,
                                           NativeAuthCallback completion) {
        if (provider.getName().equals("facebook")) {
            INSTANCE.currentProvider = new NativeFacebook(completion, fromActivity);
        } else {
            throw new RuntimeException("Unexpected native auth provider " + provider);
        }

        INSTANCE.currentProvider.startAuthentication();
    }

    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        NativeFacebook.onActivityResult(activity, requestCode, resultCode, data);
    }

    public static enum NativeAuthError {
        CANNOT_INVOKE_FACEBOOK_OPEN_SESSION_METHODS,
        FACEBOOK_SESSION_IS_CLOSED,
        ENGAGE_ERROR,
        LOGIN_CANCELED
    }

    public static interface NativeAuthCallback {
        public void onSuccess(JRDictionary payload);
        public void onFailure(String message, NativeAuthError errorCode, Exception exception);
    }

    /*package*/ static abstract class NativeProvider {
        /*package*/ NativeAuthCallback completion;
        /*package*/ Activity fromActivity;

        /*package*/ NativeProvider(NativeAuthCallback callback, Activity activity) {
            completion = callback;
            fromActivity = activity;
        }

        /*package*/ static Boolean canHandleAuthentication() {
            return false;
        }

        /*package*/ abstract String provider();

        /*package*/ abstract void startAuthentication();

        /*package*/ void getAuthInfoTokenForAccessToken(String accessToken) {
            final Dialog progressDialog = UiUtils.getProgressDialog(fromActivity);

            ApiConnection.FetchJsonCallback handler = new ApiConnection.FetchJsonCallback() {
                public void run(JSONObject json) {
                    progressDialog.dismiss();

                    String status = json.optString("stat");

                    if (json == null || json.optString("stat") == null || !json.optString("stat").equals("ok")) {
                        completion.onFailure("Bad Json: " + json, NativeAuthError.ENGAGE_ERROR, null);
                        return;
                    }

                    String auth_token = json.optString("token");

                    JRDictionary payload = new JRDictionary();
                    payload.put("token", auth_token);
                    payload.put("auth_info", new JRDictionary());

                    completion.onSuccess(payload);
                }
            };

            progressDialog.show();

            ApiConnection connection =
                    new ApiConnection(JRSession.getInstance().getRpBaseUrl() + "/signin/oauth_token");

            connection.addAllToParams("token", accessToken, "provider", provider());
            connection.fetchResponseAsJson(handler);

        }

    }
}


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
import com.janrain.android.utils.LogUtils;
import com.janrain.android.utils.UiUtils;
import org.json.JSONObject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JRNativeAuth {

    // XXX the <?> fixes a warning when getMethod is called
    //     unchecked call to getMethod(java.lang.String,java.lang.Class<?>...)
    //         as a member of the raw type java.lang.Class
    private static Class<?> fbSessionClass;
    private static Class fbCallbackClass;
    private static Class fbCanceledExceptionClass;

    public static boolean canHandleProvider(JRProvider provider) {
        return provider.getName().equals("facebook") && loadNativeFacebookDependencies();
    }

    public static void startAuthOnProvider(JRProvider provider, Activity fromActivity,
                                           NativeAuthCallback completion) {
        if (provider.getName().equals("facebook")) {
            fbNativeAuthWithCompletion(fromActivity, completion);
        }
    }

    public static void facebookOnActivityResult(
            Activity activity, int requestCode, int resultCode, Intent data) {

        if (fbSessionClass != null) {
            try {
                Method getActiveSession = fbSessionClass.getMethod("getActiveSession");
                Object session = getActiveSession.invoke(fbSessionClass);

                Method onActivityResult = fbSessionClass.getMethod(
                        "onActivityResult", Activity.class, int.class, int.class, Intent.class);

                onActivityResult.invoke(session, activity, requestCode, resultCode, data);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean loadNativeFacebookDependencies() {
        ClassLoader classLoader = JRNativeAuth.class.getClassLoader();
        try {
            fbSessionClass = classLoader.loadClass("com.facebook.Session");
            fbCallbackClass = classLoader.loadClass("com.facebook.Session$StatusCallback");
            fbCanceledExceptionClass =
                classLoader.loadClass("com.facebook.FacebookOperationCanceledException");
        } catch (ClassNotFoundException e) {
            LogUtils.logd("Could not load Native Facebook SDK: " + e);

            return false;
        }

        return true;
    }

    private static void fbNativeAuthWithCompletion(
            Activity fromActivity, NativeAuthCallback completion) {
        Object fbCallback = getFacebookCallBack(fromActivity, completion);
        NativeAuthError authError = NativeAuthError.CANNOT_INVOKE_FACEBOOK_OPEN_SESSION_METHODS;

        try {
            Method getActiveSession = fbSessionClass.getMethod("getActiveSession");
            Object session = getActiveSession.invoke(fbSessionClass);

            if (session != null && isFacebookSessionOpened(session)) {
                String accessToken = getFacebookAccessToken(session);
                getAuthInfoTokenForFacebookAccessToken(fromActivity, accessToken, "facebook", completion);
            } else {
                Method openActiveSession = fbSessionClass.getMethod("openActiveSession",
                        Activity.class, boolean.class, fbCallbackClass);

                openActiveSession.invoke(fbSessionClass, fromActivity, true, fbCallback);
            }
        } catch (NoSuchMethodException e) {
            completion.onFailure("Could not open Facebook Session", authError, e);
        } catch (InvocationTargetException e) {
            completion.onFailure("Could not open Facebook Session", authError, e);
        } catch (IllegalAccessException e) {
            completion.onFailure("Could not open Facebook Session", authError, e);
        }
    }

    private static Object getFacebookCallBack(
            final Activity fromActivity, final NativeAuthCallback completion) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                if (method.getName().equals("call")) {
                    Object fbSession = objects[0],
                           sessionState = objects[1];
                    Exception exception = (Exception) objects[2];

                    if (isFacebookSessionOpened(fbSession)) {
                        String accessToken = getFacebookAccessToken(fbSession);
                        getAuthInfoTokenForFacebookAccessToken(fromActivity, accessToken, "facebook", completion);
                    } else if (isFacebookSessionClosed(fbSession)){
                        if (fbCanceledExceptionClass.isInstance(exception)) {
                            completion.onFailure(
                                    "Facebook login canceled",
                                    NativeAuthError.LOGIN_CANCELED,
                                    exception);
                        } else {
                            completion.onFailure(
                                    "Could not open Facebook Session",
                                    NativeAuthError.FACEBOOK_SESSION_IS_CLOSED,
                                    exception);
                        }
                    }
                }
                return null;
            }
        };
        return Proxy.newProxyInstance(
                fbCallbackClass.getClassLoader(), new Class[] { fbCallbackClass }, handler);
    }

    private static boolean isFacebookSessionOpened(Object session) {
        return getBoolForFbSessionAndMethod(session, "isOpened");
    }

    private static boolean isFacebookSessionClosed(Object session) {
        return getBoolForFbSessionAndMethod(session, "isClosed");
    }

    private static boolean getBoolForFbSessionAndMethod(Object session, String methodName) {
        boolean out = false;

        try {
            Method isOpened = session.getClass().getMethod(methodName);
            Boolean result = (Boolean) isOpened.invoke(session.getClass().cast(session));
            out = result.booleanValue();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return out;
    }

    private static String getFacebookAccessToken(Object session) {
        String out = null;

        try {
            Method getAccessToken = session.getClass().getMethod("getAccessToken");
            out = (String) getAccessToken.invoke(session.getClass().cast(session));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    private static void getAuthInfoTokenForFacebookAccessToken(
            Activity fromActivity, String accessToken, String provider, final NativeAuthCallback completion) {

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

        connection.addAllToParams("token", accessToken, "provider", provider);
        connection.fetchResponseAsJson(handler);
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
}


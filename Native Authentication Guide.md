# Engage Custom Provider Guide

This guide describes the process of integrating with native Android authentication systems. The Engage library
has historically supported authentication by means of a WebView running a traditional web OAuth flow. Support
is now introduced for authentication by means of native identity-provider libraries.

## Supported Providers

- Facebook

Google+ support is coming soon, Twitter is being investigated.

Native authentication is supported by the Engage Library, and is compatible with both the Engage-only and
Engage-and-Capture deployments.

At this time native authentication is available for authentication only, and not for social-identity-resource
authorization (e.g. sharing.)

## 10,000' View
1. Configure the native authentication framework
2. Start JUMP sign-in or Engage authentication
3. The Janrain library will delegate the authentication to the native authentication framework
4. The Janrain library delegate message will fire when native authentication completes

## Configure the Native Authentication Framework


Follow the steps for creating a new Android project in the "Getting Started with the Facebook SDK for Android"
guide at [developers.facebook.com](developers.facebook.com). Stop before the "A minimum viable social
application" section.

Make sure that both your Android application and Engage are configured to use the same Facebook Application
App ID.


Import the Facebook library in the activity that you will be launching JUMP from.

    import com.facebook.Session;

Override the `onActivityResult` method in your activity to delegate to Facebook. This is required when
skipping the provider list and launching authentication directly from your activity.

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

## Begin Sign-In or Authentication

Start authentication or sign-in as normal (described in 'Jump\_Integration\_Guide.md' and
'Engage\_Only\_Integration\_Guide.md'.) If the Facebook Android SDK is compiled into your app, it will be used
to perform all Facebook authentication.

## Release Builds

If you plan to use ProGuard on your release builds add the following to your ProGuard configuration file

    -keep class com.facebook.** {*;}

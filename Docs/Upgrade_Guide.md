# JUMP Android Upgrade Guide

This guide describes the steps required to upgrade from different versions of the library.

## Generalized Upgrade Process

1. Remove **Jump** or **JREngage** modules from your project.
2. Follow the steps in **Jump_Integration_Guide.md**

## Upgrading v2.0.1-v2.0.12 to v4.2.0

1. Remove the **JREngage** module from your project.
2. Follow the steps in **Engage_Only_Integration_Guide.md** (v2.0.1 did not support Capture.)

       **Note:** Be sure to update your `AndroidManifest.xml`.

## Solutions

* **java: package com.janrain.android.engage.R does not exist**

    Use `R` instead.

* **java: package com.janrain.android.engage.utils does not exist**

    Import `com.janrain.android.utils.PrefUtils` and change references to `Prefs` to `PrefUtils`.

* **java: reference to showAuthenticationDialog is ambiguous, both method
    showAuthenticationDialog(android.app.Activity,java.lang.String) in com.janrain.android.engage.JREngage and
    method showAuthenticationDialog(java.lang.Boolean,java.lang.String) in com.janrain.android.engage.JREngage
    match**

    `showAuthenticationDialg(Boolean, String)` has been deprecated, use
    `showAuthenticationDialog(Activity, String)` instead and pass in the Activity that is launching the
     authentication as the Activity.

     For example in the old version of our SimpleDemo we had
     `mEngage.showAuthenticationDialog(null, "facebook")` in `MainActivity`'s `onCreate`. To upgrade we
     replaced it with `mEngage.showAuthenticationDialog(MainActivity.this, "facebook")`.
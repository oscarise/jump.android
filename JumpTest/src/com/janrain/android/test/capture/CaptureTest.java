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

package com.janrain.android.test.capture;

import android.test.AndroidTestCase;
import android.util.Pair;
import com.janrain.android.Jump;
import com.janrain.android.capture.Capture;
import com.janrain.android.capture.CaptureApiConnection;
import com.janrain.android.capture.CaptureRecord;
import org.json.JSONObject;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.janrain.android.test.Asserts.assertRequestParamsEqual;
public class CaptureTest extends AndroidTestCase {

    @Test
    public void test_updateUserProfile() throws Exception {
        Map<String, Object> flow = new HashMap<String, Object>() {{
            put("version", "123456");
            put("fields", new HashMap<String, Map>() {{
                put("editProfileForm", new HashMap<String, List>() {{
                    put("fields", Arrays.asList(new String[]{"name"}));
                }});
                put("name", new HashMap<String, Map>() {{
                    put("schemaId", new HashMap<String, String>() {{
                        put("firstName", "givenName");
                        put("lastName", "familyName");
                    }});
                }});
            }});
        }};

        JSONObject userJson = new JSONObject("{ \"givenName\" : \"David\", " +
                                             "\"familyName\" : \"Bowman\" }");
        Class<?>[] paramTypes = new Class<?>[]{JSONObject.class, String.class, String.class};
        Object[] params = new Object[]{userJson, "12345689", "secret"};
        CaptureRecord user = Whitebox.invokeConstructor(CaptureRecord.class, paramTypes, params);
        Whitebox.setInternalState(user, "accessToken", "12345abcdef");

        Object state = Whitebox.getInternalState(Jump.class, "state");
        Whitebox.setInternalState(state, "captureClientId", "abc123");
        Whitebox.setInternalState(state, "captureLocale", "US-en");
        Whitebox.setInternalState(state, "captureDomain", "base.uri");
        Whitebox.setInternalState(state, "captureFlowName", "standard_flow");
        Whitebox.setInternalState(state, "captureFlow", flow);
        Whitebox.setInternalState(state, "captureEditUserProfileFormName", "editProfileForm");

        String expectedUri = "https://base.uri/oauth/update_profile_native";
        Set<Pair<String, String>> expectedParams = new HashSet<Pair<String, String>>();
        expectedParams.add(new Pair<String, String>("client_id", "abc123"));
        expectedParams.add(new Pair<String, String>("access_token", "12345abcdef"));
        expectedParams.add(new Pair<String, String>("locale", "US-en"));
        expectedParams.add(new Pair<String, String>("form", "editProfileForm"));
        expectedParams.add(new Pair<String, String>("flow", "standard_flow"));
        expectedParams.add(new Pair<String, String>("flow_version", "123456"));
        expectedParams.add(new Pair<String, String>("name[firstName]", "David"));
        expectedParams.add(new Pair<String, String>("name[lastName]", "Bowman"));

        CaptureApiConnection c = Whitebox.invokeMethod(Capture.class, "getUpdateUserProfileConnection", user);

        String actualUri = Whitebox.getInternalState(c, "url");
        Set<Pair<String, String>> actualParams = Whitebox.getInternalState(c, "params");

        assertEquals(expectedUri, actualUri);
        assertRequestParamsEqual(expectedParams, actualParams);
    }
}

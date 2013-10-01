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
import com.janrain.android.capture.CaptureFlowUtils;
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

public class CaptureFlowUtilsTest extends AndroidTestCase {

    @Test
    public void test_getFormFields() throws Exception {
        Map<String, Object> flow = new HashMap<String, Object>() {{
                put("fields", new HashMap<String, Map>() {{
                        put("editProfileForm", new HashMap<String, List>() {{
                                put("fields", Arrays.asList(new String[]{"message"}));
                        }});
                        put("message", new HashMap<String, String>() {{
                                put("schemaId", "basicString");
                        }});
                }});
        }};

        JSONObject userJson = new JSONObject("{ \"basicString\" : \"hello\" }");
        Class<?>[] paramTypes = new Class<?>[]{JSONObject.class, String.class, String.class};
        Object[] params = new Object[]{userJson, "12345689", "secret"};
        CaptureRecord user = Whitebox.invokeConstructor(CaptureRecord.class, paramTypes, params);

        Set<Pair<String, String>> expectedFormValues = new HashSet<Pair<String, String>>();
        expectedFormValues.add(new Pair<String, String>("message", "hello"));

        Set<Pair<String, String>> actualFormValues =
                CaptureFlowUtils.getFormFields(user, "editProfileForm", flow);

        assertEquals(expectedFormValues, actualFormValues);
    }

    @Test
    public void test_getFormFields_complex_fields() throws Exception {
        Map<String, Object> flow = new HashMap<String, Object>() {{
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
        Class<?>[] paramTypes = new Class<?>[]{JSONObject.class, String.class};
        Object[] params = new Object[]{userJson, "12345689"};
        CaptureRecord user = Whitebox.invokeConstructor(CaptureRecord.class, paramTypes, params);

        Set<Pair<String, String>> expectedFormValues = new HashSet<Pair<String, String>>();
        expectedFormValues.add(new Pair<String, String>("name[firstName]", "David"));
        expectedFormValues.add(new Pair<String, String>("name[lastName]", "Bowman"));

        Set<Pair<String, String>> actualFormValues =
                CaptureFlowUtils.getFormFields(user, "editProfileForm", flow);

        assertEquals(expectedFormValues, actualFormValues);
    }

    @Test
    public void test_getFormFields_special_handling_of_dates() throws Exception {
        Map<String, Object> flow = new HashMap<String, Object>() {{
            put("fields", new HashMap<String, Map>() {{
                put("editProfileForm", new HashMap<String, List>() {{
                    put("fields", Arrays.asList(new String[]{"birthdate"}));
                }});
                put("birthdate", new HashMap<String, String>() {{
                    put("schemaId", "birthday");
                    put("type", "dateselect");
                }});
            }});
        }};

        JSONObject userJson = new JSONObject("{ \"birthday\" : \"1959-04-22\" }");

        Class<?>[] paramTypes = new Class<?>[]{JSONObject.class, String.class};
        Object[] params = new Object[]{userJson, "12345689"};
        CaptureRecord user = Whitebox.invokeConstructor(CaptureRecord.class, paramTypes, params);

        Set<Pair<String, String>> expectedFormValues = new HashSet<Pair<String, String>>();
        expectedFormValues.add(new Pair<String, String>("birthdate[dateselect_day]", "22"));
        expectedFormValues.add(new Pair<String, String>("birthdate[dateselect_month]", "04"));
        expectedFormValues.add(new Pair<String, String>("birthdate[dateselect_year]", "1959"));


        Set<Pair<String, String>> actualFormValues =
                CaptureFlowUtils.getFormFields(user, "editProfileForm", flow);

        assertEquals(expectedFormValues, actualFormValues);
    }

    @Test
    public void test_getFormFields_throws_exception_when_date_is_incorrect_format() throws Exception {
        Map<String, Object> flow = new HashMap<String, Object>() {{
            put("fields", new HashMap<String, Map>() {{
                put("editProfileForm", new HashMap<String, List>() {{
                    put("fields", Arrays.asList(new String[]{"birthdate"}));
                }});
                put("birthdate", new HashMap<String, String>() {{
                    put("schemaId", "birthday");
                    put("type", "dateselect");
                }});
            }});
        }};

        JSONObject userJson = new JSONObject("{ \"birthday\" : \"19590422\" }");

        Class<?>[] paramTypes = new Class<?>[]{JSONObject.class, String.class};
        Object[] params = new Object[]{userJson, "12345689"};
        CaptureRecord user = Whitebox.invokeConstructor(CaptureRecord.class, paramTypes, params);

        try {
            CaptureFlowUtils.getFormFields(user, "editProfileForm", flow);
            fail();
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "birthdate must be in yyyy-MM-dd format");
        }
    }
}

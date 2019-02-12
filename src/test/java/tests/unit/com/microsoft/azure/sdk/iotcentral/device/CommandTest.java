// Copyright (c) Luca Druda. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package tests.unit.com.github.lucadruda.iotc.device;

import org.junit.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.github.lucadruda.iotc.device.Command;

import static org.junit.Assert.*;

/**
 * Unit tests for Iothub connection string. Methods: 100% Lines: 97%
 */
public class CommandTest {
    // Tests command name
    @Test
    public void hasRightName() {
        String name = "test";
        String payload = "payload";
        String requestId = "0";
        // arrange
        Command command = new Command(name, payload, requestId);

        // act
        String commandName = command.getName();

        // assert
        assertEquals(name, commandName);
    }

    // Tests command name
    @Test
    public void hasRightpayload() {
        String name = "test";
        String payload = "payload";
        String requestId = "0";
        // arrange
        Command command = new Command(name, payload, requestId);

        // act
        String commandPayload = command.getPayload();

        // assert
        assertEquals(payload, commandPayload);
    }

    // Tests command name
    @Test
    public void hasRightrequestId() {
        String name = "test";
        String payload = "payload";
        String requestId = "0";
        // arrange
        Command command = new Command(name, payload, requestId);

        // act
        String commandRequestId = command.getRequestId();

        // assert
        assertEquals(requestId, commandRequestId);
    }

    // Tests command name
    @Test
    public void hasRightResponse() {

        String message = "message";
        String name = "test";
        String payload = "payload";
        String requestId = "0";
        Map<String, Object> respMap = new HashMap<String, Object>();
        HashMap<String, String> valueMap = new HashMap<String, String>();
        valueMap.put("value", message);
        respMap.put(name, valueMap);

        // arrange
        Command command = new Command(name, payload, requestId);

        Object result = command.getResponseObject(message);
        // assert
        assertTrue(result instanceof HashMap<?, ?>);

        // act
        HashMap<String, String> resultMap = (HashMap<String, String>) result;

        // assert
        assertEquals(respMap, resultMap);
    }
}
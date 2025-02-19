/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.webserver.api.passwordless;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.supertokens.Main;
import io.supertokens.config.Config;
import io.supertokens.passwordless.Passwordless;
import io.supertokens.passwordless.Passwordless.DeviceWithCodes;
import io.supertokens.passwordless.exceptions.Base64EncodingException;
import io.supertokens.pluginInterface.RECIPE_ID;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.passwordless.PasswordlessCode;
import io.supertokens.utils.Utils;
import io.supertokens.webserver.InputParser;
import io.supertokens.webserver.WebserverAPI;

public class GetCodesAPI extends WebserverAPI {

    private static final long serialVersionUID = -4641988458637882374L;

    public GetCodesAPI(Main main) {
        super(main, RECIPE_ID.PASSWORDLESS.toString());
    }

    @Override
    public String getPath() {
        return "/recipe/signinup/codes";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        // logic based on: https://app.code2flow.com/Odo88u7TNKIk

        String email = InputParser.getQueryParamOrThrowError(req, "email", true);
        String phoneNumber = InputParser.getQueryParamOrThrowError(req, "phoneNumber", true);
        String deviceId = InputParser.getQueryParamOrThrowError(req, "deviceId", true);
        String deviceIdHash = InputParser.getQueryParamOrThrowError(req, "preAuthSessionId", true);

        if (Stream.of(email, phoneNumber, deviceId, deviceIdHash).filter(Objects::nonNull).count() != 1) {
            throw new ServletException(new BadRequestException(
                    "Please provide exactly one of email, phoneNumber, deviceId or preAuthSessionId"));
        }

        long passwordlessCodeLifetime = Config.getConfig(main).getPasswordlessCodeLifetime();

        try {
            List<Passwordless.DeviceWithCodes> devicesInfos;
            if (deviceId != null) {
                DeviceWithCodes deviceWithCodes = Passwordless.getDeviceWithCodesById(main, deviceId);
                devicesInfos = deviceWithCodes == null ? Collections.emptyList()
                        : Collections.singletonList(deviceWithCodes);
            } else if (deviceIdHash != null) {
                DeviceWithCodes deviceWithCodes = Passwordless.getDeviceWithCodesByIdHash(main, deviceIdHash);
                devicesInfos = deviceWithCodes == null ? Collections.emptyList()
                        : Collections.singletonList(deviceWithCodes);
            } else if (email != null) {
                devicesInfos = Passwordless.getDevicesWithCodesByEmail(main, Utils.normaliseEmail(email));
            } else {
                devicesInfos = Passwordless.getDevicesWithCodesByPhoneNumber(main, phoneNumber);
            }

            JsonObject result = new JsonObject();
            result.addProperty("status", "OK");

            JsonArray jsonDeviceArr = new JsonArray();
            for (Passwordless.DeviceWithCodes deviceInfo : devicesInfos) {
                JsonObject jsonDevice = new JsonObject();
                jsonDevice.addProperty("preAuthSessionId", deviceInfo.device.deviceIdHash);
                jsonDevice.addProperty("failedCodeInputAttemptCount", deviceInfo.device.failedAttempts);

                if (deviceInfo.device.email != null) {
                    jsonDevice.addProperty("email", deviceInfo.device.email);
                }

                if (deviceInfo.device.phoneNumber != null) {
                    jsonDevice.addProperty("phoneNumber", deviceInfo.device.phoneNumber);
                }

                JsonArray jsonCodeArr = new JsonArray();
                for (PasswordlessCode code : deviceInfo.codes) {
                    JsonObject jsonCode = new JsonObject();
                    jsonCode.addProperty("codeId", code.id);
                    jsonCode.addProperty("timeCreated", code.createdAt);
                    jsonCode.addProperty("codeLifetime", passwordlessCodeLifetime);

                    jsonCodeArr.add(jsonCode);
                }

                jsonDevice.add("codes", jsonCodeArr);

                jsonDeviceArr.add(jsonDevice);
            }

            result.add("devices", jsonDeviceArr);

            super.sendJsonResponse(200, result, resp);
        } catch (Base64EncodingException ex) {
            throw new ServletException(new BadRequestException("Input encoding error in " + ex.source));
        } catch (NoSuchAlgorithmException | StorageTransactionLogicException | StorageQueryException e) {
            throw new ServletException(e);
        }
    }
}

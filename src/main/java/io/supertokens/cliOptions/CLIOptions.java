/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This program is licensed under the SuperTokens Community License (the
 *    "License") as published by VRAI Labs. You may not use this file except in
 *    compliance with the License. You are not permitted to transfer or
 *    redistribute this file without express written permission from VRAI Labs.
 *
 *    A copy of the License is available in the file titled
 *    "SuperTokensLicense.pdf" inside this repository or included with your copy of
 *    the software or its source code. If you have not received a copy of the
 *    License, please write to VRAI Labs at team@supertokens.io.
 *
 *    Please read the License carefully before accessing, downloading, copying,
 *    using, modifying, merging, transferring or sharing this software. By
 *    undertaking any of these activities, you indicate your agreement to the terms
 *    of the License.
 *
 *    This program is distributed with certain software that is licensed under
 *    separate terms, as designated in a particular file or component or in
 *    included license documentation. VRAI Labs hereby grants you an additional
 *    permission to link the program and your derivative works with the separately
 *    licensed software that they have included with this program, however if you
 *    modify this program, you shall be solely liable to ensure compliance of the
 *    modified program with the terms of licensing of the separately licensed
 *    software.
 *
 *    Unless required by applicable law or agreed to in writing, this program is
 *    distributed under the License on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *
 */

package io.supertokens.cliOptions;

import io.supertokens.Main;
import io.supertokens.ResourceDistributor;
import io.supertokens.exceptions.QuitProgramException;

import java.io.File;

public class CLIOptions extends ResourceDistributor.SingletonResource {

    private static final String RESOURCE_KEY = "io.supertokens.cliOptions.CLIOptions";
    private static final String CONFIG_FILE_KEY = "configFile=";
    private static final String PORT_FILE_KEY = "port=";
    private static final String HOST_FILE_KEY = "host=";
    private final String installationPath;
    private final String userDevProductionMode;
    private final String configFilePath;
    private final Integer port;
    private final String host;

    private CLIOptions(String[] args) {
        checkIfArgsIsCorrect(args);
        String installationPath = args[0];
        String userDevProductionMode = args[1];
        if (!userDevProductionMode.equals("DEV") && !userDevProductionMode.equals("PRODUCTION")) {
            throw new QuitProgramException("Invalid devProduction mode");
        }
        String configFilePathTemp = null;
        Integer portTemp = null;
        String hostTemp = null;
        for (int i = 2; i < args.length; i++) {
            String curr = args[i];
            if (curr.startsWith(CONFIG_FILE_KEY)) {
                configFilePathTemp = curr.split(CONFIG_FILE_KEY)[1];
                if (!new File(configFilePathTemp).isAbsolute()) {
                    throw new QuitProgramException("configPath option must be an absolute path only");
                }
            } else if (curr.startsWith(PORT_FILE_KEY)) {
                portTemp = Integer.parseInt(curr.split(PORT_FILE_KEY)[1]);
            } else if (curr.startsWith(HOST_FILE_KEY)) {
                hostTemp = curr.split(HOST_FILE_KEY)[1];
            }
        }
        this.configFilePath = configFilePathTemp;
        this.installationPath = installationPath;
        this.userDevProductionMode = userDevProductionMode;
        this.port = portTemp;
        this.host = hostTemp;
    }

    private static CLIOptions getInstance(Main main) {
        return (CLIOptions) main.getResourceDistributor().getResource(RESOURCE_KEY);
    }

    public static void load(Main main, String[] args) {
        if (getInstance(main) == null) {
            main.getResourceDistributor().setResource(RESOURCE_KEY, new CLIOptions(args));
        }
    }

    public static CLIOptions get(Main main) {
        if (getInstance(main) == null) {
            throw new QuitProgramException("Please call load() function before get");
        }
        return getInstance(main);
    }

    private void checkIfArgsIsCorrect(String[] args) {
        if (args.length == 0) {
            throw new QuitProgramException(
                    "Please provide installation path location and dev/production mode for SuperTokens");
        }
        if (args.length == 1) {
            throw new QuitProgramException("Please provide dev/production mode for SuperTokens");
        }
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    public String getUserDevProductionMode() {
        return userDevProductionMode;
    }

    // NOTE: this value will be fixed depending on operating system being used.. it
    // will be passed from the CLI
    public String getInstallationPath() {
        if (installationPath.endsWith("/")) {
            return installationPath;
        } else {
            return installationPath + "/";
        }
    }

    public Integer getPort() {
        return this.port;
    }

    public String getHost() {
        return this.host;
    }
}

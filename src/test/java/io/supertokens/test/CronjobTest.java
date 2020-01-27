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

package io.supertokens.test;

import io.supertokens.Main;
import io.supertokens.ProcessState;
import io.supertokens.ResourceDistributor;
import io.supertokens.cronjobs.CronTask;
import io.supertokens.cronjobs.Cronjobs;
import io.supertokens.exceptions.QuitProgramException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.*;

public class CronjobTest {

    static int normalCronjobCounter = 0;
    static int errorCronjobCounter = 0;

    static class QuitProgramExceptionCronjob extends CronTask {

        private static final String RESOURCE_ID = "io.supertokens.test.CronjobTest" +
                ".QuitProgramExceptionCronjob";

        private QuitProgramExceptionCronjob(Main main) {
            super("QuitProgramExceptionCronjob", main);
        }

        public static QuitProgramExceptionCronjob getInstance(Main main) {
            ResourceDistributor.SingletonResource instance = main.getResourceDistributor()
                    .getResource(RESOURCE_ID);
            if (instance == null) {
                instance = main.getResourceDistributor()
                        .setResource(RESOURCE_ID,
                                new QuitProgramExceptionCronjob(main));
            }
            return (QuitProgramExceptionCronjob) instance;
        }

        @Override
        protected void doTask() {
            throw new QuitProgramException("Cronjob Threw QuitProgramException");

        }

        @Override
        public int getIntervalTimeSeconds() {
            return 1;
        }

        @Override
        public int getInitialWaitTimeSeconds() {
            return 0;
        }
    }


    static class ErrorCronjob extends CronTask {

        private static final String RESOURCE_ID = "io.supertokens.test.CronjobTest.ErrorCronjob";

        private ErrorCronjob(Main main) {
            super("ErrorCronjob", main);
        }

        public static ErrorCronjob getInstance(Main main) {
            ResourceDistributor.SingletonResource instance = main.getResourceDistributor()
                    .getResource(RESOURCE_ID);
            if (instance == null) {
                instance = main.getResourceDistributor()
                        .setResource(RESOURCE_ID,
                                new ErrorCronjob(main));
            }
            return (ErrorCronjob) instance;
        }

        @Override
        protected void doTask() throws Exception {
            errorCronjobCounter++;
            throw new Exception("ERROR thrown from ErrorCronjobTest");

        }

        @Override
        public int getIntervalTimeSeconds() {
            return 1;
        }

        @Override
        public int getInitialWaitTimeSeconds() {
            return 0;
        }
    }


    static class NormalCronjob extends CronTask {

        private static final String RESOURCE_ID = "io.supertokens.test.CronjobTest.NormalCronjob";

        private NormalCronjob(Main main) {
            super("NormalCronjob", main);
        }

        public static NormalCronjob getInstance(Main main) {
            ResourceDistributor.SingletonResource instance = main.getResourceDistributor()
                    .getResource(RESOURCE_ID);
            if (instance == null) {
                instance = main.getResourceDistributor()
                        .setResource(RESOURCE_ID,
                                new NormalCronjob(main));
            }
            return (NormalCronjob) instance;
        }

        @Override
        protected void doTask() {
            normalCronjobCounter++;
        }

        @Override
        public int getIntervalTimeSeconds() {
            return 1;
        }

        @Override
        public int getInitialWaitTimeSeconds() {
            return 0;
        }
    }

    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }


    @Test
    public void testThatCronjobThrowsQuitProgramExceptionAndQuits() throws Exception {
        String[] args = {"../", "DEV"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);

        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));
        Cronjobs.addCronjob(process.getProcess(), QuitProgramExceptionCronjob.getInstance(process.getProcess()));

        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

        process.kill();

    }

    @Test
    public void testThatCronjobThrowsError() throws Exception {
        String[] args = {"../", "DEV"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);

        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        Cronjobs.addCronjob(process.getProcess(), ErrorCronjob.getInstance(process.getProcess()));

        ProcessState.EventAndException e = process
                .checkOrWaitForEvent(ProcessState.PROCESS_STATE.CRON_TASK_ERROR_LOGGING);
        assertNotNull(e);
        assertEquals(e.exception.getMessage(), "ERROR thrown from ErrorCronjobTest");

        Thread.sleep(5000);

        assertTrue(errorCronjobCounter >= 4 && errorCronjobCounter <= 8);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

    }

    @Test
    public void testNormalCronjob() throws Exception {
        String[] args = {"../", "DEV"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        assertEquals(normalCronjobCounter, 0);
        Cronjobs.addCronjob(process.getProcess(), NormalCronjob.getInstance(process.getProcess()));

        Thread.sleep(5000);
        assertTrue(normalCronjobCounter > 3 && normalCronjobCounter < 8);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

    }


}


/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *
 */
package org.nuxeo.publisher.gdrive;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({
    "org.nuxeo.publisher.gdrive.nuxeo-gdrive-publisher-core",
    "org.nuxeo.ecm.platform.oauth",
    "org.nuxeo.ecm.liveconnect.core",
    "org.nuxeo.ecm.liveconnect.google.drive",
    "org.nuxeo.ecm.core.cache"})
public class TestOperation {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Test
    public void testPublishBlob() {
        Blob blob = new StringBlob("This is a string");
        OperationContext ctx = new OperationContext();
        ctx.setInput(blob);
        ctx.setCoreSession(session);
        OperationChain chain = new OperationChain("TestPublish");
        chain.add(PublishToGdrive.ID);
        try {
            automationService.run(ctx, chain);
            Assert.assertTrue(false);
        } catch (OperationException e) {
            Assert.assertTrue(true);
        }

    }


}

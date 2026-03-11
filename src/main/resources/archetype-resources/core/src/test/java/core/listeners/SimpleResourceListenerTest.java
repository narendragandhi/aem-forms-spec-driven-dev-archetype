/*
 *  Copyright 2018 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package ${package}.core.listeners;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class SimpleResourceListenerTest {

    private SimpleResourceListener fixture = new SimpleResourceListener();

    private TestLogger logger = TestLoggerFactory.getTestLogger(fixture.getClass());

    @BeforeEach
    void setUp() {
        TestLoggerFactory.clear();
    }

    @Test
    void handleEventAdded() {
        ResourceChange change = new ResourceChange(ChangeType.ADDED, "/content/test", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);

        assertAll(
                () -> assertEquals(Level.DEBUG, event.getLevel()),
                () -> assertEquals(3, event.getArguments().size()),
                () -> assertEquals(ChangeType.ADDED, event.getArguments().get(0)),
                () -> assertEquals("/content/test", event.getArguments().get(1)),
                () -> assertEquals(Boolean.FALSE, event.getArguments().get(2))
        );
    }

    @Test
    void handleEventRemoved() {
        ResourceChange change = new ResourceChange(ChangeType.REMOVED, "/content/deleted-page", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);

        assertEquals(ChangeType.REMOVED, event.getArguments().get(0));
        assertEquals("/content/deleted-page", event.getArguments().get(1));
    }

    @Test
    void handleEventChanged() {
        ResourceChange change = new ResourceChange(ChangeType.CHANGED, "/content/modified-page", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());

        assertEquals(ChangeType.CHANGED, events.get(0).getArguments().get(0));
    }

    @Test
    void handleEventProviderAdded() {
        ResourceChange change = new ResourceChange(ChangeType.PROVIDER_ADDED, "/apps/provider", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());

        assertEquals(ChangeType.PROVIDER_ADDED, events.get(0).getArguments().get(0));
    }

    @Test
    void handleEventProviderRemoved() {
        ResourceChange change = new ResourceChange(ChangeType.PROVIDER_REMOVED, "/apps/provider", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());

        assertEquals(ChangeType.PROVIDER_REMOVED, events.get(0).getArguments().get(0));
    }

    @Test
    void handleExternalChange() {
        ResourceChange change = new ResourceChange(ChangeType.ADDED, "/content/external", true);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());

        assertEquals(Boolean.TRUE, events.get(0).getArguments().get(2));
    }

    @Test
    void handleMultipleChanges() {
        ResourceChange change1 = new ResourceChange(ChangeType.ADDED, "/content/page1", false);
        ResourceChange change2 = new ResourceChange(ChangeType.CHANGED, "/content/page2", false);
        ResourceChange change3 = new ResourceChange(ChangeType.REMOVED, "/content/page3", false);

        fixture.onChange(Arrays.asList(change1, change2, change3));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(3, events.size());

        assertEquals(ChangeType.ADDED, events.get(0).getArguments().get(0));
        assertEquals(ChangeType.CHANGED, events.get(1).getArguments().get(0));
        assertEquals(ChangeType.REMOVED, events.get(2).getArguments().get(0));
    }

    @Test
    void handleEmptyChangeList() {
        fixture.onChange(Collections.emptyList());

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(0, events.size());
    }

    @Test
    void handleDamResourceChange() {
        ResourceChange change = new ResourceChange(ChangeType.ADDED, "/content/dam/images/new-image.jpg", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        assertEquals("/content/dam/images/new-image.jpg", events.get(0).getArguments().get(1));
    }

    @Test
    void handleDeepPathChange() {
        String deepPath = "/content/mysite/en/products/category/subcategory/item/jcr:content/par/component";
        ResourceChange change = new ResourceChange(ChangeType.CHANGED, deepPath, false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(deepPath, events.get(0).getArguments().get(1));
    }

    @Test
    void handleAppsPathChange() {
        ResourceChange change = new ResourceChange(ChangeType.ADDED, "/apps/myproject/components/newcomponent", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals("/apps/myproject/components/newcomponent", events.get(0).getArguments().get(1));
    }

    @Test
    void handleConfPathChange() {
        ResourceChange change = new ResourceChange(ChangeType.CHANGED, "/conf/mysite/settings/wcm/templates", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals("/conf/mysite/settings/wcm/templates", events.get(0).getArguments().get(1));
    }

    @Test
    void allEventsLoggedAtDebugLevel() {
        ResourceChange change1 = new ResourceChange(ChangeType.ADDED, "/content/a", false);
        ResourceChange change2 = new ResourceChange(ChangeType.REMOVED, "/content/b", true);

        fixture.onChange(Arrays.asList(change1, change2));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertTrue(events.stream().allMatch(e -> e.getLevel() == Level.DEBUG));
    }

    @Test
    void handleMixedExternalAndInternalChanges() {
        ResourceChange internalChange = new ResourceChange(ChangeType.ADDED, "/content/internal", false);
        ResourceChange externalChange = new ResourceChange(ChangeType.ADDED, "/content/external", true);

        fixture.onChange(Arrays.asList(internalChange, externalChange));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(2, events.size());
        assertEquals(Boolean.FALSE, events.get(0).getArguments().get(2));
        assertEquals(Boolean.TRUE, events.get(1).getArguments().get(2));
    }

    @Test
    void handleSpecialCharactersInPath() {
        ResourceChange change = new ResourceChange(ChangeType.ADDED, "/content/test-page_v2.0", false);

        fixture.onChange(Arrays.asList(change));

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals("/content/test-page_v2.0", events.get(0).getArguments().get(1));
    }
}
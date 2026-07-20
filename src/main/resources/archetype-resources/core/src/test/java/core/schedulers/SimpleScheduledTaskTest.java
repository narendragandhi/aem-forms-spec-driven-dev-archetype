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
package ${package}.core.schedulers;

import java.util.List;

import com.google.common.base.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class SimpleScheduledTaskTest {

    private SimpleScheduledTask fixture = new SimpleScheduledTask();

    private TestLogger logger = TestLoggerFactory.getTestLogger(fixture.getClass());

    @BeforeEach
    void setup() {
        TestLoggerFactory.clear();
    }

    @Test
    void run() {
        SimpleScheduledTask.Config config = mock(SimpleScheduledTask.Config.class);
        when(config.myParameter()).thenReturn("parameter value");

        fixture.activate(config);
        fixture.run();

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        LoggingEvent event = events.get(0);
        assertEquals(Level.DEBUG, event.getLevel());
        assertEquals(1, event.getArguments().size());
        assertEquals("parameter value", event.getArguments().get(0));
    }

    @Test
    void runWithEmptyParameter() {
        SimpleScheduledTask.Config config = mock(SimpleScheduledTask.Config.class);
        when(config.myParameter()).thenReturn("");

        fixture.activate(config);
        fixture.run();

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        assertEquals("", events.get(0).getArguments().get(0));
    }

    @Test
    void runWithNullParameter() {
        SimpleScheduledTask.Config config = mock(SimpleScheduledTask.Config.class);
        when(config.myParameter()).thenReturn(null);

        fixture.activate(config);
        fixture.run();

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        // slf4j-test stores null log arguments as Guava Optional.absent()
        assertEquals(Optional.absent(), events.get(0).getArguments().get(0));
    }

    @Test
    void runWithoutActivateDoesNotThrow() {
        assertDoesNotThrow(() -> fixture.run());
    }

    @Test
    void activateUpdatesParameterForSubsequentRuns() {
        SimpleScheduledTask.Config config1 = mock(SimpleScheduledTask.Config.class);
        when(config1.myParameter()).thenReturn("first");
        fixture.activate(config1);
        fixture.run();

        TestLoggerFactory.clear();

        SimpleScheduledTask.Config config2 = mock(SimpleScheduledTask.Config.class);
        when(config2.myParameter()).thenReturn("second");
        fixture.activate(config2);
        fixture.run();

        List<LoggingEvent> events = logger.getLoggingEvents();
        assertEquals(1, events.size());
        assertEquals("second", events.get(0).getArguments().get(0));
    }
}

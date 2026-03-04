package com.healthstream.orchestrator.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreamGobblerTest {

    @Test
    void shouldConsumeAllLinesFromInputStream() throws Exception {

        // Simulated process output
        String simulatedOutput = "line1\nline2\nline3\n";

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(simulatedOutput.getBytes(StandardCharsets.UTF_8));

        List<String> collected = new ArrayList<>();

        StreamGobbler gobbler = new StreamGobbler(inputStream, collected::add);

        Thread thread = new Thread(gobbler);
        thread.start();
        thread.join(); // wait for gobbler to finish

        System.out.println(collected);

        // Assertions
        assertEquals(3, collected.size());
        assertEquals("line1", collected.get(0));
        assertEquals("line2", collected.get(1));
        assertEquals("line3", collected.get(2));

    }
}
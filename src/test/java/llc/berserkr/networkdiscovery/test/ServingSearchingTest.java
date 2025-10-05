package llc.berserkr.networkdiscovery.test;

import llc.berserkr.search.Searching;
import llc.berserkr.serve.Serving;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ServingSearchingTest {

    private Searching.FoundConnection found = null;

    @Test
    public void testSearchFindWithOthers() {

        final Serving serving = new Serving(1501);
        serving.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        final List<String> hosts = new ArrayList<>();

        for(int i = 0; i < 2; i++) {

            for(int j = 0; j < 255; j++) {
                hosts.add("192.168." + i + "." + j);
            }

        }

        hosts.add(
        "localhost"
        );

        final List<Integer> ports = List.of(1500, 1501);

        final Searching searching = new Searching(hosts, ports);
        searching.start(foundConnection -> {
            System.out.println("FOUND " + foundConnection);
            found = foundConnection;
        });

        while(searching.isSearching()) {

            System.out.println("Still searching");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        searching.stop();
        assertNotNull(found);

    }

    @Test
    public void testSearchDontFindWithOthers() {

        final Serving serving = new Serving(1555);
        serving.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        final List<String> hosts = List.of(
                "192.168.1.1",
                "192.168.1.2",
                "192.168.1.3",
                "192.168.1.4",
                "192.168.1.5",
                "localhost"
        );

        final List<Integer> ports = List.of(1500, 1501);

        final Searching searching = new Searching(hosts, ports);
        searching.start(foundConnection -> {
            found = foundConnection;
        });

        while(searching.isSearching()) {

            System.out.println("Still searching");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        searching.stop();

        assertNull(found);

    }
}

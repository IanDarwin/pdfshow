package net.rejmi.pdfshow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MyObserverTest {
    ObservableHelper target;

    @BeforeEach
    public void doFirst() {
        target = new ObservableHelper();
    }

    @Test
    public void testAddAndCall() {
        final boolean[] seen = {false};
        Observer obs = new Observer() {
            @Override
            public void letMeKnow(Object o) {
                seen[0] = true;
            }
        };
        target.register(obs);
        target.tellThem("Hello");
        assertTrue(seen[0], "Observer failed to set flag");
    }
}

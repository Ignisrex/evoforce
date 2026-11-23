package com.silverignis.systems;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepClockTest {

            private final FixedStepClock clock = new FixedStepClock();

            private int frame(float delta) {
                clock.add(delta);
                int ticks = 0;
                while (clock.step()) ticks++;
                return ticks;
            }

            @Test
            void sixtyFpsGivesOneTickPerFrame() {
                for (int i = 0; i < 600; i++) assertEquals(1, frame(1f / 60f));
                assertEquals(600, clock.tick());
            }

            @Test
            void thirtyFpsGivesTwoTicksPerFrame() {
                for (int i = 0; i < 300; i++) assertEquals(2, frame(1f / 30f));
            }

            @Test
            void highRefreshStillRunsSixtyTicksPerSecond() {
                int emptyFrames = 0;
                for (int i = 0; i < 160; i++) {
                    if (frame(1f / 160f) == 0) emptyFrames++;
                    assertTrue(clock.alpha() >= 0f && clock.alpha() < 1f);
                }
                assertTrue(clock.tick() >= 59 && clock.tick() <= 61, "ticks: " + clock.tick());
                assertTrue(emptyFrames > 0);
            }

            @Test
            void stallIsCappedAndLeavesNoBacklog() {
                assertEquals(5, frame(2f));
                assertEquals(1, frame(1f / 60f));
            }
        }

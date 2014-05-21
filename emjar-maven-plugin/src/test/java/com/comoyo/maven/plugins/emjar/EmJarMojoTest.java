package com.comoyo.maven.plugins.emjar;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.collect.Collections2.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(JUnit4.class)
public class EmJarMojoTest
{
    private static final Artifact ART_A = testArtifact("idA");
    private static final Artifact ART_B = testArtifact("idB");
    private static final Artifact ART_C = testArtifact("idC");
    private static final Artifact ART_D = testArtifact("idD");
    private static final Artifact ART_E = testArtifact("idE");

    private static final List<Artifact> ALL_ARTS = new ArrayList<Artifact>() {{
            add(ART_A);
            add(ART_B);
            add(ART_C);
            add(ART_D);
            add(ART_E);
        }};

    private static Artifact testArtifact(String id)
    {
        return new DefaultArtifact("group", id, "0.0", "compile", "jar", "", null);
    }

    private static String toSpec(Artifact artifact)
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    @Test
    public void testMissingOrdering()
        throws Exception
    {
        final AtomicBoolean seen1 = new AtomicBoolean(false);
        final AtomicBoolean seen2 = new AtomicBoolean(false);
        final EmJarMojo mojo = new EmJarMojo()
            {
                @Override
                protected void reportConflict(Artifact one, Artifact two, Set<String> files)
                {
                    if (one.equals(ART_A) && two.equals(ART_E) && files.contains("a")) {
                        seen1.set(true);
                    }
                    else if (one.equals(ART_D) && two.equals(ART_E) && files.contains("a")) {
                        seen2.set(true);
                    }
                    else {
                        fail("Unexpected conflict between " + toSpec(one) + " and "
                             + toSpec(two) + " on " + files);
                    }
                }
            };
        mojo.addConflicts(new Artifact[]{ART_A, ART_D, ART_E}, "a");

        final Ordering[] orderings = new Ordering[]{
            new Ordering(toSpec(ART_A), toSpec(ART_B)),
            new Ordering(toSpec(ART_B), toSpec(ART_C)),
            new Ordering(toSpec(ART_C), toSpec(ART_D))
        };

        final List<Artifact> ordered = mojo.orderArtifacts(ALL_ARTS, orderings);

        final List<Artifact> knownOrdered = new ArrayList<Artifact>() {{
                add(ART_A);
                add(ART_B);
                add(ART_C);
                add(ART_D);
                add(ART_E);
            }};

        assertEquals("Returned artifact list was not ordered as expected", knownOrdered, ordered);
        assertTrue("Conflict between " + toSpec(ART_A) + " and " + toSpec(ART_E) + " on `a` not seen",
                   seen1.get());
        assertTrue("Conflict between " + toSpec(ART_D) + " and " + toSpec(ART_E) + " on `a` not seen",
                   seen2.get());
    }

    @Test
    public void testPartialOrdering()
        throws Exception
    {
        for (List<Artifact> perm : permutations(ALL_ARTS)) {
            final EmJarMojo mojo = new EmJarMojo();
            final Ordering[] orderings = new Ordering[]{
                new Ordering(toSpec(ART_A), toSpec(ART_B))
            };

            final List<Artifact> ordered = mojo.orderArtifacts(perm, orderings);
            assertTrue(toSpec(ART_A) + " was not ordered before " + toSpec(ART_B)
                       + " for input " + perm,
                       ordered.indexOf(ART_A) < ordered.indexOf(ART_B));
        }
    }

    @Test
    public void testFullOrdering()
        throws Exception
    {
        for (List<Artifact> perm : permutations(ALL_ARTS)) {
            final EmJarMojo mojo = new EmJarMojo()
                {
                    @Override
                    protected void reportConflict(Artifact one, Artifact two, Set<String> files)
                    {
                        fail("Unexpected conflict between " + toSpec(one) + " and " + toSpec(two)
                             + " on " + files);
                    }
                };
            mojo.addConflicts(new Artifact[]{ART_A, ART_B}, "b");
            mojo.addConflicts(new Artifact[]{ART_B, ART_C}, "c");
            mojo.addConflicts(new Artifact[]{ART_C, ART_D}, "d");
            mojo.addConflicts(new Artifact[]{ART_A, ART_D}, "a");
            mojo.addConflicts(new Artifact[]{ART_A, ART_E}, "a");

            final Ordering[] orderings = new Ordering[]{
                new Ordering(toSpec(ART_A), toSpec(ART_B)),
                new Ordering(toSpec(ART_B), toSpec(ART_C)),
                new Ordering(toSpec(ART_C), toSpec(ART_D)),
                new Ordering(toSpec(ART_A), toSpec(ART_E)),
                new Ordering(toSpec(ART_E), toSpec(ART_B))
            };

            final List<Artifact> ordered = mojo.orderArtifacts(perm, orderings);
            final List<Artifact> knownOrdered = new ArrayList<Artifact>() {{
                    add(ART_A);
                    add(ART_E);
                    add(ART_B);
                    add(ART_C);
                    add(ART_D);
                }};
            assertEquals(
                "Returned artifact list was not ordered as expected for input "
                    + perm, knownOrdered, ordered);
        }
    }

    @Test
    public void testIllegalOrdering()
        throws Exception
    {
        for (List<Artifact> perm : permutations(ALL_ARTS)) {
            final EmJarMojo mojo = new EmJarMojo();
            final Ordering[] orderings = new Ordering[]{
                new Ordering(toSpec(ART_A), toSpec(ART_B)),
                new Ordering(toSpec(ART_B), toSpec(ART_A))
            };

            try {
                mojo.orderArtifacts(perm, orderings);
            }
            catch (MojoExecutionException e) {
                if (e.getMessage().contains("Unable to order")) {
                    return;
                }
            }
            fail("Illegal ordering was not caught");
        }
    }
}

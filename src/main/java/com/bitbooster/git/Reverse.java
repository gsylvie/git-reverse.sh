/* com.bitbooster.git.Reverse

Java code to reverse a git repo.
https://bit-booster.com/doing-git-wrong/2017/03/30/howto-reverse-a-git-repo/

YMMV - I only tested this on Ubuntu 16.04.

Copyright (c) 2017, G. Sylvie Davies (sylvie@bit-booster.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies,
either expressed or implied, of bit-booster.com
*/
package com.bitbooster.git;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Reverse {

    private static File TEMP_FILE = null;
    private static ExecutorService POOL = null;
    private static String FINAL_COMMIT = null;
    private static int TOTAL = 0;
    private static int COUNT = 0;
    private static NumberFormat FORMAT = NumberFormat.getPercentInstance();
    private final static String PATTERN = "GO-GO-BIT-BOOSTER|%T|%H|%aI|%aE|%aN|%cI|%cE|%cN|%B".replace("|", "%x02");

    private static void processCommitData(
            StringBuilder currentTokLine, StringBuilder currentBody,
            final Map<String, Set<String>> originalReversedParents,
            final Map<String, String> rewrittenHashes
    ) throws Exception {

        String tokLine = currentTokLine.toString();
        String[] toks = tokLine.split("\u0002");
        String treeId = toks[1];
        String commitId = toks[2];
        String authorTime = toks[3];
        String authorName = toks[4];
        String authorMail = toks[5];
        String commitTime = toks[6];
        String commitName = toks[7];
        String commitMail = toks[8];
        String body;
        if (toks.length > 9) {
            body = toks[9] + currentBody.toString();
        } else {
            body = "<empty-commit-msg>";
        }

        FileOutputStream fout = new FileOutputStream(TEMP_FILE, false);
        fout.write(body.getBytes(StandardCharsets.UTF_8));
        fout.close();
        String path = TEMP_FILE.getAbsolutePath();

        String[] env = new String[]{
                "GIT_AUTHOR_DATE=" + authorTime,
                "GIT_AUTHOR_EMAIL=" + authorMail,
                "GIT_AUTHOR_NAME=" + authorName,
                "GIT_COMMITTER_DATE=" + commitTime,
                "GIT_COMMITTER_EMAIL=" + commitMail,
                "GIT_COMMITTER_NAME=" + commitName
        };
        List<String> args = new ArrayList<>(10);
        args.add("git");
        args.add("commit-tree");
        Set<String> parents = originalReversedParents.get(commitId);
        if (parents != null) {
            for (String parent : parents) {
                String p = rewrittenHashes.get(parent);
                if (p == null) {
                    throw new RuntimeException("Cannot remap original reversed parent: " + parent);
                }
                args.add("-p");
                args.add(p);
            }
        }
        args.add("-F");
        args.add(path);
        args.add(treeId);

        String[] cmd = args.toArray(new String[args.size()]);

        LineProcessor hashUpdater = line -> {
            String commit = line.trim();
            rewrittenHashes.put(commitId, commit);
            FINAL_COMMIT = commit;
            COUNT++;
            double d = (1.0 * COUNT) / TOTAL;
            System.out.println(commit + " - Reversed " + commitId + " - " + COUNT + " of " + TOTAL + " (" + FORMAT.format(d) + ")");
        };
        exec(cmd, env, hashUpdater, false);

        currentTokLine.delete(0, currentTokLine.length());
        currentBody.delete(0, currentBody.length());
    }

    public static void rewriteRef(String prevRefLine, String currentRefLine, Map<String, String> h2h) {
        String action = "action";
        String[] toks = prevRefLine.split("\\s+");
        String id = toks[0].trim();
        String ref = toks[1].trim();

        String[] newToks = currentRefLine.split("\\s+");
        String newRef = newToks.length > 1 ? newToks[1].trim() : "";

        boolean isTag = ref.startsWith("refs/tags/");
        boolean keep = ref.startsWith("refs/tags/") && ref.endsWith("^{}");
        if (!keep) {
            keep = ref.startsWith("refs/heads/");
        }
        if (!keep) {
            // Simple (un-annotated) tags should hit here:
            keep = isTag && !newRef.endsWith("^{}");
        }
        if (!keep) {
            return;
        }
        try {
            String rewritten = h2h.get(id);
            if (rewritten == null) {
                System.out.println("WARNING: " + id + " (from " + ref + ") could not be mapped to a reversed commit.");
                return;
            } else {
                rewritten = rewritten.trim();
            }
            if (isTag) {
                ref = ref.substring("refs/tags/".length());
                if (ref.endsWith("^{}")) {
                    ref = ref.substring(0, ref.length() - 3).trim();
                }
                action = "tag";
            } else {
                ref = ref.substring("refs/heads/".length());
                action = "branch";
            }
            String[] forceCmd = new String[]{"git", action, "-f", ref, rewritten};
            exec(forceCmd, null, null, false);
        } catch (Exception e) {
            throw new RuntimeException("git " + action + " -f " + ref + " (rewritten:" + id + ") failed: " + e, e);
        }
    }

    public static void main(String[] args) throws Exception {
        TEMP_FILE = File.createTempFile("git", "msg");
        POOL = Executors.newCachedThreadPool();

        final Map<String, Set<String>> m = new HashMap<>();
        final Map<String, String> h2h = new HashMap<>();

        final int[] lineCount = new int[1];

        LineProcessor mapper = line -> {
            lineCount[0]++;
            String[] toks = line.split("\\s+");
            String val = toks[0];
            for (int i = 1; i < toks.length; i++) {
                String key = toks[i];
                Set<String> v = m.computeIfAbsent(key, k -> new HashSet<>());
                v.add(val);
            }
        };

        final StringBuilder currentTokLine = new StringBuilder();
        final StringBuilder currentBody = new StringBuilder();
        LineProcessor reverser = line -> {

            if (line.startsWith("GO-GO-BIT-BOOSTER\u0002")) {
                if (currentTokLine.length() > 0) {
                    try {
                        processCommitData(currentTokLine, currentBody, m, h2h);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed " + e, e);
                    }
                }
                currentTokLine.append(line);

            } else {
                currentBody.append("\n").append(line);
            }

        };


        // Get parentage map, and reverse it
        // git log --all --date-order --pretty=%H %P
        String[] cmd = new String[]{"git", "log", "--all", "--date-order", "--pretty=%H %P"};
        exec(cmd, mapper);
        TOTAL = lineCount[0];

        cmd = new String[]{"git", "log", "--all", "--date-order", "--pretty=" + PATTERN};
        exec(cmd, reverser);
        processCommitData(currentTokLine, currentBody, m, h2h);

        // Need to get off 'master' branch before we force-update all branches.
        cmd = new String[]{"git", "checkout", FINAL_COMMIT};
        exec(cmd, true);

        final StringBuilder prevRefLine = new StringBuilder();
        LineProcessor refRewriter = line -> {
            line = line.trim();
            try {
                String prevLine = prevRefLine.toString();
                if ("".equals(prevLine)) {
                    return;
                }

                rewriteRef(prevLine, line, h2h);

            } finally {
                prevRefLine.delete(0, prevRefLine.length());
                prevRefLine.append(line);
            }
        };


        cmd = new String[]{"git", "show-ref", "--dereference"};
        exec(cmd, refRewriter);
        rewriteRef(prevRefLine.toString(), "", h2h);

        cmd = new String[]{"git", "branch", "-f", "master", FINAL_COMMIT};
        exec(cmd, false);

        cmd = new String[]{"git", "checkout", "master"};
        exec(cmd, true);

        POOL.shutdown();

        System.out.println("");
        System.out.println("*********************************************");
        System.out.println("| Git repo successfully reversed!!! :-) (-: |");
        System.out.println("*********************************************");
        System.out.println("To push the reversed repo:");
        System.out.println("  rm .git/packed-refs ");
        System.out.println("  rm -rf .git/refs/remotes ");
        System.out.println("  git push --mirror [git-reversed-clone-url]  ");
        System.out.println("");
        System.out.println("WARNING:");
        System.out.println("========");
        System.out.println("Pushing a reversed git repo is a profoundly destructive and confusing operation.");
        System.out.println("");
        System.out.println("You have a full 'git clone --mirror' backup stored somewhere safe, right?");
        System.out.println("");
    }

    public interface LineProcessor {
        void process(String line);
    }

    public static class Run {
        String stdout;
        String stderr;
    }

    private static Run exec(String[] args, boolean ignoreErrors) throws Exception {
        return exec(args, null, null, ignoreErrors);
    }

    private static Run exec(String[] args, LineProcessor lp) throws Exception {
        return exec(args, null, lp, false);
    }

    private static Run exec(String[] args, String[] env, LineProcessor lp, boolean ignoreErrors) throws Exception {

        String cmd = Arrays.toString(args).replace(",", "");
        // System.out.println(cmd);

        final Process p = Runtime.getRuntime().exec(args, env);
        final InputStream stdout = p.getInputStream();
        final InputStream stderr = p.getErrorStream();
        final StringBuilder out = new StringBuilder();
        final StringBuilder err = new StringBuilder();
        Runnable readStdOut = consumer(stdout, out, lp);
        Runnable readStdErr = consumer(stderr, err, null);

        Future<StringBuilder> f2 = POOL.submit(readStdErr, err);
        readStdOut.run();
        f2.get();
        int exitCode = p.waitFor();

        Run r = new Run();
        r.stdout = out.toString().trim();
        r.stderr = err.toString().trim();
        if (exitCode != 0) { // || !"".equals(r.stderr.trim())) {
            if (!ignoreErrors) {
                System.out.println("----------------------------------------------");
                System.out.println("exit=" + exitCode + " for: " + cmd);
                System.out.println("stdout=[" + r.stdout + "]");
                System.out.println("stderr=[" + r.stderr + "]");
            }
        }
        return r;
    }

    private static Runnable consumer(InputStream in, StringBuilder buf, LineProcessor lp) {
        return () -> {
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                isr = new InputStreamReader(in, StandardCharsets.UTF_8);
                br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (lp != null) {
                        lp.process(line);
                    } else {
                        buf.append(line).append('\n');
                    }
                }
            } catch (IOException ioe) {
                buf.append("ERROR: ").append(ioe.toString()).append("\n");
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                    if (isr != null) {
                        isr.close();
                    }
                    in.close();
                } catch (IOException ioe) {
                    throw new RuntimeException("hopeless", ioe);
                }
            }
        };
    }
}

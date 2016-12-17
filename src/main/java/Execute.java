import java.io.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by Maochuan on 2016/12/14.
 */
public class Execute {

    public static class ExecuteParams {
        private Map<String, String> get;
        private Map<String, String> post;
        private Map<String, String> files;

        public ExecuteParams() {
            get = new HashMap<String, String>();
            post = new HashMap<String, String>();
            files = new HashMap<String, String>();
        }

        public String genCode() {
            return "$_GET = " + genArray(get) + ";"
                    + "$_POST = " + genArray(post) + ";"
                    + "$_FILES = " + genArray(files) + ";";
        }

        public void putGet(String name, String value) {
            get.put(name, value);
        }

        public void putGet(String name, String[] values) {
            get.put(name, genArray(values));
        }

        public void putGet(String name, List<String> values) {
            get.put(name, genArray((String[]) values.toArray()));
        }

        public void putPost(String name, String value) {
            post.put(name, value);
        }

        public void putPost(String name, String[] values) {
            post.put(name, genArray(values));
        }

        public void putPost(String name, List<String> values) {
            post.put(name, genArray((String[]) values.toArray()));
        }

        public void putFile(String file, String path) {
            files.put(file, path);
        }

        private String genArray(String[] values) {
            StringBuilder builder = new StringBuilder();
            builder.append("array(");
            for (String s : values) {
                builder.append(getValue(s));
                builder.append(',');
            }
            builder.append(')');
            return builder.toString();
        }

        private String genArray(Map<String, String> map) {
            StringBuilder builder = new StringBuilder();
            builder.append("array(");
            map.forEach((String s, String s2) -> {
                builder.append('\'');
                builder.append(getKey(s));
                builder.append('\'');
                builder.append("=>'");
                builder.append(getValue(s2));
                builder.append("',");
            });
            builder.append(")");
            return builder.toString();
        }

        private String getKey(String key) {
            return key.replaceAll("'", "");
        }

        private String getValue(String value) {
            // FIXME：
            return value;
        }
    }

    private static Process createProcess(String process) throws IOException {
        return Runtime.getRuntime().exec(process);
    }

    private static void writeCode(Process process, File file, ExecuteParams params) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
        writer.write("<?php\n");
        writer.write(params.genCode());
        writer.write("require('" + file.getAbsolutePath() + "');");
        writer.close();
    }

    private static String getProcessOutput(Process process) throws IOException {
        InputStreamReader SR = new InputStreamReader(process.getInputStream());
        BufferedReader reader = new BufferedReader(SR);

        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            output.append(line + "\r\n");
        }
        return output.toString();
    }

    //
    // blocking and exec
    public static String exec(File file, ExecuteParams params) throws IOException {
        String phpPath = "./php/php.exe";
        Process process = createProcess(phpPath);
        writeCode(process, file, params);
        return getProcessOutput(process);
    }
}

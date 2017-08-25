package com.tns;

import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Build;
import android.util.Log;

class AppConfig {
    private final static String AndroidKey = "android";

    private final File appDir;
    private final Object[] values;

    public AppConfig(File appDir) {
        this.appDir = appDir;

        values = new Object[KnownKeys.values().length];

        JSONObject rootObject = null;
        JSONObject androidObject = null;
        File packageInfo = new File(appDir, "/app/package.json");
        if (packageInfo.exists()) {
            try {
                rootObject = FileSystem.readJSONFile(packageInfo);
                androidObject = rootObject != null && rootObject.has(AndroidKey) ? rootObject.getJSONObject(AndroidKey) : null;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        for(KnownKeys option : KnownKeys.values()) {
            values[option.ordinal()] = option.read(this, rootObject, androidObject);
        }
    }

    public Object[] getAsArray() {
        return values;
    }

    public Object getValue(KnownKeys option) {
        return values[option.ordinal()];
    }

    public int getGcThrottleTime() { return (int)getValue(KnownKeys.gcThrottleTime); }
    public int getMemoryCheckInterval() { return (int)getValue(KnownKeys.memoryCheckInterval); }
    public double getFreeMemoryRatio() { return (double)getValue(KnownKeys.freeMemoryRatio); }
    public String getProfilingMode() { return (String)getValue(KnownKeys.profiling); }
    public MarkingMode getMarkingMode() { return (MarkingMode)getValue(KnownKeys.markingMode); }

    protected enum KnownKeys {
        v8Flags(OptionType.Android, "--expose_gc") {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                return source.getString(name());
            }
        },
        codeCache(OptionType.Android, false) {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                return source.getBoolean(name());
            }
        },
        heapSnapshotScript(OptionType.Android, "") {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                String value = source.getString(KnownKeys.heapSnapshotScript.name());
                return FileSystem.resolveRelativePath(config.appDir.getPath(), value, config.appDir + "/app/");
            }
        },
        heapSnapshotBlob(OptionType.Android, "") {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                String value = source.getString(name());
                String path = FileSystem.resolveRelativePath(config.appDir.getPath(), value, config.appDir + "/app/");
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    // this path is expected to be a directory, containing three sub-directories: armeabi-v7a, x86 and arm64-v8a
                    path = path + "/" + Build.CPU_ABI + "/snapshot.blob";
                    return path;
                }
                return defaultValue;
            }
        },
        profilerOutputDir(OptionType.Android, "") {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                return source.getString(name());
            }
        },
        gcThrottleTime(OptionType.Android, 0) {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                return source.getInt(name());
            }
        },
        memoryCheckInterval(OptionType.Android, 0) {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                return source.getInt(name());
            }
        },
        freeMemoryRatio(OptionType.Android, 0.0) {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                return source.getDouble(name());
            }
        },
        profiling(OptionType.Shared, "") {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                return source.getString(name());
            }
        },
        markingMode(OptionType.Android, com.tns.MarkingMode.full) {
            @Override
            protected Object read(AppConfig config, JSONObject source) throws JSONException {
                try {
                    return MarkingMode.valueOf(source.getString(name()));
                } catch(Exception e) {
                    Log.v("JS", "Failed to parse marking mode. The default " + ((MarkingMode)defaultValue).name() + " will be used.");
                    throw e;
                }
            }
        };

        protected final OptionType optionType;
        protected final Object defaultValue;
        KnownKeys(OptionType optionType, Object defaultValue) {
            this.optionType = optionType;
            this.defaultValue = defaultValue;
        }

        protected abstract Object read(AppConfig config, JSONObject source) throws JSONException;

        protected final Object read(AppConfig config, JSONObject root, JSONObject android) {
            try {
                JSONObject jsonValueSource = optionType.getJsonSource(root, android);
                if (jsonValueSource != null && jsonValueSource.has(name())) {
                    return read(config, jsonValueSource);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            return defaultValue;
        }

        protected enum OptionType {
            /**
             * Shared options are these that are exposed in all runtimes and are stored on the root object in the app/packagea.json.
             */
            Shared {
                @Override
                JSONObject getJsonSource(JSONObject root, JSONObject android) {
                    return root;
                }
            },

            /**
             * Android options are these that are Android specific and are stored in the "android" object in the app/package.json.
             */
            Android {
                @Override
                JSONObject getJsonSource(JSONObject root, JSONObject android) {
                    return android;
                }
            };

            abstract JSONObject getJsonSource(JSONObject root, JSONObject android);
        }
    }
}

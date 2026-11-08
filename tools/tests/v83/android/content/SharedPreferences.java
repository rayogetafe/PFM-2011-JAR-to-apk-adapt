package android.content;

import java.util.HashMap;
import java.util.Map;

public interface SharedPreferences {
    Map<String, ?> getAll();
    String getString(String key, String fallback);
    int getInt(String key, int fallback);
    Editor edit();

    interface Editor {
        Editor clear();
        Editor putInt(String key, int value);
        Editor putString(String key, String value);
        Editor remove(String key);
        boolean commit();
        void apply();
    }

    final class Memory implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();
        public Map<String, ?> getAll() { return new HashMap<>(values); }
        public String getString(String key, String fallback) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : fallback;
        }
        public int getInt(String key, int fallback) {
            Object value = values.get(key);
            return value instanceof Integer ? (Integer) value : fallback;
        }
        public Editor edit() {
            return new Editor() {
                private final Map<String, Object> pending = new HashMap<>();
                private boolean clear;
                public Editor clear() { clear = true; return this; }
                public Editor putInt(String key, int value) { pending.put(key, value); return this; }
                public Editor putString(String key, String value) { pending.put(key, value); return this; }
                public Editor remove(String key) { pending.put(key, null); return this; }
                public boolean commit() {
                    if (clear) values.clear();
                    for (Map.Entry<String, Object> entry : pending.entrySet()) {
                        if (entry.getValue() == null) values.remove(entry.getKey());
                        else values.put(entry.getKey(), entry.getValue());
                    }
                    return true;
                }
                public void apply() { commit(); }
            };
        }
    }
}

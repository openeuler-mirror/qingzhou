package qingzhou.deployer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qingzhou.api.Lang;
import qingzhou.engine.util.Utils;

class CharMap {
    private static final Map<Character, Character> ZH_TR_MAP = new HashMap<>();
    private static final Set<Character> DETECTED = new CopyOnWriteArraySet<>();

    static {
        try {
            Properties props = Utils.streamToProperties(CharMap.class.getResourceAsStream("/" + CharMap.class.getPackage().getName().replace(".", "/") + "/CharMap.txt"));
            String zh = props.getProperty("zh");
            String tr = props.getProperty("tr");
            for (int i = 0; i < zh.length(); i++) {
                Character check = ZH_TR_MAP.put(zh.charAt(i), tr.charAt(i));
                if (check != null) {
                    throw new IllegalArgumentException("Please remove duplicate characters");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 字符串是否包含中文
     */
    static boolean containsZHChar(String str) {
        if (str == null) return false;
        str = str.trim();
        if (str.isEmpty()) return false;

        Pattern p = Pattern.compile("[\u4E00-\u9FA5\\！\\，\\。\\（\\）\\《\\》\\“\\”\\？\\：\\；\\【\\】]");
        Matcher m = p.matcher(str);
        return m.find();
    }

    static String zh2tr(String msg) {
        if (msg == null) return null;
        msg = msg.trim();
        if (msg.isEmpty()) return null;

        StringBuilder twMsg = new StringBuilder();
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            Character twChar = ZH_TR_MAP.get(c);
            if (twChar == null) {
                twChar = c;

                // 记录，以更新繁体字的字典
                if (containsZHChar(String.valueOf(c))) {
                    DETECTED.add(c);
                    StringBuilder needAdd = new StringBuilder();
                    for (Character character : DETECTED) {
                        needAdd.append(character);
                    }
                    System.out.println(Lang.tr.info + " char (" + c + ") not found for: " + msg);
                    System.out.println(Lang.tr.info + " chars not found: " + needAdd);
                }
            }
            twMsg.append(twChar);
        }
        return twMsg.toString();
    }
}

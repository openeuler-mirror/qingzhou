package qingzhou.framework.impl;

import qingzhou.framework.console.Lang;
import qingzhou.framework.util.ObjectUtil;
import qingzhou.framework.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CharMap {
    private static final Map<Character, Character> zh_tr_Map = new HashMap<>();
    private static final Set<Character> detected = new CopyOnWriteArraySet<>();

    static {
        try {
            Properties props = ObjectUtil.streamToProperties(CharMap.class.getResourceAsStream("/charmap.txt"));
            String zh = props.getProperty("zh");
            String tr = props.getProperty("tr");
            for (int i = 0; i < zh.length(); i++) {
                Character check = zh_tr_Map.put(zh.charAt(i), tr.charAt(i));
                if (check != null) {
                    throw new IllegalArgumentException("Please remove duplicate characters");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String zh2tr(String msg) {
        if (msg == null) return null;
        msg = msg.trim();
        if (msg.isEmpty()) return null;

        StringBuilder twMsg = new StringBuilder();
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            Character twChar = zh_tr_Map.get(c);
            if (twChar == null) {
                twChar = c;

                // 记录，以更新繁体字的字典
                if (StringUtil.containsZHChar(String.valueOf(c))) {
                    detected.add(c);
                    StringBuilder needAdd = new StringBuilder();
                    for (Character character : detected) {
                        needAdd.append(character);
                    }
                    System.out.println(Lang.tr.getFullName() + " char (" + c + ") not found for: " + msg);
                    System.out.println(Lang.tr.getFullName() + " chars not found: " + needAdd);
                }
            }
            twMsg.append(twChar);
        }
        return twMsg.toString();
    }
}

package qingzhou.deployer.impl;

import qingzhou.api.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ModelManagerImpl {
    private static class ActionMethodImpl implements ActionInfo.InvokeMethod {
        private final Object instance;
        private final Method method;

        private ActionMethodImpl(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }

        @Override
        public void invokeMethod(Object... args) throws Exception {
            method.invoke(instance, args);
        }
    }

    @Override

    @Override

    @Override
    public Options getOptions(Request request, String modelName, String fieldName) {
        Options userOptions = modelInfoMap.get(modelName).instance.options(request, fieldName);
        if (userOptions != null) return userOptions;

        Options defaultOptions = getDefaultOptions(modelName, fieldName);
        if (defaultOptions != null) {
        }
        List<Option> merge = new ArrayList<>(defaultOptions.options());
        merge.addAll(userOptions.options());
        return () -> merge;
    }

    private Options getDefaultOptions(String modelName, String fieldName) {
        ModelFieldData modelField = getModelField(modelName, fieldName);

        if (modelField.type() == FieldType.selectCharset) {
            return Options.of("UTF-8", "GBK", "GB18030", "GB2312", "UTF-16", "US-ASCII");
        }

        if (modelField.type() == FieldType.bool) {
            return Options.of(Boolean.TRUE.toString(), Boolean.FALSE.toString());
        }

        return null;
    }

    @Override

    @Override
    public Group getGroup(String modelName, String groupName) {
        ModelBase modelInstance = null;//TODO getModelInstance(modelName);
        Groups groups = modelInstance.groups();
        if (groups != null) {
            for (Group group : groups.groups()) {
                if (group.name().equals(groupName)) {
                    return group;
                }
            }
        }

        if ("OTHERS".equals(groupName)) {
            return Group.of("OTHERS", new String[]{"其它", "en:OTHERS"});
        }

        return null;
    }

    @Override
    public String[] getFieldNamesByGroup(String modelName, String groupName) {
        List<String> fieldNames = new ArrayList<>();

        ModelInfo modelInfo = getModelInfo(modelName);
        modelInfo.fieldInfoMap.forEach((field, fieldInfo) -> {
            if (fgroup().equals(groupName)) {
                fieldNames.add(field);
            }
        });

        return fieldNames.toArray(new String[0]);
    }
}

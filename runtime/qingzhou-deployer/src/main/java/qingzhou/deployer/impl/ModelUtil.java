package qingzhou.deployer.impl;

import qingzhou.api.*;
import qingzhou.api.metadata.ModelData;

import java.util.ArrayList;
import java.util.List;

public class ModelUtil {
    public static ModelFieldDataImpl toModelFieldData(ModelField modelField) {
        ModelFieldDataImpl modelFieldData = new ModelFieldDataImpl();
        modelFieldData.setGroup(modelField.group());
        modelFieldData.setNameI18n(modelField.nameI18n());
        modelFieldData.setInfoI18n(modelField.infoI18n());
        modelFieldData.setRequired(modelField.required());
        modelFieldData.setType(modelField.type());
        modelFieldData.setMax(modelField.numberMax());
        modelFieldData.setMaxLength(modelField.lengthMax());
        modelFieldData.setMin(modelField.numberMin());
        modelFieldData.setMinLength(modelField.lengthMin());
        modelFieldData.setIpOrHostname(modelField.asHostname());
        modelFieldData.setPort(modelField.asPort());
        modelFieldData.setURL(modelField.asURL());
        modelFieldData.setNotSupportedCharacters(modelField.unsupportedCharacters());
        modelFieldData.setNotSupportedStrings(modelField.unsupportedStrings());
        modelFieldData.setEffectiveWhen(modelField.effectiveWhen());
        modelFieldData.setDisableOnCreate(modelField.cannotAdd());
        modelFieldData.setDisableOnEdit(modelField.cannotUpdate());
        modelFieldData.setShowToList(modelField.shownOnList());
        modelFieldData.setMonitorField(modelField.isMonitorField());
        modelFieldData.setSupportGraphical(modelField.supportGraphical());
        modelFieldData.setSupportGraphicalDynamic(modelField.supportGraphicalDynamic());

        return modelFieldData;
    }

    public static ModelData toModelData(Model model) {
        ModelDataImpl modelData = new ModelDataImpl();
        modelData.setEntryAction(model.entryAction());
        modelData.setInfoI18n(model.infoI18n());
        modelData.setIcon(model.icon());
        modelData.setMenuName(model.menuName());
        modelData.setMenuOrder(model.menuOrder());
        modelData.setNameI18n(model.nameI18n());
        modelData.setName(model.name());
        modelData.setShowToMenu(model.showToMenu());

        return modelData;
    }

    public static ModelActionDataImpl toModelActionData(ModelAction modelAction) {
        ModelActionDataImpl modelActionData = new ModelActionDataImpl();
        modelActionData.setDisabled(modelAction.disabled());
        modelActionData.setEffectiveWhen(modelAction.effectiveWhen());
        modelActionData.setForwardToPage(modelAction.forwardTo());
        modelActionData.setIcon(modelAction.icon());
        modelActionData.setNameI18n(modelAction.nameI18n());
        modelActionData.setSupportBatch(modelAction.supportBatch());
        modelActionData.setName(modelAction.name());
        modelActionData.setInfoI18n(modelAction.infoI18n());

        return modelActionData;
    }

    public static GroupsImpl toGroupsImpl(Groups groups) {
        if (groups == null) return null;
        List<Group> groupList = new ArrayList<>();
        groups.groups().forEach(group -> groupList.add(new GroupImpl(group.name(), group.i18n())));

        return new GroupsImpl(groupList);
    }
}

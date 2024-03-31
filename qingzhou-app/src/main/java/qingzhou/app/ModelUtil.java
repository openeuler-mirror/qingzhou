package qingzhou.app;

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
        modelFieldData.setRefModel(modelField.refModel());
        modelFieldData.setMax(modelField.max());
        modelFieldData.setMaxLength(modelField.maxLength());
        modelFieldData.setMin(modelField.min());
        modelFieldData.setMinLength(modelField.minLength());
        modelFieldData.setIpOrHostname(modelField.isIpOrHostname());
        modelFieldData.setWildcardIp(modelField.isWildcardIp());
        modelFieldData.setPattern(modelField.isPattern());
        modelFieldData.setPort(modelField.isPort());
        modelFieldData.setURL(modelField.isURL());
        modelFieldData.setNoGreaterThan(modelField.noGreaterThan());
        modelFieldData.setNoGreaterThanMinusOne(modelField.noGreaterThanMinusOne());
        modelFieldData.setNoGreaterOrEqualThanDate(modelField.noGreaterOrEqualThanDate());
        modelFieldData.setNoLessOrEqualThanDate(modelField.noLessOrEqualThanDate());
        modelFieldData.setNoLessThan(modelField.noLessThan());
        modelFieldData.setNoLessThanCurrentTime(modelField.noLessThanCurrentTime());
        modelFieldData.setNoSupportZHChar(modelField.noSupportZHChar());
        modelFieldData.setNotSupportedCharacters(modelField.notSupportedCharacters());
        modelFieldData.setNotSupportedStrings(modelField.notSupportedStrings());
        modelFieldData.setCannotBeTheSameAs(modelField.cannotBeTheSameAs());
        modelFieldData.setSkipCharacterCheck(modelField.skipCharacterCheck());
        modelFieldData.setSkipSafeCheck(modelField.skipSafeCheck());
        modelFieldData.setCheckXssLevel1(modelField.checkXssLevel1());
        modelFieldData.setClientEncrypt(modelField.clientEncrypt());
        modelFieldData.setEffectiveWhen(modelField.effectiveWhen());
        modelFieldData.setDisableOnCreate(modelField.disableOnCreate());
        modelFieldData.setDisableOnEdit(modelField.disableOnEdit());
        modelFieldData.setShowToEdit(modelField.showToEdit());
        modelFieldData.setShowToList(modelField.showToList());
        modelFieldData.setLinkModel(modelField.linkModel());
        modelFieldData.setValueFrom(modelField.valueFrom());
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
        modelActionData.setForwardToPage(modelAction.forwardToPage());
        modelActionData.setIcon(modelAction.icon());
        modelActionData.setNameI18n(modelAction.nameI18n());
        modelActionData.setOrderOnList(modelAction.orderOnList());
        modelActionData.setShowToList(modelAction.showToList());
        modelActionData.setSupportBatch(modelAction.supportBatch());
        modelActionData.setName(modelAction.name());
        modelActionData.setInfoI18n(modelAction.infoI18n());
        modelActionData.setShowToListHead(modelAction.showToListHead());

        return modelActionData;
    }

    public static GroupsImpl toGroupsImpl(Groups groups) {
        if (groups == null) return null;
        List<Group> groupList = new ArrayList<>();
        groups.groups().forEach(group -> groupList.add(new GroupImpl(group.name(), group.i18n())));

        return new GroupsImpl(groupList);
    }
}

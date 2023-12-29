package qingzhou.console.master;

import qingzhou.api.console.ConsoleContext;
import qingzhou.api.console.Model;
import qingzhou.api.console.ModelAction;
import qingzhou.api.console.ModelField;
import qingzhou.api.console.ModelManager;
import qingzhou.api.console.data.Request;
import qingzhou.api.console.data.Response;
import qingzhou.api.console.model.AddModel;
import qingzhou.api.console.model.EditModel;
import qingzhou.api.console.model.ListModel;
import qingzhou.api.console.model.ShowModel;
import qingzhou.api.console.option.Option;
import qingzhou.api.console.option.OptionManager;
import qingzhou.console.ServerXml;
import qingzhou.console.impl.ConsoleWarHelper;
import qingzhou.console.util.Constants;
import qingzhou.console.util.StringUtil;
import qingzhou.console.util.XmlUtil;

import java.util.ArrayList;
import java.util.List;

@Model(name = Constants.MODEL_NAME_favorites, icon = "star", menuOrder = 1,
        nameI18n = {"我的收藏", "en:My Favorites"},
        infoI18n = {"查看当前登录用户收藏的功能列表。",
                "en:View the list of features favorited by the currently logged-in user."})
public class Favorites extends MasterModelBase implements ListModel {

    @ModelField(
            required = true, unique = true,
            nameI18n = {"ID", "en:ID"},
            infoI18n = {"ID。", "en:ID."})
    public String id;

    @ModelField(
            required = true,
            nameI18n = {"收藏", "en:Favorite"},
            infoI18n = {"当前登录用户收藏的功能入口项。", "en:Feature entry items favorites by the currently logged-in user."})
    private String favorite;

    @Override
    public void list(Request request, Response response) throws Exception {
        try {
            String loginUser = request.getUserName();
            List<String> userFavorites = ServerXml.getMyFavorites(loginUser);
            for (String userFavorite : userFavorites) {
                Favorites favorites = new Favorites();
                favorites.id = userFavorite;
                favorites.favorite = userFavorite;
                response.addDataObject(favorites);
            }
        } catch (Exception e) {
            response.setSuccess(false);
            ConsoleWarHelper.getLogger().warn(e.getMessage(), e);
        }
    }

    @ModelAction(name = "addfavorite", icon = "star",
            nameI18n = {"添加", "en:Add"},
            infoI18n = {"将该功能入口添加至我的收藏。", "en:Add this feature entry to My Favorites."})
    public void addFavorite(Request request, Response response) {
        try {
            String loginUser = request.getUserName();
            final String modelAndAction = checkModelAndAction(request, response);
            if (modelAndAction == null /*|| !AccessControl.canAccess(modelAndAction, loginUser, true)*/) {
                response.setSuccess(false);
                return;
            }
            OptionManager optionManager = favoriteOptions(false);
            for (Option option : optionManager.options()) {
                if (!option.value().equals(modelAndAction)) {
                    response.setSuccess(false);
                    return;
                }
            }

            List<String> temp = ServerXml.getMyFavorites(loginUser);
            temp.add(modelAndAction);
            List<String> userFavorites = new ArrayList<>();
            for (String s : temp) {
                if (!userFavorites.contains(s)) {
                    userFavorites.add(s);
                }
            }
            writeUserFavorites(loginUser, userFavorites);
        } catch (Exception e) {
            response.setSuccess(false);
            ConsoleWarHelper.getLogger().warn(e.getMessage(), e);
        }
    }

    @ModelAction(name = "cancelfavorites", icon = "star-empty",
            nameI18n = {"取消", "en:Cancel"},
            infoI18n = {"从我的收藏中移除该功能入口。", "en:Remove the feature entry from My Collections."})
    public void cancelFavorites(Request request, Response response) {
        try {
            final String modelAndAction = checkModelAndAction(request, response);
            if (modelAndAction == null) {
                return;
            }

            String loginUser = request.getUserName();
            List<String> userFavorites = ServerXml.getMyFavorites(loginUser);
            userFavorites.remove(modelAndAction);
            writeUserFavorites(loginUser, userFavorites);
        } catch (Exception e) {
            response.setSuccess(false);
        }
    }

    @Override
    public OptionManager fieldOptions(Request request, String fieldName) {
        if ("favorite".equals(fieldName)) {
            return favoriteOptions(true); // 生成 文档的时候要给出所有的候选项，不要角色控制
        }

        return super.fieldOptions(request, fieldName);
    }

    private OptionManager favoriteOptions(boolean ignoreAccessControl) {
        // 屏蔽一些后端接口，如 update 等。
        String[] allowed = {AddModel.ACTION_NAME_CREATE, EditModel.ACTION_NAME_EDIT,
                Constants.ACTION_NAME_INDEX, "overview", ListModel.ACTION_NAME_LIST,
                ShowModel.ACTION_NAME_SHOW,// 集中配置
                "monitor"// 如 收藏 jvm 监视页面
        };
        List<Option> list = new ArrayList<>();
        ModelManager modelManager = getConsoleContext().getModelManager();
        for (String model : modelManager.getAllModelNames()) {
            if (model.equals(Constants.MODEL_NAME_home)) {
                continue;
            }

            for (ModelAction modelAction : getConsoleContext().getModelManager().getModelActions(model)) {
                String actionName = modelAction.name();
                String modelActionData = model + "/" + actionName;
                for (String name : allowed) {
                    if (name == EditModel.ACTION_NAME_EDIT) {
                        Class<?> modelClass = modelManager.getModelClass(model);
                        if (ListModel.class.isAssignableFrom(modelClass)) {
                            continue;
                        }
                    }
                    if (actionName.equals(name)) {
                        if (ignoreAccessControl /*|| AccessControl.canAccess(modelActionData, actionContext.getLoginUser(), true)*/) {
                            list.add(new Option() {
                                @Override
                                public String value() {
                                    return modelActionData;
                                }

                                @Override
                                public String[] i18n() {
                                    return modelAction.nameI18n();
                                }
                            });
                        }
                    }
                }
            }
        }

        return () -> list;
    }

    private void writeUserFavorites(String loginUser, List<String> userFavorites) throws Exception {
        String favorites = StringUtil.join(userFavorites, ",");
        XmlUtil consoleXml = new XmlUtil(ConsoleWarHelper.getServerXml());
        consoleXml.setAttribute(ServerXml.getTenantUserNodeExpression(ServerXml.getTenant(loginUser), ServerXml.getLoginUserName(loginUser)), "favorites", favorites);
        consoleXml.write();
    }

    private String checkModelAndAction(Request request, Response response) {
        String favorite = request.getParameter("favorite");
        int i = favorite.indexOf("/");
        if (i > 0) {
            String[] favorites = favorite.split("/");
            String targetType = favorites[0];//TODO
            String targetName = favorites[1];
            String appName = favorites[2];
            ConsoleContext consoleContext = ConsoleWarHelper.getAppConsoleContext(appName);
            if (consoleContext != null) {
                String model = favorites[3];
                String action = favorites[4];
                final ModelAction modelAction = consoleContext.getModelManager().getModelAction(model, action);
                if (modelAction != null) {
                    return appName + "/" + model + "/" + action;
                }
            }
        }

        response.setSuccess(false);
        return null;
    }
}
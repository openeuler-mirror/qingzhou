package qingzhou.api;

public interface Request {
    String getAppName();

    String getModelName();

    String getActionName();

    String getViewName();

    String getId();

    String[] getParameterNames();

    String getParameter(String parameterName);

    String getUserName();

    Lang getI18nLang();
}

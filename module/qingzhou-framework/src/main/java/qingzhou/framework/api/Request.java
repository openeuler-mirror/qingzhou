package qingzhou.framework.api;

public interface Request {
    String getAppName();

    String getModelName();

    String getActionName();

    String getViewName();

    String getId();

    String[] getParameterNames();

    int getParamToInt(String parameterName, int defaultValue);
    
    String getParameter(String parameterName);
    
    String getParameter(String parameterName, String defaultValue);
    
    String getUserName();

    Lang getI18nLang();
}

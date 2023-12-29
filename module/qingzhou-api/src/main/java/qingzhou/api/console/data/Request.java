package qingzhou.api.console.data;

public interface Request {
    String getTargetType();

    String getTargetName();

    String getAppName();

    String getModelName();

    String getActionName();

    String getViewName();

    String getId();

    String[] getParameterNames();

    String getParameter(String parameterName);

    String getUserName();
}

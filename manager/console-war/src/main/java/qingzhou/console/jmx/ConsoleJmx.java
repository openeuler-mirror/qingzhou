package qingzhou.console.jmx;

import qingzhou.console.controller.rest.RESTController;
import qingzhou.console.i18n.ConsoleI18n;
import qingzhou.console.i18n.I18n;

import javax.servlet.http.HttpSession;
import java.util.Properties;

public class ConsoleJmx implements ConsoleJmxMBean {

    @Override
    public String invoke(String appName, String modelName, String actionName, Properties args) {
        HttpSession session = null;
        try {
            JmxHttpServletRequest request = new JmxHttpServletRequest(appName, modelName, actionName, args);
            session = request.getSession(false);
            if (session == null) {
                throw new RuntimeException(ConsoleI18n.getI18n(I18n.getI18nLang(), "jmx.authentication.invalid"));
            }
            JmxHttpServletResponse response = new JmxHttpServletResponse();
            I18n.setI18nLang(request, null);

            RESTController.invokeReq(request, response);
            return response.getResult();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            I18n.resetI18nLang();

            if (session != null) {
                session.invalidate();
            }
        }
    }
}

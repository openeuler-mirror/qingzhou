package qingzhou.console.jmx;

import qingzhou.console.ConsoleI18n;
import qingzhou.console.I18n;
import qingzhou.console.controller.rest.RESTController;

import javax.servlet.http.HttpSession;
import java.util.Properties;

public class JmxImpl implements JmxImplMBean {

    @Override
    public String callServerMethod(String appName, String modelName, String actionName, Properties args) throws Exception {
        try {
            JmxHttpServletRequest request = new JmxHttpServletRequest(appName, modelName, actionName, args);
            HttpSession session = request.getSession(false);
            if (session == null) {
                throw new RuntimeException(ConsoleI18n.getI18n(I18n.getI18nLang(), "jmx.authentication.invalid"));
            }
            JmxHttpServletResponse response = new JmxHttpServletResponse();
            I18n.setI18nLang(request, null);

            RESTController.invokeReq(request, response);
            return response.getResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            I18n.resetI18nLang();
        }
    }
}

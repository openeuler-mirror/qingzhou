package qingzhou.console.controller.jmx;

import java.util.Properties;
import javax.servlet.http.HttpSession;

import qingzhou.console.controller.I18n;
import qingzhou.console.controller.rest.RESTController;
import qingzhou.deployer.JmxServiceAdapter;

public class JmxInvokerImpl implements JmxServiceAdapter.JmxInvoker {
    static {
        I18n.addKeyI18n("jmx.authentication.invalid", new String[]{"JMX 认证无效", "en:JMX authentication is invalid"});
    }

    @Override
    public String invoke(String appName, String modelName, String actionName, Properties args) {
        HttpSession session;
        try {
            JmxHttpServletRequest request = new JmxHttpServletRequest(appName, modelName, actionName, args);
            session = request.getSession(false);
            if (session == null) {
                throw new RuntimeException(I18n.getKeyI18n("jmx.authentication.invalid"));
            }
            JmxHttpServletResponse response = new JmxHttpServletResponse();
            I18n.setI18nLang(request, null);

            RESTController.invokeReq(request, response);
            return response.getResult();
        } catch (Exception e) {
            return JmxInvokerImpl.class.getSimpleName() + " error: " + e.getMessage();
        } finally {
            I18n.resetI18nLang();
        }
    }
}

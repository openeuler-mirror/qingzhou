package qingzhou.app.master;

import qingzhou.api.*;
import qingzhou.api.type.Editable;
import qingzhou.api.type.Showable;

@Model(name = "password", icon = "key",
        menuOrder = 99,
        showToMenu = false,
        entryAction = Editable.ACTION_NAME_EDIT,
        nameI18n = {"修改密码", "en:Change Password"},
        infoI18n = {"用于修改当前登录用户的密码。", "en:Used to change the password of the currently logged-in user."})
public class Password extends ModelBase implements Editable {
    @ModelField(
            effectiveWhen = "update2FA=false",
            required = true, type = FieldType.password,
            nameI18n = {"原始密码", "en:Original Password"},
            infoI18n = {"登录系统的原始密码。", "en:The original password to log in to the system."})
    public String originalPassword;

    @ModelField(
            effectiveWhen = "update2FA=false",
            required = true,
            type = FieldType.password,
            nameI18n = {"新密码", "en:Password"},
            infoI18n = {"用于登录系统的新密码。", "en:The new password used to log in to the system."})
    public String newPassword;

    @ModelField(
            effectiveWhen = "update2FA=false",
            required = true,
            type = FieldType.password,
            nameI18n = {"确认密码", "en:Confirm Password"},
            infoI18n = {"确认用于登录系统的新密码。", "en:Confirm the new password used to log in to the system."})
    public String confirmPassword;

    @ModelField(
            type = FieldType.bool,
            nameI18n = {"更新双因子认证密钥", "en:Update Two-factor Authentication Key"},
            infoI18n = {"安全起见，建议定期刷新双因子认证密钥。刷新后，需要重新在用户终端的双因子认证客户端设备进行绑定。",
                    "en:For security reasons, it is recommended to periodically refresh the two-factor authentication key. After refreshing, you need to re-bind it on the two-factor authentication client device of the user terminal."}
    )
    public boolean update2FA = false;

    @Override
    public void init() {
        AppContext master = getAppContext();
        master.addI18n("update2FA.rebind", new String[]{"双因子认证密钥更新成功，请扫描二维码重新绑定双因子认证密钥",
                "en:The two-factor authentication key is updated successfully, please scan the QR code to re-bind the two-factor authentication key"});
        master.addI18n("password.confirm.failed", new String[]{"确认密码与新密码不一致", "en:Confirm that the password does not match the new password"});
        master.addI18n("password.original.failed", new String[]{"原始密码错误", "en:The original password is wrong"});
        master.addI18n("password.change.not", new String[]{"新密码与原始密码是一样的，没有发生改变", "en:The new password is the same as the original password and has not changed"});
        master.addI18n("password.min", new String[]{"未达到密码最短使用期限 %s 天，上次修改时间为：%s", "en:The minimum password age of %s days has not been reached, last modified: %s"});
        master.addI18n("password.doNotUseOldPasswords", new String[]{"出于安全考虑，禁止使用最近 %s 次使用过的旧密码",
                "en:For security reasons, the use of old passwords that have been used last %s is prohibited"});

        master.addI18n("password.format", new String[]{"密码须包含大小写字母、数字、特殊符号，长度至少10位", "en:Password must contain uppercase and lowercase letters, numbers, special symbols, and must be at least 10 characters long"});
        master.addI18n("password.continuousChars", new String[]{"密码不能包含三个或三个以上相同或连续的字符", "en:A weak password, the password cannot contain three or more same or consecutive characters"});
    }

    @ModelAction(name = Editable.ACTION_NAME_EDIT,
            icon = "edit", forwardToPage = "form",
            nameI18n = {"编辑", "en:Edit"},
            infoI18n = {"获得可编辑的数据或界面。", "en:Get editable data or interfaces."})
    public void edit(Request request, Response response) throws Exception {
        response.addModelData(new Password());
    }

    @ModelAction(name = Showable.ACTION_NAME_SHOW,
            icon = "info-sign", forwardToPage = "show",
            nameI18n = {"查看", "en:Show"},
            infoI18n = {"查看该组件的详细配置信息。", "en:View the detailed configuration information of the component."})
    public void show(Request request, Response response) {
    }
//
//    @Override
//    public void update(Request request, Response response) throws Exception {
//        Map<String, String> p = new HashMap<>();
//        Map<String, String> paramMap = prepareParameters(request);
//
//        String loginUser = request.getUserName();
//        if (Boolean.parseBoolean(p.getOrDefault("update2FA", "false"))) {
//            // p.put("keyFor2FA", User.refresh2FA()); TODO
//            p.put("bound2FA", "false");
//        } else { //ITAIT-4537
//            if (forbidResetInitPwd(request)) {
//                return;
//            }
//
//            Map<String, String> loginUserPro = ServerXml.get().user(loginUser);
//
//            String digestAlg = loginUserPro.get("digestAlg");
//            if (StringUtil.isBlank(digestAlg)) {
//                digestAlg = "SHA-256";
//            }
//            String saltLength = loginUserPro.get("saltLength");
//            if (StringUtil.isBlank(saltLength)) {
//                saltLength = String.valueOf(User.defSaltLength);
//            }
//            String iterations = loginUserPro.get("iterations");
//            if (StringUtil.isBlank(iterations)) {
//                iterations = String.valueOf(User.defIterations);
//            }
//            MessageDigest digest = SystemController.getAppMetadata.getMessageDigest();
//            p.put(User.pwdKey,
//                    digest.digest(
//                            paramMap.get("newPassword"),
//                            digestAlg,
//                            Integer.parseInt(saltLength),
//                            Integer.parseInt(iterations)));
//            p.put("changeInitPwd", "false");
//
//            SystemController.getLogger().info(String.format("encrypted password with, digestAlg: %s, saltLength: %s, iterations: %s", digestAlg, saltLength, iterations));
//
//            User.insertPasswordModifiedTime(p);
//
//            String oldPasswords = User.cutOldPasswords(
//                    loginUserPro.remove("oldPasswords"),
//                    loginUserPro.get("limitRepeats"),
//                    p.get(User.pwdKey));
//            p.put("oldPasswords", oldPasswords);
//        }
//
//        XmlUtil xmlUtils = new XmlUtil(SystemController.getServerXml());// TODO: 不要直接依赖 server.xml 配置文件，使用 Config 接口来处理？
//        xmlUtils.setAttributes(ServerXml.getTenantUserNodeExpression(ServerXml.getTenant(loginUser), ServerXml.getLoginUserName(loginUser)), p);
//        xmlUtils.write();
//
//        if (Boolean.parseBoolean(paramMap.getOrDefault("update2FA", "false"))) {
//            response.setMsg(getConsoleContext().getI18n("update2FA.rebind"));
//        } else {
//            // 注销在其它机器上忘记注销的会话
////            String encodeUser = LoginManager.encodeUser(actionContext.getId());
////            ActionContext.invalidateAllSessionAsAttribute(actionContext.getHttpServletRequestInternal(),
////                    LoginManager.LOGIN_USER, encodeUser);
////
////            actionContext.invalidate();// 注销当前会话，强制自己重新登录
//        }
//    }
//
//    @Override
//    public String validate(Request request, String fieldName) {
//        if (!ServerXml.get().authEnabled()) {
//            return null;
//        }
//
//        String loginUser = request.getUserName();
//        String update2FA = request.getParameter("update2FA");
//        String newValue;
//        if (StringUtil.isBlank(update2FA) || !Boolean.parseBoolean(update2FA)) {
//            if ("originalPassword".equals(fieldName)) {
//                newValue = request.getParameter("originalPassword");
//                if (newValue == null) { // fix #ITAIT-2849
//                    return getConsoleContext().getI18n("validator.require");
//                }
//
//                String pwd = ServerXml.get().user(loginUser).get(User.pwdKey);
//
//                MessageDigest digest = SystemController.getAppMetadata.getMessageDigest();
//                if (!digest.matches(newValue, pwd)) {
//                    return getConsoleContext().getI18n("password.original.failed");
//                }
//            }
//
//            if ("newPassword".equals(fieldName)) {
//                newValue = request.getParameter("newPassword");
//                if (newValue == null) { // fix #ITAIT-2849
//                    return getConsoleContext().getI18n("validator.require");
//                }
//
//                String msg = User.checkPwd(newValue, loginUser);
//                if (msg != null) {
//                    return msg;
//                }
//
//                String pwd = ServerXml.get().user(loginUser).get(User.pwdKey);
//                MessageDigest digest = SystemController.getAppMetadata.getMessageDigest();
//                boolean matches = digest.matches(newValue, pwd);
//                if (matches) {
//                    return getConsoleContext().getI18n("password.change.not");
//                }
//
//                Map<String, String> userP = ServerXml.get().user(loginUser);
//                if (!Boolean.parseBoolean(userP.getOrDefault("changeInitPwd", "false"))) {
//                    String passwordLastModifiedTime = userP.get("passwordLastModifiedTime");
//                    if (StringUtil.notBlank(passwordLastModifiedTime)) {
//                        try {
//                            long time = new SimpleDateFormat(ConsoleConstants.DATE_FORMAT).parse(passwordLastModifiedTime).getTime();
//                            if (Boolean.parseBoolean(userP.get("enablePasswordAge"))) {
//                                String minAge = userP.get("passwordMinAge");
//                                if (minAge != null && !minAge.equals("0")) {
//                                    long min = time + Integer.parseInt(minAge) * ConsoleConstants.DAY_MILLIS_VALUE;
//                                    if (TimeUtil.getCurrentTime() < min) {
//                                        return String.format(getConsoleContext().getI18n("password.min"), minAge, passwordLastModifiedTime);
//                                    }
//                                }
//                            }
//                        } catch (ParseException ignored) {
//                        }
//                    }
//                }
//
//                Map<String, String> loginUserPro = ServerXml.get().user(loginUser);
//                if (loginUserPro != null) {
//                    String oldPasswords = loginUserPro.get("oldPasswords");
//                    if (StringUtil.notBlank(oldPasswords)) {
//                        for (String oldPass : oldPasswords.split(ConsoleConstants.DATA_SEPARATOR)) {
//                            if (!oldPass.isEmpty() && digest.matches(newValue, oldPass)) {
//                                String limitRepeats = loginUserPro.get("limitRepeats");
//                                if (StringUtil.isBlank(limitRepeats)) {
//                                    limitRepeats = String.valueOf(User.defLimitRepeats);
//                                }
//                                return String.format(getConsoleContext().getI18n("password.doNotUseOldPasswords"), limitRepeats);
//                            }
//                        }
//                    }
//                }
//            }
//
//            if ("confirmPassword".equals(fieldName)) {
//                newValue = request.getParameter("confirmPassword");
//                if (newValue == null) { // fix #ITAIT-2849
//                    return getConsoleContext().getI18n("validator.require");
//                }
//                if (!newValue.equals(request.getParameter("newPassword"))) {
//                    return getConsoleContext().getI18n("password.confirm.failed");
//                }
//            }
//        }
//
//        return null;
//    }
//
//    private boolean forbidResetInitPwd(Request request) {
//        String loginUser = request.getUserName();
//        if (!ServerXml.ConsoleRole.systemXUsers().contains(loginUser)) { // 非三员或特殊用户不必限制
//            return false;
//        }
//
////        Properties userP = ConsoleXml.getInstance().getUserProperties().get(loginUser);
////        // 首次修改密码，只能在本机进行，因为默认密码是公开的，防止通过公网抢先修改
////        if (Utils.isBlank(userP.getProperty("passwordLastModifiedTime"))) { // 不要靠 changeInitPwd 来判定，靠 passwordLastModifiedTime，改过一次之后就不必要限制本机了
////            if (!ConsoleUtil.trustedIP(request.getClientIp())) {
////                request.setSuccess(false);
////                request.setMsg(I18n.getString("client.trusted.not"));
////                LOGGER.warn("The operation has been forbidden, client is not trusted: " + request.getClientIp());
////                return true;
////            }
////        }
//
//        return false;
//    }
//
//    @ModelAction(name = "key", icon = "shield", nameI18n = {"二维码", "en:QR Code"},
//            infoI18n = {"获取当前用户的双因子认证密钥，以二维码形式提供给用户。", "en:Obtain the current user two-factor authentication key and provide it to the user in the form of a QR code."})
//    public void showKeyFor2FA(Request request, Response response) throws Exception {
//        /*String loginUser = request.getLoginUser();
//        Map<String,String> attributes = ConsoleXml.get().user(loginUser);
//        String key = "keyFor2FA";
//        String keyFor2FA = attributes.get(key);
//        if (StringUtil.isBlank(keyFor2FA)) {
//            //keyFor2FA = User.refresh2FA();
//            User.updateXmlProperty(loginUser, key, keyFor2FA);
//        }
//        String qrCode = Totp.generateTotpString(loginUser, keyFor2FA);
//
//        request.setResponseHeader("Pragma", "no-cache");
//        request.setResponseHeader("Cache-Control", "no-cache");
//        String format = "PNG";
//        request.setResponseContentType("image/" + format);
//        // 请求来自内部命令行调用
//        if (StringUtil.notBlank(request.getHeader(LoginManager.HEADER_CLIENT_TYPE))) {
//            request.setResponseHeader("Content-disposition", "attachment; filename=" + key + ".png");
//        }
//
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        TWQRCodeConfig twqrCodeConfig = new TWQRCodeConfig();
//        twqrCodeConfig.setText(qrCode)
//                .setWidth(300)
//                .setHeight(300)
//                .setFormat(format)
//                .setOutputStream(bos);
//        QRCodeGenerator.generateImage(twqrCodeConfig);
//
//        // 二维码返回到浏览器
//        request.getResponseOutputStream().write(bos.toByteArray());
//        request.getResponseOutputStream().flush();*/
//    }
//
//    @ModelAction(name = "validate", icon = "question-sign",
//            nameI18n = {"双因子密码验证", "en:Validate 2FA Code"},
//            infoI18n = {"验证双因子密码是否正确。", "en:Verify that the two-factor password is correct."})
//    public void validate(Request request, Response response) throws Exception {
//        boolean result = false;
//        String reqCode = request.getParameter(ConsoleConstants.LOGIN_2FA);
//        if (StringUtil.notBlank(reqCode)) {
//            String loginUser = request.getUserName();
//            Map<String, String> attributes = ServerXml.get().user(loginUser);
//            String keyFor2FA = attributes.get("keyFor2FA");
//            try {
//                //result = Totp.verify(keyFor2FA, reqCode); TODO
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            if (result) {
//                User.updateXmlProperty(loginUser, "bound2FA", "true");
//            }
//        }
//        //response.setDirectBody("{\"result\":" + result + "}");
//    }
}

package qingzhou.registry.ai;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import qingzhou.ai.ToolInterceptor;
import qingzhou.ai.ToolService;
import qingzhou.api.action.*;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;
import qingzhou.dto.meta.annotation.App;
import qingzhou.json.Json;
import qingzhou.logger.Logger;
import qingzhou.registry.AppStub;
import qingzhou.registry.Constants;
import qingzhou.registry.Registry;
import qingzhou.registry.web.PermissionChecker;
import qingzhou.registry.web.RequestParameters;

@Component(immediate = true)
public class RegisterActionTools {
    // 业务字段集合：写操作的字段清单依模型而定，无法静态声明，故收敛为一个 JSON 对象字符串参数
    private static final String DATA = "data";
    private static final String TOOL_NAME_PREFIX = "qingzhou_action_";

    @Reference
    private Registry registry;
    @Reference
    private Logger logger;
    @Reference
    private Json json;

    private BundleContext bundleContext;
    private final List<ServiceRegistration> registrations = new ArrayList<>();

    @Activate
    public void init(ComponentContext context) {
        bundleContext = context.getBundleContext();

        AtomicInteger p = new AtomicInteger();
        Hashtable<String, String> sharedProperties = new Hashtable<>();
        sharedProperties.put(ToolService.PARAMETER_NAME + "." + p.incrementAndGet(), RequestParameters.INSTANCE_ID);
        sharedProperties.put(ToolService.PARAMETER_DESCRIPTION + "." + p.get(), "应用所在的轻舟实例的 ID，每个应用都有所属的轻舟实例，只有先确定实例，才能确定应用。");

        sharedProperties.put(ToolService.PARAMETER_NAME + "." + p.incrementAndGet(), RequestParameters.APP_CODE);
        sharedProperties.put(ToolService.PARAMETER_DESCRIPTION + "." + p.get(), "应用的唯一编码，该编码在同一个轻舟实例下不会重复。");

        sharedProperties.put(ToolService.PARAMETER_NAME + "." + p.incrementAndGet(), RequestParameters.MODEL_CODE);
        sharedProperties.put(ToolService.PARAMETER_DESCRIPTION + "." + p.get(), "模块的唯一编码，该编码在同一个应用内不会重复。");

        sharedProperties.put(ToolService.PARAMETER_NAME + "." + p.incrementAndGet(), RequestParameters.DATA_ID);
        sharedProperties.put(ToolService.PARAMETER_DESCRIPTION + "." + p.get(), "模块内某条业务数据的唯一ID。如果当前调用的模块同时支持 list 和 show 两种操作，并且用户当前调用的操作不是 list（即为 show 或其他操作），那么此参数为必需参数；否则（即模块不同时具备这两种操作，或当前操作是 list），此参数为非必需参数。");
        sharedProperties.put(ToolService.PARAMETER_REQUIRED + "." + p.get(), "false");

        Map<String, String> tools = new HashMap<>();
        tools.put(Page.ACTION_CODE_PAGE, "该接口返回某个应用模块的业务数据或资源的列表信息。列表数据分页返回，每次调用只返回一页，可通过 " + Constants.AI_REQUEST_PARAMETER_PAGE_NUM + " 指定页码。"
                + "如果返回内容以【分页提示】开头，说明列表共有多页，此时须遵守以下规则："
                + "1. 即使用户要求列出全部数据，也只查询这一页；用户指定页码前，不要查询其他页，也不要通过猜测编号、逐条调用其他工具等方式获取本页以外的数据。"
                + "2. 如实列出本页数据，禁止推测或编造其他页的内容；如果用户的问题涉及全部数据（如统计、排序、查找最大值），须说明结论仅基于本页。"
                + "3. 如果返回内容被截断，如实说明本页有部分数据未能显示，不要推测被截断的内容，也不要为补全而重复调用工具。"
                + "4. 回复末尾注明“第 X/Y 页”（最后一页注明“已是最后一页”），并提示用户可回复页码（如“第 2 页”或“下一页”）查看其他页；页码超出范围时，告知用户有效的页码范围。"
                + "5. 用户指定页码后，使用相同的参数并传入 " + Constants.AI_REQUEST_PARAMETER_PAGE_NUM + " 再次调用本工具。");
        tools.put(Show.ACTION_CODE_SHOW, "该接口返回某模块或模块内某业务数据或资源的详细信息。");
        tools.put(Monitor.ACTION_CODE_MONITOR, "该接口用于获取某模块或模块内某业务数据或资源的实时状态，用来反映资源用量、检查系统健康、告警安全阈值等。");

        tools.put(SwitchSpace.ACTION_CODE_SWITCHSPACE, "该接口将应用的模块切换到指定的空间上。");
        tools.put(SwitchSpace.ACTION_CODE_CURRENTSPACE, "该接口返回应用当前使用的模块空间。");

        tools.put(Add.ACTION_CODE_ADD, "该接口在指定模块中新建一条业务数据，这是会影响真实数据的写操作。"
                + "调用前须先用获取模块详细信息的工具取得该模块的新增字段清单与校验规则，禁止凭常识猜测字段编码。"
                + DATA + " 为 JSON 对象字符串，形如 {\"字段编码\":\"值\"}，只提交清单内的字段，未提交的字段由后端取默认值；"
                + "多值字段用分隔符拼成一个字符串。"
                + "返回 success=true 表示新建成功；失败时返回内容会给出每个字段的错误原因，据其修正后可重试。");
        tools.put(Update.ACTION_CODE_UPDATE, "该接口更新指定模块中已有的一条业务数据，这是会影响真实数据的写操作。"
                + "只更新 " + DATA + " 中出现的字段，其余字段保持不变。执行前须向用户复述待更新的对象并取得确认。"
                + "调用前须先用获取模块详细信息的工具取得该模块的更新字段清单与校验规则，禁止凭常识猜测字段编码。"
                + RequestParameters.DATA_ID + " 为可选参数，如果模块支持" + Page.ACTION_CODE_PAGE + "操作，则必需指定，用于指定要更新哪条记录。");
        tools.put(Delete.ACTION_CODE_DELETE, "该接口删除指定模块中的一条业务数据。此操作通常不可恢复，执行前须向用户复述待删除的对象并取得确认。"
                + RequestParameters.DATA_ID + " 为必选参数，用于指定要删除哪条记录。");

        int addIndex = p.incrementAndGet();
        tools.forEach((actionCode, toolDescription) -> {
            Hashtable<String, String> properties = (Hashtable<String, String>) sharedProperties.clone();
            applyToolParameters(actionCode, addIndex, properties);
            properties.put(ToolService.PARENT_SKILL, InvokeActionSkill.TOOL_LABEL);
            properties.put(ToolService.TOOL_NAME, TOOL_NAME_PREFIX + actionCode);
            properties.put(ToolService.TOOL_DESCRIPTION, toolDescription);
            ToolService systemToolService = toolArgs -> RegisterActionTools.this.invokeActionTool(actionCode, toolArgs);
            registrations.add(bundleContext.registerService(ToolService.class, systemToolService, properties));
        });

        addToolInterceptor(tools.keySet());
    }

    @Deactivate
    public void destroy() {
        registrations.forEach(ServiceRegistration::unregister);
    }

    private void addToolInterceptor(Set<String> interceptActions) {
        registrations.add(bundleContext.registerService(ToolInterceptor.class, (toolName, toolArgs, c) -> {
            if (toolName.startsWith(TOOL_NAME_PREFIX)) {
                String actionCode = toolName.substring(TOOL_NAME_PREFIX.length());
                if (interceptActions.contains(actionCode)) {
                    String[] roles = c.getRoles();
                    AppStub appStub = retrieveAppStub(toolArgs);
                    String modelCode = (String) toolArgs.get(RequestParameters.MODEL_CODE);
                    // 参数缺失/应用不存在时 fail-closed：拒绝执行而非放行，防止绕过权限模型
                    if (appStub == null || modelCode == null) {
                        return "操作执行失败：无法定位目标应用或模块，请确认参数后重试。";
                    }

                    boolean allowed = PermissionChecker.isAllowed(appStub.getAppMeta().getApp(), modelCode, actionCode, roles);
                    if (allowed) return null;
                    else return "操作执行失败：当前身份缺少对应工具执行权限，请检查权限配置后重试。";
                }
            }
            return null;// 不拦截
        }, null));
    }

    private AppStub retrieveAppStub(Map<String, Object> toolArgs) {
        if (toolArgs == null) return null;
        String instanceId = (String) toolArgs.get(RequestParameters.INSTANCE_ID);
        String appCode = (String) toolArgs.get(RequestParameters.APP_CODE);
        String modelCode = (String) toolArgs.get(RequestParameters.MODEL_CODE);
        if (instanceId == null || appCode == null || modelCode == null) return null;

        return registry.getAppStub(instanceId, appCode);
    }

    private String invokeActionTool(String actionCode, Map<String, Object> toolArgs) {
        AppStub appStub = retrieveAppStub(toolArgs);
        if (appStub == null) return null;

        App app = appStub.getAppMeta().getApp();
        String instanceId = (String) toolArgs.get(RequestParameters.INSTANCE_ID);
        String modelCode = (String) toolArgs.get(RequestParameters.MODEL_CODE);

        RequestImpl request = new RequestImpl();
        request.setInstance(instanceId);
        request.setApp(app.code);
        request.setModel(modelCode);
        request.setAction(actionCode);
        String dataId = (String) toolArgs.get(RequestParameters.DATA_ID);
        if (dataId != null) request.setId(dataId);

        String error = preInvoke(actionCode, toolArgs, request);
        if (error != null) return error;

        try {
            appStub.invokeApp(request);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            // 不得吞成默认的 success=true：AI 会据此误判业务已写入成功
            request.getResponse().success(false).message("操作执行失败，请查看服务端日志");
        }
        ResponseImpl response = request.getResponse();

        try {
            String result = json.toJson(response);
            String postInvokeMsg = postInvoke(actionCode, response);
            return postInvokeMsg == null ? result : postInvokeMsg + "\n" + result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private void applyToolParameters(String actionCode, int index, Hashtable<String, String> properties) {
        if (Page.ACTION_CODE_PAGE.equals(actionCode)) {
            properties.put(ToolService.PARAMETER_NAME + "." + index, Constants.AI_REQUEST_PARAMETER_PAGE_NUM);
            properties.put(ToolService.PARAMETER_DESCRIPTION + "." + index, "列表页码，从 1 开始，不传则为第 1 页。仅在用户指定要查看某一页时传入。");
            properties.put(ToolService.PARAMETER_REQUIRED + "." + index, "false");
        } else {
            if (isDataAction(actionCode)) {
                properties.put(ToolService.PARAMETER_NAME + "." + index, DATA);
                properties.put(ToolService.PARAMETER_DESCRIPTION + "." + index, "业务字段集合，JSON 对象字符串，键为字段编码、值为字符串，"
                        + "形如 {\"字段编码\":\"值\"}。字段编码与取值规则须先用获取模块详细信息的工具取得，不得凭猜测填写；"
                        + "只提交该操作允许的字段，多值字段用分隔符拼成一个字符串。");
                properties.put(ToolService.PARAMETER_REQUIRED + "." + index, "true");
            }
        }
    }

    // 返回错误信息，null 表示无错误
    private String preInvoke(String actionCode, Map<String, Object> toolArgs, RequestImpl request) {
        if (Page.ACTION_CODE_PAGE.equals(actionCode)) {
            Object pageNum = toolArgs.get(Constants.AI_REQUEST_PARAMETER_PAGE_NUM);
            if (pageNum == null) return null;

            String value = pageNum instanceof Number ? String.valueOf(((Number) pageNum).intValue()) : pageNum.toString().trim();
            request.getParameters().put(Constants.WEB_REQUEST_PARAMETER_PAGE_NUM, value);
        } else {
            if (isDataAction(actionCode)) {
                Map<String, Object> data;
                try {
                    data = json.fromJson((String) toolArgs.get(DATA), Map.class);
                } catch (Exception e) {
                    return DATA + " 须为 JSON 对象字符串，形如 {\"字段编码\":\"值\"}";
                }
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    request.getParameters().put(entry.getKey(), stringify(entry.getValue(), ","));
                }
            }
        }
        return null;
    }

    // 多值字段（multiselect、sortable 等）用分隔符拼接，模型常常直接给数组
    private static String stringify(Object value, String separator) {
        if (value == null) return "";
        if (!(value instanceof Collection)) return String.valueOf(value);

        String sep = separator == null || separator.isEmpty() ? "," : separator;
        StringBuilder joined = new StringBuilder();
        for (Object item : (Collection<?>) value) {
            if (joined.length() > 0) joined.append(sep);
            joined.append(item);
        }
        return joined.toString();
    }

    private boolean isDataAction(String actionCode) {
        return Add.ACTION_CODE_ADD.equals(actionCode) || Update.ACTION_CODE_UPDATE.equals(actionCode);
    }

    // 返回附加信息
    private String postInvoke(String actionCode, ResponseImpl response) {
        if (Page.ACTION_CODE_PAGE.equals(actionCode)) {
            return buildPagingHint(response);
        }
        return null;
    }

    private static String buildPagingHint(ResponseImpl response) {
        if (!response.isSuccess() || !(response.getData() instanceof Map)) return null;

        Map<?, ?> data = (Map<?, ?>) response.getData();
        Object pageNum = data.get(Constants.WEB_RESPONSE_PARAMETER_PAGE_NUM);
        Object pageSize = data.get(Constants.WEB_RESPONSE_PARAMETER_PAGE_SIZE);
        Object totalSize = data.get(Constants.WEB_RESPONSE_PARAMETER_TOTAL_SIZE);
        if (!(pageNum instanceof Number && pageSize instanceof Number && totalSize instanceof Number)) return null;

        return pagingHint(((Number) pageNum).intValue(), ((Number) pageSize).intValue(), ((Number) totalSize).intValue());
    }

    // 提示须尽量简短：它排在返回内容最前面，每多一个字符，末尾就少显示一个字符的数据；固定的翻页规则写在工具描述里
    private static String pagingHint(int pageNum, int pageSize, int totalSize) {
        if (pageSize <= 0 || totalSize <= 0) return null;

        int totalPages = (totalSize + pageSize - 1) / pageSize;
        if (pageNum > totalPages) {
            return String.format("【分页提示】请求的第 %d 页超出范围，有效页码为 1～%d（共 %d 条），本页无数据。", pageNum, totalPages, totalSize);
        }
        if (totalPages == 1) return null;

        String ending = pageNum == totalPages ? "已是最后一页。" : "用户指定页码前不要查询其他页。";
        return String.format("【分页提示】本次为第 %d 页，共 %d 页（每页 %d 条，共 %d 条），%s", pageNum, totalPages, pageSize, totalSize, ending);
    }
}

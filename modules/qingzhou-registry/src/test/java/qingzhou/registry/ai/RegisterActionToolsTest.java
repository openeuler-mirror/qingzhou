package qingzhou.registry.ai;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;
import qingzhou.dto.Constants;
import qingzhou.dto.RequestImpl;
import qingzhou.dto.ResponseImpl;

import static org.testng.Assert.*;

public class RegisterActionToolsTest {
    @Test
    public void firstOfThreePages_pagingHint_containsPageAndTotal() throws Exception {
        String hint = pagingHint(1, 10, 25);
        assertTrue(hint.contains("第 1 页，共 3 页"));
        assertTrue(hint.contains("用户指定页码前不要查询其他页"));
    }

    @Test
    public void lastPage_pagingHint_markedAsLastPage() throws Exception {
        assertTrue(pagingHint(3, 10, 25).contains("已是最后一页"));
    }

    @Test
    public void pageOutOfRange_pagingHint_reportsValidRange() throws Exception {
        String hint = pagingHint(5, 10, 25);
        assertTrue(hint.contains("超出范围"));
        assertTrue(hint.contains("1～3"));
    }

    @Test
    public void singlePageOutOfRange_pagingHint_reportsValidRange() throws Exception {
        assertTrue(pagingHint(2, 10, 3).contains("1～1"));
    }

    @Test
    public void singlePage_pagingHint_returnsNull() throws Exception {
        assertNull(pagingHint(1, 10, 3));
    }

    @Test
    public void unpagedModel_pagingHint_returnsNull() throws Exception {
        assertNull(pagingHint(1, 10, -1));
    }

    @Test
    public void exactMultiple_pagingHint_totalPagesCorrect() throws Exception {
        assertTrue(pagingHint(1, 10, 20).contains("共 2 页"));
    }

    // 提示排在返回内容最前面，会挤占工具返回的截断额度（2000 字符），因此必须保持简短
    @Test
    public void largeNumbers_pagingHint_staysShort() throws Exception {
        assertTrue(pagingHint(99999, 100, 9999999).length() <= 80);
    }

    @Test
    public void stringPageNum_applyPageNum_passedToRequest() throws Exception {
        assertEquals(applyPageNum("2"), "2");
    }

    @Test
    public void integerPageNum_applyPageNum_passedToRequest() throws Exception {
        assertEquals(applyPageNum(2), "2");
    }

    @Test
    public void decimalPageNum_applyPageNum_convertedToInteger() throws Exception {
        assertEquals(applyPageNum(2.0), "2");
    }

    @Test
    public void noPageNum_applyPageNum_parameterNotSet() throws Exception {
        assertNull(applyPageNum(null));
    }

    @Test
    public void pagedResponse_buildPagingHint_startsWithMarker() throws Exception {
        assertTrue(buildPagingHint(pagedResponse(1, 10, 25)).startsWith("【分页提示】"));
    }

    @Test
    public void longNumbersInResponse_buildPagingHint_hintBuilt() throws Exception {
        assertNotNull(buildPagingHint(pagedResponse(1L, 10L, 25L)));
    }

    @Test
    public void appProvidedData_buildPagingHint_returnsNull() throws Exception {
        ResponseImpl response = new ResponseImpl();
        response.data("text");
        assertNull(buildPagingHint(response));
    }

    @Test
    public void existingMessage_buildPagingHint_messageUnchanged() throws Exception {
        ResponseImpl response = pagedResponse(1, 10, 25);
        response.message("原消息");
        assertNotNull(buildPagingHint(response));
        assertEquals(response.getMsg(), "原消息");
    }

    @Test
    public void failedResponse_buildPagingHint_returnsNull() throws Exception {
        ResponseImpl response = pagedResponse(1, 10, 25);
        response.error("执行失败");
        assertNull(buildPagingHint(response));
        assertEquals(response.getMsg(), "执行失败");
    }

    private static String pagingHint(int pageNum, int pageSize, int totalSize) throws Exception {
        return (String) invokeStatic("pagingHint", new Class<?>[]{int.class, int.class, int.class}, pageNum, pageSize, totalSize);
    }

    private static String applyPageNum(Object pageNum) throws Exception {
        RequestImpl request = new RequestImpl();
        invokeStatic("applyPageNum", new Class<?>[]{RequestImpl.class, Object.class}, request, pageNum);
        return request.getParameter("pageNum");
    }

    private static String buildPagingHint(ResponseImpl response) throws Exception {
        return (String) invokeStatic("buildPagingHint", new Class<?>[]{ResponseImpl.class}, response);
    }

    private static ResponseImpl pagedResponse(Object pageNum, Object pageSize, Object totalSize) {
        Map<String, Object> data = new HashMap<>();
        data.put(Constants.REQUEST_PARAMETER_NAME_PAGE_NUM, pageNum);
        data.put(Constants.REQUEST_PARAMETER_NAME_PAGE_SIZE, pageSize);
        data.put(Constants.REQUEST_PARAMETER_NAME_TOTAL_SIZE, totalSize);
        ResponseImpl response = new ResponseImpl();
        response.data(data);
        return response;
    }

    private static Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = RegisterActionTools.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}

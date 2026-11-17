/**
 * 内置动作的能力接口。
 * <p>
 * 模块实现某个接口，构建期平台即按方法签名为其生成对应动作
 * （{@code page}、{@code add}、{@code update}、{@code show}、{@code delete}、{@code monitor}、
 * {@code file}+{@code download}、{@code namespace}+{@code current}），运行期反射回调；不实现就没有该动作，
 * 调用返回 404。因此"实现接口"即"暴露动作"，参数收集、字段裁剪、按钮与权限元数据全部由框架承担，
 * 应用只需关心数据本身。
 */
package qingzhou.api.action;

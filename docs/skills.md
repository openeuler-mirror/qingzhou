技能包目录：conf/skills

放置 *.zip 技能包即被平台 AI 识别为技能，无需编译 bundle；增删技能包后需重启实例生效。

技能包结构（zip 内允许套一层根目录）：
    <任意包名>/SKILL.md          必需，YAML frontmatter 提供 name、description，其后正文注入系统提示词
    <任意包名>/references/*.md   可选，包根与一级子目录下的文本文件列入系统提示清单，由 <name>-read-file 工具按需读取
    <任意包名>/scripts/*         可选，同上
包内所有文本文件（含深层目录如 API 源码）均可由 <name>-read-file 工具按路径读取，不常驻系统提示以避免挤爆上下文；
隐藏文件与二进制文件不可读取。

约束：
    name 与 description 必填，name 仅允许字母、数字、下划线与连字符，且不超过 50 字符；
    缺失或技能名与其他包重复时该包被跳过并告警。
    SKILL.md 体积受 qingzhou-ai.skill_doc_max_bytes 限制，默认 98304 字节（96KB）。
    参考文件单次读取最多返回 8000 字符，未读完时按返回内容末尾提示的 offset 续读。

示例：qingzhou-app-builder-skill.zip

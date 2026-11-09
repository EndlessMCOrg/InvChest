# InvChest

> 本项目中包含AI生成内容，请不要将其二次投入AI训练！

- 项目地址：https://github.com/EndlessMCOrg/InvChest/
- 作者：OutHimic、EndlessMCOrg

一个为 Bukkit/Spigot/Paper/Purpur/Folia 服务器设计的插件，允许玩家绑定容器并将其中物品按策略自动传输到玩家背包。支持多语言与可配置的传输策略，并为后续拓展 Sponge、(Neo)Forge、Fabric 预留架构。

## Overview (English)
InvChest is a Minecraft server plugin for Bukkit-family cores that lets players bind a container (chest, trapped chest, barrel, shulker box) and automatically transfer items into their inventory at an interval. It includes language localization, permissions, configurable strategies, and initial Folia-friendly scheduling approach. Architecture is prepared to extend to Sponge and modded cores later.

## 安装
- 将打包生成的 `InvChest.jar` 放入 `plugins/` 目录
- 首次启动后会生成 `config.yml` 与 `lang/` 目录（内置 17 种语言，默认 `en_us` 与 `zh_cn`）
- 如需修改语言，在 `config.yml` 中设置 `lang`

## 构建
- JDK：8 或更高版本（目标字节码为 Java 8）
- 构建工具：Maven
- 执行 `mvn -DskipTests clean package` 生成插件
- 默认版本号由 `pom.xml` 中的 `revision` 属性控制；构建时可覆盖，例如 `mvn -Drevision=1.9.1 clean package`

## 兼容性
- 支持核心：Bukkit、Spigot、Paper、Folia（支持）、Purpur
- 目标版本：Minecraft 1.12+
- 说明：本版本优先实现 Bukkit/Paper 系，Sponge 将在后续以独立适配层接入，同一 `.jar` 兼容将尽量保证

## 命令
- `/invchest bind [名称] [区块范围]` 或 `/ic bind ...`：绑定玩家当前所看的容器
- `/invchest unbind [名称]` 或 `/ic unbind ...`：解绑；不填名称时解绑当前所看的容器
- `/invchest list [页码]` 或 `/ic list [页码]`：查看自己的绑定列表
- `/invchest lock <名称> <true|false>` 或 `/ic lock ...`：设置其他玩家能否打开该容器
- `/invchest start|stop [名称]` 或 `/ic start|stop ...`：开始/暂停自己的传输；不填名称时作用于全部
- 管理员：
  - `/invchest abind <玩家> [名称] [区块范围]` 或 `/ic abind ...`：为指定玩家绑定（管理员需面向目标容器）
  - `/invchest aunbind <玩家> [名称]` 或 `/ic aunbind ...`：为指定玩家解绑
  - `/invchest alist [玩家|@all] [页码]` 或 `/ic alist ...`：查看指定玩家或全服绑定
  - `/invchest astart|astop <玩家|@all> [名称]` 或 `/ic astart|astop ...`：开始/暂停指定玩家或全服传输
  - `/invchest reload` 或 `/ic reload`：重载配置

## 权限
- `invchest.bind`：绑定/解绑/查看自己的容器（默认开启）
- `invchest.lock`：修改自己容器的访问锁定状态（默认开启）
- `invchest.statue`：开始/暂停自己的传输（默认开启）
- `invchest.abind`：管理员为他人绑定/解绑/查看（默认 OP）
- `invchest.astatue`：管理员为他人或全局开始/暂停传输（默认 OP）
- `invchest.inf`：在保护提示中显示容器所有者（默认 OP）
- `invchest.reload`：重载配置（默认 OP）

## 配置（`config.yml`）
- `transfer-interval`：传输间隔（秒），默认 `30`
- `transfer-mode`：`all` 或 `try`
  - `all`：尽量将容器物品移入玩家背包；背包放不下的部分掉落在玩家位置；达到单次上限后剩余物品保留到下次
  - `try`：按顺序尽量移入；遇到无法容纳的物品时停止；容器保留剩余物品
- `max-items-per-transfer`：单次传输的物品数量上限（按堆叠数量累加，`all` 与 `try` 模式均生效）
- `lang`：如 `zh_cn`、`en_us` 或 `auto_en_us`（控制台固定指定语言，玩家根据客户端语言自动匹配）
- `bind.max-per-player`：每位玩家最多可绑定的容器数量（`0` 为不限制）
- `bind.default-chunk-range`：默认区块常加载范围（`-1` 不常加载，`0` 仅所在区块，`>0` 半径）
- `bind.auto-name-format`：自动命名格式，`{number}` 会替换为序号
- `chunk-loading.enabled`：是否启用区块常加载
- `chunk-loading.max-range`：允许的最大区块加载范围
- `ui.list-items-per-page`：列表命令每页显示条数
- `transfer-message-enabled`：是否在发生传输时发送提示

## 容器与保护
- 支持容器：箱子、陷阱箱、桶、潜影盒（双箱/双陷阱箱会作为整体处理；不支持铜质容器）
- 绑定后：
  - 任何玩家打开该容器将被阻止
  - 破坏受保护容器仅允许绑定者或具有 `invchest.abind` 的玩家
  - 允许外部输入（如漏斗、投掷器），禁止外部输出（如漏斗、漏斗矿车）
  - 容器被破坏或不存在时自动解绑

## 多语言
- 资源目录：`src/main/resources/lang/`
- 运行时目录：`plugins/InvChest/lang/`
- 默认提供：`zh_cn.yml`、`zh_tw.yml`、`zh_hk.yml`、`en_us.yml`、`en_gb.yml`、`en_au.yml`、`ja_jp.yml`、`ko_kr.yml`、`ru_ru.yml`、`kk_kz.yml`、`mn_mn.yml`、`de_de.yml`、`fr_fr.yml`、`it_it.yml`、`es_es.yml`、`pt_pt.yml`、`ar_sa.yml`
- 新增语言：将 `<code>.yml` 放入资源目录后构建即可
- 自动模式：`lang: auto_<code>` 时，控制台按 `<code>` 输出，玩家依据客户端语言匹配，不存在则回退到 `<code>`

## 架构与性能
- 模块化：核心逻辑、命令、监听、配置、存储分包
- 事件：保护、解绑、传输可通过内部事件管线扩展（后续开放）
- 性能：
  - 传输调度尽量保持主线程安全
  - 分页处理与上限控制，避免一次性搬运过大
  - 容器位置缓存，减少重复计算
- 指标：单次传输尽量控制在 50ms 内；基础运行内存小于 50MB；可同时处理 100+ 容器绑定

## 安全
- 验证容器所有权、阻止外部提取，降低复制风险
- 权限与命令参数严格校验

## API（预留）
- 后续将提供事件与接口（绑定、解绑、传输）供其他插件集成

## 变更日志
- `1.9`：包名迁移至 `org.endlessmc.invchest`，更新作者与项目地址，补充 MIT 许可证，完善 `.gitignore`，清理死代码并修复区块加载与传输上限等问题
- `0.1.0` 初始版本：绑定/解绑、传输策略、容器保护、多语言与配置

## License
本项目使用 MIT 许可证，详见 [LICENSE](LICENSE)。

Copyright (c) 2024-2026 EndlessMC Organization

## 致谢 Thanks
- Bukkit/Spigot/Paper 社区
- Purpur/Folia 项目
- Trae 提供的几乎*完美*的IDEA
- CloseAI的GPT-5-high
- pama1234的经济支持
